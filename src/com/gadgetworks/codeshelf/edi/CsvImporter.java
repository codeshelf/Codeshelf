/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.3 2012/10/24 07:12:11 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class CsvImporter implements ICsvImporter {

	private static final Log		LOGGER	= LogFactory.getLog(EdiProcessor.class);

	private ITypedDao<OrderGroup>	mOrderGroupDao;
	private ITypedDao<OrderHeader>	mOrderHeaderDao;
	private ITypedDao<OrderDetail>	mOrderDetailDao;
	private ITypedDao<Container>	mContainerDao;
	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<Item>			mItemDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public CsvImporter(final ITypedDao<OrderGroup> inOrderGroupDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final ITypedDao<Container> inContainerDao,
		final ITypedDao<ItemMaster> inItemMasterDao,
		final ITypedDao<Item> inItemDao,
		final ITypedDao<UomMaster> inUomMaster) {

		mOrderGroupDao = inOrderGroupDao;
		mOrderHeaderDao = inOrderHeaderDao;
		mOrderDetailDao = inOrderDetailDao;
		mContainerDao = inContainerDao;
		mItemMasterDao = inItemMasterDao;
		mItemDao = inItemDao;
		mUomMasterDao = inUomMaster;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importOrdersFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final void importOrdersFromCsvStream(final InputStreamReader inCsvStreamReader, final Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<CsvOrderImportBean> strategy = new HeaderColumnNameMappingStrategy<CsvOrderImportBean>();
			strategy.setType(CsvOrderImportBean.class);

			CsvToBean<CsvOrderImportBean> csv = new CsvToBean<CsvOrderImportBean>();
			List<CsvOrderImportBean> list = csv.parse(strategy, csvReader);

			for (CsvOrderImportBean importBean : list) {
				importCsvOrderBean(importBean, inFacility);
			}

			csvReader.close();
		} catch (FileNotFoundException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final void importInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<CsvInventoryImportBean> strategy = new HeaderColumnNameMappingStrategy<CsvInventoryImportBean>();
			strategy.setType(CsvInventoryImportBean.class);

			CsvToBean<CsvInventoryImportBean> csv = new CsvToBean<CsvInventoryImportBean>();
			List<CsvInventoryImportBean> list = csv.parse(strategy, csvReader);

			for (CsvInventoryImportBean importBean : list) {
				importCsvInventoryBean(importBean, inFacility);
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
	private void importCsvOrderBean(final CsvOrderImportBean inCsvImportBean, final Facility inFacility) {

		LOGGER.info(inCsvImportBean);

		OrderGroup group = ensureOptionalOrderGroup(inCsvImportBean, inFacility);
		OrderHeader order = ensureOrderHeader(inCsvImportBean, inFacility, group);
		Container container = ensureContainer(inCsvImportBean, inFacility, order);
		UomMaster uomMaster = ensureUomMaster(inCsvImportBean.getUomId(), inFacility);
		ItemMaster itemMaster = ensureItemMaster(inCsvImportBean.getItemId(), inFacility, uomMaster);
		OrderDetail orderDetail = ensureOrderDetail(inCsvImportBean, inFacility, order, uomMaster, itemMaster);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 */
	private void importCsvInventoryBean(final CsvInventoryImportBean inCsvImportBean, final Facility inFacility) {

		LOGGER.info(inCsvImportBean);

		UomMaster uomMaster = ensureUomMaster(inCsvImportBean.getUomId(), inFacility);
		ItemMaster itemMaster = ensureItemMaster(inCsvImportBean.getItemId(), inFacility, uomMaster);
		Item item = ensureItem(inCsvImportBean, inFacility, itemMaster, uomMaster);
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
				inFacility.addContainer(result);
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
	private ItemMaster ensureItemMaster(final String inItemMasterId, final Facility inFacility, final UomMaster inUomMaster) {
		ItemMaster result = null;

		result = mItemMasterDao.findByDomainId(inFacility, inItemMasterId);
		if (result == null) {

			result = new ItemMaster();
			result.setParentFacility(inFacility);
			result.setItemMasterId(inItemMasterId);
			result.setStandardUom(inUomMaster);
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
	private UomMaster ensureUomMaster(final String inUomId, final Facility inFacility) {
		UomMaster result = null;

		result = inFacility.getUomMaster(inUomId);

		if (result == null) {
			result = new UomMaster();
			result.setParentFacility(inFacility);
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

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @return
	 */
	private Item ensureItem(final CsvInventoryImportBean inCsvImportBean, final Facility inFacility, final ItemMaster inItemMaster, final UomMaster inUomMaster) {
		Item result = null;

		LocationABC location = inFacility.getLocationByFullId(inCsvImportBean.getLocationId());

		// We couldn't find the location, so assign the inventory to the facility itself (which is a location);
		if (location == null) {
			location = inFacility;
		}

		result = location.getItem(inCsvImportBean.getItemId());
		if ((result == null) && (inCsvImportBean.getItemId() != null) && (inCsvImportBean.getItemId().length() > 0)) {
			result = new Item();
			result.setParentItemMaster(inItemMaster);
			result.setItemId(inCsvImportBean.getItemId());
			result.setUomMaster(inUomMaster);
			result.setQuantity(Double.valueOf(inCsvImportBean.getQuantity()));
			inItemMaster.addItem(result);
			result.setLocation(location);
			location.addItem(inCsvImportBean.getItemId(), result);
			try {
				mItemDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

}
