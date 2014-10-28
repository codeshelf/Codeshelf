/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.event.EventSeverity;
import com.gadgetworks.codeshelf.event.EventTag;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
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
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class CrossBatchCsvImporter extends CsvImporter<CrossBatchCsvBean> implements ICsvCrossBatchImporter {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(CrossBatchCsvImporter.class);

	private ITypedDao<OrderGroup>	mOrderGroupDao;
	private ITypedDao<OrderHeader>	mOrderHeaderDao;
	private ITypedDao<OrderDetail>	mOrderDetailDao;
	private ITypedDao<Container>	mContainerDao;
	private ITypedDao<ContainerUse>	mContainerUseDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public CrossBatchCsvImporter(final EventProducer inProducer, final ITypedDao<OrderGroup> inOrderGroupDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final ITypedDao<Container> inContainerDao,
		final ITypedDao<ContainerUse> inContainerUseDao,
		final ITypedDao<UomMaster> inUomMasterDao) {
		
		super(inProducer);
		mOrderGroupDao = inOrderGroupDao;
		mOrderHeaderDao = inOrderHeaderDao;
		mOrderDetailDao = inOrderDetailDao;
		mContainerDao = inContainerDao;
		mContainerUseDao = inContainerUseDao;
		mUomMasterDao = inUomMasterDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final int importCrossBatchesFromCsvStream(Reader inCsvStreamReader,
		Facility inFacility,
		Timestamp inProcessTime) {

		LOGGER.debug("Begin cross batch import.");
		mOrderGroupDao.clearAllCaches(); // avoids a class cast exception if ebeans had trimmed some objects
		List<CrossBatchCsvBean> crossBatchBeanList = toCsvBean(inCsvStreamReader, CrossBatchCsvBean.class);

		int importedRecords = 0;
		Set<String> importedContainerIds = new HashSet<String>();
		// Iterate over the put batch import beans.
		for (CrossBatchCsvBean crossBatchBean : crossBatchBeanList) {
			try {
				Container container = crossBatchCsvBeanImport(crossBatchBean, inFacility, inProcessTime);
				importedContainerIds.add(container.getContainerId());
				importedRecords++;
				produceRecordSuccessEvent(crossBatchBean);
			}
			catch(InputValidationException e) {
				produceRecordViolationEvent(EventSeverity.WARN, e, crossBatchBean);
				LOGGER.warn("Unable to process record: " + crossBatchBean, e);
			}
			catch(Exception e) {
				produceRecordViolationEvent(EventSeverity.ERROR, e, crossBatchBean);
				LOGGER.error("Unable to process record: " + crossBatchBean, e);
			}
		}

		archiveCheckCrossBatches(inFacility, inProcessTime, importedContainerIds);

		LOGGER.debug("End cross batch import.");
		return importedRecords;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckCrossBatches(final Facility inFacility,
		final Timestamp inProcessTime,
		final Set<String> inImportedContainerIds) {
		LOGGER.debug("Archive unreferenced put batch data");

		// Inactivate the WONDERWALL order detail that don't match the import timestamp.
		try {
			mOrderHeaderDao.beginTransaction();
			for (OrderHeader order : inFacility.getOrderHeaders()) {
				if (order.getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) {
					Boolean shouldArchiveOrder = true;
					// Make this more robust if the beans are not quite consistent. We do not want to totally fail out of EDI just because we cannot fully investigate
					// (I only got this by deleting containerUses table, so order.getContainerId() threw EntityNotFound)
					// By catching here and below, we accomplish two things. 1) Orders that can be archived are. 2) The batch file is processed and is not left as .FAILED
					try {
						if (!inImportedContainerIds.contains(order.getContainerId())) {
							shouldArchiveOrder = false;
						} else {
							for (OrderDetail orderDetail : order.getOrderDetails()) {
								if (orderDetail.getUpdated().equals(inProcessTime)) {
									shouldArchiveOrder = false;
								} else {
									LOGGER.debug("Archive old wonderwall order detail: " + orderDetail.getDomainId());
									orderDetail.setActive(false);
									mOrderDetailDao.store(orderDetail);
								}
							}
						}
					} catch (PersistenceException e) {
						LOGGER.error("Caught exception investigating an order in archiveCheckCrossBatches", e);
					}

					if (shouldArchiveOrder) {
						try {

							order.setActive(false);
							mOrderHeaderDao.store(order);

							ContainerUse containerUse = order.getContainerUse();
							if (containerUse != null) {
								containerUse.setActive(false);
								mContainerUseDao.store(containerUse);
							}
						} catch (PersistenceException e) {
							LOGGER.error("Caught exception archiving order or containerUse in archiveCheckCrossBatches", e);
						}

					}
				}
			}
			mOrderHeaderDao.commitTransaction();
		} finally {
			mOrderHeaderDao.endTransaction();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private Container crossBatchCsvBeanImport(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) throws InputValidationException {

		DefaultErrors errors = new DefaultErrors(inCsvBean.getClass());  
		try {
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				throw new InputValidationException(inCsvBean, errorMsg);
			} 

			mOrderHeaderDao.beginTransaction();

			LOGGER.info(inCsvBean.toString());
			try {
				int quantity = Integer.valueOf(inCsvBean.getQuantity());
				// Only create the CROSS detail if the quantity is > 0.
				if (quantity <= 0) {
					errors.minViolation("quantity", quantity, 0);
				}
			}
			catch(NumberFormatException e) {
				errors.bindViolation("quantity", inCsvBean.getQuantity(), Integer.class);
			}

			
			ItemMaster itemMaster = inFacility.getItemMaster(inCsvBean.getItemId());
			if (itemMaster == null) {
				errors.rejectValue("itemId", inCsvBean.getItemId(), ErrorCode.FIELD_REFERENCE_NOT_FOUND);
			} 
			
			if (!errors.hasErrors()) {
				OrderGroup group = updateOptionalOrderGroup(inCsvBean, inFacility, inEdiProcessTime);
				OrderHeader order = updateOrderHeader(inCsvBean, inFacility, inEdiProcessTime, group);
				UomMaster uomMaster = updateUomMaster(inCsvBean, inFacility);
				@SuppressWarnings("unused")
				OrderDetail detail = updateOrderDetail(inCsvBean,
					inFacility,
					inEdiProcessTime,
					order,
					itemMaster,
					uomMaster);
				Container container = updateContainer(inCsvBean, inFacility, inEdiProcessTime, order);
				mOrderHeaderDao.commitTransaction();
				return container;
			}
		} catch (Exception e) {
			errors.reject(ErrorCode.GENERAL, e.toString());
		} finally {
			mOrderHeaderDao.endTransaction();
		}
		throw new InputValidationException(errors);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @return
	 */
	private OrderGroup updateOptionalOrderGroup(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {
		OrderGroup result = null;

		result = inFacility.getOrderGroup(inCsvBean.getOrderGroupId());
		if ((result == null) && (inCsvBean.getOrderGroupId() != null) && (inCsvBean.getOrderGroupId().length() > 0)) {
			result = new OrderGroup();
			result.setParent(inFacility);
			result.setOrderGroupId(inCsvBean.getOrderGroupId());
			result.setDescription(OrderGroup.DEFAULT_ORDER_GROUP_DESC_PREFIX + inCsvBean.getOrderGroupId());
			inFacility.addOrderGroup(result);
		}

		if (result != null) {
			result.setStatusEnum(OrderStatusEnum.CREATED);
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
	 * @param inOrderGroup
	 * @return
	 */
	private OrderHeader updateOrderHeader(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderGroup inOrderGroup) {
		OrderHeader result = null;

		result = inFacility.getOrderHeader(OrderHeader.computeCrossOrderId(inCsvBean.getContainerId(), inEdiProcessTime));

		if (result == null) {
			result = new OrderHeader();
			result.setParent(inFacility);
			result.setDomainId(OrderHeader.computeCrossOrderId(inCsvBean.getContainerId(), inEdiProcessTime));
			result.setStatusEnum(OrderStatusEnum.CREATED);
			inFacility.addOrderHeader(result);
		}

		try {
			result.setOrderTypeEnum(OrderTypeEnum.CROSS);

			if (inOrderGroup != null) {
				inOrderGroup.addOrderHeader(result);
				result.setOrderGroup(inOrderGroup);
			}

			// It's not clear to me that this is correct.
			// Do we want the client to be able to setup put batches for a particular date?
			// Right now, OrderGroupDetail is the thing that groups put batch orders.
			result.setOrderDate(inEdiProcessTime);
			result.setDueDate(inEdiProcessTime);

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
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @param inOrder
	 * @param inUomMaster
	 * @param inItemMaster
	 * @return
	 */
	private OrderDetail updateOrderDetail(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderHeader inOrder,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) {
		OrderDetail result = null;

		result = inOrder.getOrderDetail(inCsvBean.getItemId());
		if (result == null) {
			result = new OrderDetail();
			result.setParent(inOrder);
			result.setDomainId(inCsvBean.getItemId());
			result.setStatusEnum(OrderStatusEnum.CREATED);

			inOrder.addOrderDetail(result);
		}

		result.setItemMaster(inItemMaster);
		result.setDescription(inItemMaster.getDescription());
		// In CrossBatch orders the nominal, min and max quantities are always equal on the cross order side.
		result.setQuantities(Integer.valueOf(inCsvBean.getQuantity()));

		result.setUomMaster(inUomMaster);
		result.setUpdated(inEdiProcessTime);
		if (result.getQuantity() == 0) {
			result.setActive(false);
		} else {
			result.setActive(true);
		}

		try {
			mOrderDetailDao.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
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
	private Container updateContainer(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderHeader inOrder) {
		Container result = null;

		if ((inCsvBean.getContainerId() != null) && (inCsvBean.getContainerId().length() > 0)) {
			result = inFacility.getContainer(inCsvBean.getContainerId());

			if (result == null) {
				result = new Container();
				result.setParent(inFacility);
				result.setContainerId(inCsvBean.getContainerId());
				result.setKind(inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
				inFacility.addContainer(result);
			}

			result.setUpdated(inEdiProcessTime);
			result.setActive(true);
			try {
				mContainerDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

			// Now create the container use for this.
			ContainerUse use = result.getContainerUse(inOrder);
			if (use == null) {
				use = new ContainerUse();
				use.setDomainId(inOrder.getOrderId());
				use.setOrderHeader(inOrder);
				use.setParent(result);
			}
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			use.setUsedOn(timestamp);
			use.setActive(true);
			use.setUpdated(inEdiProcessTime);

			try {
				mContainerUseDao.store(use);
				inOrder.setContainerUse(use);
				mOrderHeaderDao.store(inOrder);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
			result.addContainerUse(use);

		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inUomId
	 * @param inFacility
	 * @return
	 */
	private UomMaster updateUomMaster(final CrossBatchCsvBean inCsvBean, final Facility inFacility) {
		UomMaster result = null;

		String uomId = inCsvBean.getUom();
		result = inFacility.getUomMaster(uomId);

		if (result == null) {
			result = new UomMaster();
			result.setParent(inFacility);
			result.setUomMasterId(uomId);
			inFacility.addUomMaster(result);

			try {
				mUomMasterDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}
	

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_CROSSBATCH);
	}
}
