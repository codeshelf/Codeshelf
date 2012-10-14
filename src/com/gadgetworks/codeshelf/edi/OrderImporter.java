/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporter.java,v 1.3 2012/10/14 01:05:23 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public OrderImporter(final ITypedDao<OrderGroup> inOrderGroupDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final ITypedDao<ItemMaster> inItemMasterDao,
		final ITypedDao<UomMaster> inUomMasterDao) {

		mOrderGroupDao = inOrderGroupDao;
		mOrderHeaderDao = inOrderHeaderDao;
		mOrderDetailDao = inOrderDetailDao;
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
		ItemMaster itemMaster = ensureItemMaster(inCsvImportBean, inFacility);
		OrderDetail orderDetail = ensureOrderDetail(inCsvImportBean, order, itemMaster);
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
	private ItemMaster ensureItemMaster(final CsvOrderImportBean inCsvImportBean, final Facility inFacility) {
		ItemMaster result = null;

		result = mItemMasterDao.findByDomainId(inFacility, inCsvImportBean.getItemId());
		if (result == null) {

			UomMaster uomMaster = mUomMasterDao.findByDomainId(inFacility, inCsvImportBean.getUomId());
			if (uomMaster == null) {
				uomMaster = new UomMaster();
				uomMaster.setParentFacility(inFacility);
				uomMaster.setUomMasterId(inCsvImportBean.getUomId());
				inFacility.addUomMaster(uomMaster);
				try {
					mUomMasterDao.store(uomMaster);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
			result = new ItemMaster();
			result.setParentFacility(inFacility);
			result.setItemMasterId(inCsvImportBean.getItemId());
			result.setStandardUoM(uomMaster);
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
	 * @param inOrder
	 * @param inItemMaster
	 * @return
	 */
	private OrderDetail ensureOrderDetail(final CsvOrderImportBean inCsvImportBean, final OrderHeader inOrder, final ItemMaster inItemMaster) {
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
		result.setUomId(inCsvImportBean.getUomId());
		result.setOrderDate(Timestamp.valueOf(inCsvImportBean.getOrderDate()));

		try {
			mOrderDetailDao.store(result);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
		return result;
	}
}
