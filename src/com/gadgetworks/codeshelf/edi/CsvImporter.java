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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
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

			Timestamp processTime = new Timestamp(System.currentTimeMillis());

			LOGGER.debug("Begin order import.");

			for (OrderCsvImportBean importBean : list) {
				orderCsvBeanImport(importBean, inFacility, processTime);
			}

			archiveOrderStatuses(inFacility, processTime);
			archiveContainerStatuses(inFacility, processTime);

			LOGGER.debug("End order import.");

			csvReader.close();

			cleanupArchivedOrders();

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

				Timestamp processTime = new Timestamp(System.currentTimeMillis());

				LOGGER.debug("Begin DDC inventory import.");

				// Iterate over the inventory import beans.
				for (DdcInventoryCsvImportBean importBean : inventoryImportBeanList) {
					String errorMsg = importBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						ddcInventoryCsvBeanImport(importBean, inFacility, processTime);
					}
				}

				archiveItemStatuses(inFacility, processTime);

				LOGGER.debug("End DDC inventory import.");

				inFacility.recomputeDdcPositions();
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

				Timestamp processTime = new Timestamp(System.currentTimeMillis());

				LOGGER.debug("Begin slotted inventory import.");

				// Iterate over the inventory import beans.
				for (SlottedInventoryCsvImportBean importBean : inventoryImportBeanList) {
					slottedInventoryCsvBeanImport(importBean, inFacility, processTime);
				}

				archiveItemStatuses(inFacility, processTime);

				LOGGER.debug("End slotted inventory import.");
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
	private void archiveItemStatuses(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced item data");

		// Inactivate the DDC item that don't match the import timestamp.
		try {
			mItemMasterDao.beginTransaction();
			for (ItemMaster itemMaster : inFacility.getItemMasters()) {
				Boolean itemMasterIsActive = false;
				for (Item item : itemMaster.getItems()) {
					if (item.getUpdated().equals(inProcessTime)) {
						itemMasterIsActive = true;
					} else {
						LOGGER.debug("Archive old item: " + itemMaster.getItemId());
						item.setActive(false);
						mItemDao.store(item);
					}
				}

				if (!itemMasterIsActive) {
					LOGGER.debug("Archive old item master: " + itemMaster.getItemId());
					itemMaster.setActive(false);
					mItemMasterDao.store(itemMaster);
				}
			}
			mItemMasterDao.commitTransaction();
		} finally {
			mItemMasterDao.endTransaction();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveOrderStatuses(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced order data");

		// Inactivate the orders that don't match the import timestamp.
		// All orders and related items get marked with the same timestamp when imported from the same interchange.
		try {
			mOrderGroupDao.beginTransaction();

			// Iterate all of the order groups to see if they're still active.
			for (OrderGroup group : inFacility.getOrderGroups()) {

				if (!group.getUpdated().equals(inProcessTime)) {
					LOGGER.debug("Archive old order group: " + group.getOrderGroupId());
					group.setActive(false);
					mOrderGroupDao.store(group);
				}
			}
			
			// Iterate all of the order headers in this order group to see if they're still active.
			for (OrderHeader order : inFacility.getOrderHeaders()) {
				Boolean orderHeaderIsActive = false;

				// Iterate all of the order details in this order header to see if they're still active.
				for (OrderDetail orderDetail : order.getOrderDetails()) {
					if (orderDetail.getUpdated().equals(inProcessTime)) {
						orderHeaderIsActive = true;
					} else {
						LOGGER.debug("Archive old order detail: " + orderDetail.getOrderDetailId());
						orderDetail.setActive(false);
						mOrderDetailDao.store(orderDetail);
					}
				}

				if (!orderHeaderIsActive) {
					LOGGER.debug("Archive old order header: " + order.getOrderId());
					order.setActive(false);
					mOrderHeaderDao.store(order);
				}
			}

			mOrderGroupDao.commitTransaction();
		} finally {
			mOrderGroupDao.endTransaction();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveContainerStatuses(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced container data");

		// Inactivate the orders that don't match the import timestamp.
		// All orders and related items get marked with the same timestamp when imported from the same interchange.
		try {
			mContainerDao.beginTransaction();

			// Iterate all of the containers to see if they're still active.
			for (Container container : inFacility.getContainers()) {
				Boolean containerIsActive = false;

				for (ContainerUse containerUse : container.getUses()) {
					if (containerUse.getUpdated().equals(inProcessTime)) {
						containerIsActive = true;
					} else {
						containerUse.setActive(false);
						mContainerUseDao.store(containerUse);
					}
				}

				if (!containerIsActive) {
					container.setActive(false);
					mContainerDao.store(container);
				}
			}

			mContainerDao.commitTransaction();
		} finally {
			mContainerDao.endTransaction();
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
	 * @param inCsvImportBean
	 * @param inFacility
	 */
	@Transactional
	private void orderCsvBeanImport(final OrderCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		LOGGER.info(inCsvImportBean.toString());

		try {
			mOrderHeaderDao.beginTransaction();

			try {
				OrderGroup group = updateOptionalOrderGroup(inCsvImportBean, inFacility, inEdiProcessTime);
				OrderHeader order = updateOrderHeader(inCsvImportBean, inFacility, inEdiProcessTime, group);
				Container container = updateContainer(inCsvImportBean, inFacility, inEdiProcessTime, order);
				UomMaster uomMaster = updateUomMaster(inCsvImportBean.getUom(), inFacility);
				ItemMaster itemMaster = updateItemMaster(inCsvImportBean.getItemId(),
					inCsvImportBean.getDescription(),
					inFacility,
					inEdiProcessTime,
					uomMaster);
				OrderDetail orderDetail = updateOrderDetail(inCsvImportBean,
					inFacility,
					inEdiProcessTime,
					order,
					uomMaster,
					itemMaster);
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
	 * @param inCsvImportBean
	 * @param inFacility
	 */
	private void ddcInventoryCsvBeanImport(final DdcInventoryCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mItemDao.beginTransaction();

			LOGGER.debug("Import ddc item: " + inCsvImportBean.toString());

			UomMaster uomMaster = updateUomMaster(inCsvImportBean.getUom(), inFacility);

			// Create or update the DDC item master, and then set the DDC ID for it.
			ItemMaster itemMaster = updateItemMaster(inCsvImportBean.getItemId(),
				inCsvImportBean.getDescription(),
				inFacility,
				inEdiProcessTime,
				uomMaster);
			itemMaster.setDdcId(inCsvImportBean.getDdcId());
			itemMaster.setDescription(inCsvImportBean.getDescription());

			try {
				mItemMasterDao.store(itemMaster);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

			Item item = updateDdcItem(inCsvImportBean, inFacility, inEdiProcessTime, itemMaster, uomMaster);

			mItemDao.commitTransaction();

		} finally {
			mItemDao.endTransaction();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 */
	private void slottedInventoryCsvBeanImport(final SlottedInventoryCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mItemDao.beginTransaction();

			LOGGER.info(inCsvImportBean.toString());

			UomMaster uomMaster = updateUomMaster(inCsvImportBean.getUom(), inFacility);
			ItemMaster itemMaster = updateItemMaster(inCsvImportBean.getItemId(),
				inCsvImportBean.getDescription(),
				inFacility,
				inEdiProcessTime,
				uomMaster);
			Item item = updateSlottedItem(inCsvImportBean, inFacility, inEdiProcessTime, itemMaster, uomMaster);

			mItemDao.commitTransaction();

		} finally {
			mItemDao.endTransaction();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @return
	 */
	private OrderGroup updateOptionalOrderGroup(final OrderCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {
		OrderGroup result = null;

		result = inFacility.findOrderGroup(inCsvImportBean.getOrderGroupId());
		if ((result == null) && (inCsvImportBean.getOrderGroupId() != null) && (inCsvImportBean.getOrderGroupId().length() > 0)) {
			result = new OrderGroup();
			result.setParent(inFacility);
			result.setOrderGroupId(inCsvImportBean.getOrderGroupId());
			result.setDescription(OrderGroup.DEFAULT_ORDER_GROUP_DESC_PREFIX + inCsvImportBean.getOrderGroupId());
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
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inOrder
	 * @return
	 */
	private Container updateContainer(final OrderCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderHeader inOrder) {
		Container result = null;

		if ((inCsvImportBean.getPreAssignedContainerId() != null) && (inCsvImportBean.getPreAssignedContainerId().length() > 0)) {
			result = inFacility.getContainer(inCsvImportBean.getPreAssignedContainerId());

			if (result == null) {
				result = new Container();
				result.setParent(inFacility);
				result.setContainerId(inCsvImportBean.getPreAssignedContainerId());
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
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
			result.addContainerUse(use);

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
	private OrderHeader updateOrderHeader(final OrderCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final OrderGroup inOrderGroup) {
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
				// First try to parse it as a SQL timestamp.
				result.setOrderDate(Timestamp.valueOf(inCsvImportBean.getOrderDate()));
			} catch (IllegalArgumentException e) {
				// Then try to parse it as just a SQL date.
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					dateFormat.setLenient(true);
					Date date = dateFormat.parse(inCsvImportBean.getOrderDate());
					result.setOrderDate(new Timestamp(date.getTime()));
				} catch (IllegalArgumentException | ParseException e1) {
					LOGGER.error("", e1);
				}
			}
		}

		if (inCsvImportBean.getDueDate() != null) {
			try {
				// First try to parse it as a SQL timestamp.
				result.setDueDate(Timestamp.valueOf(inCsvImportBean.getDueDate()));
			} catch (IllegalArgumentException e) {
				// Then try to parse it as just a SQL date.
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
					dateFormat.setLenient(true);
					Date date = dateFormat.parse(inCsvImportBean.getDueDate());
					result.setDueDate(new Timestamp(date.getTime()));
				} catch (IllegalArgumentException | ParseException e1) {
					LOGGER.error("", e1);
				}
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
			result.setParent(inFacility);
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
		final Timestamp inEdiProcessTime,
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
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inItemMaster
	 * @param inUomMaster
	 * @return
	 */
	private Item updateDdcItem(final DdcInventoryCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) {
		Item result = null;

		// Get or create the item at the specified location.
		result = inItemMaster.getItem(inCsvImportBean.getItemDetailId());
		if ((result == null) && (inCsvImportBean.getItemDetailId() != null) && (inCsvImportBean.getItemDetailId().length() > 0)) {
			result = new Item();
			result.setParent(inItemMaster);
			result.setItemDetailId(inCsvImportBean.getItemDetailId());
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setStoredLocation(inFacility);
			result.setUomMaster(inUomMaster);
			result.setQuantity(Double.valueOf(inCsvImportBean.getQuantity()));
			inItemMaster.addItem(result);
			inFacility.addItem(result);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
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
	private Item updateSlottedItem(final SlottedInventoryCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) {
		Item result = null;

		LocationABC location = (LocationABC) inFacility.getSubLocationById(inCsvImportBean.getLocationId());

		// We couldn't find the location, so assign the inventory to the facility itself (which is a location);
		if (location == null) {
			location = inFacility;
		}

		// Get or create the item at the specified location.
		result = location.getItem(inCsvImportBean.getItemDetailId());
		if ((result == null) && (inCsvImportBean.getItemId() != null) && (inCsvImportBean.getItemId().length() > 0)) {
			result = new Item();
			result.setParent(inItemMaster);
			result.setItemDetailId(inCsvImportBean.getItemId());
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setStoredLocation(location);
			result.setUomMaster(inUomMaster);
			result.setQuantity(Double.valueOf(inCsvImportBean.getQuantity()));
			inItemMaster.addItem(result);
			location.addItem(result);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mItemDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}

}
