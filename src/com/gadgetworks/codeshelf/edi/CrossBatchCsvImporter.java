/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameMappingStrategy;

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
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class CrossBatchCsvImporter implements ICsvCrossBatchImporter {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<OrderGroup>	mOrderGroupDao;
	private ITypedDao<OrderHeader>	mOrderHeaderDao;
	private ITypedDao<OrderDetail>	mOrderDetailDao;
	private ITypedDao<Container>	mContainerDao;
	private ITypedDao<ContainerUse>	mContainerUseDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public CrossBatchCsvImporter(final ITypedDao<OrderGroup> inOrderGroupDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final ITypedDao<Container> inContainerDao,
		final ITypedDao<ContainerUse> inContainerUseDao,
		final ITypedDao<UomMaster> inUomMasterDao) {

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
	public final void importCrossBatchesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<CrossBatchCsvBean> strategy = new HeaderColumnNameMappingStrategy<CrossBatchCsvBean>();
			strategy.setType(CrossBatchCsvBean.class);

			CsvToBean<CrossBatchCsvBean> csv = new CsvToBean<CrossBatchCsvBean>();
			List<CrossBatchCsvBean> crossBatchBeanList = csv.parse(strategy, csvReader);

			if (crossBatchBeanList.size() > 0) {

				LOGGER.debug("Begin cross batch import.");

				// Iterate over the put batch import beans.
				for (CrossBatchCsvBean crossBatchBean : crossBatchBeanList) {
					String errorMsg = crossBatchBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Cross-batch: import errors: " + errorMsg);
					} else {
						crossBatchCsvBeanImport(crossBatchBean, inFacility, inProcessTime);
					}
				}

				archiveCheckCrossBatches(inFacility, inProcessTime);

				LOGGER.debug("End cross batch import.");
			}

			csvReader.close();
		} catch (FileNotFoundException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckCrossBatches(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced put batch data");

		// Inactivate the WONDERWALL order detail that don't match the import timestamp.
		try {
			mOrderHeaderDao.beginTransaction();
			for (OrderHeader order : inFacility.getOrderHeaders()) {
				if (order.getOrderTypeEnum().equals(OrderTypeEnum.CROSS)) {
					Boolean shouldArchiveOrder = true;
					for (OrderDetail orderDetail : order.getOrderDetails()) {
						if (orderDetail.getUpdated().equals(inProcessTime)) {
							shouldArchiveOrder = false;
						} else {
							LOGGER.debug("Archive old wonderwall order detail: " + orderDetail.getDomainId());
							orderDetail.setActive(false);
							orderDetail.setQuantity(0);
							mOrderDetailDao.store(orderDetail);
						}
					}
					
					if (shouldArchiveOrder) {
						order.setActive(false);
						mOrderHeaderDao.store(order);
						
						ContainerUse containerUse = order.getContainerUse();
						if (containerUse != null) {
							containerUse.setActive(false);
							mContainerUseDao.store(containerUse);
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
	private void crossBatchCsvBeanImport(final CrossBatchCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mOrderHeaderDao.beginTransaction();

			LOGGER.info(inCsvBean.toString());

			try {
				ItemMaster itemMaster = inFacility.getItemMaster(inCsvBean.getItemId());

				if (itemMaster == null) {
					LOGGER.error("Cross-batch import: unknown item master sent.");
				} else {
					OrderGroup group = updateOptionalOrderGroup(inCsvBean, inFacility, inEdiProcessTime);
					OrderHeader order = updateOrderHeader(inCsvBean, inFacility, inEdiProcessTime, group);
					Container container = updateContainer(inCsvBean, inFacility, inEdiProcessTime, order);
					UomMaster uomMaster = updateUomMaster(inCsvBean, inFacility);
					OrderDetail detail = updateOrderDetail(inCsvBean,
						inFacility,
						inEdiProcessTime,
						order,
						itemMaster,
						container,
						uomMaster);
				}
			} catch (Exception e) {
				LOGGER.error("", e);
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
		final Container inContainer,
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
		result.setQuantity(Integer.valueOf(inCsvBean.getQuantity()));
		result.setUomMaster(inUomMaster);

		try {
			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
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
}
