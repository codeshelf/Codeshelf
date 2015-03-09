/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PickStrategyEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.ItemMaster.ItemMasterDao;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.platform.caching.DomainObjectCache;
import com.codeshelf.service.PropertyService;
import com.codeshelf.util.DateTimeParser;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.InputValidationException;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OutboundOrderCsvImporter extends CsvImporter<OutboundOrderCsvBean> implements ICsvOrderImporter {

	private static final Logger		LOGGER					= LoggerFactory.getLogger(OutboundOrderCsvImporter.class);

	DateTimeParser					mDateTimeParser;

	@Getter
	@Setter
	private Boolean					locapickValue			= null;

	@Getter
	@Setter
	private String					oldPreferredLocation	= null;													// for DEV-596

	private ArrayList<Item>			mEvaluationList;
	
	DomainObjectCache<ItemMaster> itemMasterCache = new DomainObjectCache<ItemMaster>(ItemMaster.DAO);	
	
	@Inject
	public OutboundOrderCsvImporter(final EventProducer inProducer) {

		super(inProducer);

		mDateTimeParser = new DateTimeParser();

		mEvaluationList = new ArrayList<Item>();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.edi.ICsvImporter#importOrdersFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
	 */
	public final BatchResult<Object> importOrdersFromCsvStream(final Reader inCsvReader,
		final Facility inFacility,
		Timestamp inProcessTime) {
		// Get our LOCAPICK configuration value. It will not change during importing one file.
		boolean locapickValue = PropertyService.getInstance().getBooleanPropertyFromConfig(inFacility, DomainObjectProperty.LOCAPICK);
		setLocapickValue(locapickValue);
		mEvaluationList.clear();

		List<OutboundOrderCsvBean> list = toCsvBean(inCsvReader, OutboundOrderCsvBean.class);
		ArrayList<OrderHeader> orderSet = new ArrayList<OrderHeader>();
		boolean undefinedGroupUpdated = false;

		LOGGER.debug("Begin order import.");
		
		long startTime = System.currentTimeMillis();

		// cache item master
		itemMasterCache.reset();
		if (list.size()>=10) {
			// don't prefetch for small order batches
			itemMasterCache.load(inFacility);
		}
		LOGGER.info("ItemMaster cache populated with "+this.itemMasterCache.size()+" entries");
		
		int lineCount = 1;
		BatchResult<Object> batchResult = new BatchResult<Object>();
		for (OutboundOrderCsvBean orderBean : list) {
			try {
				OrderHeader order = orderCsvBeanImport(orderBean, inFacility, inProcessTime);
				if ((order != null) && (!orderSet.contains(order))) {
					orderSet.add(order);
				}
				batchResult.add(orderBean);
				produceRecordSuccessEvent(orderBean);
				if (order.getOrderGroup() == null) {
					undefinedGroupUpdated = true;
				}
			} catch (Exception e) {
				LOGGER.error("unable to import order line: " + orderBean, e);
				batchResult.addLineViolation(lineCount, orderBean, e);
			}
		}

		if (orderSet.size() == 0) {
			// Do nothing.
		} else if (orderSet.size() == 1) {
			// If we've only imported one order then don't change the status of other orders.
			archiveCheckOneOrder(orderSet.get(0), inProcessTime);
		} else {
			// If we've imported more than one order then do a full archive.
			archiveCheckAllOrders(inFacility, inProcessTime, undefinedGroupUpdated);
			long endTime = System.currentTimeMillis();
			LOGGER.info("Imported "+orderSet.size()+" orders in "+(endTime-startTime)/1000+" seconds");
		}
		archiveCheckAllContainers(inFacility, inProcessTime);
		archiveEmptyGroups(inFacility.getPersistentId());
		itemMasterCache.reset();

		LOGGER.debug("End order import.");

		cleanupArchivedOrders();
		evaluateMovedItems();
		return batchResult;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderIdList
	 * @param inProcessTime
	 */
	private void archiveCheckOneOrder(OrderHeader order, Timestamp inProcessTime) {
		for (OrderDetail orderDetail : order.getOrderDetails()) {
			if (!Objects.equal(orderDetail.getUpdated(), inProcessTime)) {
				LOGGER.trace("Archive old order detail: " + orderDetail.getOrderDetailId());
				orderDetail.setActive(false);
				//orderDetail.setQuantity(0);
				//orderDetail.setMinQuantity(0);
				//orderDetail.setMaxQuantity(0);
				OrderDetail.DAO.store(orderDetail);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckAllOrders(final Facility inFacility, final Timestamp inProcessTime, final boolean undefinedGroupUpdated) {
		LOGGER.debug("Archive unreferenced order data");
		
		// Iterate all of the order headers in this order group to see if they're still active.
		for (OrderHeader order : inFacility.getOrderHeaders()) {
			//Skip all orders from groups not updated during the current order import
			OrderGroup group = order.getOrderGroup();
			if (!shouldOldOrbersBeArchivedInThisGroup(group, inProcessTime, undefinedGroupUpdated)) {
				continue;
			}
			try {
				if (order.getOrderType().equals(OrderTypeEnum.OUTBOUND)) {
					Boolean orderHeaderIsActive = false;

					// Iterate all of the order details in this order header to see if they're still active.
					for (OrderDetail orderDetail : order.getOrderDetails()) {
						try {
							if (orderDetail.getUpdated().equals(inProcessTime)) {
								orderHeaderIsActive = true;
							} else {
								LOGGER.trace("Archive old order detail: " + orderDetail.getOrderDetailId());
								orderDetail.setActive(false);
								// orderDetail.setQuantity(0);
								// orderDetail.setMinQuantity(0);
								// orderDetail.setMaxQuantity(0);
								OrderDetail.DAO.store(orderDetail);
							}
						} catch (RuntimeException e) {
							LOGGER.warn("Unable to archive order detail: " + orderDetail, e);
							throw e;
						}
					}

					if (!orderHeaderIsActive) {
						LOGGER.trace("Archive old order header: " + order.getOrderId());
						order.setActive(false);
						OrderHeader.DAO.store(order);
					}
				}
			} catch (RuntimeException e) {
				LOGGER.warn("Unable to archive order: " + order, e);
				throw e;
			}
		}
	}
	
	/**
	 * This method looks at an OrderGroup and determines whether any old orders in it should be archived.
	 * If the group is "undefined" (null), check if the current order import process contained any orders without a group. If yes - it's OK to archive old orders
	 * If it is a regular group, check if it had been updated by the import. If yes - archive non-updated orders. If no - do not modify this group.  
	 */
	private boolean shouldOldOrbersBeArchivedInThisGroup(OrderGroup examinedGroup, Timestamp inProcessTime, boolean undefinedGroupUpdated){
		if (examinedGroup == null) {
			return undefinedGroupUpdated;
		}
		return examinedGroup.getUpdated().equals(inProcessTime);
	}
	
	private void archiveEmptyGroups(UUID inFacilityId){
		Facility facility = Facility.DAO.findByPersistentId(inFacilityId);
		for (OrderGroup group : facility.getOrderGroups()) {
			boolean archiveGroup = true;
			for (OrderHeader header : group.getOrderHeaders()) {
				if (header.getActive()) {
					archiveGroup = false;
				}
			}
			if (archiveGroup) {
				group.setActive(false);
				OrderGroup.DAO.store(group);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckAllContainers(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced container data");

		// Iterate all of the containers to see if they're still active.
		for (Container container : inFacility.getContainers()) {
			Boolean shouldInactivateContainer = true;

			for (ContainerUse containerUse : container.getUses()) {
				// ContainerUse.getOrderHeader() can be null if the EDI is not fully consistent. Don't NPE. Don't fail to archive.
				OrderHeader header = containerUse.getOrderHeader();
				if (header == null) {
					shouldInactivateContainer = false; // Should the container be inactivated? If this is the only use for the container, perhaps.
					// neglect this case for now.
					containerUse.setActive(false);
					ContainerUse.DAO.store(containerUse);
				} else if (!header.getOrderType().equals(OrderTypeEnum.OUTBOUND)) {
					shouldInactivateContainer = false;
				} else {
					if (containerUse.getUpdated().equals(inProcessTime)) {
						shouldInactivateContainer = false;
					} else {
						containerUse.setActive(false);
						ContainerUse.DAO.store(containerUse);
					}
				}
			}

			if (shouldInactivateContainer) {
				container.setActive(false);
				Container.DAO.store(container);
			}
		}

	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void cleanupArchivedOrders() {
		// ContainerUse
		// Container
		// WorkInstructions
		// OrderDetail
		// OrderHeader
	}

	private void addItemToEvaluationList(Item inItem) {
		LOGGER.info("Adding to item evaluation list: " + inItem.toLogString());

		//ArrayList allow duplicates, so check ourselves
		if (!mEvaluationList.contains(inItem))
			mEvaluationList.add(inItem);
	}

	/*
	 * Hope it is safe. Follow our parent/child hibernate pattern.
	 */
	private void safelyDeleteItem(Item inItem) {
		// for the moment, just archive it?
		//inItem.setActive(false);
		LOGGER.info("Deleting inventory item: " + inItem.toLogString());

		// item is in the parent master list and in location item lists
		ItemMaster itsMaster = inItem.getParent();
		itsMaster.removeItemFromMaster(inItem);

		Location itsLocation = inItem.getStoredLocation();
		itsLocation.removeStoredItem(inItem);

		Item.DAO.delete(inItem);
	}

	/**
	 * Helper function. Pass in values from Item that are most efficient to evaluate.
	 * Would be more direct to pass the item, but an outer loop fetches this data once for many calls.
	 */
	private boolean detailMatchesItemValues(OrderDetail inDetail,
		ItemMaster inItemsMaster,
		UomMaster inItemsUom,
		String inItemsLocAlias) {
		if (!inDetail.getActive())
			return false;
		String preferredLoc = inDetail.getPreferredLocation();
		if (preferredLoc == null || preferredLoc.isEmpty())
			return false;
		if (!inDetail.getItemMaster().equals(inItemsMaster))
			return false;
		if (!inDetail.getUomMaster().equals(inItemsUom))
			return false;
		if (preferredLoc.equals(inItemsLocAlias))
			return true;
		return false;
	}

	/**
	 * mEvaluationList has a list it items that used to be referenced by orderDetail, but the orderDetail update resulted in new item.
	 * Therefore, it is list of candidate items to delete if no other orderDetail refers to it.
	 */
	private void evaluateMovedItems() {
		if (mEvaluationList.size() == 0)
			return;

		LOGGER.warn("Querying and evaluating moved items");
		Long startTimestamp = System.currentTimeMillis();

		Integer evaluationSize = mEvaluationList.size(); // remember the starting size because we will remove part of the list later.

		// For each moved item, determine if any active order details still need to go there.
		// Let's do one query, however painful.
		List<OrderDetail> detailList = OrderDetail.DAO.getAll(); // improve! we only want active, this facility, and not complete status. findByFilter()
		/*
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("assignedChe.persistentId", inChe.getPersistentId()));
		filterParams.add(Restrictions.in("type", wiTypes));
		List<OrderDetail> detailList = OrderDetail.DAO.findByFilter(filterParams);
		*/

		// if an item is not referred to by any detail, then it is a candidate for archive or delete
		ArrayList<Item> referencedItems = new ArrayList<Item>();

		for (Item item : mEvaluationList) {
			// get the itemValues we need for the comparison
			ItemMaster itemsMaster = item.getParent();
			UomMaster itemsUom = item.getUomMaster();
			String itemsLocAlias = item.getItemLocationAlias();

			for (OrderDetail detail : detailList) {
				if (detailMatchesItemValues(detail, itemsMaster, itemsUom, itemsLocAlias)) {
					referencedItems.add(item);
					break; // break out of detail loop
				}
			}
		}

		mEvaluationList.removeAll(referencedItems);
		// Now we can delete or archive the rest
		for (Item item : mEvaluationList) {
			safelyDeleteItem(item);
		}

		//Log time if over 1 seconds
		Long queryDurationMs = System.currentTimeMillis() - startTimestamp;
		if (queryDurationMs > 300) {
			LOGGER.warn("evaluateMovedItems() took {} ms; totalItems= {};", queryDurationMs, evaluationSize);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 */
	// @Transactional
	private OrderHeader orderCsvBeanImport(final OutboundOrderCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		OrderHeader order = null;

		LOGGER.info(inCsvBean.toString());

		String errorMsg = inCsvBean.validateBean();
		if (errorMsg != null) {
			throw new InputValidationException(inCsvBean, errorMsg);
		}

		OrderGroup group = updateOptionalOrderGroup(inCsvBean, inFacility, inEdiProcessTime);
		order = updateOrderHeader(inCsvBean, inFacility, inEdiProcessTime, group);
		@SuppressWarnings("unused")
		Container container = updateContainer(inCsvBean, inFacility, inEdiProcessTime, order);
		UomMaster uomMaster = updateUomMaster(inCsvBean.getUom(), inFacility);
		String itemId = inCsvBean.getItemId();
		ItemMaster itemMaster = updateItemMaster(itemId, inCsvBean.getDescription(), inFacility, inEdiProcessTime, uomMaster);
		OrderDetail orderDetail = updateOrderDetail(inCsvBean, inFacility, inEdiProcessTime, order, uomMaster, itemMaster);

		Item updatedOrCreatedItem = null;
		// If preferredLocation is there, we set it on the detail. LOCAPICK controls whether we also create new inventory to match.
		if (getLocapickValue()) {
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
								/* Bjoern none of this is necessary. Just debug aid */

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
					updatedOrCreatedItem = importer.updateSlottedItem(false,
						itemBean,
						location,
						inEdiProcessTime,
						itemMaster,
						uomMaster);
					// if we created a new item, then throw the old item on a list for evaluation
					if (oldItem != null && !oldItem.equals(updatedOrCreatedItem))
						addItemToEvaluationList(oldItem);
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
		OrderGroup result = null;

		result = inFacility.getOrderGroup(inCsvBean.getOrderGroupId());
		if ((result == null) && (inCsvBean.getOrderGroupId() != null) && (inCsvBean.getOrderGroupId().length() > 0)) {
			result = new OrderGroup();
			result.setOrderGroupId(inCsvBean.getOrderGroupId());
			result.setDescription(OrderGroup.DEFAULT_ORDER_GROUP_DESC_PREFIX + inCsvBean.getOrderGroupId());
			result.setStatus(OrderStatusEnum.RELEASED);
			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
			inFacility.addOrderGroup(result);
			try {
				OrderGroup.DAO.store(result);
			} catch (DaoException e) {
				LOGGER.error("updateOptionalOrderGroup storing new orderGroup", e);
			}
		}

		if (result != null) {
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				OrderGroup.DAO.store(result);
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
		Container result = null;

		if ((inCsvBean.getPreAssignedContainerId() != null) && (inCsvBean.getPreAssignedContainerId().length() > 0)) {
			result = inFacility.getContainer(inCsvBean.getPreAssignedContainerId());

			if (result == null) {
				result = new Container();
				result.setContainerId(inCsvBean.getPreAssignedContainerId());
				ContainerKind kind = inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND);
				result.setKind(kind);
				inFacility.addContainer(result);
			}

			result.setUpdated(inEdiProcessTime);
			result.setActive(true);
			try {
				Container.DAO.store(result);
			} catch (DaoException e) {
				LOGGER.error("updateContainer storing Container", e);
			}

			// Now create the container use for this. ContainerUse has Container, OrderHead as potential parent.  (Che also, but not set here.)
			ContainerUse use = result.getContainerUse(inOrder);
			if (use == null) {
				use = new ContainerUse();
				use.setDomainId(inOrder.getOrderId());
				inOrder.addHeadersContainerUse(use);
				result.addContainerUse(use);
			} else {
				OrderHeader prevOrder = use.getOrderHeader();
				// No worries for container, as no way containerUse can change to different container owner.
				if (prevOrder == null)
					inOrder.addHeadersContainerUse(use);
				else if (!prevOrder.equals(inOrder)) {
					prevOrder.removeHeadersContainerUse(use);
					inOrder.addHeadersContainerUse(use);
				}
			}

			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			use.setUsedOn(timestamp);
			use.setActive(true);
			use.setUpdated(inEdiProcessTime);

			try {
				ContainerUse.DAO.store(use);
				// order-containerUse is one-to-one, so add above set a persistable field on the orderHeader
				OrderHeader.DAO.store(inOrder);
			} catch (DaoException e) {
				LOGGER.error("updateContainer storing ContainerUse", e);
			}

		}

		return result;
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

		result = inFacility.getOrderHeader(inCsvBean.getOrderId());

		if (result == null) {
			LOGGER.debug("Creating new OrderHeader instance for " + inCsvBean.getOrderId() + " , for facility: " + inFacility);
			result = new OrderHeader();
			result.setDomainId(inCsvBean.getOrderId());
			inFacility.addOrderHeader(result);
		}

		result.setOrderType(OrderTypeEnum.OUTBOUND);
		result.setStatus(OrderStatusEnum.RELEASED);
		result.setCustomerId(inCsvBean.getCustomerId());
		result.setShipperId(inCsvBean.getShipperId());

		if (inCsvBean.getOrderDate() != null) {
			try {
				Date date = mDateTimeParser.parse(inCsvBean.getDueDate());
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
				Date date = mDateTimeParser.parse(inCsvBean.getDueDate());
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
			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
			OrderHeader.DAO.store(result);
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
		ItemMaster result = null;

		// retrieve item master from cache
		result = this.itemMasterCache.get(inItemId);
	
		// create a new item, if needed
		if (result == null) {
			result = new ItemMaster();
			result.setDomainId(inItemId);
			result.setItemId(inItemId);
			inFacility.addItemMaster(result);
			this.itemMasterCache.put(result);
		}

		// If we were able to get/create an item master then update it.
		if (result != null) {
			// update only if
			result.setDescription(inDescription);
			result.setStandardUom(inUomMaster);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				ItemMaster.DAO.store(result);
			} catch (DaoException e) {
				LOGGER.error("updateItemMaster", e);
			}
		}
		return result;
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
				UomMaster.DAO.store(result);
			} catch (DaoException e) {
				LOGGER.error("updateUomMaster", e);
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
		result = findOrder(inOrder, detailId, inItemMaster, inUomMaster);
		// DEV-596 if existing order detail had a preferredLocation, we need remember what it was.
		setOldPreferredLocation(null);
		String oldDetailId = null;
		if (result == null) {
			result = new OrderDetail();
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
			LocationAlias locationAlias = inFacility.getLocationAlias(preferredLocation);
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
		result.setQuantities(quantities);

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

		result.setActive(true);
		result.setUpdated(inEdiProcessTime);

		//The order detail's id might have changed. Make sure the order header has it under the new id 
		if (result.getParent() != null) {
			inOrder.removeOrderDetail(oldDetailId);
		}
		inOrder.addOrderDetail(result);
		OrderDetail.DAO.store(result);
		return result;
	}
	
	private OrderDetail findOrder(OrderHeader header, String domainId, ItemMaster item, UomMaster uom) {
		OrderDetail domainMatch = header.getOrderDetail(domainId);
		if (item == null) {
			return domainMatch;
		}
		List<OrderDetail> details = header.getOrderDetails();
		String examinedKey;
		for (OrderDetail detail : details) {
			examinedKey = genItemUomKey(detail.getItemMaster(), detail.getUomMaster());
			if (genItemUomKey(item, uom).equals(examinedKey) && detail.getActive()){
				return detail;
			}
		}
	
		return domainMatch;
	}
	
	private String genItemUomKey(ItemMaster item, UomMaster uom) {
		if (item == null) {return null;}
		return item.getDomainId() + ((uom==null || uom.getDomainId().isEmpty())?"":"-"+uom.getDomainId());
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_OUTBOUND);
	}
}
