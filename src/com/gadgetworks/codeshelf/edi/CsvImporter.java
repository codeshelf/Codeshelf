/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.16 2013/04/11 07:42:45 jeffw Exp $
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

import com.avaje.ebean.annotation.Transactional;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.PickStrategyEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
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

	private static final Logger		LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<OrderGroup>	mOrderGroupDao;
	private ITypedDao<OrderHeader>	mOrderHeaderDao;
	private ITypedDao<OrderDetail>	mOrderDetailDao;
	private ITypedDao<Container>	mContainerDao;
	private ITypedDao<ContainerUse>	mContainerUseDao;
	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<Item>			mItemDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public CsvImporter(final ITypedDao<OrderGroup> inOrderGroupDao,
		final ITypedDao<OrderHeader> inOrderHeaderDao,
		final ITypedDao<OrderDetail> inOrderDetailDao,
		final ITypedDao<Container> inContainerDao,
		final ITypedDao<ContainerUse> inContainerUseDao,
		final ITypedDao<ItemMaster> inItemMasterDao,
		final ITypedDao<Item> inItemDao,
		final ITypedDao<UomMaster> inUomMaster) {

		mOrderGroupDao = inOrderGroupDao;
		mOrderHeaderDao = inOrderHeaderDao;
		mOrderDetailDao = inOrderDetailDao;
		mContainerDao = inContainerDao;
		mContainerUseDao = inContainerUseDao;
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

			HeaderColumnNameMappingStrategy<OrderCsvImportBean> strategy = new HeaderColumnNameMappingStrategy<OrderCsvImportBean>();
			strategy.setType(OrderCsvImportBean.class);

			CsvToBean<OrderCsvImportBean> csv = new CsvToBean<OrderCsvImportBean>();
			List<OrderCsvImportBean> list = csv.parse(strategy, csvReader);

			for (OrderCsvImportBean importBean : list) {
				orderCsvBeanImport(importBean, inFacility);
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
	public final void importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<DdcInventoryCsvImportBean> strategy = new HeaderColumnNameMappingStrategy<DdcInventoryCsvImportBean>();
			strategy.setType(DdcInventoryCsvImportBean.class);

			CsvToBean<DdcInventoryCsvImportBean> csv = new CsvToBean<DdcInventoryCsvImportBean>();
			List<DdcInventoryCsvImportBean> inventoryImportBeanList = csv.parse(strategy, csvReader);

			if (inventoryImportBeanList.size() > 0) {
				
				LOGGER.debug("Clear existing inventory");
				
				// Delete the entire DDC inventory and replace it with what's in the import.
				for (ItemMaster itemMaster : inFacility.getItemMasters()) {
					if (itemMaster.isDdcItem()) {
						for (Item item : itemMaster.getItems()) {
							mItemDao.delete(item);
						}
					}
				}

				LOGGER.debug("Begin DDC inventory import.");
				
				// Iterate over the inventory import beans.
				for (DdcInventoryCsvImportBean importBean : inventoryImportBeanList) {
					ddcInventoryCsvBeanImport(importBean, inFacility);
				}

				LOGGER.debug("End DDC inventory import.");

				inFacility.recomputeDdcItems();
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
	public final void importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<SlottedInventoryCsvImportBean> strategy = new HeaderColumnNameMappingStrategy<SlottedInventoryCsvImportBean>();
			strategy.setType(SlottedInventoryCsvImportBean.class);

			CsvToBean<SlottedInventoryCsvImportBean> csv = new CsvToBean<SlottedInventoryCsvImportBean>();
			List<SlottedInventoryCsvImportBean> inventoryImportBeanList = csv.parse(strategy, csvReader);

			if (inventoryImportBeanList.size() > 0) {
				// Delete the entire slotted inventory and replace it with what's in the import.
				for (ItemMaster itemMaster : inFacility.getItemMasters()) {
					if (!itemMaster.isDdcItem()) {
						for (Item item : itemMaster.getItems()) {
							mItemDao.delete(item);
						}
					}
				}

				// Iterate over the inventory import beans.
				for (SlottedInventoryCsvImportBean importBean : inventoryImportBeanList) {
					slottedInventoryCsvBeanImport(importBean, inFacility);
				}
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
	@Transactional
	private void orderCsvBeanImport(final OrderCsvImportBean inCsvImportBean, final Facility inFacility) {

		LOGGER.info(inCsvImportBean.toString());

		try {
			OrderGroup group = updateOptionalOrderGroup(inCsvImportBean, inFacility);
			OrderHeader order = updateOrderHeader(inCsvImportBean, inFacility, group);
			Container container = updateContainer(inCsvImportBean, inFacility, order);
			UomMaster uomMaster = updateUomMaster(inCsvImportBean.getUom(), inFacility);
			ItemMaster itemMaster = updateItemMaster(inCsvImportBean.getItemId(), inFacility, uomMaster);
			OrderDetail orderDetail = updateOrderDetail(inCsvImportBean, inFacility, order, uomMaster, itemMaster);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 */
	private void ddcInventoryCsvBeanImport(final DdcInventoryCsvImportBean inCsvImportBean, final Facility inFacility) {

		LOGGER.info(inCsvImportBean.toString());

		UomMaster uomMaster = updateUomMaster(inCsvImportBean.getUom(), inFacility);
		
		// Create or update the DDC item master, and then set the DDC ID for it.
		ItemMaster itemMaster = updateItemMaster(inCsvImportBean.getItemId(), inFacility, uomMaster);
		itemMaster.setDdcId(inCsvImportBean.getDdcId());
		try {
			mItemMasterDao.store(itemMaster);
		} catch (DaoException e) {
			LOGGER.error("", e);
		}
		
		Item item = updateDdcItem(inCsvImportBean, inFacility, itemMaster, uomMaster);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 */
	private void slottedInventoryCsvBeanImport(final SlottedInventoryCsvImportBean inCsvImportBean, final Facility inFacility) {

		LOGGER.info(inCsvImportBean.toString());

		UomMaster uomMaster = updateUomMaster(inCsvImportBean.getUom(), inFacility);
		ItemMaster itemMaster = updateItemMaster(inCsvImportBean.getItemId(), inFacility, uomMaster);
		Item item = updateSlottedItem(inCsvImportBean, inFacility, itemMaster, uomMaster);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @return
	 */
	private OrderGroup updateOptionalOrderGroup(final OrderCsvImportBean inCsvImportBean, final Facility inFacility) {
		OrderGroup result = null;

		result = inFacility.findOrderGroup(inCsvImportBean.getOrderGroupId());
		if ((result == null) && (inCsvImportBean.getOrderGroupId() != null) && (inCsvImportBean.getOrderGroupId().length() > 0)) {
			result = new OrderGroup();
			result.setParent(inFacility);
			result.setOrderGroupId(inCsvImportBean.getOrderGroupId());
			result.setDescription(OrderGroup.DEFAULT_ORDER_GROUP_DESC_PREFIX + inCsvImportBean.getOrderGroupId());
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
	private Container updateContainer(final OrderCsvImportBean inCsvImportBean, final Facility inFacility, final OrderHeader inOrder) {
		Container result = null;

		if ((inCsvImportBean.getPreAssignedContainerId() != null) && (inCsvImportBean.getPreAssignedContainerId().length() > 0)) {
			result = inFacility.getContainer(inCsvImportBean.getPreAssignedContainerId());

			if (result == null) {
				result = new Container();
				result.setParent(inFacility);
				result.setContainerId(inCsvImportBean.getPreAssignedContainerId());
				result.setKind(inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
				inFacility.addContainer(result);
				try {
					mContainerDao.store(result);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}

			ContainerUse use = result.getContainerUse(inOrder);
			if (use == null) {
				// Now create the container use for this.
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				use = new ContainerUse();
				use.setDomainId(timestamp.toString());
				use.setOrderHeader(inOrder);
				use.setParent(result);
				use.setUsedOn(timestamp);
				try {
					mContainerUseDao.store(use);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
				result.addContainerUse(use);
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
	private OrderHeader updateOrderHeader(final OrderCsvImportBean inCsvImportBean, final Facility inFacility, final OrderGroup inOrderGroup) {
		OrderHeader result = null;

		result = inFacility.findOrder(inCsvImportBean.getOrderId());

		if (result == null) {
			result = new OrderHeader();
			result.setParent(inFacility);
			result.setDomainId(inCsvImportBean.getOrderId());
		}

		result.setStatusEnum(OrderStatusEnum.CREATED);
		result.setCustomerId(inCsvImportBean.getCustomerId());
		result.setShipmentId(inCsvImportBean.getShipmentId());
		if (inCsvImportBean.getWorkSequence() != null) {
			result.setWorkSequence(Integer.valueOf(inCsvImportBean.getWorkSequence()));
		}
		if (inCsvImportBean.getOrderDate() != null) {
			try {
				result.setOrderDate(Timestamp.valueOf(inCsvImportBean.getOrderDate()));
			} catch (IllegalArgumentException e) {
				LOGGER.error("", e);
			}
		}
		if (inCsvImportBean.getDueDate() != null) {
			try {
				result.setDueDate(Timestamp.valueOf(inCsvImportBean.getDueDate()));
			} catch (IllegalArgumentException e) {
				LOGGER.error("", e);
			}
		}

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

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @return
	 */
	private ItemMaster updateItemMaster(final String inItemId, final Facility inFacility, final UomMaster inUomMaster) {
		ItemMaster result = null;

		result = mItemMasterDao.findByDomainId(inFacility, inItemId);
		if (result == null) {

			result = new ItemMaster();
			result.setParent(inFacility);
			result.setDomainId(inItemId);
			result.setItemId(inItemId);
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
	private UomMaster updateUomMaster(final String inUomId, final Facility inFacility) {
		UomMaster result = null;

		result = inFacility.getUomMaster(inUomId);

		if (result == null) {
			result = new UomMaster();
			result.setParent(inFacility);
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
	private OrderDetail updateOrderDetail(final OrderCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final OrderHeader inOrder,
		final UomMaster inUomMaster,
		final ItemMaster inItemMaster) {
		OrderDetail result = null;

		result = inOrder.findOrderDetail(inCsvImportBean.getOrderDetailId());
		if (result == null) {
			result = new OrderDetail();
			result.setParent(inOrder);
			result.setDomainId(inCsvImportBean.getOrderDetailId());
			result.setStatusEnum(OrderStatusEnum.CREATED);

			inOrder.addOrderDetail(result);
		}

		result.setItemMaster(inItemMaster);
		result.setDescription(inCsvImportBean.getDescription());
		result.setQuantity(Integer.valueOf(inCsvImportBean.getQuantity()));
		result.setUomMaster(inUomMaster);

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
	 * @param inItemMaster
	 * @param inUomMaster
	 * @return
	 */
	private Item updateDdcItem(final DdcInventoryCsvImportBean inCsvImportBean, final Facility inFacility, final ItemMaster inItemMaster, final UomMaster inUomMaster) {
		Item result = null;

		// Get or create the item at the specified location.
		result = inFacility.getItem(inCsvImportBean.getItemId());
		if ((result == null) && (inCsvImportBean.getItemId() != null) && (inCsvImportBean.getItemId().length() > 0)) {
			result = new Item();
			result.setItemMaster(inItemMaster);
			result.setItemId(inCsvImportBean.getItemId());
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setParent(inFacility);
			result.setUomMaster(inUomMaster);
			result.setQuantity(Double.valueOf(inCsvImportBean.getQuantity()));
			inItemMaster.addItem(result);
			inFacility.addItem(inCsvImportBean.getItemId(), result);
			try {
				mItemDao.store(result);
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
	 * @param inItemMaster
	 * @param inUomMaster
	 * @return
	 */
	private Item updateSlottedItem(final SlottedInventoryCsvImportBean inCsvImportBean, final Facility inFacility, final ItemMaster inItemMaster, final UomMaster inUomMaster) {
		Item result = null;

		LocationABC location = (LocationABC) inFacility.getSubLocationById(inCsvImportBean.getLocationId());

		// We couldn't find the location, so assign the inventory to the facility itself (which is a location);
		if (location == null) {
			location = inFacility;
		}

		// Get or create the item at the specified location.
		result = location.getItem(inCsvImportBean.getItemId());
		if ((result == null) && (inCsvImportBean.getItemId() != null) && (inCsvImportBean.getItemId().length() > 0)) {
			result = new Item();
			result.setItemMaster(inItemMaster);
			result.setItemId(inCsvImportBean.getItemId());
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setParent(location);
			result.setUomMaster(inUomMaster);
			result.setQuantity(Double.valueOf(inCsvImportBean.getQuantity()));
			inItemMaster.addItem(result);
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
