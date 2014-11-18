/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.event.EventTag;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.util.DateTimeParser;
import com.gadgetworks.codeshelf.validation.BatchResult;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class OutboundOrderCsvImporter extends CsvImporter<OutboundOrderCsvBean> implements ICsvOrderImporter {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(OutboundOrderCsvImporter.class);

	private ITypedDao<OrderGroup>	mOrderGroupDao;
	private ITypedDao<OrderHeader>	mOrderHeaderDao;
	private ITypedDao<OrderDetail>	mOrderDetailDao;
	private ITypedDao<Container>	mContainerDao;
	private ITypedDao<ContainerUse>	mContainerUseDao;
	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<UomMaster>	mUomMasterDao;
	DateTimeParser					mDateTimeParser;

	@Inject
	public OutboundOrderCsvImporter(final EventProducer inProducer, final ITypedDao<OrderGroup> inOrderGroupDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final ITypedDao<Container> inContainerDao,
		final ITypedDao<ContainerUse> inContainerUseDao,
		final ITypedDao<ItemMaster> inItemMasterDao,
		final ITypedDao<UomMaster> inUomMaster) {

		super(inProducer);

		mOrderGroupDao = inOrderGroupDao;
		mOrderHeaderDao = inOrderHeaderDao;
		mOrderDetailDao = inOrderDetailDao;
		mContainerDao = inContainerDao;
		mContainerUseDao = inContainerUseDao;
		mItemMasterDao = inItemMasterDao;
		mUomMasterDao = inUomMaster;
		mDateTimeParser = new DateTimeParser();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importOrdersFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final BatchResult<Object> importOrdersFromCsvStream(final Reader inCsvReader,
		final Facility inFacility,
		Timestamp inProcessTime) {
		mOrderGroupDao.clearAllCaches();
		List<OutboundOrderCsvBean> list = toCsvBean(inCsvReader, OutboundOrderCsvBean.class);
/*
		try (CSVReader csvReader = new CSVReader(inCsvStreamReader);) {
			HeaderColumnNameMappingStrategy<OutboundOrderCsvBean> strategy = new HeaderColumnNameMappingStrategy<OutboundOrderCsvBean>();
			strategy.setType(OutboundOrderCsvBean.class);

			CsvToBean<OutboundOrderCsvBean> csv = new CsvToBean<OutboundOrderCsvBean>();
			List<OutboundOrderCsvBean> list = csv.parse(strategy, csvReader);

			List<OrderHeader> orderList = new ArrayList<OrderHeader>();

			LOGGER.debug("Begin order import.");
			BatchResult<Object> batchResult = new BatchResult<Object>();
			int lineCount = 1;
			for (OutboundOrderCsvBean orderBean : list) {
				String errorMsg = orderBean.validateBean();
				if (errorMsg != null) {
					LOGGER.error("Import errors: " + errorMsg);
					batchResult.addLineViolation(lineCount, orderBean, errorMsg);
				} else {
					try {
						OrderHeader order = orderCsvBeanImport(orderBean, inFacility, inProcessTime);
						if ((order != null) && (!orderList.contains(order))) {
							orderList.add(order);
						}
						batchResult.add(orderBean);
					} catch (Exception e) {
						batchResult.addLineViolation(lineCount, orderBean, e);
					}
*/
		Set<OrderHeader> orderSet = Sets.newHashSet();

		LOGGER.debug("Begin order import.");
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
			} catch(Exception e) {
				LOGGER.error("unable to import order line: " + orderBean, e);
				batchResult.addLineViolation(lineCount, orderBean, e);
			}
		}

		if (orderSet.size() == 0) {
			// Do nothing.
		} else if (orderSet.size() == 1) {
			// If we've only imported one order then don't change the status of other orders.
			archiveCheckOneOrder(inFacility, orderSet, inProcessTime);
		} else {
			// If we've imported more than one order then do a full archive.
			archiveCheckAllOrders(inFacility, inProcessTime);
		}
		archiveCheckAllContainers(inFacility, inProcessTime);

		LOGGER.debug("End order import.");

		cleanupArchivedOrders();
		return batchResult;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inOrderIdList
	 * @param inProcessTime
	 */
	private void archiveCheckOneOrder(final Facility inFacility, final Collection<OrderHeader> inOrderList, final Timestamp inProcessTime) {
		for (OrderHeader order : inOrderList) {
			for (OrderDetail orderDetail : order.getOrderDetails()) {
				if (!Objects.equal(orderDetail.getUpdated(), inProcessTime)) {
					LOGGER.trace("Archive old order detail: " + orderDetail.getOrderDetailId());
					orderDetail.setActive(false);
					//orderDetail.setQuantity(0);
					//orderDetail.setMinQuantity(0);
					//orderDetail.setMaxQuantity(0);
					mOrderDetailDao.store(orderDetail);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckAllOrders(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced order data");

		// Inactivate the *order details* that don't match the import timestamp.
		// All orders and related items get marked with the same timestamp when imported from the same interchange.
		try {
			//mOrderGroupDao.beginTransaction();

			// Iterate all of the order groups to see if they're still active.
			for (OrderGroup group : inFacility.getOrderGroups()) {
				if (!group.getUpdated().equals(inProcessTime)) {
					LOGGER.trace("Archive old order group: " + group.getOrderGroupId());
					group.setActive(false);
					mOrderGroupDao.store(group);
				}
			}

			// Iterate all of the order headers in this order group to see if they're still active.
			for (OrderHeader order : inFacility.getOrderHeaders()) {
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
									mOrderDetailDao.store(orderDetail);
								}
							} catch (RuntimeException e) {
								LOGGER.warn("Unable to archive order detail: " + orderDetail, e);
								throw e;
							}
						}

						if (!orderHeaderIsActive) {
							LOGGER.trace("Archive old order header: " + order.getOrderId());
							order.setActive(false);
							mOrderHeaderDao.store(order);
						}
					}
				} catch (RuntimeException e) {
					LOGGER.warn("Unable to archive order: " + order, e);
					throw e;
				}
			}

			//mOrderGroupDao.commitTransaction();
		} finally {
			//mOrderGroupDao.endTransaction();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckAllContainers(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced container data");

		try {
			//mContainerDao.beginTransaction();

			// Iterate all of the containers to see if they're still active.
			for (Container container : inFacility.getContainers()) {
				Boolean shouldInactivateContainer = true;

				for (ContainerUse containerUse : container.getUses()) {
					if (!containerUse.getOrderHeader().getOrderType().equals(OrderTypeEnum.OUTBOUND)) {
						shouldInactivateContainer = false;
					} else {
						if (containerUse.getUpdated().equals(inProcessTime)) {
							shouldInactivateContainer = false;
						} else {
							containerUse.setActive(false);
							mContainerUseDao.store(containerUse);
						}
					}
				}

				if (shouldInactivateContainer) {
					container.setActive(false);
					mContainerDao.store(container);
				}
			}

			//mContainerDao.commitTransaction();
		} finally {
			//mContainerDao.endTransaction();
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

		try {
			//mOrderHeaderDao.beginTransaction();
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				throw new InputValidationException(inCsvBean, errorMsg);
			} 

			OrderGroup group = updateOptionalOrderGroup(inCsvBean, inFacility, inEdiProcessTime);
			order = updateOrderHeader(inCsvBean, inFacility, inEdiProcessTime, group);
			@SuppressWarnings("unused")
			Container container = updateContainer(inCsvBean, inFacility, inEdiProcessTime, order);
			UomMaster uomMaster = updateUomMaster(inCsvBean.getUom(), inFacility);
			ItemMaster itemMaster = updateItemMaster(inCsvBean.getItemId(),
				inCsvBean.getDescription(),
				inFacility,
				inEdiProcessTime,
				uomMaster);
			@SuppressWarnings("unused")
			OrderDetail orderDetail = updateOrderDetail(inCsvBean, inFacility, inEdiProcessTime, order, uomMaster, itemMaster);
			//mOrderHeaderDao.commitTransaction();
		} finally {
			//mOrderHeaderDao.endTransaction();
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
			result.setStatus(OrderStatusEnum.CREATED);
			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
			inFacility.addOrderGroup(result);
			try {
				mOrderGroupDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		if (result != null) {
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mOrderGroupDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
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
				mContainerDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
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
				else if (!prevOrder.equals(inOrder)){
					prevOrder.removeHeadersContainerUse(use);
					inOrder.addHeadersContainerUse(use);					
				}
			}			

			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			use.setUsedOn(timestamp);
			use.setActive(true);
			use.setUpdated(inEdiProcessTime);

			try {
				mContainerUseDao.store(use);
				// order-containerUse is one-to-one, so add above set a persistable field on the orderHeader
				mOrderHeaderDao.store(inOrder);
			} catch (DaoException e) {
				LOGGER.error("", e);
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
		result.setStatus(OrderStatusEnum.CREATED);
		result.setCustomerId(inCsvBean.getCustomerId());
		result.setShipmentId(inCsvBean.getShipmentId());
		if (inCsvBean.getWorkSequence() != null) {
			result.setWorkSequence(Integer.valueOf(inCsvBean.getWorkSequence()));
		}

		if (inCsvBean.getOrderDate() != null) {
			try {
				Date date = mDateTimeParser.parse(inCsvBean.getDueDate());
				// date may be null if string was blank
				if (date != null)
					result.setOrderDate(new Timestamp(date.getTime()));
				// Note: on an update, cannot clear a previous set time back to null. Could do it, just haven't bothered here.
				// Mandatory field?

			} catch (IllegalArgumentException e1) {
				LOGGER.error("", e1);
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
				LOGGER.error("", e1);
			}
		}

		PickStrategyEnum pickStrategy = PickStrategyEnum.SERIAL;
		String pickStrategyEnumId = inCsvBean.getPickStrategy();
		if ((pickStrategyEnumId != null) && (pickStrategyEnumId.length() > 0)) {
			pickStrategy = PickStrategyEnum.valueOf(pickStrategyEnumId);
		}
		result.setPickStrategy(pickStrategy);
		if (inOrderGroup != null) {
			inOrderGroup.addOrderHeader(result);
			result.setOrderGroup(inOrderGroup);
		}
		try {
			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
			mOrderHeaderDao.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
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

		result = mItemMasterDao.findByDomainId(inFacility, inItemId);
		if (result == null) {
			result = new ItemMaster();
			result.setDomainId(inItemId);
			result.setItemId(inItemId);
			inFacility.addItemMaster(result);
		}

		// If we were able to get/create an item master then update it.
		if (result != null) {
			result.setDescription(inDescription);
			result.setStandardUom(inUomMaster);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mItemMasterDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
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
				mUomMasterDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
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

		String detailId = "";
		if (inCsvBean.getOrderDetailId() != null) {
			// If we have an order detail ID then use that.
			detailId = inCsvBean.getOrderDetailId();
		} else {
			// Else use the item ID.
			detailId = inItemMaster.getItemId();
		}

		result = inOrder.getOrderDetail(detailId);
		if (result == null) {
			result = new OrderDetail();
			result.setOrderDetailId(detailId);
		}

		result.setStatus(OrderStatusEnum.CREATED);
		result.setItemMaster(inItemMaster);
		result.setDescription(inCsvBean.getDescription());
		result.setUomMaster(inUomMaster);
		result.setQuantities(Integer.valueOf(inCsvBean.getQuantity()));

		try {
			// Override the min quantity if specified - otherwise make the same as the nominal quantity.
			if (inCsvBean.getMinQuantity() != null) {
				Integer minVal = Integer.valueOf(inCsvBean.getMinQuantity());
				if (minVal > result.getQuantity())
					LOGGER.warn("minQuantity may not be higher than quantity");
				else
					result.setMinQuantity(minVal);
			}

			// Override the max quantity if specified - otherwise make the same as the nominal quantity.
			if (inCsvBean.getMaxQuantity() != null) {
				Integer maxVal = Integer.valueOf(inCsvBean.getMaxQuantity());
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

		if (result.getParent() == null) {
			inOrder.addOrderDetail(result);
		}
		mOrderDetailDao.store(result);
		return result;
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_OUTBOUND);
	}
}
