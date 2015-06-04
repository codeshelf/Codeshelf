package com.codeshelf.edi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Date;
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
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.ExtensionPointService;
import com.codeshelf.util.DateTimeParser;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.validation.BatchResult;
import com.google.inject.Inject;

public class OutboundOrderPrefetchCsvImporter extends CsvImporter<OutboundOrderCsvBean> implements ICsvOrderImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutboundOrderPrefetchCsvImporter.class);

	@Getter @Setter
	int maxRetries = 10;
	
	DateTimeParser					mDateTimeParser;

	@Getter @Setter
	private Boolean					locaPick			= null;

	@Getter @Setter
	private Boolean					scanPick			= null;

	@Getter
	@Setter
	private String					oldPreferredLocation	= null;													// for DEV-596
	
	long startTime;
	long endTime;
	
	@Getter @Setter
	int maxOrderLines = 500;
	
	@Getter
	private ConcurrentLinkedQueue<OutboundOrderBatch> batchQueue = new ConcurrentLinkedQueue<OutboundOrderBatch>();
	
	@Getter @Setter
	int numWorkerThreads = 1;
	
	@Getter
	ExtensionPointService extensionPointService = null;

	@Inject
	public OutboundOrderPrefetchCsvImporter(final EventProducer inProducer) {
		super(inProducer);
		mDateTimeParser = new DateTimeParser();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.edi.ICsvImporter#importOrdersFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
	 */
	public final BatchResult<Object> importOrdersFromCsvStream(Reader inCsvReader, Facility facility, Timestamp inProcessTime) {
		
		this.startTime = System.currentTimeMillis();
		
		// make sure the facility is up-to-date
		facility = facility.reload();
		
		// initialize scripting service
		try {
			extensionPointService = ExtensionPointService.createInstance();
			extensionPointService.load(facility);
		} 
		catch (Exception e) {
			LOGGER.error("Failed to initialize extension point service", e);
		}
		
		BatchResult<Object> batchResult = new BatchResult<Object>();
		
		// Get our LOCAPICK and SCANPICK configuration values. It will not change during importing one file.
		this.locaPick = PropertyService.getInstance().getBooleanPropertyFromConfig(facility, DomainObjectProperty.LOCAPICK);
		
		this.scanPick = false;
		DomainObjectProperty scanPickProp = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.SCANPICK);
		if (scanPickProp!=null) {
			if (!DomainObjectProperty.Default_SCANPICK.equals(scanPickProp.getValue())) {
				this.scanPick = true;				
			}
		}
		
		// transform order lines/header, if extension point is defined
		if (extensionPointService.hasExtensionPoint(ExtensionPointType.OrderImportLineTransformation) 
				|| extensionPointService.hasExtensionPoint(ExtensionPointType.OrderImportHeaderTransformation)) {
			BufferedReader br = new BufferedReader(inCsvReader);
			StringBuffer buffer = new StringBuffer();
			// process file header
			if (getExtensionPointService().hasExtensionPoint(ExtensionPointType.OrderImportHeaderTransformation)) {
				LOGGER.info("Order import header transformation is enabled");
				try {
					String header = br.readLine();
					Object[] params = { header };
					String transformedHeader = (String) getExtensionPointService().eval(ExtensionPointType.OrderImportHeaderTransformation, params);
					buffer.append(transformedHeader);
				}
				catch (Exception e) {
					LOGGER.error("Failed to transform order file header",e);
					return batchResult;
				}
			}
			else {
				try {
					// use header as-is
					String header = br.readLine();
					buffer.append(header);
				}
				catch (Exception e) {
					LOGGER.error("Failed to read order file header",e);
					return batchResult;
				}				
			}
			// process file body
			if (getExtensionPointService().hasExtensionPoint(ExtensionPointType.OrderImportLineTransformation)) {
				LOGGER.info("Order import line transformation is enabled");
				// transform order lines
				try {
					String line = null;
				    while ((line = br.readLine()) != null) {
						Object[] params = { line };
						String transformedLine = (String) getExtensionPointService().eval(ExtensionPointType.OrderImportLineTransformation, params);
						buffer.append("\r\n"+transformedLine);
				    }
				}
				catch (Exception e) {
					LOGGER.error("Failed to read order file line",e);
					return batchResult;
				}				
			}
			else {
				// use order lines as-is
				try {
					String line = null;
				    while ((line = br.readLine()) != null) {
				    	buffer.append("\r\n"+line);
				    }
				}
				catch (Exception e) {
					LOGGER.error("Failed to read order file line",e);
					return batchResult;
				}				
			}
			try {
				br.close();
			} 
			catch (IOException e) {
				LOGGER.warn("Failed to close order input stream", e);
			}
			// swap out reader
			inCsvReader = new StringReader(buffer.toString());
		}

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
		
		batchResult.setReceived(new Date(startTime));
		batchResult.setStarted(new Date(startTime));
		batchResult.setCompleted(new Date(endTime));
		batchResult.setOrdersProcessed(numOrders);
		batchResult.setLinesProcessed(numLineItems);
		return batchResult;
	}
	
	@Override
	public void persistDataReceipt(Facility facility, String username, String filename, long received, BatchResult<?> results) {

		DataImportReceipt receipt = new DataImportReceipt();
		receipt.setUsername(username);
		receipt.setFilename(filename);
		receipt.setReceived(new Date(received));
		receipt.setStarted(results.getStarted());
		receipt.setCompleted(results.getCompleted());
		receipt.setOrdersProcessed(results.getOrdersProcessed());
		receipt.setLinesProcessed(results.getLinesProcessed());
		
		receipt.setParent(facility);
		receipt.setDomainId("Import-"+results.getStarted());
		if (results.isSuccessful()) {
			receipt.setStatus(DataImportStatus.Completed);
			LOGGER.info("Imported " + receipt);
		}
		else {
			receipt.setStatus(DataImportStatus.Failed);
			LOGGER.error("Failed to import " + receipt);
		}

		DataImportReceipt.staticGetDao().store(receipt);

	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_OUTBOUND);
	}

}
