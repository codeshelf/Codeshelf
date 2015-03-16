package com.codeshelf.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.LineScanDeviceLogic;
import com.codeshelf.flyweight.command.ColorEnum;
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
import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.codeshelf.ws.jetty.protocol.response.InventoryLightResponse;
import com.codeshelf.ws.jetty.protocol.response.InventoryUpdateResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

public class InventoryService implements IApiService {

	LightService lightService;
	private static final Logger	LOGGER	= LoggerFactory.getLogger(LineScanDeviceLogic.class);
	private final static int				defaultLedsToLight			= 4; 	// IMPORTANT. This should be synched with WIFactory.maxLedsToLight
	
	@Inject
	public InventoryService(LightService inLightService){
		this.lightService = inLightService;
	}
	
	public InventoryUpdateResponse moveOrCreateInventory(String inGtin, String inLocation, UUID inChePersistentId){
		
		InventoryUpdateResponse response = new InventoryUpdateResponse();
		Che che = Che.staticGetDao().findByPersistentId(inChePersistentId);
		if (che == null) {
			response.appendStatusMessage("moveOrCreateInventory ERROR: Could not find che: " + inChePersistentId.toString());
			response.setFoundGtin(false);
			response.setFoundLocation(false);
			response.setStatus(ResponseStatus.Fail);
			return response;
		}
		
		Facility facility = che.getFacility();
		
		if (inLocation == null || inLocation.isEmpty()) {
			response.appendStatusMessage("moveOrCreateInventory ERROR: Location is not specified.");
			response.setFoundGtin(false);
			response.setFoundLocation(false);
			response.setStatus(ResponseStatus.Fail);
			return response;
		}
		
		Location location = findLocation(facility, inLocation);
		if (location.equals(facility)) {
			response.setFoundLocation(false);
			response.appendStatusMessage("moveOrCreateInventory WARN: Could not find location: " + inLocation + ". Using facility.");
		}

		List<Gtin> gtins = Gtin.staticGetDao().findByFilter(ImmutableList.<Criterion>of(Restrictions.eq("domainId", inGtin)));
		
		Timestamp createTime = null;
		ItemMaster itemMaster = null;
		UomMaster uomMaster = null;
		Gtin gtin = null;
		
		if (gtins.isEmpty()) {
			// Creating a new item if we cannot find GTIN
			response.appendStatusMessage(" Could not find GTIN: " + inGtin + ". Attempting to create GTIN: " + inGtin);
			response.setFoundGtin(false);
			
			createTime = new Timestamp(System.currentTimeMillis());
			uomMaster = guessUomForItem(inGtin, facility);
			
			itemMaster = createItemMaster(inGtin, facility, createTime, uomMaster);
			if (itemMaster == null) {
				response.appendStatusMessage(" Failed to create item master for GTIN: " + inGtin);
				LOGGER.error("Unable to create ItemMaster for GTIN: {}", inGtin);
				response.setStatus(ResponseStatus.Fail);
				return response;
			}
			
			gtin = itemMaster.createGtin(inGtin, uomMaster);
			if (gtin != null) {
				Gtin.staticGetDao().store(gtin);
			} else {
				response.appendStatusMessage(" Could not create GTIN: " + inGtin);
				LOGGER.error("Was unable to create GTIN for GTIN: {}", inGtin);
			}

		} else {
			gtin = gtins.get(0);
			itemMaster = gtin.getParent();
			uomMaster = gtin.getUomMaster();
			response.setFoundGtin(true);
		}

		Item result = null;
		if (itemMaster != null ) {
			// findOrdCreateItem determines if an item is created or moved 
			result = itemMaster.findOrCreateItem(location, uomMaster);
		}
		
		if (result != null) {
			if (result.getQuantity() == null) {
				result.setQuantity(0.0);
			}
			
			if (result.getCmFromLeft() == null) {
				result.setCmFromLeft(0);
				result.setCmFromLeftui("0");
			}
			
			createTime = new Timestamp(System.currentTimeMillis());
			result.setActive(true);
			result.setUpdated(createTime);
			Item.staticGetDao().store(result);
			
			lightService.lightItemSpecificColor(facility.getPersistentId().toString(), result.getPersistentId().toString(), che.getColor());
			response.setStatus(ResponseStatus.Success);
		} else {
			LOGGER.error("Was unable to create/get item with GTIN: {} Loc: {} UOM: {}", inGtin, location.toString(), uomMaster.getDomainId());
			response.setStatus(ResponseStatus.Fail);
		}
		
		return response;
	}
	
	
	public InventoryLightResponse lightInventoryByGtin(String inGtin, UUID inChePersistentId){
		InventoryLightResponse response = new InventoryLightResponse();
		
		Che che = Che.staticGetDao().findByPersistentId(inChePersistentId);
		ColorEnum color = che.getColor();
		Facility facility = che.getFacility();
		
		List<Gtin> gtins = Gtin.staticGetDao().findByFilter(ImmutableList.<Criterion>of(Restrictions.eq("domainId", inGtin)));
		if (gtins.isEmpty()) {
			response.setFoundGtin(false);
			response.setStatus(ResponseStatus.Fail);
			response.appendStatusMessage("Cound not find GTIN");
			return response;
		} else {
			response.setFoundGtin(true);
		}
		
		Gtin gtin = gtins.get(0);
		ItemMaster itemMaster = gtin.getParent();
		List<Item> items = itemMaster.getItemsOfUom(gtin.getUomMaster().getDomainId());
		
		Future<Void> sl = lightService.lightItemsSpecificColor(facility.getPersistentId().toString(), items, color);
		try {
			sl.get();
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		} catch (ExecutionException e) {
			LOGGER.error("", e);
		}
				
		response.setStatus(ResponseStatus.Success);
		return response;
	}
	
	private ItemMaster createItemMaster(final String inItemId,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final UomMaster inUomMaster) {
		
		ItemMaster result = null;
		result = new ItemMaster();
		
		// If we were able to get/create an item master then update it.
		if (result != null) {
			
			result.setDomainId(inItemId);
			result.setItemId(inItemId);
			inFacility.addItemMaster(result);
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
	
	private Location findLocation(Facility inFacility, String inLocation) {
		Location location = inFacility.findSubLocationById(inLocation);

		// Remember, findSubLocationById will find inactive locations.
		// We couldn't find the location, so assign the inventory to the facility itself (which is a location);  Not sure this is best, but it is the historical behavior from pre-v1.
		if (location == null) {
			location = inFacility;
		}
		// If location is inactive, then what? Would we want to move existing inventory there to facility? Doing that initially mostly because it is easier.
		// Might be better to ask if this inventory item is already in that inactive location, and not move it if so.
		else if (!location.isActive()) {
			location = inFacility;
		}
		
		return location;
	}
}
