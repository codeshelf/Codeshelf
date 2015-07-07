/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.LedSample;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.device.PosControllerInstr.PosConInstrGroupSerializer;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.LightService;
import com.codeshelf.util.SequenceNumber;
import com.google.common.base.Strings;

/**
 * This generates work instructions
 * First the new housekeeping work instructions. Then normal and shorts also.
 *
 */
public class WiFactory {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(WiFactory.class);

	// For now, a public enum, but not stored on the WI. Just passed along in create methods so that we know what the location and/or order location mean.
	public enum WiPurpose {
		WiPurposeUnknown,
		WiPurposeHousekeep,
		WiPurposeOutboundPick, // this is still multipurpose: SKU-pick, detail-pick, and pick-to-order.
		WiPurposePutWallPut,
		WiPurposeCrossBatchPut,
		WiPurposeReplenishPut,
		WiPurposeRestockPut
	}

	// IMPORTANT. This should be synched with LightService.defaultLedsToLight
	private static final int	maxLedsToLight	= 4;

	public static WorkInstruction createForLocation(Location inLocation) {
		long seq = SequenceNumber.generate();
		String wiDomainId = Long.toString(seq);

		WorkInstruction resultWi = new WorkInstruction();
		resultWi.setParent(inLocation.getFacility());
		resultWi.setDomainId(wiDomainId);
		resultWi.setCreated(new Timestamp(System.currentTimeMillis()));
		resultWi.setLedCmdStream("[]"); // empty array
		resultWi.setStatus(WorkInstructionStatusEnum.NEW); // perhaps there could be a general housekeep status as there is for short,
		// but short denotes completion as short, even if it was short from the start and there was never a chance to complete or short.

		resultWi.setLocation(inLocation);
		resultWi.setLocationId(inLocation.getFullDomainId());
		if (inLocation.isFacility())
			resultWi.setPosAlongPath(0.0);
		else {
			resultWi.setPosAlongPath(inLocation.getPosAlongPath());
		}

		resultWi.setType(WorkInstructionTypeEnum.PLAN);
		resultWi.setPickInstruction("");
		resultWi.setItemMaster(null);
		resultWi.setContainer(null);
		resultWi.setOrderDetail(null);
		resultWi.setPlanQuantity(0);
		resultWi.setPlanMinQuantity(0);
		resultWi.setPlanMaxQuantity(0);
		resultWi.setActualQuantity(0);
		return resultWi;
	}

	/**
	 * The API to create housekeeping work instruction
	 */
	public static WorkInstruction createHouseKeepingWi(WorkInstructionTypeEnum inType,
		Facility inFacility,
		WorkInstruction inPrevWi,
		WorkInstruction inNextWi) {

		// Let's declare that inPrevWi must be there, but inNextWi might be null (if for example there is QA function after the last one done).
		if (inPrevWi == null) {
			LOGGER.error("unexpected null CHE in createHouseKeepingWi");
			return null;
		}
		// These values may come from the wi parameters
		Che ourChe = inPrevWi.getAssignedChe();
		if (ourChe == null) {
			LOGGER.error("unexpected null CHE in createHouseKeepingWi");
			return null;
		}
		Timestamp assignTime = inPrevWi.getAssigned();
		Container ourCntr = inPrevWi.getContainer();

		WorkInstruction resultWi = new WorkInstruction();
		resultWi.setOrderDetail(null);
		resultWi.setCreated(new Timestamp(System.currentTimeMillis()));
		resultWi.setLedCmdStream("[]"); // empty array
		resultWi.setParent(inFacility);
		setWorkInstructionLedPatternForHK(resultWi, inType, inPrevWi);

		long seq = SequenceNumber.generate();
		String wiDomainId = Long.toString(seq);
		resultWi.setDomainId(wiDomainId);
		resultWi.setType(inType);
		resultWi.setStatus(WorkInstructionStatusEnum.NEW); // perhaps there could be a general housekeep status as there is for short,
		// but short denotes completion as short, even if it was short from the start and there was never a chance to complete or short.

		resultWi.setLocation(inFacility);
		resultWi.setLocationId(inFacility.getFullDomainId());
		resultWi.setItemMaster(null);
		resultWi.setDescription(getDescriptionForHK(inType));
		resultWi.doSetPickInstruction(getPickInstructionForHK(inType)); // This is normally the location name
		resultWi.setPosAlongPath(inPrevWi.getPosAlongPath()); // Need to matche this to the surrounding work instructions. Or else when che scans start location, these may filter out or reorder.

		resultWi.setPlanQuantity(0);
		resultWi.setPlanMinQuantity(0);
		resultWi.setPlanMaxQuantity(0);
		resultWi.setActualQuantity(0);
		resultWi.setParent(inFacility);

		// The container provides our map to the position controller. That is the only way the user can acknowledge the housekeeping command.
		resultWi.setContainer(ourCntr);
		resultWi.setAssigned(assignTime);
		ourChe.addWorkInstruction(resultWi);

		try {
			WorkInstruction.staticGetDao().store(resultWi);
		} catch (DaoException e) {
			LOGGER.error("createHouseKeepingWi", e);
		}

		return resultWi;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a work instruction for and order item quantity picked into a container at a location.
	 * @param inStatus
	 * @param inType
	 * @param inOrderDetail
	 * @param inContainer
	 * @param inChe
	 * @param inLocation
	 * @param inTime
	 * @return
	 */
	public static WorkInstruction createWorkInstruction(WorkInstructionStatusEnum inStatus,
		WorkInstructionTypeEnum inType,
		OrderDetail inOrderDetail,
		Container inContainer,
		Che inChe,
		Location inLocation,
		final Timestamp inTime,
		WiPurpose purpose) throws DaoException {
		return createWorkInstruction(inStatus, inType, inOrderDetail, inContainer, inChe, inLocation, inTime, purpose, true);
	}

	/**
	 * Create a work instruction for and order item quantity picked into a container at a location.
	 * @param inStatus
	 * @param inType
	 * @param inOrderDetail
	 * @param inContainer
	 * @param inChe
	 * @param inLocation
	 * @param inTime
	 * @return
	 */
	public static WorkInstruction createWorkInstruction(WorkInstructionStatusEnum inStatus,
		WorkInstructionTypeEnum inType,
		OrderDetail inOrderDetail,
		Container inContainer,
		Che inChe,
		Location inLocation,
		final Timestamp inTime,
		WiPurpose purpose,
		boolean linkInstructionToDetail) throws DaoException {

		WorkInstruction resultWi = createWorkInstruction(inStatus, inType, inOrderDetail, inChe, linkInstructionToDetail, inTime);
		if (resultWi == null) { //no more work to do
			return null;
		}

		resultWi.setContainer(inContainer);
		resultWi.setLocation(inLocation);
		resultWi.setLocationId(inLocation.getFullDomainId());
		LocationAlias locAlias = inLocation.getPrimaryAlias();
		if (inOrderDetail.isPreferredDetail()) {
			resultWi.doSetPickInstruction(inOrderDetail.getPreferredLocation());
		} else if (locAlias != null) {
			resultWi.doSetPickInstruction(locAlias.getAlias());
		} else {
			resultWi.doSetPickInstruction(resultWi.getLocationId());
		}

		boolean isInventoryPickInstruction = false;
		ColorEnum cheColor = inChe.getColor();

		// Set the LED lighting pattern for this WI.
		if (inStatus == WorkInstructionStatusEnum.SHORT) {
			// But not if it is a short WI (made to the facility location)
		} else {
			if (inOrderDetail.getParent().getOrderType().equals(OrderTypeEnum.CROSS)) {
				// We currently have no use case that gets here. We never make direct work instruction from Cross order (which is a vendor put away).
				setCrossWorkInstructionLedPattern(resultWi,
					inOrderDetail.getItemMasterId(),
					inLocation,
					inOrderDetail.getUomMasterId(),
					cheColor);
				setPosConInstructions(resultWi, inLocation);
				LOGGER.error("unexpected call to code that should be dead."); // If we see this, investigate and build a unit test.
			} else {
				// This might be a cross batch case, or a put wall put. Position and leds come from the outbound order.			
				if (purpose == WiPurpose.WiPurposeCrossBatchPut || purpose == WiPurpose.WiPurposePutWallPut) {
					// Then the location and lights come from order location
					OrderHeader passedInDetailParent = inOrderDetail.getParent();
					setWorkInstructionLedPatternFromOrderLocations(resultWi, passedInDetailParent, cheColor);
					setPosConInstructions(resultWi, passedInDetailParent.getActiveOrderLocations());
				}
				// for now, assume a pick. As of April 2015 and v15, no replenish or restock.
				else {
					isInventoryPickInstruction = true;
					setOutboundWorkInstructionLedPatternAndPosAlongPathFromInventoryItem(resultWi,
						inLocation,
						inOrderDetail.getItemMasterId(),
						inOrderDetail.getUomMasterId(),
						cheColor);
					setPosConInstructions(resultWi, inLocation);
				}
			}
		}
		Facility facility = inOrderDetail.getParent().getFacility();
		if (inLocation.isFacility() || facility.getUnspecifiedLocation().equals(inLocation)) {
			//consider inLocation.getPathSegment() == null ; Note that the getWork by location queries for > posAlongPath
			resultWi.setPosAlongPath(0.0);
		} else {
			if (isInventoryPickInstruction) {
				// do nothing as it was set with the leds
			} else {
				resultWi.setPosAlongPath(inLocation.getPosAlongPath());
			}
		}

		WorkInstruction.staticGetDao().store(resultWi);
		return resultWi;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a work instruction for an orderdetail with no location or container
	 * @return
	 */
	public static WorkInstruction createWorkInstruction(WorkInstructionStatusEnum inStatus,
		WorkInstructionTypeEnum inType,
		OrderDetail inOrderDetail,
		Che inChe,
		boolean linkInstructionToDetail,
		final Timestamp inTime) throws DaoException {
		Facility facility = inOrderDetail.getFacility();
		Integer qtyToPick = inOrderDetail.getQuantity();
		Integer minQtyToPick = inOrderDetail.getMinQuantity();
		Integer maxQtyToPick = inOrderDetail.getMaxQuantity();

		WorkInstruction resultWi = null;
		// Important for DEV-592 note below. If there is a WI already on the order detail of type PLAN, we will recycle it.
		for (WorkInstruction wi : inOrderDetail.getWorkInstructions()) {
			if (wi.getType().equals(WorkInstructionTypeEnum.PLAN)) {
				resultWi = wi;
				if (!wi.getFacility().equals(inOrderDetail.getFacility())) {
					LOGGER.error("Strange: Work instruction " + resultWi.getPersistentId() + " in OrderDetail "
							+ inOrderDetail.getDomainId() + " does not belong to Facility "
							+ inOrderDetail.getFacility().getDomainId() + " (continuing)");
				}
				else {
					LOGGER.info("Recyle existing PLAN wi for {} from OrderDetail:{}", wi.getItemId(), inOrderDetail.getDomainId());
					// If you chase the code through, you will see that although the wi itself is recycled, every field is redone as if it were new.
				}
				break;
			} else if (wi.getType().equals(WorkInstructionTypeEnum.ACTUAL)) {
				// Deduct any WIs already completed for this line item.
				qtyToPick -= wi.getActualQuantity();
				minQtyToPick = Math.max(0, minQtyToPick - wi.getActualQuantity());
				maxQtyToPick = Math.max(0, maxQtyToPick - wi.getActualQuantity());
			}
		}

		// Check if there is any left to pick.
		if (qtyToPick > 0) {
			boolean isNewWi = false;
			// If there is no planned WI then create one.
			if (resultWi == null) {
				resultWi = new WorkInstruction();
				resultWi.setCreated(new Timestamp(System.currentTimeMillis()));
				resultWi.setLedCmdStream("[]"); // empty array
				resultWi.setStatus(WorkInstructionStatusEnum.NEW);
				resultWi.setParent(facility);
				if (linkInstructionToDetail) {
					inOrderDetail.addWorkInstruction(resultWi);
				}
				inChe.addWorkInstruction(resultWi);
				isNewWi = true;
			}

			// Update the WI
			long seq = SequenceNumber.generate();
			String wiDomainId = Long.toString(seq);
			resultWi.setDomainId(wiDomainId);
			resultWi.setType(inType);
			resultWi.setStatus(inStatus);

			resultWi.setItemMaster(inOrderDetail.getItemMaster());
			String cookedDesc = WorkInstruction.cookDescription(inOrderDetail.getItemMaster().getDescription());
			resultWi.setDescription(cookedDesc);

			resultWi.setPlanQuantity(qtyToPick);
			resultWi.setPlanMinQuantity(minQtyToPick);
			resultWi.setPlanMaxQuantity(maxQtyToPick);
			resultWi.setActualQuantity(0);
			resultWi.setAssigned(inTime);
			resultWi.setType(inType);
			
			// set gtin field on work instruction based on order detail
			resultWi.setGtin(null);
			if (inOrderDetail != null) {
				resultWi.setGtin(inOrderDetail.getGtinId());
				// set needs scan field based on order detail
				resultWi.setNeedsScan(inOrderDetail.getNeedsScan());
			}			

			// DEV-592 comments. Usually resultWi is newly made, but if setting up the CHE again, or another CHE, it may be the same old WI.
			// If the same old one, already the orderDetail and facility relationship is correct. The CHE might be correct, or not if now going to a different one.
			// Important: 	inChe.addWorkInstruction(resultWi) will fail if the wi is currently on another CHE.

			if (!isNewWi) {
				// remove and add? Or just add? Not too elegant
				Che oldChe = resultWi.getAssignedChe();
				if (oldChe != null && !oldChe.equals(inChe)) {
					oldChe.removeWorkInstruction(resultWi);
					inChe.addWorkInstruction(resultWi);
				} else if (oldChe == null || !oldChe.equals(inChe)) {
					inChe.addWorkInstruction(resultWi);
				}

			}

			resultWi.setLocation(inOrderDetail.getFacility().getUnspecifiedLocation());
			String preferredLocation = inOrderDetail.getPreferredLocation();
			resultWi.setLocationId(Strings.nullToEmpty(preferredLocation));
			resultWi.setContainer(null);
			if (inOrderDetail.getItemMaster().getDdcId() != null) {
				resultWi.doSetPickInstruction(inOrderDetail.getItemMaster().getDdcId());
			} else {
				// This is a little tricky with preferredLocation.
				// If LOCAPICK was true, the inventory was made at the preferred location, so the wi location works normally
				// If LOCAPICK was false, use the preferred location for the pick instruction, even though there is no such location.
				String locStr = resultWi.getLocationId();
				if (locStr.isEmpty() && preferredLocation != null)
					locStr = preferredLocation;
				// pickInstruction is not nullable, so must set to something, even if still blank.
				resultWi.doSetPickInstruction(locStr);
			}

			WorkInstruction.staticGetDao().store(resultWi);
		}
		return resultWi;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inOrder
	 */
	private static void setWorkInstructionLedPatternFromOrderLocations(final WorkInstruction inWi,
		final OrderHeader inOrder,
		final ColorEnum inColor) {
		// This is used for GoodEggs cross batch processs. The order header passed in is the outbound order (which has order locations),
		// but inWi was generated from the cross batch order detail.

		if (inWi == null) {
			LOGGER.error("Unexpected null WorkInstruction processing " + inOrder == null ? "null order" : inOrder.getOrderId());
			return;
		}

		// Warning: the ledCmdStream must be set to "[]" if we bail. If not, site controller will NPE. Hence the check at this late stage
		// This does not bail intentionally. Perhap should if led = 0.
		String existingCmdString = inWi.getLedCmdStream();

		if (existingCmdString == null || existingCmdString.isEmpty()) {
			inWi.setLedCmdStream("[]"); // empty array
			LOGGER.error("work instruction was not initialized");
		}

		List<LedCmdGroup> ledCmdGroupList = getLedCmdGroupListForLocationList(inOrder.getActiveOrderLocations(), inColor);
		if (ledCmdGroupList.size() > 0)
			inWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
		checkDoubleCmdStreams(inWi);
	}

	private static void setPosConInstructions(WorkInstruction wi, List<OrderLocation> locations) {
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		for (OrderLocation location : locations) {
			LightService.getInstructionsForPosConRange(wi.getParent(), wi, location.getLocation(), instructions, null);
		}
		setPosConInstructionsHelper(wi, instructions);
	}

	private static void setPosConInstructions(WorkInstruction wi, Location location) {
		if (location.isLightablePoscon()) {
			List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
			LightService.getInstructionsForPosConRange(wi.getParent(), wi, location, instructions, null);
			setPosConInstructionsHelper(wi, instructions);
		}
	}

	private static void setPosConInstructionsHelper(WorkInstruction wi, List<PosControllerInstr> instructions) {
		if (!instructions.isEmpty()) {
			String instrStr = PosConInstrGroupSerializer.serializePosConInstrString(instructions);
			wi.setPosConCmdStream(instrStr);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * For pick work instruction, set LEDs for where the inventory is. Also set the WI pos along path from where the inventory is.
	 * @param inWi
	 * @param inLocation
	 * @param inItemMasterId
	 * @param inUomId
	 */
	private static void setOutboundWorkInstructionLedPatternAndPosAlongPathFromInventoryItem(final WorkInstruction inWi,
		final Location inLocation,
		final String inItemMasterId,
		final String inUomId,
		final ColorEnum inColor) {

		if (inWi == null) {
			LOGGER.error("Unexpected null WorkInstruction processing " + inItemMasterId);
			return;
		}

		// Warning: the ledCmdStream must be set to "[]" if we bail. If not, site controller will NPE. Hence the check at this late stage
		String existingCmdString = inWi.getLedCmdStream();
		if (existingCmdString == null || existingCmdString.isEmpty()) {
			inWi.setLedCmdStream("[]"); // empty array
			LOGGER.error("work instruction was not initialized");
		}

		// This work instruction should have been generated from a pick order, so there must be inventory for the pick at the location.
		if (inWi == null || inLocation == null) {
			LOGGER.error("unexpected null condition in setOutboundWorkInstructionLedPatternFromInventoryItem");
			return;
		}
		if (inLocation.isFacility()) {
			LOGGER.error("inappropriate call to setOutboundWorkInstructionLedPatternFromInventoryItem (location is Facility)");
			return;
		}

		// We expect to find an inventory item at the location. Be sure to get item and set posAlongPath always, before bailing out on the led command.
		Double posAlongPath = null;
		Item theItem = inLocation.getStoredItemFromMasterIdAndUom(inItemMasterId, inUomId);
		if (theItem == null) {
			//The below warning was removed due to DEV-695. The error was firing needlessly when LOCAPICK was off but the preferred location was specified.
			//In the future, we'll need to handle moving preferred location whenever users move the Item in the UI
			//LOGGER.warn("did not find item in setOutboundWorkInstructionLedPatternFromInventoryItem using location" );
			posAlongPath = inLocation.getPosAlongPath();
			//return;
		} else {
			// Set the pos along path using item as it may be an unslotted item
			posAlongPath = theItem.getPosAlongPath();
		}

		inWi.setPosAlongPath(posAlongPath);

		// if the location does not have led numbers, we do not have tubes or lasers there. Do not proceed.
		if (!inLocation.isLightableAisleController())
			return;

		// if the location does not have controller associated, we would NPE below. Might as well check now.
		LedController theLedController = inLocation.getEffectiveLedController();
		if (theLedController == null) {
			LOGGER.warn("Cannot set LED pattern on new pick WorkInstruction because no aisle controller for location "
					+ inLocation.getPrimaryAliasId());
			// Note that is aisles are created with 0 LEDs, then this error is not hit. This denotes an intent to light, but probably forgot to assign controller.
			return;
		}

		// We will light the inventory where it is in blue
		List<LedCmdGroup> ledCmdGroupList = getLedCmdGroupListForItemInLocation(theItem, inColor, inLocation);

		if (ledCmdGroupList.size() > 0)
			inWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
		checkDoubleCmdStreams(inWi);
	}

	// --------------------------------------------------------------------------
	/**
	 * API to get LED group to light a location
	 */
	public List<LedCmdGroup> getLedCmdGroupListForLocation(final Location inLocation, final ColorEnum inColor) {
		return getLedCmdGroupListForItemOrLocation(null, inColor, inLocation);
	}

	// --------------------------------------------------------------------------
	/**
	 * API to get LED group to light an inventory item
	 */
	public List<LedCmdGroup> getLedCmdGroupListForInventoryItem(final Item inItem, final ColorEnum inColor) {
		Location location = inItem.getStoredLocation();
		return getLedCmdGroupListForItemOrLocation(inItem, inColor, location);
	}

	// --------------------------------------------------------------------------
	/**
	 * API used by setOutboundWorkInstructionLedPatternAndPosAlongPathFromInventoryItem
	 */
	private static List<LedCmdGroup> getLedCmdGroupListForItemInLocation(final Item inItem,
		final ColorEnum inColor,
		final Location inLocation) {
		return getLedCmdGroupListForItemOrLocation(inItem, inColor, inLocation);
	}

	// --------------------------------------------------------------------------
	/**
	 * Utility function to create LED command group. Will return a list, which may be empty if there is nothing to send. Caller should check for empty list.
	 * Called now for setting WI LED pattern for inventory pick.
	 * Also called for directly lighting inventory item or location
	 * @param inNetGuidStr
	 * @param inItem
	 * @param inColor
	 */
	//public for sake of a test
	public static List<LedCmdGroup> getLedCmdGroupListForItemOrLocation(final Item inItem,
		final ColorEnum inColor,
		final Location inLocation) {

		short firstLedPosNum = 0;
		short lastLedPosNum = 0;
		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();

		if (inLocation != null && !inLocation.isLightableAisleController()) {
			return ledCmdGroupList;
		}

		if (inItem != null) {
			// Use our utility function to get the leds for the item
			LedRange theRange = inItem.getFirstLastLedsForItem();
			firstLedPosNum = theRange.getFirstLedToLight();
			lastLedPosNum = theRange.getLastLedToLight();
		} else if (inLocation != null) { // null item. Just get the location values.
			LedRange theRange = inLocation.getFirstLastLedsForLocation();
			firstLedPosNum = theRange.getFirstLedToLight();
			lastLedPosNum = theRange.getLastLedToLight();
		} else {
			LOGGER.error("getLedCmdGroupListForItemOrLocation  no item nor location");
			return ledCmdGroupList;
		}

		LedController theLedController = inLocation.getEffectiveLedController();
		if (theLedController == null) {
			LOGGER.error("getLedCmdGroupListForItemOrLocation");
			return ledCmdGroupList;
		}
		String netGuidStr = theLedController.getDeviceGuidStr();

		// if the led number is zero, we do not have tubes or lasers there. Do not proceed.
		if (firstLedPosNum == 0)
			return ledCmdGroupList;

		// This is how we send LED data to the remote controller. In this case, only one led sample range.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		LedCmdGroup ledCmdGroup = new LedCmdGroup(netGuidStr, inLocation.getEffectiveLedChannel(), firstLedPosNum, ledSamples);

		int countUsed = 0;
		for (short ledPos = firstLedPosNum; ledPos <= lastLedPosNum; ledPos++) {
			LedSample ledSample = new LedSample(ledPos, inColor);
			ledSamples.add(ledSample);
			countUsed++;
			if (countUsed >= maxLedsToLight)
				break;
		}
		ledCmdGroup.setLedSampleList(ledSamples);

		ledCmdGroupList.add(ledCmdGroup);
		return ledCmdGroupList;
	}

	// --------------------------------------------------------------------------
	/**
	 * Utility function to create LED command group. Will return a list, which may be empty if there is nothing to send. Caller should check for empty list.
	 * Called now for setting WI LED pattern for crossbatch put. Or could possibly be called for put wall with LEDs
	 * This filters out locations that are not lightable by LED, so caller does not need to check
	 * @param inLocationList
	 * @param inColor
	 */
	private static List<LedCmdGroup> getLedCmdGroupListForLocationList(final List<OrderLocation> inLocationList,
		final ColorEnum inColor) {
		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();
		for (OrderLocation orderLocation : inLocationList) {
			Location theLocation = orderLocation.getLocation(); // this should never be null by database constraint
			if (theLocation == null) {
				LOGGER.error("null order location in getLedCmdGroupListForLocationList. How?");
				continue;
			}
			if (!theLocation.isLightableAisleController()) {
				continue;
			}

			short firstLedPosNum = theLocation.getFirstLedNumAlongPath();
			short lastLedPosNum = theLocation.getLastLedNumAlongPath();

			// Put the positions into increasing order.
			if (firstLedPosNum > lastLedPosNum) {
				Short temp = firstLedPosNum;
				firstLedPosNum = lastLedPosNum;
				lastLedPosNum = temp;
			}

			// The new way of sending LED data to the remote controller. Note getEffectiveXXX instead of getLedController
			// This will throw if aisles/tiers are not configured yet. Lets avoid by the null checks.
			LedController theController = null;
			Short theChannel = 0;
			theController = theLocation.getEffectiveLedController();
			theChannel = theLocation.getEffectiveLedChannel();

			// If this location has no controller, let's bail on led pattern
			if (theController == null || theChannel == null || theChannel == 0)
				continue; // just don't add a new ledCmdGrop to the WI command list

			List<LedSample> ledSamples = new ArrayList<LedSample>();
			LedCmdGroup ledCmdGroup = new LedCmdGroup(theController.getDeviceGuidStr(), theChannel, firstLedPosNum, ledSamples);

			// IMPORTANT. When DEV-411 resumes, change back to <=.  For now, we want only 3 LED lit at GoodEggs.
			// Bug: DEV-519 Make it work if firstLedPosNum = lastLedPosNum, but still limit to 3.
			int countUsed = 0;
			for (short ledPos = firstLedPosNum; ledPos <= lastLedPosNum; ledPos++) {
				LedSample ledSample = new LedSample(ledPos, inColor);
				ledSamples.add(ledSample);
				countUsed++;
				if (countUsed >= 3)
					break;
			}
			ledCmdGroup.setLedSampleList(ledSamples);
			ledCmdGroupList.add(ledCmdGroup);
		}
		return ledCmdGroupList;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create the LED lighting pattern for the WI.
	 * Note: no current use case gets us here
	 * @param inWi
	 * @param inOrderType
	 * @param inItemId
	 * @param inLocation
	 */
	private static void setCrossWorkInstructionLedPattern(final WorkInstruction inWi,
		final String inItemMasterId,
		final Location inLocation,
		final String inUom,
		final ColorEnum inColor) {

		if (inWi == null) {
			LOGGER.error("Unexpected null WorkInstruction processing " + inItemMasterId);
			return;
		}

		// Warning: the ledCmdStream must be set to "[]" if we bail. If not, site controller will NPE. Hence the check at this late stage
		// This does not bail intentionally. Perhap should if led = 0.
		String existingCmdString = inWi.getLedCmdStream();
		if (existingCmdString == null || existingCmdString.isEmpty()) {
			inWi.setLedCmdStream("[]"); // empty array
			LOGGER.error("work instruction was not initialized");
		}

		if (!inLocation.isLightableAisleController())
			return;

		String itemDomainId = Item.makeDomainId(inItemMasterId, inLocation, inUom);
		short firstLedPosNum = inLocation.getFirstLedPosForItemId(itemDomainId);
		short lastLedPosNum = inLocation.getLastLedPosForItemId(itemDomainId);

		// Put the positions into increasing order.
		if (firstLedPosNum > lastLedPosNum) {
			Short temp = firstLedPosNum;
			firstLedPosNum = lastLedPosNum;
			lastLedPosNum = temp;
		}

		// The new way of sending LED data to the remote controller.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();
		LedCmdGroup ledCmdGroup = new LedCmdGroup(inLocation.getEffectiveLedController().getDeviceGuidStr(),
			inLocation.getEffectiveLedChannel(),
			firstLedPosNum,
			ledSamples);

		for (short ledPos = firstLedPosNum; ledPos < lastLedPosNum; ledPos++) {
			LedSample ledSample = new LedSample(ledPos, inColor);
			ledSamples.add(ledSample);
		}
		ledCmdGroup.setLedSampleList(ledSamples);

		ledCmdGroupList.add(ledCmdGroup);
		inWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
		checkDoubleCmdStreams(inWi);
	}

	/**
	 * The main housekeeping description
	 */
	private static String getDescriptionForHK(WorkInstructionTypeEnum inType) {
		String returnStr = "";
		switch (inType) {

			case HK_REPEATPOS:
				returnStr = "Repeat Container";
				break;

			case HK_BAYCOMPLETE:
				returnStr = "Bay Change";
				break;

			default:
				returnStr = "Unknown Housekeeping Plan";
				LOGGER.error("getDescriptionForHK unknown case");
				break;
		}
		return returnStr;
	}

	/**
	 * Normally this is the location. What should show for housekeeping?
	 */
	private static String getPickInstructionForHK(WorkInstructionTypeEnum inType) {
		String returnStr = "";
		return returnStr;
	}

	private static void checkDoubleCmdStreams(WorkInstruction wi) {
		String wiCmdString = wi.getPosConCmdStream();
		if (wiCmdString == null || wiCmdString.equals("[]")) {
			return;
		}
		// so there is a poscon stream. Is there also a led command stream
		String ledCmdString = wi.getLedCmdStream();
		if (ledCmdString == null || ledCmdString.equals("[]")) {
			return;
		}
		LOGGER.error("checkDoubleCmdStreams found double lighting streams");
	}

	/**
	* Set an aisle led pattern on the inTargetWi; or do nothing
	*/
	private static void setWorkInstructionLedPatternForHK(WorkInstruction inTargetWi,
		WorkInstructionTypeEnum inType,
		WorkInstruction inPrevWi) {
		// The empty pattern is already initialized, so it is ok to do nothing and return if no aisle lighting should be done
		if (inPrevWi == null || inTargetWi == null) {
			LOGGER.error("setWorkInstructionLedPattern");
			return;
		}

		List<LedCmdGroup> ledCmdGroupList = getLedCmdGroupListForHK(inType, inTargetWi.getLocation());
		if (ledCmdGroupList.size() > 0)
			inTargetWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
		checkDoubleCmdStreams(inTargetWi);
	}

	/**
	 * ok to return null if no aisle lights. Only some kinds of housekeeps involve aisle lights.
	 */
	private static List<LedCmdGroup> getLedCmdGroupListForHK(WorkInstructionTypeEnum inType, Location inLocation) {
		return Collections.<LedCmdGroup> emptyList(); // returns empty immutable list
	}

}
