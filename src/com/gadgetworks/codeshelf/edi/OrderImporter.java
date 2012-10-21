/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporter.java,v 1.4 2012/10/21 02:02:18 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameMappingStrategy;

import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class OrderImporter implements IOrderImporter {

	private static final Log		LOGGER	= LogFactory.getLog(EdiProcessor.class);

	private ITypedDao<OrderGroup>	mOrderGroupDao;
	private ITypedDao<OrderHeader>	mOrderHeaderDao;
	private ITypedDao<OrderDetail>	mOrderDetailDao;
	private ITypedDao<Container>	mContainerDao;
	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public OrderImporter(final ITypedDao<OrderGroup> inOrderGroupDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final ITypedDao<Container> inContainerDao,
		final ITypedDao<ItemMaster> inItemMasterDao,
		final ITypedDao<UomMaster> inUomMasterDao) {

		mOrderGroupDao = inOrderGroupDao;
		mOrderHeaderDao = inOrderHeaderDao;
		mOrderDetailDao = inOrderDetailDao;
		mContainerDao = inContainerDao;
		mItemMasterDao = inItemMasterDao;
		mUomMasterDao = inUomMasterDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.IOrderImporter#importerFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final void importerFromCsvStream(final InputStreamReader inStreamReader, final Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inStreamReader);

			HeaderColumnNameMappingStrategy<CsvOrderImportBean> strategy = new HeaderColumnNameMappingStrategy<CsvOrderImportBean>();
			strategy.setType(CsvOrderImportBean.class);

			CsvToBean<CsvOrderImportBean> csv = new CsvToBean<CsvOrderImportBean>();
			List<CsvOrderImportBean> list = csv.parse(strategy, csvReader);

			for (CsvOrderImportBean importBean : list) {
				importCsvBean(importBean, inFacility);
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
	 * @param inCsvImportBean
	 * @param inFacility
	 */
	private void importCsvBean(final CsvOrderImportBean inCsvImportBean, final Facility inFacility) {

		LOGGER.info(inCsvImportBean);

		OrderGroup group = ensureOptionalOrderGroup(inCsvImportBean, inFacility);
		OrderHeader order = ensureOrderHeader(inCsvImportBean, inFacility, group);
		Container container = ensureContainer(inCsvImportBean, inFacility, order);
		UomMaster uomMaster = ensureUomMaster(inCsvImportBean, inFacility);
		ItemMaster itemMaster = ensureItemMaster(inCsvImportBean, inFacility, uomMaster);
		OrderDetail orderDetail = ensureOrderDetail(inCsvImportBean, inFacility, order, uomMaster, itemMaster);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @return
	 */
	private OrderGroup ensureOptionalOrderGroup(final CsvOrderImportBean inCsvImportBean, final Facility inFacility) {
		OrderGroup result = null;

		result = inFacility.findOrderGroup(inCsvImportBean.getOrderGroupId());
		if ((result == null) && (inCsvImportBean.getOrderGroupId() != null) && (inCsvImportBean.getOrderGroupId().length() > 0)) {
			result = new OrderGroup();
			result.setParentFacility(inFacility);
			result.setOrderGroupId(inCsvImportBean.getOrderGroupId());
			result.setStatusEnum(OrderStatusEnum.CREATED);
			inFacility.addOrderGroup(result);
			try {
				mOrderGroupDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inOrder
	 * @return
	 */
	private Container ensureContainer(final CsvOrderImportBean inCsvImportBean, final Facility inFacility, final OrderHeader inOrder) {
		Container result = null;

		if ((inCsvImportBean.getPreAssignedContainerId() != null) && (inCsvImportBean.getPreAssignedContainerId().length() > 0)) {
			result = inFacility.getContainer(inCsvImportBean.getPreAssignedContainerId());

			if (result == null) {
				result = new Container();
				result.setParentFacility(inFacility);
				result.setContainerId(inCsvImportBean.getPreAssignedContainerId());
				result.setKind(inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
				try {
					mContainerDao.store(result);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inOrderGroup
	 * @return
	 */
	private OrderHeader ensureOrderHeader(final CsvOrderImportBean inCsvImportBean, final Facility inFacility, final OrderGroup inOrderGroup) {
		OrderHeader result = null;

		result = inFacility.findOrder(inCsvImportBean.getOrderId());

		if (result == null) {
			result = new OrderHeader();
			result.setParentFacility(inFacility);
			result.setShortDomainId(inCsvImportBean.getOrderId());
			result.setStatusEnum(OrderStatusEnum.CREATED);

			PickStrategyEnum pickStrategy = PickStrategyEnum.SERIAL;
			String pickStrategyEnumId = inCsvImportBean.getPickStrategy();
			if ((pickStrategyEnumId != null) && (pickStrategyEnumId.length() > 0)) {
				pickStrategy = PickStrategyEnum.valueOf(pickStrategyEnumId);
			}
			result.setPickStrategyEnum(pickStrategy);
			inFacility.addOrderHeader(result);
			if (inOrderGroup != null) {
				inOrderGroup.addOrderHeader(result);
				result.setOrderGroup(inOrderGroup);
			}
			try {
				mOrderHeaderDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @return
	 */
	private ItemMaster ensureItemMaster(final CsvOrderImportBean inCsvImportBean, final Facility inFacility, final UomMaster inUomMaster) {
		ItemMaster result = null;

		result = mItemMasterDao.findByDomainId(inFacility, inCsvImportBean.getItemId());
		if (result == null) {

			result = new ItemMaster();
			result.setParentFacility(inFacility);
			result.setItemMasterId(inCsvImportBean.getItemId());
			result.setStandardUoM(inUomMaster);
			inFacility.addItemMaster(result);
			try {
				mItemMasterDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @return
	 */
	private UomMaster ensureUomMaster(final CsvOrderImportBean inCsvImportBean, final Facility inFacility) {
		UomMaster result = null;

		String uomId = inCsvImportBean.getUomId();
		result = inFacility.getUomMaster(uomId);

		if (result == null) {
			result = new UomMaster();
			result.setParentFacility(inFacility);
			result.setUomMasterId(uomId);
			inFacility.addUomMaster(uomId, result);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inOrder
	 * @param inItemMaster
	 * @return
	 */
	private OrderDetail ensureOrderDetail(final CsvOrderImportBean inCsvImportBean,
		final Facility inFacility,
		final OrderHeader inOrder,
		final UomMaster inUomMaster,
		final ItemMaster inItemMaster) {
		OrderDetail result = null;

		result = inOrder.findOrderDetail(inCsvImportBean.getOrderDetailId());
		if (result == null) {
			result = new OrderDetail();
			result.setParentOrderHeader(inOrder);
			result.setShortDomainId(inCsvImportBean.getOrderDetailId());
			result.setStatusEnum(OrderStatusEnum.CREATED);

			inOrder.addOrderDetail(result);
		}

		result.setItemMaster(inItemMaster);
		result.setDescription(inCsvImportBean.getDescription());
		result.setQuantity(Integer.valueOf(inCsvImportBean.getQuantity()));
		result.setUomMaster(inUomMaster);
		result.setOrderDate(Timestamp.valueOf(inCsvImportBean.getOrderDate()));

		try {
			mOrderDetailDao.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
		return result;
	}
}
