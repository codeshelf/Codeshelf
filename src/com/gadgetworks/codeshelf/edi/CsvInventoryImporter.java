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
public class CsvInventoryImporter implements ICsvInventoryImporter {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<Item>			mItemDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public CsvInventoryImporter(final ITypedDao<ItemMaster> inItemMasterDao,
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
					String errorMsg = importBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						slottedInventoryCsvBeanImport(importBean, inFacility, processTime);
					}
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
			result.setPosAlongPath(location.getPosAlongPath());
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
