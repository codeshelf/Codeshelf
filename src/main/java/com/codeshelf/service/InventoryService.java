package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Che;
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
import com.google.common.collect.ImmutableList;

public class InventoryService implements IApiService {

	
	public Item moveOrCreateInventory(String inGtin, String inLocation, UUID inChePersistentId){
		
		Che che = Che.staticGetDao().findByPersistentId(inChePersistentId);
		Facility facility = che.getFacility();
		
		Location location = facility.findSubLocationById(inLocation);
		// Remember, findSubLocationById will find inactive locations.
		// We couldn't find the location, so assign the inventory to the facility itself (which is a location);  Not sure this is best, but it is the historical behavior from pre-v1.
		if (location == null) {
			location = facility;
		}
		// If location is inactive, then what? Would we want to move existing inventory there to facility? Doing that initially mostly because it is easier.
		// Might be better to ask if this inventory item is already in that inactive location, and not move it if so.
		else if (!location.isActive()) {
			location = facility;
		}

		List<Gtin> gtins = Gtin.staticGetDao().findByFilter(ImmutableList.<Criterion>of(Restrictions.eq("domainId", inGtin)));
		
		ItemMaster itemMaster = null;
		UomMaster uomMaster = null;
		Gtin gtin = null;
		
		if (gtins.isEmpty()) {
			// If we don't have the GTIN we are assuming this is a new item.
			// Will not add GTIN to existing item.
			Timestamp createTime = new Timestamp(System.currentTimeMillis());
			uomMaster = guessUomForItem(inGtin, facility);
			itemMaster = createItemMaster(inGtin, facility, createTime, uomMaster);
			gtin = itemMaster.createGtin(inGtin, uomMaster);
			
			Gtin.staticGetDao().store(gtin);
		} else {
			gtin = gtins.get(0);
			itemMaster = gtin.getParent();
			uomMaster = gtin.getUomMaster();
		}
		
		// This will find/create. If found it will move if necessary
		// Look in this function call for whether a item will be moved or created
		Item result = itemMaster.findOrCreateItem(location, uomMaster);
		
		return result;
	}
	
	
	public boolean lightInventory(String inGTIN, UUID inChePersistentId){
		
		return false;
	}
	
	private ItemMaster createItemMaster(final String inItemId,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final UomMaster inUomMaster) {
		ItemMaster result = null;


		result = new ItemMaster();
		result.setDomainId(inItemId);
		result.setItemId(inItemId);
		inFacility.addItemMaster(result);


		// If we were able to get/create an item master then update it.
		if (result != null) {
			result.setStandardUom(inUomMaster);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				ItemMaster.staticGetDao().store(result);
			} catch (DaoException e) {
				//LOGGER.error("updateItemMaster", e);
			}
		}
		return result;
	}
	
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
				//LOGGER.error("upsertUomMaster save", e);
			}
			/*
			catch (InputValidationException e) {
				// can we catch this here? If the UOM did not save, the next transaction might fail trying to reference it
				LOGGER.error("upsertUomMaster validate", e);
			} */
		}

		return result;
	}
	
	private UomMaster guessUomForItem(String inGtin, Facility inFacility){
		String defaultUom = "EA";
		return upsertUomMaster(defaultUom, inFacility);
	}
}
