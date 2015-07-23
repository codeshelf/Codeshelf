package com.codeshelf.edi;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.StaleObjectStateException;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PickStrategyEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.DomainObjectCache;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.service.NotificationService;
import com.codeshelf.util.DateTimeParser;
import com.codeshelf.util.UomNormalizer;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.InputValidationException;
import com.google.common.base.Strings;

public class OutboundOrderBatchProcessor implements Runnable {

	private static final Logger							LOGGER					= LoggerFactory.getLogger(OutboundOrderBatchProcessor.class);

	private OutboundOrderPrefetchCsvImporter			importer;

	DateTimeParser										dateTimeParser;

	@Getter
	private Timestamp									processTime;

	@Getter
	@Setter
	private String										oldPreferredLocation	= null;

	@Getter
	private BatchResult<Object>							batchResult				= new BatchResult<Object>();

	private Facility									facility;

	private int											processorId;

	Map<String, OrderHeader>							orderMap;

	DomainObjectCache<ItemMaster>						itemMasterCache			= null;
	DomainObjectCache<OrderHeader>						orderHeaderCache		= null;
	HashMap<String, Gtin>								gtinCache				= null;

	private HashMap<String, Map<String, OrderDetail>>	orderlineMap;
	private HashMap<String, Container>					containerMap;
	private HashMap<String, ContainerUse>				containerUseMap;

	long												startTime;
	long												endTime;
	int													numOrders;
	int													numLines;

	@Getter
	@Setter
	int													maxProcessingAttempts	= 10;

	Map<String, Boolean>								orderChangeMap;

	public OutboundOrderBatchProcessor(int procId,
		OutboundOrderPrefetchCsvImporter importer,
		Timestamp processTime,
		Facility facility) {
		this.importer = importer;
		this.processTime = processTime;
		this.facility = facility;
		this.processorId = procId;
		this.dateTimeParser = new DateTimeParser();
	}

	@Override
	public void run() {
		OutboundOrderBatch batch = importer.getBatchQueue().poll();
		while (batch != null) {
			// process batch
			batch.setProcessingAttempts(batch.getProcessingAttempts() + 1);
			LOGGER.info("Worker #" + processorId + " is processing " + batch + " in " + batch.getProcessingAttempts() + ". attempt");

			try {
				TenantPersistenceService.getInstance().beginTransaction();

				// attach facility to new session
				Facility facility = Facility.staticGetDao().reload(this.facility);

				itemMasterCache = new DomainObjectCache<ItemMaster>(ItemMaster.staticGetDao());
				orderHeaderCache = new DomainObjectCache<OrderHeader>(OrderHeader.staticGetDao());

				ArrayList<OrderHeader> orderSet = new ArrayList<OrderHeader>();

				LOGGER.debug("Begin order import.");

				this.startTime = System.currentTimeMillis();
				LOGGER.info(batch.getItemIds().size() + " distinct items found in batch");

				// cache item master
				itemMasterCache.reset();
				itemMasterCache.setFetchOnMiss(false);
				itemMasterCache.load(facility, batch.getItemIds());
				LOGGER.info("ItemMaster cache populated with " + this.itemMasterCache.size() + " entries");

				// cache order headers
				orderHeaderCache.reset();
				orderHeaderCache.setFetchOnMiss(false);
				orderHeaderCache.load(facility, batch.getOrderIds());
				LOGGER.info("OrderHeader cache populated with " + this.orderHeaderCache.size() + " entries");

				// cache gtin
				gtinCache = generateGtinCache(facility);
				LOGGER.info("Gtin cache populated with " + this.gtinCache.size() + " entries");

				// prefetch order details already associated with orders
				this.orderChangeMap = new HashMap<String, Boolean>();
				this.orderlineMap = new HashMap<String, Map<String, OrderDetail>>();
				if (orderHeaderCache.size() == 0) {
					LOGGER.info("Skipping order line loading, since all orders are new.");
				} else {
					LOGGER.info("Loading line items for " + orderHeaderCache.size() + " orders.");
					Criteria criteria = OrderDetail.staticGetDao().createCriteria();
					criteria.add(Restrictions.in("parent", orderHeaderCache.getAll()));
					List<OrderDetail> orderDetails = OrderDetail.staticGetDao().findByCriteriaQuery(criteria);
					for (OrderDetail line : orderDetails) {
						Map<String, OrderDetail> l = this.orderlineMap.get(line.getOrderId());
						if (l == null) {
							l = new HashMap<String, OrderDetail>();
							this.orderlineMap.put(line.getOrderId(), l);
							this.orderChangeMap.put(line.getOrderId(), false);
						}
						l.put(line.getOrderDetailId(), line);
					}
				}

				// prefetch container uses already associated with orders
				this.containerUseMap = new HashMap<String, ContainerUse>();
				if (orderHeaderCache.size() == 0) {
					LOGGER.info("Skipping container use loading, since all orders are new.");
				} else {
					LOGGER.info("Loading container use for " + orderHeaderCache.size() + " orders.");
					Criteria criteria = ContainerUse.staticGetDao().createCriteria();
					criteria.add(Restrictions.in("orderHeader", orderHeaderCache.getAll()));
					List<ContainerUse> containerUses = ContainerUse.staticGetDao().findByCriteriaQuery(criteria);
					for (ContainerUse cu : containerUses) {
						this.containerUseMap.put(cu.getOrderHeader().getOrderId(), cu);
					}
				}

				// prefetch containers that are referenced in current order file.
				// because of m:m container to use relationship, containers that have
				// no active use are not deactivated here, but in a background process.
				// this process only deals with data directly related to the order batch.
				HashSet<String> containerIds = batch.getContainerIds();
				this.containerMap = new HashMap<String, Container>();
				Criteria criteria = Container.staticGetDao().createCriteria();
				criteria.add(Restrictions.in("domainId", containerIds));
				List<Container> containers = Container.staticGetDao().findByCriteriaQuery(criteria);
				this.containerMap = new HashMap<String, Container>();
				for (Container container : containers) {
					containerMap.put(container.getDomainId(), container);
				}
				criteria = Container.staticGetDao().createCriteria();
				criteria.add(Restrictions.in("domainId", containerIds));

				// process order file
				List<OutboundOrderCsvBean> lines = batch.getLines();
				//Check if destinationId, shipperId, or customerId values vary within individual orders
				checkForChangingFields(lines);
				int lineCount = 1;
				int count = 1, size = lines.size();
				for (OutboundOrderCsvBean orderBean : lines) {
					// transform order bean with groovy script, if enabled
					if (importer.getExtensionPointService().hasExtensionPoint(ExtensionPointType.OrderImportBeanTransformation)) {
						Object[] params = { orderBean };
						try {
							orderBean = (OutboundOrderCsvBean) importer.getExtensionPointService()
								.eval(ExtensionPointType.OrderImportBeanTransformation, params);
						} catch (Exception e) {
							LOGGER.error("Failed to evaluate OrderImportBeanTransformation extension point", e);
						}
					}
					// process order bean
					try {
						OrderHeader order = orderCsvBeanImport(orderBean, facility, processTime, count++ + "/" + size);
						if ((order != null) && (!orderSet.contains(order))) {
							orderSet.add(order);
						}
						batchResult.add(orderBean);
						importer.produceRecordSuccessEvent(orderBean);
					} catch (Exception e) {
						String errorMessage = String.format("Unable to import order line %d: %s",
							orderBean.getLineNumber(),
							e.toString());
						LOGGER.error(errorMessage);
						batchResult.addLineViolation(lineCount, orderBean, errorMessage);
					}
				}

				// init empty order map
				Map<String, Boolean> isEmptyOrder = new HashMap<String, Boolean>();
				for (OrderHeader order : this.orderHeaderCache.getAll()) {
					isEmptyOrder.put(order.getOrderId(), true);
				}

				// loop through order items and deactivate items that have not been touched by this batch
				// also set empty order flag to false, if order has non-zero item quantities
				Collection<Map<String, OrderDetail>> allOrderLines = orderlineMap.values();
				for (Map<String, OrderDetail> orderlines : allOrderLines) {
					for (OrderDetail line : orderlines.values()) {
						// deactivate out-dated items
						if (line.getActive() == true && !line.getUpdated().equals(processTime)) {
							line.setActive(false);
							OrderDetail.staticGetDao().store(line);
							LOGGER.info("Deactivating order item " + line);
							this.orderChangeMap.put(line.getOrderId(), true);
						}
						// reset empty order flag
						if (line.getActive() && line.getQuantity() != null && line.getQuantity() > 0) {
							isEmptyOrder.put(line.getOrderId(), false);
						}
					}
				}

				// reactivate changed orders
				for (Entry<String, Boolean> e : this.orderChangeMap.entrySet()) {
					if (e.getValue()) {
						OrderHeader order = this.orderHeaderCache.get(e.getKey());
						if (!isEmptyOrder.get(order.getOrderId())) {
							LOGGER.info("Order " + order + " changed during import");
							if (!order.getActive() || order.getStatus() != OrderStatusEnum.RELEASED) {
								LOGGER.info("Order " + order + " reactivated. Status set to 'released'.");
								order.setActive(true);
								order.setStatus(OrderStatusEnum.RELEASED);
								OrderHeader.staticGetDao().store(order);
								// TODO: check if order was on cart or (partially) picked and create event
							}
						}
					}
				}

				// deactivate empty orders
				for (OrderHeader order : this.orderHeaderCache.getAll()) {
					if (isEmptyOrder.get(order.getOrderId()) == true) {
						LOGGER.info("Deactivating empty order " + order);
						order.setActive(false);
						OrderHeader.staticGetDao().store(order);
					}
				}

				// loop through container uses and deactivate uses that have not been touched by this batch
				Collection<ContainerUse> allUses = this.containerUseMap.values();
				for (ContainerUse use : allUses) {
					if (use.getActive() == true && !use.getUpdated().equals(processTime)) {
						// use is out-dated -> deactivate
						use.setActive(false);
						ContainerUse.staticGetDao().store(use);
						LOGGER.info("Deactivating container use " + use);
					}
				}
				this.numOrders = allOrderLines.size();
				TenantPersistenceService.getInstance().commitTransaction();
				LOGGER.info("Completed processing " + batch);

				//TODO switch to callable and wait for the futures
			} catch (StaleObjectStateException e) {
				LOGGER.warn("Failed to process order batch " + batch + " due to stale data.", e);
				if (batch.getProcessingAttempts() > this.maxProcessingAttempts) {
					LOGGER.error("Giving up on processing order batch " + batch + ".  Retry import at a later time.");
				} else {
					// returning batch to the queue to give it another chance
					LOGGER.warn("Returning batch " + batch + " to import queue to retry.");
					importer.getBatchQueue().add(batch);
				}
				TenantPersistenceService.getInstance().rollbackTransaction();
			} catch (Exception e) {
				LOGGER.info("Failed to process " + batch, e);
				TenantPersistenceService.getInstance().rollbackTransaction();
			}
			// pull next batch off queue
			batch = importer.getBatchQueue().poll();
		}
	}

	private OrderHeader orderCsvBeanImport(final OutboundOrderCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final String counterStr) {

		OrderHeader order = null;

		LOGGER.info(counterStr + ", " + inCsvBean.toString());

		inCsvBean.fillDefaultDueDate();
		String errorMsg = inCsvBean.validateBean();
		if (errorMsg != null) {
			LOGGER.error("Bean validation error: " + errorMsg);
			throw new InputValidationException(inCsvBean, errorMsg);
		}

		OrderGroup group = updateOptionalOrderGroup(inCsvBean, inFacility, inEdiProcessTime);
		order = updateOrderHeader(inCsvBean, inFacility, inEdiProcessTime, group);
		updateContainer(inCsvBean, inFacility, inEdiProcessTime, order);
		UomMaster uomMaster = updateUomMaster(inCsvBean.getUom(), inFacility);
		String itemId = inCsvBean.getItemId();
		ItemMaster itemMaster = updateItemMaster(itemId,
			inCsvBean.getDescription(),
			inFacility,
			inEdiProcessTime,
			uomMaster,
			inCsvBean.getGtin());
		OrderDetail orderDetail = updateOrderDetail(inCsvBean, inFacility, inEdiProcessTime, order, uomMaster, itemMaster);
		@SuppressWarnings("unused")
		Gtin gtinMap = upsertGtin(inFacility, itemMaster, inCsvBean, uomMaster);

		// If preferredLocation is there, we set it on the detail. LOCAPICK controls whether we also create new inventory to match.
		if (importer.getLocaPick()) {
			String locationValue = orderDetail.getPreferredLocation(); // empty string if location did not validate
			if (locationValue != null && !locationValue.isEmpty()) {
				// somewhat cloned from UiUpdateService.upsertItem(); Could refactor
				InventoryCsvImporter importer = new InventoryCsvImporter(new EventProducer());

				InventorySlottedCsvBean itemBean = new InventorySlottedCsvBean();
				itemBean.setItemId(itemId);
				itemBean.setLocationId(locationValue);
				itemBean.setCmFromLeft(inCsvBean.getCmFromLeft());
				itemBean.setQuantity("0");
				itemBean.setUom(uomMaster.getDomainId());
				Location location = inFacility.findSubLocationById(locationValue);
				// location has to be good because we did all validation before allowing it into OrderDetail.prefferedLocation field.
				if (location == null)
					LOGGER.error("Unexpected bad location in orderCsvBeanImport. Did not create item");
				else {
					// DEV-596 This is a very tricky question. If the item already existed, and there are no other order details that have the old preferredLocation
					// then we would want to either move that inventory to the location, or archive the old and let the new one be made.
					String oldStr = getOldPreferredLocation(); // is only set if the detail existed before and had a preferredLocation

					boolean thisDetailHadOldDifferentPreferredLocation = (oldStr != null && !oldStr.isEmpty() && !oldStr.equals(locationValue));
					Item oldItem = null;
					if (thisDetailHadOldDifferentPreferredLocation) {
						// We need to find the item at the old location. Then determine if a new item was made at the new location. If so, add the old item to a list for investigation.
						Location oldLocation = inFacility.findSubLocationById(oldStr);
						// DEV-635 note  if location is not resolved, but order detail will be ok with preferred location and sequence, then we can make inventory if we wish at facility. Only if LOCAPICK is true.
						if (oldLocation != null) {
							// we would normally expect the old location to have an inventory item there.
							LOGGER.info("Old location for changing orderdetail was " + oldStr);
							oldItem = itemMaster.getActiveItemMatchingLocUom(oldLocation, uomMaster);
							if (oldItem == null) {
								/* Just debug aid */
								LOGGER.error("probable error");
								Collection<Item> locItems = oldLocation.getStoredItems().values();
								List<Item> masterItems = itemMaster.getItems();
								LOGGER.error("location has " + locItems.size() + " items. Master has " + masterItems.size());
								if (locItems.size() == 1) {
									Item fromMasterItems = masterItems.get(0);
									LOGGER.error("fromLocItems: " + fromMasterItems.toLogString());
								}
								oldItem = itemMaster.getActiveItemMatchingLocUom(oldLocation, uomMaster); // just to step in again to see the uomMaster is not loaded						
							}
						}
					}

					// updateSlottedItem is going to make new inventory if location changed for cases, and also for each if EACHMULT is true
					importer.updateSlottedItem(false, itemBean, location, inEdiProcessTime, itemMaster, uomMaster);
				}
			}
		}
		return order;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @return
	 */
	private OrderGroup updateOptionalOrderGroup(final OutboundOrderCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {
		String orderGroupId = inCsvBean.getOrderGroupId();
		if (orderGroupId == null || orderGroupId.length() == 0) {
			// order group in undefined
			return null;
		}
		OrderGroup result = inFacility.getOrderGroup(inCsvBean.getOrderGroupId());
		if ((result == null)) {
			// create new order group
			result = new OrderGroup();
			result.setOrderGroupId(orderGroupId);
			result.setDescription(OrderGroup.DEFAULT_ORDER_GROUP_DESC_PREFIX + inCsvBean.getOrderGroupId());
			result.setStatus(OrderStatusEnum.RELEASED);
			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
			inFacility.addOrderGroup(result);
			try {
				OrderGroup.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("updateOptionalOrderGroup storing new orderGroup", e);
			}
		}

		if (result != null) {
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				OrderGroup.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("updateOptionalOrderGroup", e);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @param inOrder
	 * @return
	 */
	private Container updateContainer(final OutboundOrderCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderHeader inOrder) {
		Container container = null;
		String containerId = inCsvBean.getPreAssignedContainerId();

		if ((containerId != null) && (containerId.length() > 0)) {
			//result = inFacility.getContainer(inCsvBean.getPreAssignedContainerId());
			container = this.containerMap.get(containerId);
			if (container == null) {
				// create new container, if it does not already exist
				container = new Container();
				container.setContainerId(containerId);
				ContainerKind kind = inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND);
				container.setKind(kind);
				container.setParent(inFacility);
				this.containerMap.put(containerId, container);
				// code below taken out to improve performance. it's safe as long as 
				// containers are not retrieved via facility in this transaction.
				// inFacility.addContainer(container);
			}
			// update container
			container.setUpdated(inEdiProcessTime);
			container.setActive(true);
			try {
				Container.staticGetDao().store(container);
			} catch (DaoException e) {
				LOGGER.error("updateContainer storing Container", e);
			}

			// Now create the container use for this. ContainerUse has Container, OrderHead as potential parent.  (Che also, but not set here.)
			// ContainerUse use = container.getContainerUse(inOrder);
			ContainerUse use = this.containerUseMap.get(inOrder.getOrderId());
			if (use == null) {
				use = new ContainerUse();
				use.setDomainId(inOrder.getOrderId());
				inOrder.addHeadersContainerUse(use);
				container.addContainerUse(use);
				this.containerUseMap.put(inOrder.getOrderId(), use);
			} else {
				OrderHeader prevOrder = use.getOrderHeader();
				// No worries for container, as no way containerUse can change to different container owner.
				if (prevOrder == null) {
					inOrder.addHeadersContainerUse(use);
					Container.staticGetDao().store(inOrder);
				} else if (!prevOrder.equals(inOrder)) {
					prevOrder.removeHeadersContainerUse(use);
					Container.staticGetDao().store(prevOrder);
					inOrder.addHeadersContainerUse(use);
					Container.staticGetDao().store(inOrder);
				}
			}

			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			use.setUsedOn(timestamp);
			use.setActive(true);
			use.setUpdated(inEdiProcessTime);

			try {
				ContainerUse.staticGetDao().store(use);
				// order-containerUse is one-to-one, so add above set a persistable field on the orderHeader
				OrderHeader.staticGetDao().store(inOrder);
			} catch (DaoException e) {
				LOGGER.error("updateContainer storing ContainerUse", e);
			}
		}

		return container;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inOrderGroup
	 * @return
	 */
	private OrderHeader updateOrderHeader(final OutboundOrderCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderGroup inOrderGroup) {
		OrderHeader result = null;

		// fetch from cache instead of hitting database
		// result = inFacility.getOrderHeader(inCsvBean.getOrderId());
		result = this.orderHeaderCache.get(inCsvBean.getOrderId());

		if (result == null) {
			LOGGER.debug("Creating new OrderHeader instance for " + inCsvBean.getOrderId() + " , for facility: " + inFacility);
			result = new OrderHeader();
			result.setDomainId(inCsvBean.getOrderId());
			this.orderHeaderCache.put(result);
			result.setStatus(OrderStatusEnum.RELEASED);
			result.setActive(true);
			result.setParent(inFacility);
		}

		result.setOrderType(OrderTypeEnum.OUTBOUND);
		result.setCustomerId(inCsvBean.getCustomerId());
		result.setShipperId(inCsvBean.getShipperId());
		result.setDestinationId(inCsvBean.getDestinationId());

		if (inCsvBean.getOrderDate() != null) {
			try {
				Date date = dateTimeParser.parse(inCsvBean.getOrderDate());
				// date may be null if string was blank
				if (date != null)
					result.setOrderDate(new Timestamp(date.getTime()));
				// Note: on an update, cannot clear a previous set time back to null. Could do it, just haven't bothered here.
				// Mandatory field?

			} catch (IllegalArgumentException e1) {
				LOGGER.error("updateOrderHeader orderDate", e1);
			}
		}

		if (inCsvBean.getDueDate() != null) {
			try {
				Date date = dateTimeParser.parse(inCsvBean.getDueDate());
				// date may be null if string was blank
				if (date != null)
					result.setDueDate(new Timestamp(date.getTime()));
				// Note: on an update, cannot clear a previous set time back to null. Could do it, just haven't bothered here.
			} catch (IllegalArgumentException e1) {
				LOGGER.error("updateOrderHeader dueDate", e1);
			}
		}

		PickStrategyEnum pickStrategy = PickStrategyEnum.SERIAL;
		String pickStrategyEnumId = inCsvBean.getPickStrategy();
		if ((pickStrategyEnumId != null) && (pickStrategyEnumId.length() > 0)) {
			pickStrategy = PickStrategyEnum.valueOf(pickStrategyEnumId);
		}
		result.setPickStrategy(pickStrategy);
		//If the imported order group is "undefined" but the header was already assigned to a group, unassign it.
		OrderGroup oldGroup = result.getOrderGroup();
		if (oldGroup != null) {
			oldGroup.removeOrderHeader(result.getOrderId());
		}
		result.setOrderGroup(null);
		if (inOrderGroup != null) {
			//If the order was imported with a group, make sure it is assigned to it.
			inOrderGroup.addOrderHeader(result);
			result.setOrderGroup(inOrderGroup);
		}
		try {
			result.setUpdated(inEdiProcessTime);
			OrderHeader.staticGetDao().store(result);
		} catch (DaoException e) {
			LOGGER.error("updateOrderHeader", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * helper for GTIN_case3 in updateItemMaster
	 */
	private boolean gtinIsAnOnboardingManufacture(Gtin inGtin) {
		// If user scanned an inventory for an unknown GTIN, then we make phony item (to track the inventory) and phony itemMaster
		// because the item must have itemMaster. We give the GTIN ID. Aside from that, gtin and master domainID would never match.
		if (inGtin == null) {
			LOGGER.error("null value in gtinIsAnOnboardingManufacture");
			return false;
		}
		ItemMaster master = inGtin.getParent();
		return inGtin.getDomainId().equals(master.getDomainId());
	}

	// --------------------------------------------------------------------------
	/**
	 * Called during order import process. Please see very similar method in inventory service.
	 * This may have rather extreme side effects of changing the gtin domain ID, changing item master domain ID,
	 * and changing item domain ID and uom. Hopefully only for the "onboarding" process of inventory scan of a new UPC,
	 * then later getting an order that names that UPC
	 */
	private ItemMaster updateItemMaster(final String inItemId,
		final String inDescription,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final UomMaster inUomMaster,
		final String gtinId) {
		ItemMaster itemMaster = null;

		//There should not be an ItemMaster and an auto-generated Gtin's ItemMaster at the same time
		//This is due to Gtin's ItemMaster only being created when there isn't already an ItemMaster for that gtinId
		//If it does happen, ignore the Gtin's ItemMaster
		itemMaster = this.itemMasterCache.get(inItemId);

		// Try to find a matching manual-inventory Gtin
		// This is a very important case for "inventory onboarding". Initial inventory scan of gtin make a phony item and item master.
		// Now comes the good update where we have a chance to correct it. Log as WARN.
		Gtin gtin = this.gtinCache.get(gtinId);
		if (gtin == null)
			gtin = findUniqueSubstringMatch(gtinId);
		if (itemMaster == null && gtin != null) {

			if (!gtinIsAnOnboardingManufacture(gtin)) {
				LOGGER.error("GTIN_case_3ab {} already exists but for wrong SKU. Not changing to {}. SKU master may have incorrect data",
					gtin,
					inItemId);
				// 3a and 3b differ in whether the gtin parent has any items that we are concerned about. If not, changing here is reasonable.
				// Not handled yet.
				// Do not return null, which is odd. See that the master is made anyway below.
			} else {
				// see upsertGtin() code below for more context for case 3.
				LOGGER.warn("GTIN_case_3c {} already exists from apparent inventory process. Changing to {}", gtin, inItemId);
				// if the the current gtin sku is exactly the same as gtin domainId, then we just want to update the master and items uom as we do next.
				// However, host system data errors can lead to gtin or sku changes on apparently valid data

				itemMaster = gtin.getParent();
				List<Item> items = itemMaster.getItems();
				for (Item item : items) {
					if (gtin.equals(item.getGtin())) {
						LOGGER.info("Changing UOM of item location {} at {}", item.getItemId(), item.getItemLocationName());
						item.setUomMaster(inUomMaster);
						item.setDomainId(item.makeDomainId());
						Item.staticGetDao().store(item);
					}
				}
				gtin.setUomMaster(inUomMaster);
				LOGGER.warn("update the itemMaster cache.  rename ItemMaster {} to {}", itemMaster.getItemId(), inItemId);
				this.itemMasterCache.remove(itemMaster);
				itemMaster.setDomainId(inItemId);
				itemMaster.setItemId(inItemId);
				ItemMaster.staticGetDao().store(itemMaster);
				this.itemMasterCache.put(itemMaster);

				Gtin.staticGetDao().store(gtin);
				LOGGER.info("Updated GTIN to to {}", gtin);
			}
		}

		// create a new item, if needed
		if (itemMaster == null) {
			itemMaster = new ItemMaster();
			itemMaster.setDomainId(inItemId);
			itemMaster.setItemId(inItemId);
			itemMaster.setParent(inFacility);
			this.itemMasterCache.put(itemMaster);
		}

		// update only if modified or new
		if (itemMaster.getDescription() != null && itemMaster.getDescription().equals(inDescription)
				&& itemMaster.getStandardUom().equals(inUomMaster) && itemMaster.getActive() != null
				&& itemMaster.getActive() == true && itemMaster.getActive() != null && itemMaster.getUpdated() == inEdiProcessTime) {
			// already up to date
		} else {
			// new or updated. Not really logging this. Ok. Note that it does change default UOM of the master.			
			itemMaster.setDescription(inDescription);
			itemMaster.setStandardUom(inUomMaster);
			itemMaster.setActive(true);
			itemMaster.setUpdated(inEdiProcessTime);
			try {
				ItemMaster.staticGetDao().store(itemMaster);
			} catch (DaoException e) {
				LOGGER.error("updateItemMaster", e);
			}
		}
		return itemMaster;
	}

	// --------------------------------------------------------------------------
	/**
	 * Although it is called updateUomMaster, it really is a 
	 * findNormalizedMasterOrCreate
	 * Note: when creating, it creates with the passed in value, and the default normalized value.
	 * this means that is "each" is the first kind of ea found, it the master.
	 */
	private UomMaster updateUomMaster(final String inUomId, final Facility inFacility) {
		UomMaster result = null;

		result = inFacility.getNormalizedUomMaster(inUomId);
		if (result == null) {
			result = new UomMaster();
			result.setUomMasterId(inUomId);
			inFacility.addUomMaster(result);

			try {
				UomMaster.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("updateUomMaster", e);
			}
		}

		return result;
	}

	private Gtin upsertGtin(final Facility inFacility,
		final ItemMaster inItemMaster,
		final OutboundOrderCsvBean inCsvBean,
		UomMaster inUomMaster) {

		if (inCsvBean.getGtin() == null || inCsvBean.getGtin().isEmpty()) {
			return null;
		}
		String gtinId = inCsvBean.getGtin();

		Gtin result = Gtin.staticGetDao().findByDomainId(null, gtinId);
		ItemMaster previousItemMaster = null;

		/* Many cases possible!
		 * If we have this gtinId already
		 *  - 1) match the same itemMaster and UOM. No change needed. No log.
		 *  - 2) Same itemMaster. Change the UOM. Need a change if following last update wins. Need a WARN/notify
		 *  - 3) Different itemMaster.
		 *  - 3a) Different itemMaster has a gtin for this uom (different gtinID). WARN. Delete that and modify this. Or modify domainId on that?
		 *  - 3b) Different itemMaster has no gtin for this uom. WARN. Modify this, and remove this from old itemMaster?
		 * If we do not have this gtin already
		 *  - 4) But this itemMaster/uom has a gtin already. WARN. Modify that gtin, changing its domainId
		 *  - 5) This itemMater/uom does not have gtin already. Simple add. Log as INFO, as adding a gtin is relatively rare after initial usage.
		 */

		if (result != null) {
			// we have this gtinId already
			previousItemMaster = result.getParent();
			UomMaster previousUomMaster = result.getUomMaster();

			// Check if existing GTIN is associated with the ItemMaster
			if (previousItemMaster.equals(inItemMaster)) {

				// Check if the UOM specified in the inventory file matches UOM of existing GTIN
				if (!inUomMaster.equals(previousUomMaster)) {
					// This is - 2) Same itemMaster. Change the UOM. Need a change if following last update wins. Need a WARN/notify
					LOGGER.warn("GTIN_case_2 {} already exists but for wrong UOM. Changing it to {}",
						result,
						inUomMaster.getUomMasterId());
					// return null;
					result.setUomMaster(inUomMaster);
					try {
						Gtin.staticGetDao().store(result);
					} catch (DaoException e) {
						LOGGER.error("upsertGtinMap save", e);
					}
				}

			} else {
				// This is - 3) Different itemMaster.
				// Import line is attempting to associate existing GTIN with a new item.
				// Note that updateItemMaster above has already modified the gtin for case 3c. (common onboarding case)
				// Cases 3a and 3b do fall through to this, but not sure what to do
				LOGGER.debug("gtin_case_3 hit in upsertGtin(). Doing nothing ");
				// updateItemMaster logged 3a and 3b as an error already, or 3c as a WARN
				return null;
			}
		} else {
			// we do not have this exact gtinId already.
			Gtin matchingGtinForMaster = findGtinMatchOnItemMaster(gtinId, inItemMaster, inUomMaster);

			Gtin misMatchGtinForMaster = findGtinMisMatchOnItemMaster(gtinId, inItemMaster, inUomMaster, matchingGtinForMaster);

			if (matchingGtinForMaster != null) {
				if (!matchingGtinForMaster.getDomainId().equals(gtinId)) {
					// Info, because this case will happen over and over again at Accu as they send truncated gtins that do not
					// trigger any special processing.
					LOGGER.info("GTIN_case_5 Found gtin:{} even though file had this substring {}", matchingGtinForMaster, gtinId);
				}
				return matchingGtinForMaster;

			} else if (misMatchGtinForMaster != null) {
				// 4) But this itemMaster/uom has a gtin already. WARN. Modify that gtin, changing its domainId
				// But we can only do so there is not already a gtin with that domainId
				Gtin sameGtinOtherMasterOrUom = this.gtinCache.get(gtinId);

				if (sameGtinOtherMasterOrUom != null) {
					LOGGER.error("GTIN_case_4a {} already exists. Cannot modify it to {}/{}. You might do it manually",
						sameGtinOtherMasterOrUom,
						inItemMaster.getItemId(),
						inUomMaster.getUomMasterId());
					return null;
				} else {
					// Change the GTIN based on the order file
					LOGGER.warn("GTIN_case_4b {} already exists. Changing it to {}", misMatchGtinForMaster, gtinId);
					misMatchGtinForMaster.setDomainId(gtinId);
					try {
						Gtin.staticGetDao().store(misMatchGtinForMaster);
						result = misMatchGtinForMaster;
					} catch (DaoException e) {
						LOGGER.error("upsertGtinMap save", e);
					}
				}

			} else {
				// We do not have this gtin yet.
				LOGGER.warn("GTIN_case_6 Adding new gtin:{} for sku:{}/{}",
					gtinId,
					inItemMaster.getItemId(),
					inUomMaster.getUomMasterId());
				result = inItemMaster.createGtin(gtinId, inUomMaster);

				try {
					Gtin.staticGetDao().store(result);
				} catch (DaoException e) {
					LOGGER.error("upsertGtinMap save", e);
				}
			}
		}

		return result;
	}

	final int	lkMinimalSloppyGtinMatchLength	= 7;
	final int	lkMaximumSloppyGtinMatchLength	= 14;

	// --------------------------------------------------------------------------
	/**
	 * DEV-979 helper. What if we find a gtin the the gtinId is a substring of?
	 * In most calling contexts gtinIdToTest length is known to be at least 7
	 */
	private boolean isSloppyGtinMatch(String gtinIdToTest, String gtinIdFromActual) {
		// We say the adminstrator should ensure that the full gtin is in the database. So that is the longer one. We do not sloppy match the other direction.
		if (gtinIdToTest == null || gtinIdFromActual == null)
			return false;
		if (gtinIdToTest.length() > gtinIdFromActual.length())
			return false;
		return (gtinIdFromActual.startsWith(gtinIdToTest) || gtinIdFromActual.endsWith(gtinIdToTest));
	}

	// --------------------------------------------------------------------------
	/**
	 * DEV-979 Accu's truncated GTIN case. What if we find a gtin the the gtinId is a substring of?
	 * Anyway, this return either exact match or the Gtin that gtinId is a substring of if the itemMaster already had it.
	 */
	private Gtin findGtinMatchOnItemMaster(String gtinId, ItemMaster inItemMaster, UomMaster inUomMaster) {
		// inItemMaster.getGtinForUom(uomMaster); definitely returns a gtin or null. This is determining if the passed in gtinID is a close enough match to be the same.

		if (gtinId == null || gtinId.isEmpty())
			return null;

		Gtin result = inItemMaster.getGtinForUom(inUomMaster);
		if (result != null && result.getDomainId().equals(gtinId))
			return result;

		// if not an exact match, let's not bother searching if the length of the gtinId is long enough that it is doubtful it is a substring of a longer Gtin. And not trivially short.
		int gtinLength = gtinId.length();
		if (gtinLength < lkMinimalSloppyGtinMatchLength || gtinLength > lkMaximumSloppyGtinMatchLength) {
			return null;
		}

		// Does this itemMaster already have a gtin that this id is a substring of, with matching uom?
		Collection<Gtin> gtins = inItemMaster.getGtins().values();
		LOGGER.debug("Master {} has these gtins {}", inItemMaster.getItemId(), gtins);
		for (Gtin gtin : gtins) {
			String thisId = gtin.getDomainId();
			if (isSloppyGtinMatch(gtinId, thisId)) {
				if (gtin.getUomMaster().equalsNormalized(inUomMaster)) {
					return gtin;
				}
			}
			LOGGER.debug("rejected {} for master's gtin match of {}:{}/{}",
				gtin,
				gtinId,
				inItemMaster.getItemId(),
				inUomMaster.getUomMasterId());
		}
		// The test strippedLeadingZerosGtin has two inventory conversions to the same item master but different uom.
		// case 4  leaves the somewhat odd situation of gtins for the same real master pointing at separate masters as the second one has not been converted yet.
		Collection<Gtin> gtins2 = gtinCache.values();
		LOGGER.debug("Cache has these gtins {}", gtins2);
		for (Gtin gtin2 : gtins2) {
			boolean gtin2IsOnboarder = gtin2.gtinIsAnOnboardingManufacture();
			String thisId2 = gtin2.getDomainId();
			if (gtin2.getParent().equals(inItemMaster) || gtin2IsOnboarder) {
				if (isSloppyGtinMatch(gtinId, thisId2)) {
					if (gtin2.getUomMaster().equalsNormalized(inUomMaster)) {
						if (!gtin2IsOnboarder) {
							LOGGER.warn("Found gtin sloppy match of {} within {} in cache but not on Item's gtins list",
								gtinId,
								thisId2);
							LOGGER.warn("Should be a rare case after inventory new SKU, then get order with two or more uom for that SKU");
						} else {
							LOGGER.info("Found gtin sloppy match of {} within cache. {} is an onboarding gtin", gtinId, thisId2);
						}
						return gtin2;
					}
				}
			}
			LOGGER.debug("rejected {} for cache gtin match of {}:{}/{}",
				gtin2,
				gtinId,
				inItemMaster.getItemId(),
				inUomMaster.getUomMasterId());
		}

		return null;
	}

	/**
	 * This assumes findGtinMatchOnItemMaster() was called and the result given as the matchingGtin parameter.
	 * This is just to avoid multiple identical investigations
	 */
	private Gtin findGtinMisMatchOnItemMaster(String gtinId, ItemMaster inItemMaster, UomMaster uomMaster, Gtin matchingGtin) {

		if (matchingGtin != null)
			return null;
		// if the master has a gtin for the uom, and it was not matched, it must be mismatched.
		return inItemMaster.getGtinForUom(uomMaster);
	}

	/**
	 * This is a fairly expensive check within the gtin cache.
	 * We may have longer gtins in the database.
	 */
	private Gtin findUniqueSubstringMatch(String gtinId) {
		if (gtinId == null)
			return null;
		Gtin result = null;
		int foundCount = 0;
		int gtinLength = gtinId.length();
		if (gtinLength < lkMinimalSloppyGtinMatchLength || gtinLength > lkMaximumSloppyGtinMatchLength) {
			return null;
		}

		Collection<Gtin> gtins = gtinCache.values();
		for (Gtin gtin : gtins) {
			String thisId = gtin.getDomainId();
			if (thisId.length() > gtinLength) {
				if (thisId.startsWith(gtinId) || thisId.endsWith(gtinId)) {
					result = gtin;
					foundCount++;
				}
			}
		}
		if (foundCount == 1)
			return result;
		else if (foundCount > 1) {
			LOGGER.warn("GTIN_case_7. More than one match possibility for {}. Manual cleanup may be needed", gtinId);
			return null;
		} else { // none found
			return null;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @param inOrder
	 * @param inUomMaster
	 * @param inItemMaster
	 * @return
	 */
	private OrderDetail updateOrderDetail(final OutboundOrderCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderHeader inOrder,
		final UomMaster inUomMaster,
		final ItemMaster inItemMaster) {
		OrderDetail result = null;

		//If we have an order detail ID then use that.
		String detailId = inCsvBean.getOrderDetailId();
		if (detailId == null || "".equals(detailId)) {
			// Else use the itemId + uom.
			detailId = genItemUomKey(inItemMaster, inUomMaster);
		}

		//result = inOrder.getOrderDetail(detailId);
		result = findOrderDetail(inOrder.getOrderId(), detailId, inItemMaster, inUomMaster);

		// DEV-596 if existing order detail had a preferredLocation, we need remember what it was.
		setOldPreferredLocation(null);
		String oldDetailId = null;
		if (result == null) {
			result = new OrderDetail();
			// add to cache
			Map<String, OrderDetail> lines = this.orderlineMap.get(inOrder.getOrderId());
			if (lines == null) {
				lines = new HashMap<String, OrderDetail>();
				this.orderlineMap.put(inOrder.getOrderId(), lines);
			}
			lines.put(detailId, result);
			// set order change status due to addition
			this.orderChangeMap.put(inOrder.getOrderId(), true);
		} else {
			oldDetailId = result.getDomainId();
			setOldPreferredLocation(result.getPreferredLocation());
			
			//Check if detail's ItemMaster or UOM  changed since last time
			boolean masterMatch = result.getItemMaster().equals(inItemMaster);
			boolean uomMatch = UomNormalizer.normalizedEquals(result.getUomMasterId(), inUomMaster.getDomainId());
			if (!masterMatch || !uomMatch){
				String warning = String.format("OrderDetail %s changed from %s-%s to %s-%s.", detailId, result.getItemMasterId(), result.getUomMasterId(), inItemMaster.getDomainId(), inUomMaster.getDomainId());
				LOGGER.warn(warning);
				//Find all (if any) work completed for the modified details. Create events for it.
				//The following awkward wi removal is done to avoid concurrent modification exceptions on the detail's wi list 
				while (!result.getWorkInstructions().isEmpty()){
					WorkInstruction wi = result.getWorkInstructions().get(0);
					String eventWarning = warning + " Already picked " + wi.getActualQuantity() + " items.";
					result.removeWorkInstruction(wi);
					WorkInstruction.staticGetDao().delete(wi);
					
					WorkerEvent event = new WorkerEvent(WorkerEvent.EventType.DETAIL_WI_MISMATCHED, result.getFacility(), "Order Importer", eventWarning);
					new NotificationService().saveEvent(event);
				}
			}
		}
		result.setOrderDetailId(detailId);

		// set preferred location, if valid.
		// Note: we will set to blank if the value is invalid for several reasons. The idea is to only allow good data into the system.
		// From version 12, we allow  non-modeled preferred location into the system.
		String preferredLocation = inCsvBean.getLocationId();
		if (preferredLocation != null) {
			// check that location is valid
			LocationAlias locationAlias = LocationAlias.staticGetDao().findByDomainId(inFacility, preferredLocation);
			if (locationAlias == null) {
				// LOGGER.warn("location alias not found for preferredLocation: " + inCsvBean); // not much point to this warning. Will happen all the time.
				// preferredLocation = "";
			} else {
				Location location = locationAlias.getMappedLocation();
				if (location == null) {
					LOGGER.warn("alias found, but no location for preferredLocation: " + inCsvBean); // odd case, so warn
					// preferredLocation = "";
				} else {
					if (!location.getActive()) {
						LOGGER.warn("alias and inactive location found for preferredLocation: " + inCsvBean); // odd case, so warn
						// preferredLocation = "";
					}
				}
			}
			result.setPreferredLocation(preferredLocation);
		}

		result.setStatus(OrderStatusEnum.RELEASED);
		result.setItemMaster(inItemMaster);
		result.setDescription(inCsvBean.getDescription());
		result.setUomMaster(inUomMaster);

		// calculate needs scan setting
		boolean needsScan = false;
		if (inCsvBean.getNeedsScan() != null && !StringUtils.isEmpty(inCsvBean.getNeedsScan())) {
			String needsScanStr = inCsvBean.getNeedsScan().toLowerCase();
			if ("yes".equals(needsScanStr) || "y".equals(needsScanStr) || "true".equals(needsScanStr) || "t".equals(needsScanStr)
					|| "1".equals(needsScanStr)) {
				needsScan = true;
			}
		}
		// check global scanpick setting as second option
		else if (importer.getScanPick()) {
			needsScan = true;
		}
		result.setNeedsScan(needsScan);

		String workSeq = inCsvBean.getWorkSequence();
		try {
			if (workSeq != null) {
				result.setWorkSequence(toInteger((workSeq)));
			}
		} catch (NumberFormatException e) {
			LOGGER.warn("prefered sequence could not be coerced to integer, setting to null: " + inCsvBean);
			result.setWorkSequence(null);
		}

		int quantities = 0;
		try {
			if (!Strings.isNullOrEmpty(inCsvBean.getQuantity())) {
				quantities = toInteger(inCsvBean.getQuantity());
			}
		} catch (NumberFormatException e) {
			LOGGER.warn("quantity could not be coerced to integer, setting to zero: " + inCsvBean);
		}
		if (result.getQuantity() == null || result.getQuantity() != quantities) {
			if (result.getQuantity() != null) {
				// set order change flag
				this.orderChangeMap.put(inOrder.getOrderId(), true);
				LOGGER.info("Quantity changed from " + result.getQuantity() + " to " + quantities + " for " + result.getDomainId());
			}
			result.setQuantities(quantities);
		}

		try {
			// Override the min quantity if specified - otherwise make the same as the nominal quantity.
			if (inCsvBean.getMinQuantity() != null) {
				Integer minVal = toInteger(inCsvBean.getMinQuantity());
				if (minVal > result.getQuantity())
					LOGGER.warn("minQuantity may not be higher than quantity");
				else
					result.setMinQuantity(minVal);
			}

			// Override the max quantity if specified - otherwise make the same as the nominal quantity.
			if (inCsvBean.getMaxQuantity() != null) {
				Integer maxVal = toInteger(inCsvBean.getMaxQuantity());
				if (maxVal < result.getQuantity())
					LOGGER.warn("maxQuantity may not be lower than quantity");
				else
					result.setMaxQuantity(maxVal);
			}
		} catch (NumberFormatException e) {
			LOGGER.warn("bad or missing value in min or max quantity field for " + detailId);
		}

		if (result.getQuantity() == null || result.getQuantity() <= 0) {
			result.setActive(false);
		} else {
			result.setActive(true);
		}
		result.setUpdated(inEdiProcessTime);

		// The order detail's id might have changed. Make sure the order header has it under the new id
		if (result.getParent() == null) {
			inOrder.addOrderDetail(result);
		} else {
			if (!result.getOrderDetailId().equals(oldDetailId)) {
				inOrder.removeOrderDetail(oldDetailId);
				inOrder.addOrderDetail(result);
			}
		}
		OrderDetail.staticGetDao().store(result);
		return result;
	}

	private OrderDetail findOrderDetail(String orderId, String domainId, ItemMaster item, UomMaster uom) {
		// find by domain id
		//OrderDetail domainMatch = header.getOrderDetail(domainId);
		OrderDetail domainMatch = getCachedOrderDetail(orderId, domainId);

		if (item == null) {
			return domainMatch;
		}
		// find by item master and uom
		Map<String, OrderDetail> itemMap = this.orderlineMap.get(orderId);
		if (itemMap == null) {
			return null;
		}
		Collection<OrderDetail> details = itemMap.values();
		if (details == null)
			return null;
		for (OrderDetail detail : details) {
			String examinedKey = genItemUomKey(detail.getItemMaster(), detail.getUomMaster());
			if (genItemUomKey(item, uom).equals(examinedKey) && detail.getActive()) {
				return detail;
			}
		}
		/*
		List<OrderDetail> details = header.getOrderDetails();
		String examinedKey;
		for (OrderDetail detail : details) {
			examinedKey = genItemUomKey(detail.getItemMaster(), detail.getUomMaster());
			if (genItemUomKey(item, uom).equals(examinedKey) && detail.getActive()){
				return detail;
			}
		}
		*/
		return domainMatch;
	}

	private HashMap<String, Gtin> generateGtinCache(Facility facility) {
		HashMap<String, Gtin> cache = new HashMap<>();
		HashMap<String, Object> criteriaParams = new HashMap<>();
		criteriaParams.put("facilityId", facility.getPersistentId());
		List<Gtin> gtins = Gtin.staticGetDao().findByFilter("gtinsByFacility", criteriaParams);
		for (Gtin gtin : gtins) {
			cache.put(gtin.getDomainId(), gtin);
		}
		return cache;
	}

	private OrderDetail getCachedOrderDetail(String orderId, String orderLineId) {
		Map<String, OrderDetail> orderDetails = this.orderlineMap.get(orderId);
		if (orderDetails == null)
			return null;
		return orderDetails.get(orderLineId);
	}

	private void checkForChangingFields(List<OutboundOrderCsvBean> beans) {
		HashMap<String, String[]> orders = new HashMap<>();
		String orderId, destinationId, savedDestinationId, shipperId, savedShipperId, customerId, savedCustomerId, dueDate, savedDueDate, order[];
		for (OutboundOrderCsvBean bean : beans) {
			destinationId = bean.getDestinationId();
			shipperId = bean.getShipperId();
			customerId = bean.getCustomerId();
			dueDate = bean.getDueDate();
			orderId = bean.getOrderId();
			order = orders.get(orderId);
			if (order != null) {
				savedDestinationId = order[0];
				savedShipperId = order[1];
				savedCustomerId = order[2];
				savedDueDate = order[3];
				if (!strEquals(destinationId, savedDestinationId)) {
					LOGGER.warn("Changing destinationId for order {}", orderId);
				}
				if (!strEquals(shipperId, savedShipperId)) {
					LOGGER.warn("Changing shipperId for order {}", orderId);
				}
				if (!strEquals(customerId, savedCustomerId)) {
					LOGGER.warn("Changing customerId for order {}", orderId);
				}
				if (!strEquals(dueDate, savedDueDate)) {
					LOGGER.warn("Changing dueDate for order {}", orderId);
				}
			}
			String updatedOrder[] = { destinationId, shipperId, customerId, dueDate };
			orders.put(orderId, updatedOrder);
		}
	}

	private boolean strEquals(String str1, String str2) {
		return (str1 == null ? str2 == null : str1.equals(str2));
	}

	private String genItemUomKey(ItemMaster item, UomMaster uom) {
		if (item == null) {
			return null;
		}
		return item.getDomainId() + ((uom == null || uom.getDomainId().isEmpty()) ? "" : "-" + uom.getDomainId());
	}

	public int toInteger(final String inString) {
		// Integer.valueOf will throw a NumberFormatException if anything at all is wrong with the string. 
		// Let's clean up the obvious. But this still throws NumberFormatException for something like "09x " and many other bad numbers.

		String cleanString = inString;
		cleanString = cleanString.trim();
		// would also want leading zeros removed, but may need to leave one 0 for 0000.
		cleanString = cleanString.replaceFirst("^0+(?!$)", "");
		return Integer.valueOf(cleanString);
	}

}