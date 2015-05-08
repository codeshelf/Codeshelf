package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.domain.DataImportReceipt;
import com.codeshelf.model.domain.DataImportStatus;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.service.PropertyService;
import com.codeshelf.util.DateTimeParser;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.validation.BatchResult;
import com.google.inject.Inject;

public class OutboundOrderPrefetchCsvImporter extends CsvImporter<OutboundOrderCsvBean> implements ICsvOrderImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutboundOrderPrefetchCsvImporter.class);

	@Getter @Setter
	int maxRetries = 10;
	
	DateTimeParser					mDateTimeParser;

	@Getter
	@Setter
	private Boolean					locapickValue			= null;

	@Getter
	@Setter
	private String					oldPreferredLocation	= null;													// for DEV-596
	
	long startTime;
	long endTime;
	int numOrders; 
	int numLines;
	
	@Getter @Setter
	int maxOrderLines = 500;
	
	@Getter
	private ConcurrentLinkedQueue<OutboundOrderBatch> batchQueue = new ConcurrentLinkedQueue<OutboundOrderBatch>();
	
	@Getter @Setter
	int numWorkerThreads = 1;

	@Inject
	public OutboundOrderPrefetchCsvImporter(final EventProducer inProducer) {
		super(inProducer);
		mDateTimeParser = new DateTimeParser();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.edi.ICsvImporter#importOrdersFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
	 */
	public final BatchResult<Object> importOrdersFromCsvStream(final Reader inCsvReader, Facility facility,
		Timestamp inProcessTime) {
		
		this.startTime = System.currentTimeMillis();
		
		// make sure the facility is up-to-date
		facility = facility.reload();
		
		// Get our LOCAPICK configuration value. It will not change during importing one file.
		boolean locapickValue = PropertyService.getInstance().getBooleanPropertyFromConfig(facility, DomainObjectProperty.LOCAPICK);
		setLocapickValue(locapickValue);
		int numOrders = 0;
		int numLineItems = 0; 
		
		List<OutboundOrderCsvBean> list = toCsvBean(inCsvReader, OutboundOrderCsvBean.class);
		if (list.size()==0) {
			LOGGER.info("Nothing to process.  Order file is empty.");
			return null;
		}

		// instead of processing all order line items in one transaction,
		// chunk it up into small batches and write it to a queue.
		// orders from one file are always be contained by one batch.
		
		// create list of order batches
		Set<String> orderIds = new HashSet<String>();
		Set<String> orderGroupIds = new HashSet<String>();
		Map<String,OutboundOrderBatch> orderBatches = new HashMap<String, OutboundOrderBatch>();
		for (OutboundOrderCsvBean orderBean : list) {
			String orderId = orderBean.orderId;
			orderIds.add(orderId);
			String orderGroupId = orderBean.getOrderGroupId();
			if (orderGroupId!=null) {
				orderGroupIds.add(orderGroupId);		
			}
			OutboundOrderBatch batch = orderBatches.get(orderId);
			if (batch==null) {
				batch = new OutboundOrderBatch(0);
				orderBatches.put(orderId, batch);
			}
			batch.add(orderBean);
			numLineItems++;
		}
		numOrders = orderBatches.size();
		
		// add all existing orders from specified order groups, so they can be retired if needed

		if (orderGroupIds.size()>0) {
			LOGGER.info("Adding orders for "+orderGroupIds.size()+" order groups");
			for (String orderGroupId : orderGroupIds) {
				OrderGroup og = facility.getOrderGroup(orderGroupId);
				if (og!=null) {
					// add order headers to batch
					for (OrderHeader order : og.getOrderHeaders()) {
						String orderId = order.getOrderId();
						if (!orderIds.contains(orderId)) {
							// deactivate order and line items, since not included in order group
							LOGGER.info("Deactivating order not included in group: "+order);
							order.setActive(false);
							OrderHeader.staticGetDao().store(order);
							List<OrderDetail> details = order.getOrderDetails();
							for (OrderDetail detail : details) {
								detail.setActive(false);
								OrderDetail.staticGetDao().store(detail);
							}
						}
					}
				}
			}
		}
		
		// load order queue
		int numBatches = 1;
		OutboundOrderBatch combinedBatch = new OutboundOrderBatch(numBatches);
		for (String orderId : orderBatches.keySet()) {
			OutboundOrderBatch batch = orderBatches.get(orderId);
			combinedBatch.add(batch);
			if (combinedBatch.size()>maxOrderLines) {
				// queue up batch
				this.batchQueue.add(combinedBatch);
				// and create a new one
				numBatches++;
				combinedBatch = new OutboundOrderBatch(numBatches);
			}
		}
		if (combinedBatch.size()>0) {
			// add remaining left-over batch to queue
			this.batchQueue.add(combinedBatch);		
		}
		else {
			// adjust count
			numBatches--;
		}
		LOGGER.info("Order file chunked into "+numBatches+" batches.");
				
		// spawn workers and process order batch queue
        ExecutorService executor = Executors.newFixedThreadPool(numWorkerThreads);
        for (int i = 1; i <= numWorkerThreads; i++) {
        	Runnable worker = new OutboundOrderBatchProcessor(i, this, inProcessTime, facility);
        	executor.execute(worker);
        }
        
        // wait until all threads are done
        executor.shutdown();
        while (!executor.isTerminated()) {
        	ThreadUtils.sleep(100);
        }

		this.endTime = System.currentTimeMillis();
		
		DataImportReceipt receipt = new DataImportReceipt();
		receipt.setReceived(startTime);
		receipt.setStarted(startTime);
		receipt.setCompleted(endTime);
		receipt.setOrdersProcessed(numOrders);
		receipt.setLinesProcessed(numLines);
		receipt.setParent(facility);
		receipt.setDomainId("Import-"+startTime);
		boolean success = true;

		if (success) {
			receipt.setStatus(DataImportStatus.Completed);
			LOGGER.info("Imported "+numOrders+" orders and "+numLineItems+" lines in "+(endTime-startTime)/1000+" seconds");
		}
		else {
			receipt.setStatus(DataImportStatus.Failed);
			LOGGER.error("Failed to import "+numOrders+" orders and "+numLineItems+" lines in "+(endTime-startTime)/1000+" seconds");
		}
		DataImportReceipt.staticGetDao().store(receipt);
		BatchResult<Object> batchResult = new BatchResult<Object>();
		return batchResult;
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_OUTBOUND);
	}
}
