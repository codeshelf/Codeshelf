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
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.util.DateTimeParser;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.InputValidationException;
import com.google.common.base.Strings;

public class OutboundOrderBatchProcessor implements Runnable {

	private static final Logger LOGGER	= LoggerFactory.getLogger(OutboundOrderBatchProcessor.class);

	private OutboundOrderPrefetchCsvImporter importer;

	DateTimeParser dateTimeParser;

	@Getter
	private Timestamp	processTime;

	@Getter @Setter
	private String oldPreferredLocation	= null;
	
	@Getter
	BatchResult<Object> batchResult = new BatchResult<Object>();
		
	private Facility facility;

	private int	processorId;
	
	Map<String,OrderHeader> orderMap;
	
	DomainObjectCache<ItemMaster> itemMasterCache = null;
	DomainObjectCache<OrderHeader> orderHeaderCache = null;
	
	private HashMap<String, Map<String,OrderDetail>> orderlineMap;
	private HashMap<String, Container> containerMap;
	private HashMap<String, ContainerUse> containerUseMap;
	
	long startTime;
	long endTime;
	int numOrders; 
	int numLines;
	
	@Getter @Setter
	int maxProcessingAttempts = 10;

	Map<String,Boolean> orderChangeMap;
	
	public OutboundOrderBatchProcessor(int procId, OutboundOrderPrefetchCsvImporter importer, Timestamp processTime, Facility facility) {
		this.importer = importer;
		this.processTime = processTime;
		this.facility = facility;
		this.processorId = procId;
		this.dateTimeParser = new DateTimeParser();
	}
	
	@Override
	public void run() {		
		OutboundOrderBatch batch = importer.getBatchQueue().poll();
		while (batch!=null) {
			// process batch
			batch.setProcessingAttempts(batch.getProcessingAttempts()+1);
			LOGGER.info("Worker #"+processorId+" is processing "+batch+" in "+batch.getProcessingAttempts()+". attempt");
			
			try {
				TenantPersistenceService.getInstance().beginTransaction();	
				
				// attach facility to new session
				Facility facility = Facility.staticGetDao().reload(this.facility);
				
				itemMasterCache = new DomainObjectCache<ItemMaster>(ItemMaster.staticGetDao());
				orderHeaderCache = new DomainObjectCache<OrderHeader>(OrderHeader.staticGetDao());

				BatchResult<Object> batchResult = new BatchResult<Object>();
				ArrayList<OrderHeader> orderSet = new ArrayList<OrderHeader>();

				LOGGER.debug("Begin order import.");
				
				this.startTime = System.currentTimeMillis();
				LOGGER.info(batch.getItemIds().size()+" distinct items found in batch");
				
				// cache item master
				itemMasterCache.reset();
				itemMasterCache.setFetchOnMiss(false);
				itemMasterCache.load(facility,batch.getItemIds());		
				LOGGER.info("ItemMaster cache populated with "+this.itemMasterCache.size()+" entries");
		
				// cache order headers
				orderHeaderCache.reset();
				orderHeaderCache.setFetchOnMiss(false);
				orderHeaderCache.load(facility,batch.getOrderIds());		
				LOGGER.info("OrderHeader cache populated with "+this.orderHeaderCache.size()+" entries");

				// prefetch order details already associated with orders
				this.orderChangeMap = new HashMap<String,Boolean>();
				this.orderlineMap = new HashMap<String, Map<String,OrderDetail>>();
				if (orderHeaderCache.size()==0) {
					LOGGER.info("Skipping order line loading, since all orders are new.");
				}
				else {
					LOGGER.info("Loading line items for "+orderHeaderCache.size()+" orders.");
					Criteria criteria = OrderDetail.staticGetDao().createCriteria();
					criteria.add(Restrictions.in("parent", orderHeaderCache.getAll()));
					List<OrderDetail> orderDetails = OrderDetail.staticGetDao().findByCriteriaQuery(criteria);
					for (OrderDetail line : orderDetails) {
						Map<String,OrderDetail> l = this.orderlineMap.get(line.getOrderId());
						if (l==null) {
							l = new HashMap<String,OrderDetail>();
							this.orderlineMap.put(line.getOrderId(), l);
							this.orderChangeMap.put(line.getOrderId(), false);
						}
						l.put(line.getOrderDetailId(),line);
					}
				}
				
				// prefetch container uses already associated with orders
				this.containerUseMap = new HashMap<String, ContainerUse>();
				if (orderHeaderCache.size()==0) {
					LOGGER.info("Skipping container use loading, since all orders are new.");
				}
				else {
					LOGGER.info("Loading container use for "+orderHeaderCache.size()+" orders.");
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
				int lineCount = 1;
				int count = 1, size = lines.size();
				for (OutboundOrderCsvBean orderBean : lines) {
					try {
						OrderHeader order = orderCsvBeanImport(orderBean, facility, processTime, count++ + "/" + size);
						if ((order != null) && (!orderSet.contains(order))) {
							orderSet.add(order);
						}
						batchResult.add(orderBean);
						importer.produceRecordSuccessEvent(orderBean);
					} catch (Exception e) {
						LOGGER.error("unable to import order line: " + orderBean, e);
						batchResult.addLineViolation(lineCount, orderBean, e);
					}
				}
				
				// init empty order map
				Map<String,Boolean> isEmptyOrder = new HashMap<String,Boolean>();
				for (OrderHeader order : this.orderHeaderCache.getAll()) {
					isEmptyOrder.put(order.getOrderId(), true);
				}
				
				// loop through order items and deactivate items that have not been touched by this batch
				// also set empty order flag to false, if order has non-zero item quantities
				Collection<Map<String, OrderDetail>> allOrderLines = orderlineMap.values();
				for (Map<String, OrderDetail> orderlines : allOrderLines) {
					for (OrderDetail line : orderlines.values()) {
						// deactivate out-dated items
						if (line.getActive()==true && !line.getUpdated().equals(processTime)) {
							line.setActive(false);
							OrderDetail.staticGetDao().store(line);
							LOGGER.info("Deactivating order item "+line);
							this.orderChangeMap.put(line.getOrderId(),true);
						}
						// reset empty order flag
						if (line.getActive() && line.getQuantity()!=null && line.getQuantity()>0) {
							isEmptyOrder.put(line.getOrderId(), false);					
						}
					}
				}
				
				// reactivate changed orders
				for (Entry<String, Boolean> e : this.orderChangeMap.entrySet()) {
					if (e.getValue()) {
						OrderHeader order = this.orderHeaderCache.get(e.getKey());
						if (!isEmptyOrder.get(order.getOrderId())) {
							LOGGER.info("Order "+order+" changed during import");
							if (!order.getActive() || order.getStatus()!=OrderStatusEnum.RELEASED) {
								LOGGER.info("Order "+order+" reactivated. Status set to 'released'.");							
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
					if (isEmptyOrder.get(order.getOrderId())==true) {
						LOGGER.info("Deactivating empty order "+order);
						order.setActive(false);
						OrderHeader.staticGetDao().store(order);
					}
				}
				
				// loop through container uses and deactivate uses that have not been touched by this batch
				Collection<ContainerUse> allUses = this.containerUseMap.values();
				for (ContainerUse use : allUses) {
					if (use.getActive()==true && !use.getUpdated().equals(processTime)) {
						// use is out-dated -> deactivate
						use.setActive(false);
						ContainerUse.staticGetDao().store(use);
						LOGGER.info("Deactivating container use "+use);
					}
				}
				this.numOrders = allOrderLines.size();				
				TenantPersistenceService.getInstance().commitTransaction();
				LOGGER.info("Completed processing "+batch);
				this.importer.batchResult.merge(batchResult);
			}
			catch (StaleObjectStateException e) {
				LOGGER.warn("Failed to process order batch "+batch+" due to stale data.",e);
				if (batch.getProcessingAttempts()>this.maxProcessingAttempts) {
					LOGGER.error("Giving up on processing order batch "+batch+".  Retry import at a later time.");
				}
				else {
					// returning batch to the queue to give it another chance
					LOGGER.warn("Returning batch "+batch+" to import queue to retry.");
					importer.getBatchQueue().add(batch);
				}
				TenantPersistenceService.getInstance().rollbackTransaction();				
			}
			catch (Exception e) {
				LOGGER.info("Failed to process "+batch,e);
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

		String errorMsg = inCsvBean.validateBean();
		if (errorMsg != null) {
			throw new InputValidationException(inCsvBean, errorMsg);
		}

		OrderGroup group = updateOptionalOrderGroup(inCsvBean, inFacility, inEdiProcessTime);
		order = updateOrderHeader(inCsvBean, inFacility, inEdiProcessTime, group);
		updateContainer(inCsvBean, inFacility, inEdiProcessTime, order);
		UomMaster uomMaster = updateUomMaster(inCsvBean.getUom(), inFacility);
		String itemId = inCsvBean.getItemId();
		ItemMaster itemMaster = updateItemMaster(itemId, inCsvBean.getDescription(), inFacility, inEdiProcessTime, uomMaster);
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
								if (locItems.size() == 1){
									Item fromMasterItems = masterItems.get(0);
									LOGGER.error("fromLocItems: " + fromMasterItems.toLogString());
								}
								oldItem = itemMaster.getActiveItemMatchingLocUom(oldLocation, uomMaster); // just to step in again to see the uomMaster is not loaded						
							}
						}
					}

					// updateSlottedItem is going to make new inventory if location changed for cases, and also for each if EACHMULT is true
					importer.updateSlottedItem(false,
						itemBean,
						location,
						inEdiProcessTime,
						itemMaster,
						uomMaster);
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
		if (orderGroupId==null || orderGroupId.length()==0) {
			// order group in undefined
			return null;
		}
		OrderGroup result = inFacility.getOrderGroup(inCsvBean.getOrderGroupId());
		if ((result == null) ) {
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
				}
				else if (!prevOrder.equals(inOrder)) {
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

		if (inCsvBean.getOrderDate() != null) {
			try {
				Date date = dateTimeParser.parse(inCsvBean.getDueDate());
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
	 * @param inItemId
	 * @param inDescription
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @param inUomMaster
	 * @return
	 */
	private ItemMaster updateItemMaster(final String inItemId,
		final String inDescription,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final UomMaster inUomMaster) {
		ItemMaster itemMaster = null;

		// retrieve item master from cache
		itemMaster = this.itemMasterCache.get(inItemId);
	
		// create a new item, if needed
		if (itemMaster == null) {
			itemMaster = new ItemMaster();
			itemMaster.setDomainId(inItemId);
			itemMaster.setItemId(inItemId);
			itemMaster.setParent(inFacility);
			this.itemMasterCache.put(itemMaster);
		}

		// update only if modified or new
		if (itemMaster.getDescription()!=null && itemMaster.getDescription().equals(inDescription) &&
			itemMaster.getStandardUom().equals(inUomMaster) &&
			itemMaster.getActive()!=null && itemMaster.getActive()==true && 
			itemMaster.getActive()!=null && itemMaster.getUpdated()==inEdiProcessTime) {
			// already up to date
		}
		else {
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
	 * @param inUomId
	 * @param inFacility
	 * @return
	 */
	private UomMaster updateUomMaster(final String inUomId, final Facility inFacility) {
		UomMaster result = null;

		result = inFacility.getUomMaster(inUomId);

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

	private Gtin upsertGtin(final Facility inFacility, final ItemMaster inItemMaster, 
		final OutboundOrderCsvBean inCsvBean, UomMaster uomMaster) {
		
		if (inCsvBean.getGtin() == null || inCsvBean.getGtin().isEmpty()) {
			return null;
		}
		
		Gtin result = Gtin.staticGetDao().findByDomainId(null, inCsvBean.getGtin());
		ItemMaster previousItemMaster = null;
		
		if (result != null){
			previousItemMaster = result.getParent();
			
			// Check if existing GTIN is associated with the ItemMaster
			if (previousItemMaster.equals(inItemMaster)) {
				
				// Check if the UOM specified in the inventory file matches UOM of existing GTIN
				if (!uomMaster.equals(result.getUomMaster())) {
					LOGGER.warn("UOM specified in order line {} conflicts with UOM of specified existing GTIN {}." +
							" Did not change UOM for existing GTIN.", inCsvBean.toString(), result.getDomainId());
					
					return null;
				}
				
			} else {
				
				// Import line is attempting to associate existing GTIN with a new item. We do not allow this.
				LOGGER.warn("GTIN {} already exists and is associated with item {}." + 
						" GTIN will remain associated with item {}.", result.getDomainId(), result.getParent().getDomainId(),
						result.getParent().getDomainId());
				
				return null;
			}
		} else {
			
			result = inItemMaster.createGtin(inCsvBean.getGtin(), uomMaster);

			try {
				Gtin.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("upsertGtinMap save", e);
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
			Map<String,OrderDetail> lines = this.orderlineMap.get(inOrder.getOrderId());
			if (lines==null) {
				lines = new HashMap<String,OrderDetail>();
				this.orderlineMap.put(inOrder.getOrderId(), lines);
			}
			lines.put(detailId,result);
			// set order change status due to addition
			this.orderChangeMap.put(inOrder.getOrderId(),true);
		} else {
			oldDetailId = result.getDomainId();
			setOldPreferredLocation(result.getPreferredLocation());
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
		if (inCsvBean.getNeedsScan()!=null) {
			String needsScanStr = inCsvBean.getNeedsScan().toLowerCase();
			if ("yes".equals(needsScanStr)||"y".equals(needsScanStr)||"true".equals(needsScanStr)||"t".equals(needsScanStr)||"1".equals(needsScanStr)) {
				needsScan = true;
			}
		}
		else {
			// check global scanpick setting
			if (importer.getScanPick()) {
				 needsScan = true;
			}
		}
		result.setNeedsScan(needsScan);
		
		String workSeq = inCsvBean.getWorkSequence();
		try  {
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
		if (result.getQuantity()==null || result.getQuantity()!=quantities) {
			if (result.getQuantity()!=null) {
				// set order change flag
				this.orderChangeMap.put(inOrder.getOrderId(), true);
				LOGGER.info("Quantity changed from "+result.getQuantity()+" to "+quantities+" for "+result.getDomainId());
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

		if (result.getQuantity()==null || result.getQuantity()<=0) {
			result.setActive(false);			
		}
		else {
			result.setActive(true);
		}
		result.setUpdated(inEdiProcessTime);

		// The order detail's id might have changed. Make sure the order header has it under the new id
		if (result.getParent()==null) {
			inOrder.addOrderDetail(result);			
		}
		else {
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
		OrderDetail domainMatch = getCachedOrderDetail(orderId,domainId);

		if (item == null) {
			return domainMatch;
		}
		// find by item master and uom
		Map<String, OrderDetail> itemMap = this.orderlineMap.get(orderId);
		if (itemMap==null) {
			return null;
		}
		Collection<OrderDetail> details = itemMap.values();
		if (details==null) return null;
		for (OrderDetail detail : details) {
			String examinedKey = genItemUomKey(detail.getItemMaster(), detail.getUomMaster());
			if (genItemUomKey(item, uom).equals(examinedKey) && detail.getActive()){
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
	
	private OrderDetail getCachedOrderDetail(String orderId, String orderLineId) {
		Map<String, OrderDetail> orderDetails = this.orderlineMap.get(orderId);
		if (orderDetails==null) return null;
		return orderDetails.get(orderLineId);
	}
	
	private String genItemUomKey(ItemMaster item, UomMaster uom) {
		if (item == null) {return null;}
		return item.getDomainId() + ((uom==null || uom.getDomainId().isEmpty())?"":"-"+uom.getDomainId());
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
