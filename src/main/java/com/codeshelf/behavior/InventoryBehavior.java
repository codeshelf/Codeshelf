package com.codeshelf.behavior;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.CodeshelfTape;
import com.codeshelf.model.CodeshelfTape.TapeLocation;
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
import com.codeshelf.ws.protocol.response.InventoryLightItemResponse;
import com.codeshelf.ws.protocol.response.InventoryUpdateResponse;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class InventoryBehavior implements IApiBehavior {

	int							TAPEID_LENGTH	= 12;
	LightBehavior				lightService;
	private static final Logger	LOGGER			= LoggerFactory.getLogger(InventoryBehavior.class);

	@Inject
	public InventoryBehavior(LightBehavior inLightService) {
		this.lightService = inLightService;
	}

	public InventoryUpdateResponse moveOrCreateInventory(String inGtin,
		String inLocation,
		UUID inChePersistentId,
		String activeSkuWall) {
		// At this point, inLocation may be a location name (usually alias), or if a tape Id, it still has the % prefix

		LOGGER.info("moveOrCreateInventory called for gtin:{}, location:{}", inGtin, inLocation);

		InventoryUpdateResponse response = new InventoryUpdateResponse();
		Che che = Che.staticGetDao().findByPersistentId(inChePersistentId);
		if (che == null) {
			LOGGER.error("Could not load che: {}", inChePersistentId.toString());
			response.appendStatusMessage("moveOrCreateInventory ERROR: Could not find che: " + inChePersistentId.toString());
			response.setFoundGtin(false);
			response.setFoundLocation(false);
			response.setStatus(ResponseStatus.Fail);
			return response;
		}

		Facility facility = che.getFacility();

		if (inLocation == null || inLocation.isEmpty()) {
			LOGGER.error("Location not specified for GTIN: {}, CHE: {}", inGtin, che.getDomainId());
			response.appendStatusMessage("moveOrCreateInventory ERROR: Location is not specified.");
			response.setFoundGtin(false);
			response.setFoundLocation(false);
			response.setStatus(ResponseStatus.Fail);
			return response;
		}

		Location location = findLocation(facility, inLocation);

		//If the Inventory process in performed while putting a new item into the Sku wall, do not permit placement into other waklls (CD_0099B)
		if (activeSkuWall != null) {
			Location wallLocation = findLocation(facility, activeSkuWall);
			if (wallLocation.equals(facility)) {
				LOGGER.error("Could not locate wall {} (optional parameter)", activeSkuWall);
				response.appendStatusMessage("moveOrCreateInventory ERROR: Could not locate wall " + activeSkuWall
						+ " (optional parameter).");
				response.setFoundGtin(false);
				response.setFoundLocation(false);
				response.setStatus(ResponseStatus.Fail);
				return response;
			}
			if (!wallLocation.hasDescendant(location)) {
				LOGGER.error("Location {} is not within wall {}", inLocation, activeSkuWall);
				response.appendStatusMessage("moveOrCreateInventory ERROR: Location " + inLocation + " is not within wall "
						+ activeSkuWall);
				response.setFoundGtin(false);
				response.setFoundLocation(false);
				response.setStatus(ResponseStatus.Fail);
				return response;
			}
		}

		int cmFromLeft = 0;
		if (location.equals(facility)) {
			LOGGER.warn("Move request from CHE: {} for GTIN: {} could not resolve location: {}. Using facility as location.",
				che.getDomainId(),
				inGtin,
				inLocation);
			response.setFoundLocation(false);
			response.appendStatusMessage("moveOrCreateInventory WARN: Could not find location: " + inLocation + ". Using facility.");
		} else if (inLocation.startsWith(CheDeviceLogic.TAPE_PREFIX)) {
			TapeLocation tapeLocation = CodeshelfTape.findFinestLocationForTape(inLocation);
			location = tapeLocation.getLocation();
			cmFromLeft = tapeLocation.getCmOffset();
		}

		ItemMaster itemMaster = null;
		Timestamp createTime = null;
		UomMaster uomMaster = null;
		Gtin gtin = Gtin.getGtinForFacility(facility, inGtin);

		if (gtin == null) {
			// Creating a new item if we cannot find GTIN
			LOGGER.info("GTIN {} was not found.", inGtin);
			response.appendStatusMessage(" Could not find GTIN: " + inGtin + ". Attempting to create GTIN: " + inGtin);
			response.setFoundGtin(false);

			createTime = new Timestamp(System.currentTimeMillis());
			String guessedUom = guessUomForItem(inGtin, facility);
			uomMaster = upsertUomMaster(guessedUom, facility);

			try {
				itemMaster = createItemMaster(inGtin, facility, createTime, uomMaster);
			} catch (DaoException e) {
				LOGGER.error("Unable to create ItemMaster for GTIN: {}", inGtin, e);
				response.appendStatusMessage(" Failed to create item master for GTIN: " + inGtin);
				response.setStatus(ResponseStatus.Fail);
				return response;
			}

			gtin = itemMaster.createGtin(inGtin, uomMaster);
			if (gtin != null) {
				LOGGER.info("Created new gtin:{} for sku:{}/{}", gtin.getGtin(), itemMaster.getItemId(), uomMaster.getUomMasterId());
				try {
					Gtin.staticGetDao().store(gtin);
				} catch (DaoException e) {
					LOGGER.error("Unable to store new GTIN: {}", gtin, e);
				}
			} else {
				LOGGER.error("Was unable to create GTIN for GTIN: {}", inGtin);
				response.appendStatusMessage(" Could not create GTIN: " + inGtin);
			}

		} else {
			itemMaster = gtin.getParent();
			uomMaster = gtin.getUomMaster();
			response.setFoundGtin(true);
		}

		Item result = null;
		if (itemMaster != null) {
			// findOrdCreateItem determines if an item is created or moved 
			result = itemMaster.findOrCreateItem(location, uomMaster);
			LOGGER.info("Item: {} is in location(s): {}", result.getDomainId(), result.getParent().getItemLocations());
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
			result.setCmFromLeft(cmFromLeft);

			Item.staticGetDao().store(result);

			lightService.lightItemSpecificColor(facility.getPersistentId().toString(),
				result.getPersistentId().toString(),
				che.getColor());
			response.setStatus(ResponseStatus.Success);
		} else {
			LOGGER.error("Was unable to create/get item with GTIN: {} Loc: {} UOM: {}",
				inGtin,
				location.toString(),
				uomMaster.getDomainId());
			response.setStatus(ResponseStatus.Fail);
		}

		return response;
	}

	public InventoryLightItemResponse lightInventoryByGtin(String inGtin, UUID inChePersistentId) {
		InventoryLightItemResponse response = new InventoryLightItemResponse();

		Che che = Che.staticGetDao().findByPersistentId(inChePersistentId);
		if (che == null) {
			LOGGER.error("Could not load che: {}", inChePersistentId.toString());
			response.appendStatusMessage("lightInventoryByGtin ERROR: Could not find che: " + inChePersistentId.toString());
			response.setFoundGtin(false);
			response.setFoundLocation(false);
			response.setStatus(ResponseStatus.Fail);
			return response;
		}
		ColorEnum color = che.getColor();
		Facility facility = che.getFacility();

		List<Gtin> gtins = Gtin.staticGetDao().findByFilter(ImmutableList.<Criterion> of(Restrictions.eq("domainId", inGtin)));
		if (gtins.isEmpty()) {
			LOGGER.info("GTIN: {} requested to light by CHE: {} was not found.", inGtin, che.getDomainId());
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
		lightService.lightItemsSpecificColor(facility.getPersistentId().toString(), items, color);

		response.setStatus(ResponseStatus.Success);
		return response;
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
				LOGGER.error("Error saving UOM: {}", e);
			}
			/*
			catch (InputValidationException e) {
				// can we catch this here? If the UOM did not save, the next transaction might fail trying to reference it
				LOGGER.error("upsertUomMaster validate", e);
			} */
		}

		return result;
	}

	/**
	 * Sets the tapeId of a location (aisle, bay, tier, slot).
	 * 
	 * A tapeId can only be associated with a single location. If
	 * the inTapeId is already associated with a location it will be dissociated
	 * from its current location and associated with inLocation.
	 * This will produce a warning in the log.
	 * 
	 * If the inLocation does not exist the inTapeId will not be associated to anything.
	 * 
	 * If the inLocation already has an associated tapeId it will be dissociated and
	 * the inTapeId will be associated with the inLocation.
	 * 
	 * @param inFacility	The facility
	 * @param inTapeId		The tapeId to associate (Should not include distance offset)
	 * @param inLocation	The alias of the location to associate the tape
	 * @return void
	 * 
	 */
	public void setLocationTapeId(Facility inFacility, int inTapeId, String inLocation) {
		// tapeId is associated with a location
		// location already has a tapeId

		// Find location
		Location location = findLocation(inFacility, inLocation);
		if (location.equals(inFacility)) {
			LOGGER.warn("Could not find location: {}. TapeId: {} will not be associated.", inLocation, inTapeId);
			return;
		}

		// Find location that tapeId is already associated with
		Location oldLocation = CodeshelfTape.findLocationForTapeId(inTapeId);
		if (oldLocation != null) {
			LOGGER.warn("TapeId: {} is already associated with location: {}.", inTapeId, oldLocation.getDomainId());
			oldLocation.setTapeId(null);
			LOGGER.warn("TapeId: {} dissociated from location: {}", inTapeId, oldLocation.getDomainId());

			if (!location.isActive()) {
				LOGGER.warn("Location: {} is not active", location.getDomainId());
			}
		}

		// Check if location already has an associated tapeId
		if (location.getTapeId() != null) {
			LOGGER.warn("Location: {} is already associated with tapeId: {}. It will be dissociated.",
				location.getDomainId(),
				inTapeId);
		}

		// Associate with new location
		location.setTapeId(inTapeId);
		LOGGER.info("TapeId: {} associated with location: {}", inTapeId, location.getDomainId());
	}

	/**
	 * Lights a location by either location alias or tapeId
	 * 
	 * @param inLocation	location alias or tapeId
	 * @param isTape		is this a tapeId or a location alias
	 * @param inChePersistentId	persistentId of che making call
	 * @return void
	 */
	public void lightLocationByAliasOrTapeId(String inLocation, boolean isTape, UUID inChePersistentId) {
		Che che = Che.staticGetDao().findByPersistentId(inChePersistentId);
		// We could log a CHE_DISPLAY

		ColorEnum color = ColorEnum.RED;
		if (che != null) {
			color = che.getColor();
		} else {
			LOGGER.error("CHE is not resolved in lightLocationByAliasOrTapeId");
		}

		Location locToLight = null;
		Integer cmOffSet = 0;
		if (isTape) {
			// This might be guid for a tier that has slots, we would interpret the slot by the cmOffset.
			TapeLocation tapeLocation = CodeshelfTape.findFinestLocationForTape(inLocation);
			locToLight = tapeLocation.getLocation();
			if (locToLight != null && locToLight.isSlot()) {
				lightService.lightLocationServerCall(locToLight, color);
				return;
			}
			cmOffSet = tapeLocation.getCmOffset();
		} else {
			// can we find the facility somehow?
			Facility facility = null;
			if (che != null)
				facility = che.getFacility();
			if (facility != null) {
				locToLight = facility.findSubLocationById(inLocation);
			}
		}
		if (locToLight != null) {
			lightService.lightLocationCmFromLeft(locToLight, cmOffSet, color);
		}

	}

	/**
	 * Guesses the unit of measure for an item based on the GTIN and the facility.
	 * 
	 * @param inGtin		The GTIN of item to guess uom for.
	 * @param inFacility	The facility.
	 * @return String		The guessed UOM for the specified GTIN in specified facility.
	 * 
	 */
	private String guessUomForItem(String inGtin, Facility inFacility) {
		return "EA";
	}

	/**
	 * The inLocation may be a location or alias name, or from v17 a tapeId with leading %
	 */
	private Location findLocation(Facility inFacility, String inLocation) {
		if (inFacility == null) {
			LOGGER.error("null facility in findLocation()");
			// nothing safe to return. Let it NPE below to log out the stack trace.
		}
		if (inLocation == null || inLocation.isEmpty()) {
			LOGGER.error("null or empty location string in findLocation()");
			return inFacility;
		}
		Location location = inFacility.findSubLocationById(inLocation);

		// Remember, findSubLocationById will find inactive locations.
		// We couldn't find the location, so assign the inventory to the facility itself (which is a location);  Not sure this is best, but it is the historical behavior from pre-v1.
		if (location == null) {
			LOGGER.warn("Could not find location: {}. Using facility.", inLocation);
			location = inFacility;
		}

		//We do not need to drill down to a Slot in this case, return Tier 
		if (inLocation.startsWith(CheDeviceLogic.TAPE_PREFIX) && location.isSlot()) {
			location = location.getParent();
		}

		// If location is inactive, then what? Would we want to move existing inventory there to facility? Doing that initially mostly because it is easier.
		// Might be better to ask if this inventory item is already in that inactive location, and not move it if so.
		else if (!location.isActive()) {
			LOGGER.warn("Location {} is inactive. Using facility.", inLocation);
			location = inFacility;
		}

		return location;
	}

	private ItemMaster createItemMaster(final String inItemId,
		final Facility inFacility,
		final Timestamp inEdiProcessTime,
		final UomMaster inUomMaster) {

		ItemMaster result = new ItemMaster(inFacility, inItemId, inUomMaster);
		result.setUpdated(inEdiProcessTime);
		ItemMaster.staticGetDao().store(result);
		return result;
	}

}
