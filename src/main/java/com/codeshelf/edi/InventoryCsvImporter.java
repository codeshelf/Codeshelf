/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventSeverity;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.validation.DefaultErrors;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
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

	@Inject
	public InventoryCsvImporter(final EventProducer inProducer) {

		super(inProducer);

	}

	//WHEN RENABLED SPLIT TO ITS OWN IMPORTER
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
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
	 * @see com.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
	 */
	public final boolean importSlottedInventoryFromCsvStream(Reader inCsvReader, Facility inFacility, Timestamp inProcessTime) {

		List<InventorySlottedCsvBean> inventoryBeanList = toCsvBean(inCsvReader, InventorySlottedCsvBean.class);
		return importSlottedInventory(inventoryBeanList, inFacility, inProcessTime);

	}

	public boolean importSlottedInventory(List<InventorySlottedCsvBean> inventoryBeanList,
		Facility inFacility,
		Timestamp inProcessTime) {
		boolean result = true;
		// TODO Auto-generated method stub
		if (inventoryBeanList.size() > 0) {

			LOGGER.debug("Begin slotted inventory import.");

			// Iterate over the inventory import beans.
			for (InventorySlottedCsvBean slottedInventoryBean : inventoryBeanList) {
				try {
					slottedInventoryCsvBeanImport(slottedInventoryBean, inFacility, inProcessTime);
					produceRecordSuccessEvent(slottedInventoryBean);
				} catch (InputValidationException e) {
					produceRecordViolationEvent(EventSeverity.WARN, e, slottedInventoryBean);
					LOGGER.warn("Unable to process record: " + slottedInventoryBean, e);
				} catch (Exception e) {
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
	private void archiveCheckItemStatuses(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.info("Archive unreferenced item data");
		// JR says this all looks dangerous. Not calling for now.

		// Inactivate the DDC item that don't match the import timestamp.
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
				ItemMaster.staticGetDao().store(itemMaster);
			}
		}

	}
	 */
	

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	private void ddcInventoryCsvBeanImport(final InventoryDdcCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

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
				ItemMaster.staticGetDao().store(itemMaster);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}

			@SuppressWarnings("unused")
			Item item = updateDdcItem(inCsvBean, inFacility, inEdiProcessTime, itemMaster, uomMaster);

		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}
	 */

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 */
	private void slottedInventoryCsvBeanImport(final InventorySlottedCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) throws InputValidationException {

			LOGGER.info(inCsvBean.toString());
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				throw new InputValidationException(inCsvBean, errorMsg);
			}

			UomMaster uomMaster = upsertUomMaster(inCsvBean.getUom(), inFacility);
			
			String theItemID = inCsvBean.getItemId();
			ItemMaster itemMaster = updateItemMaster(theItemID, inCsvBean.getDescription(), inFacility, inEdiProcessTime, uomMaster);
			@SuppressWarnings("unused")
			Gtin gtinMap = upsertGtin(inFacility, itemMaster, inCsvBean, uomMaster);

			String theLocationID = inCsvBean.getLocationId();
			Location location = inFacility.findSubLocationById(theLocationID);
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
			
	}

	/**
	 * Will not update a GtinMap. 
	 * 
	 */
	private Gtin upsertGtin(final Facility inFacility, final ItemMaster inItemMaster, 
		final InventorySlottedCsvBean inCsvBean, UomMaster uomMaster) {
		
		if (inCsvBean.getGtin() == null || inCsvBean.getGtin().isEmpty()) {
			return null;
		}
		
		Gtin result = null;
		ItemMaster previousItemMaster = null;
		
		// Get existing GtinMap
		result = Gtin.staticGetDao().findByDomainId(null, inCsvBean.getGtin());
		
		if (result != null) {
			previousItemMaster = result.getParent();
			
			// Check if existing GTIN is associated with the ItemMaster
			if (previousItemMaster.equals(inItemMaster)) {
				
				// Check if the UOM specified in the inventory file matches UOM of existing GTIN
				if (!uomMaster.equals(result.getUomMaster())) {
					LOGGER.warn("UOM specified in order line {} conflicts with UOM of specified existing GTIN {}." +
							" Did not change UOM for existing GTIN.", inCsvBean.toString(), result.getDomainId());
					
					return null;
				}
				
			} else {
				
				// Import line is attempting to associate existing GTIN with a new item. We do not allow this.
				LOGGER.warn("GTIN {} already exists and is associated with item {}." + 
						" GTIN will remain associated with item {}.", result.getDomainId(), result.getParent().getDomainId(),
						result.getParent().getDomainId());
				
				return null;
			}
		} else {
			
			result = inItemMaster.createGtin(inCsvBean.getGtin(), uomMaster);

			try {
				Gtin.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.error("upsertGtinMap save", e);
			}
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
	public ItemMaster updateItemMaster(final String inItemId,
		final String inDescription,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final UomMaster inUomMaster) {
		ItemMaster result = null;

		result = ItemMaster.staticGetDao().findByDomainId(inFacility, inItemId);
		if (result == null) {
			result = new ItemMaster();
			result.setDomainId(inItemId);
			result.setItemId(inItemId);
			result.setParent(inFacility);
		}

		// If we were able to get/create an item master then update it.
		if (result != null) {
			result.setDescription(inDescription);
			result.setStandardUom(inUomMaster);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				ItemMaster.staticGetDao().store(result);
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
		DefaultErrors errors = new DefaultErrors(UomMaster.class);
		if (Strings.emptyToNull(inUomId) == null) {
			errors.rejectValue("uomMasterId", inUomId, ErrorCode.FIELD_REQUIRED);
			throw new InputValidationException(errors);
		}

		UomMaster result = null;

		result = inFacility.getUomMaster(inUomId);

		if (result == null) {
			result = new UomMaster();
			result.setUomMasterId(inUomId);
			inFacility.addUomMaster(result);

			try {
				UomMaster.staticGetDao().store(result);
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
	private Item updateDdcItem(final InventoryDdcCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) {
		Item result = null;

		// Get or create the item at the specified location.
		if ((result == null) && (inCsvBean.getItemId() != null) && (inCsvBean.getItemId().length() > 0)) {
			result = inItemMaster.findOrCreateItem(inFacility, inUomMaster);
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setQuantity(Double.valueOf(inCsvBean.getQuantity()));
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mItemDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		} else {
			LOGGER.warn("Failed to update inventory item with ID " + inCsvBean.getItemId());
		}

		return result;
	}
	 */

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inItemMaster
	 * @param inUomMaster
	 * @return
	 */
	public Item updateSlottedItem(boolean useLenientValidation,
		final InventorySlottedCsvBean inCsvBean,
		final Location inLocation,
		final Timestamp inEdiProcessTime,
		final ItemMaster inItemMaster,
		final UomMaster inUomMaster) throws InputValidationException {

		DefaultErrors errors = new DefaultErrors(Item.class);
		if (inLocation == null) {
			errors.rejectValue("storedLocation", inLocation, ErrorCode.FIELD_REQUIRED);
		}

		if (inItemMaster == null) {
			errors.rejectValue("parent", inItemMaster, ErrorCode.FIELD_REQUIRED);
		}

		Double quantity = 0.0d;
		String quantityString = inCsvBean.getQuantity();
		try {
			quantity = Double.valueOf(quantityString);
			if (quantity < 0.0d) {
				quantity = 0.0d;
				errors.rejectValue("quantity", quantity, ErrorCode.FIELD_NUMBER_NOT_NEGATIVE);
			}
		} catch (NumberFormatException e) {
			errors.rejectValue("quantity", quantityString, ErrorCode.FIELD_NUMBER_REQUIRED);
		}

		if (errors.hasErrors() && !useLenientValidation) {
			throw new InputValidationException(errors);
		}

		Item result = inItemMaster.findOrCreateItem(inLocation, inUomMaster);
		
		// Refine using the cm value if there is one
		String cmFromLeftString = inCsvBean.getCmFromLeft();
		if (!Strings.isNullOrEmpty(cmFromLeftString)) {
			if (inLocation != null) {
				if (inLocation.isFacility()) {
					errors.rejectValue("storedLocation", inLocation, ErrorCode.FIELD_WRONG_TYPE);
				}
			}
			else {
				errors.rejectValue("storedLocation", null, ErrorCode.FIELD_REQUIRED); //when cmFromLeft
			}
			try {
				result.setCmFromLeftui(cmFromLeftString);
			}
			catch (InputValidationException e) {
				errors.addAllErrors(e.getErrors());
			}
		}

		if (errors.hasErrors()) {
			if (!useLenientValidation) {
				throw new InputValidationException(errors);
			}
		}

		result.setQuantity(quantity);
		result.setActive(true);
		result.setUpdated(inEdiProcessTime);

		Item.staticGetDao().store(result);
		return result;
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.INVENTORY_SLOTTED);
	}
}
