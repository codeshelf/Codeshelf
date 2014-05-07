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

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class InventoryCsvImporter implements ICsvInventoryImporter {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<Item>			mItemDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public InventoryCsvImporter(final ITypedDao<ItemMaster> inItemMasterDao,
		final ITypedDao<Item> inItemDao,
		final ITypedDao<UomMaster> inUomMaster) {

		mItemMasterDao = inItemMasterDao;
		mItemDao = inItemDao;
		mUomMasterDao = inUomMaster;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importDdcInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
		Facility inFacility,
		Timestamp inProcessTime) {
		boolean result = true;
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<InventoryDdcCsvBean> strategy = new HeaderColumnNameMappingStrategy<InventoryDdcCsvBean>();
			strategy.setType(InventoryDdcCsvBean.class);

			CsvToBean<InventoryDdcCsvBean> csv = new CsvToBean<InventoryDdcCsvBean>();
			List<InventoryDdcCsvBean> inventoryBeanList = csv.parse(strategy, csvReader);

			if (inventoryBeanList.size() > 0) {

				LOGGER.debug("Begin DDC inventory import.");

				// Iterate over the inventory import beans.
				for (InventoryDdcCsvBean ddcInventoryBean : inventoryBeanList) {
					String errorMsg = ddcInventoryBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						ddcInventoryCsvBeanImport(ddcInventoryBean, inFacility, inProcessTime);
					}
				}

				archiveCheckItemStatuses(inFacility, inProcessTime);

				LOGGER.debug("End DDC inventory import.");

				inFacility.recomputeDdcPositions();
			}

			csvReader.close();
		} catch (FileNotFoundException e) {
			result = false;
			LOGGER.error("", e);
		} catch (IOException e) {
			result = false;
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importSlottedInventoryFromCsvStream(InputStreamReader inCsvStreamReader,
		Facility inFacility,
		Timestamp inProcessTime) {
		boolean result = true;
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<InventorySlottedCsvBean> strategy = new HeaderColumnNameMappingStrategy<InventorySlottedCsvBean>();
			strategy.setType(InventorySlottedCsvBean.class);

			CsvToBean<InventorySlottedCsvBean> csv = new CsvToBean<InventorySlottedCsvBean>();
			List<InventorySlottedCsvBean> inventoryBeanList = csv.parse(strategy, csvReader);

			if (inventoryBeanList.size() > 0) {

				LOGGER.debug("Begin slotted inventory import.");

				// Iterate over the inventory import beans.
				for (InventorySlottedCsvBean slottedInventoryBean : inventoryBeanList) {
					String errorMsg = slottedInventoryBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						slottedInventoryCsvBeanImport(slottedInventoryBean, inFacility, inProcessTime);
					}
				}

				archiveCheckItemStatuses(inFacility, inProcessTime);

				LOGGER.debug("End slotted inventory import.");
			}

			csvReader.close();
		} catch (FileNotFoundException e) {
			result = false;
			LOGGER.error("", e);
		} catch (IOException e) {
			result = false;
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckItemStatuses(final Facility inFacility, final Timestamp inProcessTime) {
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
	 * @param inCsvBean
	 * @param inFacility
	 */
	private void ddcInventoryCsvBeanImport(final InventoryDdcCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mItemDao.beginTransaction();

			LOGGER.debug("Import ddc item: " + inCsvBean.toString());

			try {
				UomMaster uomMaster = updateUomMaster(inCsvBean.getUom(), inFacility);

				// Create or update the DDC item master, and then set the DDC ID for it.
				ItemMaster itemMaster = updateItemMaster(inCsvBean.getItemId(),
					inCsvBean.getDescription(),
					inFacility,
					inEdiProcessTime,
					uomMaster);
				itemMaster.setDdcId(inCsvBean.getDdcId());
				itemMaster.setDescription(inCsvBean.getDescription());

				try {
					mItemMasterDao.store(itemMaster);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}

				Item item = updateDdcItem(inCsvBean, inFacility, inEdiProcessTime, itemMaster, uomMaster);

			} catch (Exception e) {
				LOGGER.error("", e);
			}
			mItemDao.commitTransaction();

		} finally {
			mItemDao.endTransaction();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 */
	private void slottedInventoryCsvBeanImport(final InventorySlottedCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mItemDao.beginTransaction();

			LOGGER.info(inCsvBean.toString());

			UomMaster uomMaster = updateUomMaster(inCsvBean.getUom(), inFacility);
			ItemMaster itemMaster = updateItemMaster(inCsvBean.getItemId(),
				inCsvBean.getDescription(),
				inFacility,
				inEdiProcessTime,
				uomMaster);
			Item item = updateSlottedItem(inCsvBean, inFacility, inEdiProcessTime, itemMaster, uomMaster);

			mItemDao.commitTransaction();

		} finally {
			mItemDao.endTransaction();
		}
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
	 * @param inCsvBean
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
	 * @param inCsvBean
	 * @param inFacility
	 * @param inItemMaster
	 * @param inUomMaster
	 * @return
	 */
	private Item updateDdcItem(final InventoryDdcCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) {
		Item result = null;

		// Get or create the item at the specified location.
		result = inItemMaster.getItem(inCsvBean.getItemId());
		if ((result == null) && (inCsvBean.getItemId() != null) && (inCsvBean.getItemId().length() > 0)) {
			result = new Item();
			result.setParent(inItemMaster);
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setStoredLocation(inFacility);
			result.setUomMaster(inUomMaster);
			result.setQuantity(Double.valueOf(inCsvBean.getQuantity()));
			inItemMaster.addItem(result);
			inFacility.addStoredItem(result);
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
	 * @param inCsvBean
	 * @param inFacility
	 * @param inItemMaster
	 * @param inUomMaster
	 * @return
	 */
	private Item updateSlottedItem(final InventorySlottedCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) {
		Item result = null;

		LocationABC location = (LocationABC) inFacility.findSubLocationById(inCsvBean.getLocationId());

		// We couldn't find the location, so assign the inventory to the facility itself (which is a location);
		if (location == null) {
			location = inFacility;
		}

		// Get or create the item at the specified location.
		result = location.getStoredItem(inCsvBean.getItemId());
		if ((result == null) && (inCsvBean.getItemId() != null) && (inCsvBean.getItemId().length() > 0)) {
			result = new Item();
			result.setParent(inItemMaster);
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setStoredLocation(location);
			result.setUomMaster(inUomMaster);
			result.setQuantity(Double.valueOf(inCsvBean.getQuantity()));
			result.setPosAlongPath(location.getPosAlongPath());
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mItemDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
			inItemMaster.addItem(result);
		}

		return result;
	}
}
