package com.codeshelf.edi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.domain.ImportReceipt;
import com.codeshelf.model.domain.ImportStatus;
import com.codeshelf.model.EdiTransportType;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.ReplenishItem;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.service.ExtensionPointEngine;
import com.codeshelf.util.DateTimeParser;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.validation.BatchResult;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class OutboundOrderPrefetchCsvImporter extends CsvImporter<OutboundOrderCsvBean> implements ICsvOrderImporter {

	private static final Logger							LOGGER					= LoggerFactory.getLogger(OutboundOrderPrefetchCsvImporter.class);

	@Getter
	@Setter
	int													maxRetries				= 10;

	DateTimeParser										mDateTimeParser;

	@Getter
	@Setter
	private Boolean										scanPick				= null;

	@Getter
	@Setter
	private String										oldPreferredLocation	= null;															// for DEV-596

	long												startTime;
	long												endTime;
	long												spentDoingExtensionsMs;

	@Getter
	@Setter
	int													maxOrderLines			= 500;

	@Getter
	private ConcurrentLinkedQueue<OutboundOrderBatch>	batchQueue				= new ConcurrentLinkedQueue<OutboundOrderBatch>();

	@Getter
	@Setter
	int													numWorkerThreads		= 1;

	@Getter
	ExtensionPointEngine								extensionPointService	= null;

	@Setter
	@Getter
	boolean												truncatedGtins			= false;

	//Used for testing purpose to simulate errors in the OrderDeletion thread
	@Setter
	@Getter
	boolean												makeOldOrderDeletionFail= false;

	@Inject
	public OutboundOrderPrefetchCsvImporter(final EventProducer inProducer) {
		super(inProducer);
		mDateTimeParser = new DateTimeParser();
	}
	
	public void makeOrderDeletionFail(boolean fail){
		this.makeOldOrderDeletionFail = fail;
	}

	/** --------------------------------------------------------------------------
	 * A simple, throw-safe mechanism to increment this time metric
	 * This has package visibility as it it called from OutboundOrderBatchProcesser also
	 */
	void addToExtensionMsFromTimeBefore(long inTimeStampBeforeExtension) {
		spentDoingExtensionsMs += System.currentTimeMillis() - inTimeStampBeforeExtension;
	}

	private void initImporter(Facility facility, BatchResult<Object> results){
		try {
			long timeBeforeExtension = System.currentTimeMillis();
			extensionPointService = ExtensionPointEngine.getInstance(facility);
			addToExtensionMsFromTimeBefore(timeBeforeExtension);
		} catch (Exception e) {
			LOGGER.error("Failed to initialize extension point service", e);
		}
		List<String> failedExtensions = extensionPointService.getFailedExtensions();
		for (String e : failedExtensions) {
			results.addViolation("ExtensionService", null, "Extension failed to load and was deactivated: " + e);
		}
		// Get our SCANPICK configuration value. It will not change during importing one file.
		this.scanPick = false;
		String scanPickProp = PropertyBehavior.getProperty(facility, FacilityPropertyType.SCANPICK);
		String defaultScanpick = FacilityPropertyType.SCANPICK.getDefaultValue();
		if (!defaultScanpick.equals(scanPickProp)) {
			this.scanPick = true;
		}
	}
	
	// --------------------------------------------------------------------------
	public final BatchResult<Object> importOrdersFromCsvStream(Reader inCsvReader, Facility facility, Timestamp inProcessTime) {
		return importOrdersFromCsvStream(inCsvReader, facility, inProcessTime, false);
	}
	
	public final BatchResult<Object> importOrdersFromCsvStream(Reader inCsvReader, Facility facility, Timestamp inProcessTime, boolean deleteOldOrders) {

		this.startTime = System.currentTimeMillis();
		this.spentDoingExtensionsMs = 0;
		BatchResult<Object> batchResultErrors = new BatchResult<Object>();
		batchResultErrors.setReceived(new Date(startTime));
		batchResultErrors.setCompleted(new Date(startTime));

		// make sure the facility is up-to-date
		facility = facility.reload();

		initImporter(facility, batchResultErrors);
		
		// DEV-978 extension point can fully supply the header.  However, that should not be done with OrderImportHeaderTransformation
		// Note: this works by passing in a good java string that is empty, and getting back a non-empty string.
		String extensionSuppliedHeader = "";
		if (extensionPointService.hasActiveExtensionPoint(ExtensionPointType.OrderImportCreateHeader)) {
			if (extensionPointService.hasActiveExtensionPoint(ExtensionPointType.OrderImportHeaderTransformation)) {
				LOGGER.warn("OrderImportCreateHeader extension ignored because OrderImportHeaderTransformation is also on. Please turn one off.");
				// Want an EDI notify event here?
			} else {
				try {
					long timeBeforeExtension = System.currentTimeMillis();

					Object[] params = { extensionSuppliedHeader };
					extensionSuppliedHeader = (String) getExtensionPointService().eval(ExtensionPointType.OrderImportCreateHeader,
						params);
					if (extensionSuppliedHeader.isEmpty()) {
						// we called the header and nothing got done. As the extension was in effect, we must assume the file itself has no header.
						LOGGER.warn("OrderImportCreateHeader in effect, but did not produce a header. Skipping this orders file.");
						return batchResultErrors;
					}

					addToExtensionMsFromTimeBefore(timeBeforeExtension);
				} catch (Exception e) {
					LOGGER.error("Extension failed to supply order file header", e);
					batchResultErrors.addViolation("OrderImportCreateHeader Script", null, e.getMessage());
					return batchResultErrors;
				}
			}
		}

		// transform order lines/header, if extension point is defined
		if (extensionPointService.hasActiveExtensionPoint(ExtensionPointType.OrderImportLineTransformation)
				|| extensionPointService.hasActiveExtensionPoint(ExtensionPointType.OrderImportHeaderTransformation)
				|| extensionPointService.hasActiveExtensionPoint(ExtensionPointType.OrderImportCreateHeader)) {
			BufferedReader br = new BufferedReader(inCsvReader);
			StringBuffer buffer = new StringBuffer();
			// process file header
			if (getExtensionPointService().hasActiveExtensionPoint(ExtensionPointType.OrderImportHeaderTransformation)) {
				LOGGER.info("Order import header transformation is enabled");
				try {
					String header = br.readLine();
					long timeBeforeExtension = System.currentTimeMillis();

					Object[] params = { header };
					String transformedHeader = (String) getExtensionPointService().eval(ExtensionPointType.OrderImportHeaderTransformation,
						params);
					addToExtensionMsFromTimeBefore(timeBeforeExtension);

					buffer.append(transformedHeader);
				} catch (Exception e) {
					LOGGER.error("Failed to transform order file header", e);
					batchResultErrors.addViolation("OrderImportHeaderTransformation Script", null, e.getMessage());
					return batchResultErrors;
				}
			} else {
				try {
					// use header as-is
					String header;
					if (!extensionSuppliedHeader.isEmpty()) {
						header = extensionSuppliedHeader;
					} else {
						header = br.readLine();
					}
					buffer.append(header);
				} catch (Exception e) {
					LOGGER.error("Failed to read order file header", e);
					return batchResultErrors;
				}
			}
			// process file body
			if (getExtensionPointService().hasActiveExtensionPoint(ExtensionPointType.OrderImportLineTransformation)) {
				try {
					String line = null;
					while ((line = br.readLine()) != null) {
						long timeBeforeExtension = System.currentTimeMillis();

						Object[] params = { line };
						String transformedLine = (String) getExtensionPointService().eval(ExtensionPointType.OrderImportLineTransformation,
							params);

						addToExtensionMsFromTimeBefore(timeBeforeExtension);
						buffer.append("\r\n" + transformedLine);
					}
				} catch (Exception e) {
					LOGGER.error("Exception during OrderImportLineTransformation", e);
					batchResultErrors.addViolation("OrderImportLineTransformation Script", null, e.getMessage());
					return batchResultErrors;
				}
			} else {
				// use order lines as-is
				try {
					String line = null;
					while ((line = br.readLine()) != null) {
						buffer.append("\r\n" + line);
					}
				} catch (Exception e) {
					LOGGER.error("Failed to read order file line", e);
					return batchResultErrors;
				}
			}
			try {
				br.close();
			} catch (IOException e) {
				LOGGER.warn("Failed to close order input stream", e);
			}
			// swap out reader
			inCsvReader = new StringReader(buffer.toString());
		}

		List<OutboundOrderCsvBean> originalBeanList = toCsvBean(inCsvReader, OutboundOrderCsvBean.class);
		BatchResult<Object> batchResult = importOrdersFromBeanList(originalBeanList, facility, inProcessTime, deleteOldOrders, true);
		return batchResult;
	}
	
	public final BatchResult<Object> importOrdersFromBeanList(List<OutboundOrderCsvBean> originalBeanList, Facility facility, Timestamp inProcessTime, boolean deleteOldOrders){
		return importOrdersFromBeanList(originalBeanList, facility, inProcessTime, deleteOldOrders, false);
	}
	
	private final BatchResult<Object> importOrdersFromBeanList(List<OutboundOrderCsvBean> originalBeanList, Facility facility, Timestamp inProcessTime, boolean deleteOldOrders, boolean alreadyInitialized){
		this.startTime = System.currentTimeMillis();
		BatchResult<Object> batchResult = new BatchResult<Object>();
		batchResult.setReceived(new Date(startTime));
		batchResult.setCompleted(new Date(startTime));

		int numOrders = 0;
		int numLineItems = 0;
		
		if (originalBeanList.size() == 0) {
			LOGGER.info("Nothing to process.  Order file is empty.");
			return null;
		}
		
		if (!alreadyInitialized){
			initImporter(facility, batchResult);
		}

		// From v20 DEV-1075
		// We need to run the order bean transforms before doing any caching or even assembling orderIds, gtins, etc.
		if (getExtensionPointService().hasActiveExtensionPoint(ExtensionPointType.OrderImportBeanTransformation)) {
			HashMap<String, String> orderImportBeanTransformationViolations = new HashMap<>();
			long timeBeforeExtension = System.currentTimeMillis();
			for (OutboundOrderCsvBean orderBean : originalBeanList) {
				// transform order bean with groovy script, if enabled
				if (getExtensionPointService().hasActiveExtensionPoint(ExtensionPointType.OrderImportBeanTransformation)) {
					Object[] params = { orderBean };
					try {
						orderBean = (OutboundOrderCsvBean) getExtensionPointService().eval(ExtensionPointType.OrderImportBeanTransformation,
							params);
					} catch (Exception e) {
						String lineNum = orderBean.getLineNumber().toString();
						String errorMessage = "Failed to evaluate OrderImportBeanTransformation extension point on line(s) %s: " + e.toString();
						String errorMessageLog = String.format(errorMessage, lineNum);
						LOGGER.error(errorMessageLog);
						String lineNumList = orderImportBeanTransformationViolations.get(errorMessage);
						if (lineNumList == null) {
							lineNumList = lineNum;
						} else {
							lineNumList += ", " + lineNum;
						}
						orderImportBeanTransformationViolations.put(errorMessage, lineNumList);
					}
				}
			}
			for (String errorMsg : orderImportBeanTransformationViolations.keySet()){
				String lineNumList = orderImportBeanTransformationViolations.get(errorMsg);
				String errorWithLines = String.format(errorMsg, lineNumList);
				batchResult.addViolation("OrderImportBeanTransformation", null, errorWithLines);
			}
			addToExtensionMsFromTimeBefore(timeBeforeExtension);
		}

		// instead of processing all order line items in one transaction,
		// chunk it up into small batches and write it to a queue.
		// orders from one file are always be contained by one batch.

		// create list of order batches
		Set<String> orderIds = new HashSet<String>();
		Set<String> itemIds = new HashSet<String>();
		Set<String> gtins = new HashSet<String>();
		Set<String> orderGroupIds = new HashSet<String>();
		Map<String, OutboundOrderBatch> orderBatches = new HashMap<String, OutboundOrderBatch>();
		//For readability, the first non-header line is indexed "2"
		for (OutboundOrderCsvBean orderBean : originalBeanList) {
			String orderId = orderBean.orderId;
			orderIds.add(orderId);
			itemIds.add(orderBean.getItemId());
			gtins.add(orderBean.getGtin());
			String orderGroupId = orderBean.getOrderGroupId();
			if (orderGroupId != null) {
				orderGroupIds.add(orderGroupId);
			}
			OutboundOrderBatch batch = orderBatches.get(orderId);
			if (batch == null) {
				batch = new OutboundOrderBatch(0);
				orderBatches.put(orderId, batch);
			}
			batch.add(orderBean);
			numLineItems++;
		}
		numOrders = orderBatches.size();

		// add all existing orders from specified order groups, so they can be retired if needed

		if (orderGroupIds.size() > 0) {
			LOGGER.info("Adding orders for " + orderGroupIds.size() + " order groups");
			for (String orderGroupId : orderGroupIds) {
				OrderGroup og = facility.getOrderGroup(orderGroupId);
				if (og != null) {
					// add order headers to batch
					for (OrderHeader order : og.getOrderHeaders()) {
						String orderId = order.getOrderId();
						if (!orderIds.contains(orderId)) {
							// deactivate order and line items, since not included in order group
							LOGGER.info("Deactivating order not included in group: " + order);
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

		if (deleteOldOrders) {
			deleteOldOrders(facility.getPersistentId(), orderIds);
		}
		
		// load order queue
		int numBatches = 1;
		OutboundOrderBatch combinedBatch = new OutboundOrderBatch(numBatches);
		for (String orderId : orderBatches.keySet()) {
			OutboundOrderBatch batch = orderBatches.get(orderId);
			combinedBatch.add(batch);
			if (combinedBatch.size() > maxOrderLines) {
				// queue up batch
				this.batchQueue.add(combinedBatch);
				// and create a new one
				numBatches++;
				combinedBatch = new OutboundOrderBatch(numBatches);
			}
		}
		if (combinedBatch.size() > 0) {
			// add remaining left-over batch to queue
			this.batchQueue.add(combinedBatch);
		} else {
			// adjust count
			numBatches--;
		}
		LOGGER.info("Order file chunked into " + numBatches + " batches.");

		// spawn workers and process order batch queue
		ExecutorService executor = Executors.newFixedThreadPool(numWorkerThreads);
		ArrayList<OutboundOrderBatchProcessor> workers = new ArrayList<>(numWorkerThreads);
		for (int i = 1; i <= numWorkerThreads; i++) {
			OutboundOrderBatchProcessor worker = new OutboundOrderBatchProcessor(i, this, inProcessTime, facility);
			workers.add(worker);
			executor.execute(worker);
		}

		// wait until all threads are done
		executor.shutdown();
		while (!executor.isTerminated()) {
			ThreadUtils.sleep(100);
		}

		for (OutboundOrderBatchProcessor worker : workers) {
			BatchResult<Object> workerResult = worker.getBatchResult();
			batchResult.getViolations().addAll(workerResult.getViolations());
			workerResult.getViolations();
		}

		this.endTime = System.currentTimeMillis();
		LOGGER.info("spent {} ms doing extensions", spentDoingExtensionsMs);

		batchResult.setCompleted(new Date(endTime));
		batchResult.setOrdersProcessed(numOrders);
		batchResult.setLinesProcessed(numLineItems);
		batchResult.setOrderIds(new ArrayList<>(orderIds));
		batchResult.setItemIds(new ArrayList<>(itemIds));
		batchResult.setGtins(new ArrayList<>(gtins));
		return batchResult;
	}

	@Override
	public void persistDataReceipt(Facility facility, String username, String filename, long received, EdiTransportType tranportType, BatchResult<?> results) {
		ImportReceipt receipt = new ImportReceipt();
		receipt.setTransportType(tranportType);
		receipt.setUsername(username);
		receipt.setFilename(filename);
		receipt.setReceived(new Date(received));
		receipt.setCompleted(results.getCompleted());
		receipt.setOrdersProcessed(results.getOrdersProcessed());
		receipt.setLinesProcessed(results.getLinesProcessed());
		receipt.setLinesFailed(results.getViolations().size());
		receipt.setOrderIdsList(results.getOrderIds());
		receipt.setItemIdsList(results.getItemIds());
		receipt.setGtinsList(results.getGtins());
		receipt.setParent(facility);
		receipt.setDomainId(receipt.getDefaultDomainIdPrefix() + "_" + receipt.getType() + "_" + results.getReceived().getTime());
		if (results.isSuccessful()) {
			receipt.setStatus(ImportStatus.Completed);
			LOGGER.info("Imported " + receipt);
		} else {
			receipt.setStatus(ImportStatus.Failed);
			LOGGER.warn("Failed to import " + receipt);
		}

		ImportReceipt.staticGetDao().store(receipt);

	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_OUTBOUND);
	}
	
	private void deleteOldOrders(final UUID facilityId, final Set<String> orderIds) {
		Runnable deleteOrdersRunnable = new Runnable() {
			@Override
			public void run() {
				TenantPersistenceService.getInstance().beginTransaction();
				Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
				int counter = 1, total = orderIds.size();
				for (String orderId : orderIds) {
					try {
						if (makeOldOrderDeletionFail){
							throw new Exception("Order Deletion Test-Triggered Error");
						}
						OrderHeader oldOrder = OrderHeader.staticGetDao().findByDomainId(facility, orderId);
						if (oldOrder != null) {
							LOGGER.info("Deleting old order {} ({}/{}) during its re-importing", orderId, counter++, total);
							OrderHeader.staticGetDao().delete(oldOrder);
						}
					} catch (Exception e) {
						LOGGER.warn("Unable to delete order " + orderId + " during re-importing", e);
					}
					if (Thread.interrupted()) {
						break;	// Executor has asked us to stop
					}
				}
				TenantPersistenceService.getInstance().commitTransaction();
			}
		};
		ExecutorService executor = Executors.newFixedThreadPool(1);
		LOGGER.info("Start order deletion thread");
		executor.execute(deleteOrdersRunnable);
		executor.shutdown();
		try {
			LOGGER.info("Await order deletion thread termination");
			boolean ternimated = executor.awaitTermination(3, TimeUnit.MINUTES);
			if (!ternimated){
				executor.shutdownNow();
				LOGGER.warn("Timeout exception while trying to delete orders before re-importing. Interrupting order deletion.");
				
			}
		} catch (InterruptedException e){
			LOGGER.warn("Order deletion thread interrupted while awaiting termination", e);
		}
		LOGGER.info("Order delition completed");
	}

	@Override
	public String createReplenishOrderForItem(Facility facility, ReplenishItem item) {
		String gtin = item.getGtin();
		String itemId = item.getItemId();
		String uom = Strings.nullToEmpty(item.getUom());
		
		String scannableId = Strings.emptyToNull(gtin) == null ? itemId : gtin;
		String location = Strings.nullToEmpty(item.getLocation());

		OutboundOrderCsvBean orderBean = new OutboundOrderCsvBean();
		orderBean.setLineNumber(1);
		orderBean.setOrderId(scannableId);
		orderBean.setItemId(itemId);
		orderBean.setQuantity("1");
		orderBean.setUom(uom);
		orderBean.setLocationId(location);
		orderBean.setPreAssignedContainerId(scannableId);
		orderBean.setWorkSequence("0");
		orderBean.setOperationType(OrderTypeEnum.REPLENISH.name());
		ArrayList<OutboundOrderCsvBean> orderBeanList = new ArrayList<>(1);
		orderBeanList.add(orderBean);
		this.importOrdersFromBeanList(orderBeanList, facility, new Timestamp(System.currentTimeMillis()), false);
		return scannableId;
	}
}
