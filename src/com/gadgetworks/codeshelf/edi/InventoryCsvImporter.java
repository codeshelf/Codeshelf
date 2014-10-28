/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.event.EventSeverity;
import com.gadgetworks.codeshelf.event.EventTag;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.util.UomNormalizer;
import com.gadgetworks.codeshelf.validation.DefaultErrors;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.Errors;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class InventoryCsvImporter extends CsvImporter<InventorySlottedCsvBean> implements ICsvInventoryImporter {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(InventoryCsvImporter.class);

	private ITypedDao<ItemMaster>	mItemMasterDao;
	private ITypedDao<Item>			mItemDao;
	private ITypedDao<UomMaster>	mUomMasterDao;

	@Inject
	public InventoryCsvImporter(final EventProducer inProducer, final ITypedDao<ItemMaster> inItemMasterDao,
		final ITypedDao<Item> inItemDao,
		final ITypedDao<UomMaster> inUomMaster) {

		super(inProducer);
		
		mItemMasterDao = inItemMasterDao;
		mItemDao = inItemDao;
		mUomMasterDao = inUomMaster;
	}

	//WHEN RENABLED SPLIT TO ITS OWN IMPORTER
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	/*
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
	}*/

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importSlottedInventoryFromCsvStream(Reader inCsvReader,
		Facility inFacility,
		Timestamp inProcessTime) {
		boolean result = true;

		List<InventorySlottedCsvBean> inventoryBeanList = toCsvBean(inCsvReader, InventorySlottedCsvBean.class);

		if (inventoryBeanList.size() > 0) {

			LOGGER.debug("Begin slotted inventory import.");

			// Iterate over the inventory import beans.
			for (InventorySlottedCsvBean slottedInventoryBean : inventoryBeanList) {
				try {
					slottedInventoryCsvBeanImport(slottedInventoryBean, inFacility, inProcessTime);
					produceRecordSuccessEvent(slottedInventoryBean);
				}
				catch(InputValidationException e) {
					produceRecordViolationEvent(EventSeverity.WARN, e, slottedInventoryBean);
					LOGGER.warn("Unable to process record: " + slottedInventoryBean, e);
				}
				catch(Exception e) {
					produceRecordViolationEvent(EventSeverity.ERROR, e, slottedInventoryBean);
					LOGGER.warn("Unable to process record: " + slottedInventoryBean, e);
				}
			}
			// JR says this looks dangerous. Any random file in import/inventory would result in inactivation of all inventory and most masters.
			// archiveCheckItemStatuses(inFacility, inProcessTime);

			LOGGER.debug("End slotted inventory import.");
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckItemStatuses(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.info("Archive unreferenced item data");
		// JR says this all looks dangerous. Not calling for now.

		// Inactivate the DDC item that don't match the import timestamp.
		try {
			mItemMasterDao.beginTransaction();
			for (ItemMaster itemMaster : inFacility.getItemMasters()) {
				Boolean itemMasterIsActive = false;
				for (Item item : itemMaster.getItems()) {
					if (item.getUpdated().equals(inProcessTime)) {
						itemMasterIsActive = true;
					} else {
						LOGGER.info("Archive old item: " + itemMaster.getItemId());
						item.setActive(false);
						mItemDao.store(item);
					}
				}

				if (!itemMasterIsActive) {
					LOGGER.info("Archive old item master: " + itemMaster.getItemId());
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
				UomMaster uomMaster = upsertUomMaster(inCsvBean.getUom(), inFacility);

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

				@SuppressWarnings("unused")
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
		final Timestamp inEdiProcessTime) throws InputValidationException {

		try {
			mItemDao.beginTransaction();
			
			LOGGER.info(inCsvBean.toString());
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				throw new InputValidationException(inCsvBean, errorMsg);
			} 
			
			UomMaster uomMaster = upsertUomMaster(inCsvBean.getUom(), inFacility);

			String theItemID = inCsvBean.getItemId();
			ItemMaster itemMaster = updateItemMaster(theItemID,
				inCsvBean.getDescription(),
				inFacility,
				inEdiProcessTime,
				uomMaster);
			
			String theLocationID = inCsvBean.getLocationId();
			ILocation<? extends IDomainObject> location = inFacility.findSubLocationById(theLocationID);
			// Remember, findSubLocationById will find inactive locations.
			// We couldn't find the location, so assign the inventory to the facility itself (which is a location);  Not sure this is best, but it is the historical behavior from pre-v1.
			if (location == null) {
				produceRecordViolationEvent(inCsvBean, "locationId", theLocationID, ErrorCode.FIELD_REFERENCE_NOT_FOUND);			
				produceRecordViolationEvent(inCsvBean, "Using facility for missing location");
				//produceRecordViolationEvent(inCsvBean, "locationId", theLocationID, ErrorCode.FIELD_IMPLIED_VALUE, inFacility.getLocationId());			
				location = inFacility;
			}
			// If location is inactive, then what? Would we want to move existing inventory there to facility? Doing that initially mostly because it is easier.
			// Might be better to ask if this inventory item is already in that inactive location, and not move it if so.
			else if (!location.isActive()) {
				produceRecordViolationEvent(inCsvBean, "locationId", theLocationID, ErrorCode.FIELD_REFERENCE_INACTIVE);			
				produceRecordViolationEvent(inCsvBean, "Using facility for inactive location");
				//produceRecordViolationEvent(inCsvBean, "locationId", theLocationID, ErrorCode.FIELD_IMPLIED_VALUE, inFacility.getLocationId());			
				location = inFacility;
			}

			if (Strings.isNullOrEmpty(inCsvBean.getCmFromLeft())) {
				inCsvBean.setCmFromLeft("0");
			}

			if (Strings.isNullOrEmpty(inCsvBean.getCmFromLeft())) {
				inCsvBean.setCmFromLeft("0");
			}
			@SuppressWarnings("unused")
			Item item = updateSlottedItem(true, inCsvBean, location, inEdiProcessTime, itemMaster, uomMaster);

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
	public ItemMaster updateItemMaster(final String inItemId,
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
				LOGGER.error("updateItemMaster", e);
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
	public UomMaster upsertUomMaster(final String inUomId, final Facility inFacility) {
		Errors errors = new DefaultErrors(UomMaster.class);
		if (Strings.emptyToNull(inUomId) == null) {
			errors.rejectValue("uomMasterId", ErrorCode.FIELD_REQUIRED, "uomMasterId is required");
			throw new InputValidationException(errors);
		}

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
				LOGGER.error("upsertUomMaster save", e);
			}
			/*
			catch (InputValidationException e) {
				// can we catch this here? If the UOM did not save, the next transaction might fail trying to reference it
				LOGGER.error("upsertUomMaster validate", e);
			} */
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
	public Item updateSlottedItem(boolean useLenientValidation, final InventorySlottedCsvBean inCsvBean,
		final ILocation<?> inLocation,
		final Timestamp inEdiProcessTime,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) throws InputValidationException {
		
		Errors errors = new DefaultErrors(Item.class);
		if (inLocation == null) {
			errors.rejectValue("storedLocation", ErrorCode.FIELD_REQUIRED, "storedLocation is required");
		}

		if (inItemMaster == null) {
			errors.rejectValue("parent", ErrorCode.FIELD_REQUIRED, "parent is required");
		}
		
		Double quantity = 0.0d;
		String quantityString = inCsvBean.getQuantity();
		try {
			quantity = Double.valueOf(quantityString);
			if (quantity < 0.0d) {
				quantity = 0.0d;
				errors.rejectValue("quantity", ErrorCode.FIELD_NUMBER_NOT_NEGATIVE, "quantity cannot be a negative number");
			}
		}
		catch(NumberFormatException e) {
			errors.rejectValue("quantity", ErrorCode.FIELD_NUMBER_REQUIRED, "quantity must be a number");
		}
		
		if (errors.hasErrors() && !useLenientValidation) {
			throw new InputValidationException(errors);
		}

		
		Item result = null;
		
		
		String normalizedUom = UomNormalizer.normalizeString(inCsvBean.getUom());
		if (normalizedUom.equals(UomNormalizer.EACH)) {
			List<Item> items = inItemMaster.getItems();
			for (Item item : items) {
				if (UomNormalizer.normalizeString(item.getUomMaster().getUomMasterId()).equals(normalizedUom)) {
					result = item;
					break;
				}
			}
		}
		else {
			result = inLocation.getStoredItemFromMasterIdAndUom(inCsvBean.getItemId(),inCsvBean.getUom());
				
		}
		if ((result == null)) {
			result = new Item();
			result.setParent(inItemMaster);
			inItemMaster.addItem(result);
		} 
		// setStoredLocation has the side effect of setting domainId, but that requires that UOM already be set. So setUomMaster first.
		result.setUomMaster(inUomMaster);
		result.setStoredLocation(inLocation);
		
		result.setQuantity(quantity);
		// This used to call only this
		// now refine using the cm value if there is one
		Integer cmValue = null;
		String cmFromLeftString = inCsvBean.getCmFromLeft();
		if (!Strings.isNullOrEmpty(cmFromLeftString)) {
			try {
				cmValue = Integer.valueOf(cmFromLeftString);
				// Our new setter
				String error = result.validatePositionFromLeft(inLocation, cmValue);
				if (error.isEmpty()) {
					result.setPositionFromLeft(cmValue);
				}  else {
					errors.rejectValue("positionFromLeft", ErrorCode.FIELD_GENERAL, error);
				}
			} catch (NumberFormatException e) {
				errors.rejectValue("positionFromLeft", ErrorCode.FIELD_NUMBER_NOT_NEGATIVE, "positionFromLeft is not a positive number");
			}
		} 
		result.setActive(true);
		result.setUpdated(inEdiProcessTime);
		
		
		if(errors.hasErrors()) {
			if (!useLenientValidation) {
				throw new InputValidationException(errors); 
			}
		} 
		mItemDao.store(result);
		return result;
	}
	
	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.INVENTORY_SLOTTED);
	}
}
