/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.model.WorkInstructionCount;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.CompareNullChecker;
import com.gadgetworks.flyweight.command.EffectEnum;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;

/**
 * @author jonranstrom
 *
 */
public class SetupOrdersDeviceLogic extends CheDeviceLogic {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger					LOGGER	= LoggerFactory.getLogger(SetupOrdersDeviceLogic.class);

	// The CHE's container map.
	private Map<String, String>					mPositionToContainerMap;

	//Map of containers to work instruction counts
	private Map<String, WorkInstructionCount>	mContainerToWorkInstructionCountMap;

	// The CHE's container map.
	private String								mContainerInSetup;

	// The CHE's current location.
	@Accessors(prefix = "m")
	@Getter
	private String								mLocationId;

	public SetupOrdersDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mPositionToContainerMap = new HashMap<String, String>();

	}

	public String getDeviceType() {
		return CsDeviceManager.DEVICETYPE_CHE_SETUPORDERS;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void setState(final CheStateEnum inCheState) {
		CheStateEnum previousState = mCheStateEnum;
		boolean isSameState = previousState == inCheState;
		mCheStateEnum = inCheState;
		LOGGER.debug("Switching to state: {} isSameState: {}", inCheState, isSameState);

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, EMPTY_MSG);
				break;

			case COMPUTE_WORK:
				sendDisplayCommand(COMPUTE_WORK_MSG, EMPTY_MSG);
				break;

			case GET_WORK:
				sendDisplayCommand(GET_WORK_MSG, EMPTY_MSG);
				break;

			case LOCATION_SELECT:
				sendDisplayCommand(SCAN_LOCATION_MSG, EMPTY_MSG, EMPTY_MSG, SHOWING_WI_COUNTS);
				this.showCartSetupFeedback();
				break;

			case LOCATION_SELECT_REVIEW:
				sendDisplayCommand(LOCATION_SELECT_REVIEW_MSG_LINE_1,
					LOCATION_SELECT_REVIEW_MSG_LINE_2,
					LOCATION_SELECT_REVIEW_MSG_LINE_3,
					SHOWING_WI_COUNTS);
				this.showCartSetupFeedback();
				break;

			case CONTAINER_SELECT:
				if (mPositionToContainerMap.size() < 1) {
					sendDisplayCommand(getContainerSetupMsg(), EMPTY_MSG);
				} else {
					sendDisplayCommand(getContainerSetupMsg(), OR_START_WORK_MSG, EMPTY_MSG, SHOWING_ORDER_IDS_MSG);
				}
				showContainerAssainments();
				break;

			case CONTAINER_POSITION:
				sendDisplayCommand(SELECT_POSITION_MSG, EMPTY_MSG);
				showContainerAssainments();
				break;

			case CONTAINER_POSITION_INVALID:
				invalidScanMsg(INVALID_POSITION_MSG, EMPTY_MSG, CLEAR_ERROR_MSG_LINE_1, CLEAR_ERROR_MSG_LINE_2);
				break;

			case CONTAINER_POSITION_IN_USE:
				invalidScanMsg(POSITION_IN_USE_MSG, EMPTY_MSG, CLEAR_ERROR_MSG_LINE_1, CLEAR_ERROR_MSG_LINE_2);
				break;

			case CONTAINER_SELECTION_INVALID:
				invalidScanMsg(INVALID_CONTAINER_MSG, EMPTY_MSG, CLEAR_ERROR_MSG_LINE_1, CLEAR_ERROR_MSG_LINE_2);
				break;

			case NO_CONTAINERS_SETUP:
				invalidScanMsg(NO_CONTAINERS_SETUP_MSG, FINISH_SETUP_MSG, CLEAR_ERROR_MSG_LINE_1, CLEAR_ERROR_MSG_LINE_2);
				break;

			case SHORT_PICK_CONFIRM:
				if (isSameState) {
					this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
				}
				sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
				break;

			case SHORT_PICK:
				if (isSameState) {
					this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
				}
				// first try. Show normally, but based on state, the wi min count will be set to zero.
				showActivePicks();
				break;

			case DO_PICK:
				if (isSameState || previousState == CheStateEnum.GET_WORK) {
					this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
				}
				showActivePicks(); // used to only fire if not already in this state. Now if setState(DO_PICK) is called, it always calls showActivePicks.
				// fewer direct calls to showActivePicks elsewhere.
				break;

			case PICK_COMPLETE:
				if (isSameState) {
					this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
				}
				sendDisplayCommand(PICK_COMPLETE_MSG, EMPTY_MSG);
				break;

			case NO_WORK:
				sendDisplayCommand(NO_WORK_MSG, EMPTY_MSG, EMPTY_MSG, SHOWING_WI_COUNTS);
				this.showCartSetupFeedback();
				break;

			default:
				break;
		}
	}

	/** 
	 * Command scans are split out by command then state because they are more likely to be state independent
	 */
	protected void processCommandScan(final String inScanStr) {

		switch (inScanStr) {

			case LOGOUT_COMMAND:
				//You can logout at any time
				logout();
				break;

			case SETUP_COMMAND:
				setupCommandReceived();
				break;

			case STARTWORK_COMMAND:
				startWorkCommandReceived();
				break;

			case SHORT_COMMAND:
				//Do not clear position controllers here
				shortPickCommandReceived();
				break;

			case YES_COMMAND:
			case NO_COMMAND:
				yesOrNoCommandReceived(inScanStr);
				break;

			case CLEAR_ERROR_COMMAND:
				clearErrorCommandReceived();
				break;

			default:

				//Legacy Behavior
				if (mCheStateEnum != CheStateEnum.SHORT_PICK_CONFIRM) {
					clearAllPositionControllers();
				}
				break;
		}
	}

	protected void clearErrorCommandReceived() {
		//Split it out by state
		switch (mCheStateEnum) {

		//Clear the error
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				clearAllPositionControllers();
				setState(CheStateEnum.CONTAINER_SELECT);
				break;

			default:
				//Reset ourselves
				//Ideally we shouldn't have to clear poscons here
				clearAllPositionControllers();
				setState(mCheStateEnum);
				break;

		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned YES or NO.
	 * @param inScanStr
	 */
	protected void yesOrNoCommandReceived(final String inScanStr) {

		switch (mCheStateEnum) {
		//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do nothing. Only a "Clear Error" will get you out
				break;

			case CONTAINER_POSITION:
				processContainerPosition(COMMAND_PREFIX, inScanStr);
				break;

			case CONTAINER_SELECT:
				processContainerSelectScan(COMMAND_PREFIX, inScanStr);
				break;

			case SHORT_PICK_CONFIRM:
				confirmShortPick(inScanStr);
				break;

			default:
				// Stay in the same state - the scan made no sense.
				invalidScanMsg(mCheStateEnum);
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Change state and display error message
	 * Send the LED error status as well (color: red effect: error channel: 0).
	 * 
	 */
	protected void invalidScanMsg(final CheStateEnum inCheState) {
		mCheStateEnum = inCheState;

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, INVALID_SCAN_MSG);
				break;

			case LOCATION_SELECT:
				sendDisplayCommand(SCAN_LOCATION_MSG, INVALID_SCAN_MSG);
				break;

			case DO_PICK:
				/* New attempts, but left as the default.  Test cases
				 * Short a housekeeping work instruction. (All E's is bad.)
				 * Yes or No on a normal pick
				 */

				// positionControllerToSendTo =  just the one, if we can figure it out. Or
				// sendPositionControllerInstructions = false;
				break;

			default:
				break;
		}
		sendErrorCodeToAllPosCons();
	}

	// --------------------------------------------------------------------------
	/**
	 * The guts of the short transaction
	 * Update the WI fields, and call out to mDeviceManager to share it back to the server.
	 */
	protected void doShortTransaction(final WorkInstruction inWi, final Integer inActualPickQuantity) {

		inWi.setActualQuantity(inActualPickQuantity);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatus(WorkInstructionStatusEnum.SHORT);

		// normal short will be in mActivePickWiList.
		// short-aheads will not be.
		if (mActivePickWiList.contains(inWi))
			mActivePickWiList.remove(inWi);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);

		//Decrement count as short
		if (!inWi.isHousekeeping()) {
			//The HK check should never be false
			String containerId = inWi.getContainerId();
			WorkInstructionCount count = this.mContainerToWorkInstructionCountMap.get(containerId);
			count.decrementGoodCountAndIncrementShortCount();

			//We can optionally change the containers map to a BiMap to avoid this reverse lookup
			Byte position = null;
			for (Entry<String, String> containerMapEntry : mPositionToContainerMap.entrySet()) {
				if (containerMapEntry.getValue().equals(containerId)) {
					position = Byte.valueOf(containerMapEntry.getKey());
					break;
				}
			}
			this.showCartRunFeedbackIfNeeded(position);

		}

	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick.
	 * @param inScanStr
	 */
	protected void confirmShortPick(final String inScanStr) {
		if (inScanStr.equals(YES_COMMAND)) {
			// HACK HACK HACK
			// StitchFix is the first client and they only pick one item at a time - ever.
			// When we have h/w that picks more than one item we'll address this.
			WorkInstruction wi = mShortPickWi;
			;
			if (wi != null) {
				doShortTransaction(wi, mShortPickQty);

				LOGGER.info("Pick shorted: " + wi);

				clearLedControllersForWi(wi);

				// DEV-582 hook up to AUTOSHRT parameter
				if (mDeviceManager.getAutoShortValue())
					doShortAheads(wi); // Jobs for the same product on the cart should automatically short, and not subject the user to them.

				if (mActivePickWiList.size() > 0) {
					// If there's more active picks then show them.
					// This is tricky. Simultaneous work instructions: which was short? All of them?
					LOGGER.error("Simulataneous work instructions turned off currently, so unexpected case in confirmShortPick");
					showActivePicks();
				} else {
					// There's no more active picks, so move to the next set.
					doNextPick();
				}
			}
		} else {
			// Just return to showing the active picks.
			setState(CheStateEnum.DO_PICK);
			// showActivePicks();
		}
	}

	// --------------------------------------------------------------------------
	/**  doNextPick has a major side effect of setState(DO_PICK) if there is more work.
	 *   Then setState(DO_PICK) calls showActivePicks()
	 */
	private void doNextPick() {
		LOGGER.debug(this + "doNextPick");

		if (mActivePickWiList.size() > 0) {
			// There are still picks in the active list.
			LOGGER.error("Unexpected case in doNextPick");
			showActivePicks();
			// each caller to doNextPick already checked mActivePickWiList.size(). Therefore new situation if found

		} else {

			if (selectNextActivePicks()) {
				setState(CheStateEnum.DO_PICK); // This will cause showActivePicks();
				// showActivePicks();
			} else {
				processPickComplete();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the WIs by their sort code.
	 */
	private class WiGroupSortComparator implements Comparator<WorkInstruction> {

		@Override
		public int compare(WorkInstruction inWi1, WorkInstruction inWi2) {

			int value = CompareNullChecker.compareNulls(inWi1, inWi2);
			if (value != 0)
				return value;

			String w1Sort = inWi1.getGroupAndSortCode();
			String w2Sort = inWi2.getGroupAndSortCode();
			value = CompareNullChecker.compareNulls(w1Sort, w2Sort);
			if (value != 0)
				return value;

			return w1Sort.compareTo(w2Sort);
		}
	};

	// --------------------------------------------------------------------------
	/**
	 */
	private boolean selectNextActivePicks() {
		final boolean kDoMultipleWiPicks = false;

		boolean result = false;

		// Loop through each container to see if there is a WI for that container at the next location.
		// The "next location" is the first location we find for the next pick.
		String firstLocationId = null;
		String firstItemId = null;
		Collections.sort(mAllPicksWiList, new WiGroupSortComparator());
		for (WorkInstruction wi : mAllPicksWiList) {
			if (mPositionToContainerMap.values().isEmpty()) {
				LOGGER.warn(this + " assigned work but no containers assigned");
			}
			for (String containerId : mPositionToContainerMap.values()) {
				// If the WI is for this container then consider it.
				if (wi.getContainerId().equals(containerId)) {
					// If the WI is INPROGRESS or NEW then consider it.
					if ((wi.getStatus().equals(WorkInstructionStatusEnum.NEW))
							|| (wi.getStatus().equals(WorkInstructionStatusEnum.INPROGRESS))) {
						if ((firstLocationId == null) || (firstLocationId.equals(wi.getLocationId()))) {
							if ((firstItemId == null) || (firstItemId.equals(wi.getItemId()))) {
								firstLocationId = wi.getLocationId();
								firstItemId = wi.getItemId();
								wi.setStarted(new Timestamp(System.currentTimeMillis()));
								mActivePickWiList.add(wi);
								result = true;
								if (!kDoMultipleWiPicks)
									return true; // bail here instead of continuing to next wi in mAllPicksWiList, looking for location/item match
							}
						}
					}
				}
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * The inShortWi was just shorted.
	 * Is inProposedWi later in sequence?
	 */
	private Boolean laterWi(final WorkInstruction inProposedWi, final WorkInstruction inShortWi) {
		String proposedSort = inProposedWi.getGroupAndSortCode();
		String shortedSort = inShortWi.getGroupAndSortCode();
		if (proposedSort == null) {
			LOGGER.error("laterWiSameProduct has wi with no sort code");
			return false;
		}
		if (shortedSort.compareTo(proposedSort) < 0)
			return true;

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * The inShortWi was just shorted.
	 * Is inProposedWi equivalent enough that it should also short?
	 */
	private Boolean sameProductLotEtc(final WorkInstruction inProposedWi, WorkInstruction inShortWi) {
		// Initially, just look at the denormalized item Id.
		String shortId = inShortWi.getItemId();
		String proposedId = inProposedWi.getItemId();
		if (shortId == null || proposedId == null) {
			LOGGER.error("sameProductLotEtc has null value");
			return false;
		}
		if (shortId.compareTo(proposedId) == 0) {
			LOGGER.info("SHORT AHEAD " + inProposedWi);
			return true;
		}

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Is this an uncompleted housekeeping WI, that if just ahead of a short-ahead job is no longer needed?
	 * Calling context is just ahead of short-ahead, so this does not need to try to check that. But knowing that, can look for other possible errors.
	 */
	private Boolean unCompletedUnneededHousekeep(final WorkInstruction inWi) {
		// Keep in mind this odd-ball case:
		// One short will short ahead and get some shorts and repeatPos.
		// A later short will short ahead and look for more. It is possible that one of the earlier short ahead is complete and just before it.
		WorkInstructionStatusEnum theStatus = inWi.getStatus();
		if (theStatus != WorkInstructionStatusEnum.NEW && theStatus != WorkInstructionStatusEnum.SHORT) {
			LOGGER.error("bad calling context 1 for unCompletedUnneededHousekeep");
			return false;
		}

		if (inWi.isHousekeeping()) {
			return true;
		}

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Update the WI fields to complete-ahead an unneeded housekeep, and call out to mDeviceManager to share it back to the server.
	 */
	private void doCompleteUnneededHousekeep(final WorkInstruction inWi) {

		inWi.setActualQuantity(0);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatus(WorkInstructionStatusEnum.COMPLETE);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
	}

	// --------------------------------------------------------------------------
	/**
	 * The inShortWi was just shorted.
	 * Compare later wis to that. If the same product, short those also, removing unnecessary housekeeping work instructions
	 */
	private void doShortAheads(final WorkInstruction inShortWi) {
		// Look for more wis in the CHE's job list that must short also if this one did.
		// Short and notify them, and remove so the worker will not encounter them. 
		// Also remove unnecessary repeats and bay changes. The algorithm for housekeep removal is:
		// - There may be bay changes or repeat containers. And only one housekeep between at a time (for now--this may break)
		// - So track the previous work instruction as we iterate. If there is a housekeep right before a short ahead, we assume it is not needed.
		// - Repeat: definitely not needed. Baychange: maybe not needed. Better to over-remove than leave a final bay change at the end of the list.

		Integer laterCount = 0;
		Integer toShortCount = 0;
		Integer removeHousekeepCount = 0;
		// Algorithm:  The all picks list is ordered by sequence. So consider anything in that list with later sequence.
		// One or two of these might also be in mActivePickWiList if it is a simultaneous work instruction pick that we are on.  Remove if found there.
		WorkInstruction prevWi = null;
		for (WorkInstruction wi : mAllPicksWiList) {
			if (laterWi(wi, inShortWi)) {
				laterCount++;
				if (sameProductLotEtc(wi, inShortWi)) {
					// When simultaneous pick work instructions return, we must not short ahead within the simultaneous pick. See CD_0043 for details.
					if (!mActivePickWiList.contains(wi)) {
						toShortCount++;
						// housekeeps that are not the current job should not be in mActivePickWiList. Just check that possibility to confirm our understanding.
						if (unCompletedUnneededHousekeep(prevWi)) {
							if (mActivePickWiList.contains(prevWi))
								LOGGER.error("unanticipated housekeep in mActivePickWiList in doShortAheads");
							removeHousekeepCount++;
							doCompleteUnneededHousekeep(prevWi);
						}
						// Short aheads will always set the actual pick quantity to zero.
						doShortTransaction(wi, 0);
					}
				}
			}
			prevWi = wi;
		}

		// Let's only report all this if there is a short ahead. We do not need to see in the log that we considered after every short.
		if (toShortCount > 0) {
			String reportShortAheadDetails = "Considered " + laterCount + " later jobs for short-ahead.";
			if (toShortCount > 0)
				reportShortAheadDetails += " Shorted " + toShortCount + " more.";
			if (removeHousekeepCount > 0)
				reportShortAheadDetails += " Also removed " + removeHousekeepCount + " housekeeping instructions.";
			LOGGER.info(reportShortAheadDetails);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void processContainerSelectScan(final String inScanPrefixStr, String inScanStr) {
		// DEV-518. Also accept the raw order ID.
		if (inScanPrefixStr.isEmpty() || CONTAINER_PREFIX.equals(inScanPrefixStr)) {

			mContainerInSetup = inScanStr;

			// Check to see if this container is already setup in a position.
			Iterator<Entry<String, String>> setIterator = mPositionToContainerMap.entrySet().iterator();
			while (setIterator.hasNext()) {
				Entry<String, String> entry = setIterator.next();
				if (entry.getValue().equals(mContainerInSetup)) {
					setIterator.remove();
					this.clearOnePositionController(Byte.valueOf(entry.getKey()));
					break;
				}
			}
			// It would be cool if we could check here. Call to REST API on the server? Needs to not block for long, though.
			// When we scan a container, that container either should match a cross batch order detail, or match an outbound order's preassigned container. If not, 
			// this is a "ride along" error. Would be nice if the user could see it immediately.
			setState(CheStateEnum.CONTAINER_POSITION);
		} else {
			setState(CheStateEnum.CONTAINER_SELECTION_INVALID);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SHORT_PICK command to tell us there is no product to pick.
	 */
	protected void shortPickCommandReceived() {
		//Split it out by state
		switch (mCheStateEnum) {

		//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do nothing. Only a "Clear Error" will get you out
				break;

			case CONTAINER_POSITION:
				processContainerPosition(COMMAND_PREFIX, SHORT_COMMAND);
				break;

			case CONTAINER_SELECT:
				processContainerSelectScan(COMMAND_PREFIX, SHORT_COMMAND);
				break;

			//Anywhere else we can start work if there's anything setup
			default:
				if (mActivePickWiList.size() > 0) {
					// short scan of housekeeping work instruction makes no sense
					WorkInstruction wi = mActivePickWiList.get(0);
					if (wi.isHousekeeping())
						invalidScanMsg(mCheStateEnum); // Invalid to short a housekeep
					else
						setState(CheStateEnum.SHORT_PICK); // Used to be SHORT_PICK_CONFIRM
				} else {
					// Stay in the same state - the scan made no sense.
					invalidScanMsg(mCheStateEnum);
				}
				break;

		}

	}

	protected void setupCommandReceived() {
		//Split it out by state
		switch (mCheStateEnum) {

			case PICK_COMPLETE:
			case NO_WORK:
				//Setup the CHE
				setupChe();
				break;

			//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do nothing. Only a "Clear Error" will get you out
				break;

			case CONTAINER_POSITION:
				processContainerPosition(COMMAND_PREFIX, SETUP_COMMAND);
				break;

			case CONTAINER_SELECT:
				processContainerSelectScan(COMMAND_PREFIX, SETUP_COMMAND);
				break;

			default:
				//DEV-577 Do nothing
				break;

		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the Idle state (not logged in), so if we get a user scan then start the setup process.
	 * @param inPrefixScanStr
	 * @param inScanStr
	 */
	private void processIdleStateScan(final String inScanPrefixStr, final String inScanStr) {

		if (USER_PREFIX.equals(inScanPrefixStr)) {
			clearAllPositionControllers();
			setState(CheStateEnum.CONTAINER_SELECT);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.IDLE);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void processLocationScan(final String inScanPrefixStr, String inScanStr) {
		if (LOCATION_PREFIX.equals(inScanPrefixStr)) {
			clearAllPositionControllers();

			this.mLocationId = inScanStr;
			mDeviceManager.getCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), inScanStr);

			setState(CheStateEnum.GET_WORK);

		} else {
			LOGGER.info("Not a location ID: " + inScanStr);
			invalidScanMsg(mCheStateEnum);
		}
	}

	// --------------------------------------------------------------------------
	/* 
	 */
	@Override
	public void processNonCommandScan(String inScanPrefixStr, String inContent) {
		// Non-command scans are split out by state then the scan content
		switch (mCheStateEnum) {
			case IDLE:
				processIdleStateScan(inScanPrefixStr, inContent);
				break;
			case NO_WORK:
				processLocationScan(inScanPrefixStr, inContent);
				break;
			case LOCATION_SELECT:
				processLocationScan(inScanPrefixStr, inContent);
				break;

			case LOCATION_SELECT_REVIEW:
				processLocationScan(inScanPrefixStr, inContent);
				break;

			case CONTAINER_SELECT:
				processContainerSelectScan(inScanPrefixStr, inContent);
				break;

			case CONTAINER_POSITION:
				// The only thing that makes sense in this state is a position assignment (or a logout covered above).
				processContainerPosition(inScanPrefixStr, inContent);
				break;

			//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do Nothing if you are in an error state and you scan something that's not "Clear Error"
				break;

			case DO_PICK:
				// At any time during the pick we can change locations.
				if (inScanPrefixStr.equals(LOCATION_PREFIX)) {
					processLocationScan(inScanPrefixStr, inContent);
				}
				break;

			default:
				break;
		}

	}

	/**
	 * Use the configuration system to return custom setup MSG. Defaults to "SCAN ORDER"
	 */
	private String getContainerSetupMsg() {
		String containerType = mDeviceManager.getContainerTypeValue();
		if (StringUtils.isEmpty(containerType)) {
			LOGGER.error("Container Type is empty");
			return cheLine("SCAN ORDER");
		} else {
			return cheLine("SCAN " + mDeviceManager.getContainerTypeValue().toUpperCase());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * This shows all the positions already assigned to containers in the mContainersMap
	 */
	private void showContainerAssainments() {
		if (mPositionToContainerMap.isEmpty()) {
			LOGGER.debug("No Container Assaigments to send");
			return;
		}
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

		for (Entry<String, String> entry : mPositionToContainerMap.entrySet()) {
			String containerId = entry.getValue();
			Byte position = Byte.valueOf(entry.getKey());

			Byte value = PosControllerInstr.DEFAULT_POSITION_ASSIGNED_CODE;
			//Use the last 1-2 characters of the containerId iff the container is numeric.
			//Otherwise stick to the default character "a"

			if (!StringUtils.isEmpty(containerId) && StringUtils.isNumeric(containerId)) {
				if (containerId.length() == 1) {
					value = Byte.valueOf(containerId);
				} else {
					value = Byte.valueOf(containerId.substring(containerId.length() - 2));
				}
			}

			if (value >= 0 && value < 10) {
				//If we are going to pass a single 0 <= digit < 10 like 9, then we must show "09" instead of just 9.
				instructions.add(new PosControllerInstr(position,
					PosControllerInstr.BITENCODED_SEGMENTS_CODE,
					PosControllerInstr.BITENCODED_DIGITS[value],
					PosControllerInstr.BITENCODED_DIGITS[0],
					PosControllerInstr.SOLID_FREQ,
					PosControllerInstr.MED_DUTYCYCLE));
			} else {
				instructions.add(new PosControllerInstr(position,
					value,
					value,
					value,
					PosControllerInstr.SOLID_FREQ,
					PosControllerInstr.MED_DUTYCYCLE));

			}
		}
		LOGGER.debug("Sending Container Assaignments {}", instructions);

		sendPositionControllerInstructions(instructions);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param totalWorkInstructionCount
	 * @param containerToWorkInstructionCountMap - Map containerIds to WorkInstructionCount objects
	 * Not final only because we let CsDeviceManager call this generically.
	 */
	public void processWorkInstructionCounts(final Integer totalWorkInstructionCount,
		final Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {

		//Store counts
		this.mContainerToWorkInstructionCountMap = containerToWorkInstructionCountMap;

		// The back-end returned the work instruction count.
		if (totalWorkInstructionCount > 0 && containerToWorkInstructionCountMap != null
				&& !containerToWorkInstructionCountMap.isEmpty()) {
			//Use the map to determine if we need to go to location_select or review

			//Check to see if we have any unknown containerIds. We must have a count for every container
			boolean doesNeedReview = !(mPositionToContainerMap.size() == containerToWorkInstructionCountMap.size());

			if (!doesNeedReview) {
				for (WorkInstructionCount wiCount : containerToWorkInstructionCountMap.values()) {
					if (wiCount.getGoodCount() == 0 || wiCount.hasBadCounts()) {
						doesNeedReview = true;
						break;
					}
				}
			}
			LOGGER.info("Got Counts {}", containerToWorkInstructionCountMap);

			if (doesNeedReview) {
				setState(CheStateEnum.LOCATION_SELECT_REVIEW);
			} else {
				setState(CheStateEnum.LOCATION_SELECT);
			}

		} else {
			setState(CheStateEnum.NO_WORK);
		}
	}

	/** Shows the count feedback on the position controller
	 */
	protected void showCartSetupFeedback() {
		//Generate position controller commands
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

		for (Entry<String, String> containerMapEntry : mPositionToContainerMap.entrySet()) {
			String containerId = containerMapEntry.getValue();
			byte position = Byte.valueOf(containerMapEntry.getKey());
			WorkInstructionCount wiCount = mContainerToWorkInstructionCountMap.get(containerId);

			//if wiCount is 0 then the server did have any WIs for the order.
			//this is an "unknown" order id
			if (wiCount == null) {
				//TODO send a special code for this?
				//Right now it matches "done for now" feedback
				instructions.add(new PosControllerInstr(position,
					PosControllerInstr.BITENCODED_SEGMENTS_CODE,
					PosControllerInstr.BITENCODED_LED_DASH,
					PosControllerInstr.BITENCODED_LED_DASH,
					PosControllerInstr.SOLID_FREQ.byteValue(),
					PosControllerInstr.DIM_DUTYCYCLE.byteValue()));
				LOGGER.info("Position {} has unknwon container id", position);
			} else {
				byte count = (byte) wiCount.getGoodCount();
				LOGGER.info("Position Feedback: Poisition {} Counts {}", position, wiCount);
				if (count == 0) {
					//0 good WI's
					if (wiCount.hasBadCounts()) {
						//If there any bad counts then we are "done for now" - dim, solid dashes
						instructions.add(new PosControllerInstr(position,
							PosControllerInstr.BITENCODED_SEGMENTS_CODE,
							PosControllerInstr.BITENCODED_LED_DASH,
							PosControllerInstr.BITENCODED_LED_DASH,
							PosControllerInstr.SOLID_FREQ.byteValue(),
							PosControllerInstr.DIM_DUTYCYCLE.byteValue()));
					} else {
						if (wiCount.getCompleteCount() == 0) {
							//This should not be possible (unless we only had a single HK WI, which would be a bug)
							//We will log this for now and treat it as a completed WI
							LOGGER.error("WorkInstructionCount has no counts {}; containerId={}", wiCount, containerId);
						}
						//Ready for packout - solid, dim oc
						instructions.add(new PosControllerInstr(position,
							PosControllerInstr.BITENCODED_SEGMENTS_CODE,
							PosControllerInstr.BITENCODED_LED_C,
							PosControllerInstr.BITENCODED_LED_O,
							PosControllerInstr.SOLID_FREQ.byteValue(),
							PosControllerInstr.DIM_DUTYCYCLE.byteValue()));
					}
				} else {
					//Non-zero good WI's means bright display

					//Blink if we have any bad counts, solid otherwise
					byte frequency = wiCount.hasBadCounts() ? PosControllerInstr.BLINK_FREQ : PosControllerInstr.SOLID_FREQ;

					instructions.add(new PosControllerInstr(position,
						count,
						count,
						count,
						frequency,
						PosControllerInstr.BRIGHT_DUTYCYCLE.byteValue()));
				}
			}
		}

		//Show counts on position controllers
		sendPositionControllerInstructions(instructions);
	}

	/** Shows the count feedback on the position controller during the cart run
	 */
	protected void showCartRunFeedbackIfNeeded(Byte inPosition) {
		if (inPosition == null) {
			LOGGER.error("showCartRunFeedbackIfNeeded was supplied a null position");
			return;
		}

		//Generate position controller commands
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

		if (PosControllerInstr.POSITION_ALL == inPosition) {
			for (Entry<String, String> containerMapEntry : mPositionToContainerMap.entrySet()) {
				String containerId = containerMapEntry.getValue();
				byte position = Byte.valueOf(containerMapEntry.getKey());
				WorkInstructionCount wiCount = mContainerToWorkInstructionCountMap.get(containerId);
				PosControllerInstr instr = this.getCartRunFeedbackInstructionForCount(wiCount, position);
				if (instr != null) {
					instructions.add(instr);
				}
			}
		} else {
			String containerId = mPositionToContainerMap.get(inPosition.toString());
			WorkInstructionCount wiCount = mContainerToWorkInstructionCountMap.get(containerId);
			PosControllerInstr instr = this.getCartRunFeedbackInstructionForCount(wiCount, inPosition);
			if (instr != null) {
				instructions.add(instr);
			}
		}

		//Show counts on position controllers
		if (!instructions.isEmpty()) {
			sendPositionControllerInstructions(instructions);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * 
	 */
	private void processContainerPosition(final String inScanPrefixStr, String inScanStr) {
		if (POSITION_PREFIX.equals(inScanPrefixStr)) {
			if (mPositionToContainerMap.get(inScanStr) == null) {
				mPositionToContainerMap.put(inScanStr, mContainerInSetup);
				mContainerInSetup = "";
				setState(CheStateEnum.CONTAINER_SELECT);
			} else {
				setState(CheStateEnum.CONTAINER_POSITION_IN_USE);
			}
		} else {
			setState(CheStateEnum.CONTAINER_POSITION_INVALID);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * 
	 */
	private void startWorkCommandReceived() {
		//Split it out by state
		switch (mCheStateEnum) {

		//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do nothing. Only a "Clear Error" will get you out
				break;

			case CONTAINER_POSITION:
				processContainerPosition(COMMAND_PREFIX, STARTWORK_COMMAND);
				break;

			//Anywhere else we can start work if there's anything setup
			case CONTAINER_SELECT:
			default:
				if (mPositionToContainerMap.values().size() > 0) {
					startWork();
				} else {
					setState(CheStateEnum.NO_CONTAINERS_SETUP);
				}
				break;

		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The start work command simply tells the user to select a starting location.
	 * It's a psychological step that makes more sense.
	 */
	private void startWork() {
		clearAllPositionControllers();
		mContainerInSetup = "";
		List<String> containerIdList = new ArrayList<String>(mPositionToContainerMap.values());
		mDeviceManager.computeCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), containerIdList);
		setState(CheStateEnum.COMPUTE_WORK);
	}

	// --------------------------------------------------------------------------
	/**
	 * Give the CHE the work it needs to do for a container.
	 * This is recomputed at the server for ALL containers on the CHE and returned in work-order.
	 * Whatever the CHE thought it needed to do before is now invalid and is replaced by what we send here.
	 * @param inContainerId
	 * @param inWorkItemList
	 * Only not final because we let CsDeviceManager call this generically.
	 */
	public void assignWork(final List<WorkInstruction> inWorkItemList) {
		if (inWorkItemList == null || inWorkItemList.size() == 0) {
			setState(CheStateEnum.NO_WORK);
		} else {
			for (WorkInstruction wi : inWorkItemList) {
				LOGGER.debug("WI: Loc: " + wi.getLocationId() + " SKU: " + wi.getItemId() + " Instr: " + wi.getPickInstruction());
			}
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
			mAllPicksWiList.addAll(inWorkItemList);
			doNextPick();
			// setState(CheStateEnum.DO_PICK);  // doNextPick will set the state.
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	private void processNormalPick(WorkInstruction inWi, Integer inQuantity) {

		inWi.setActualQuantity(inQuantity);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatus(WorkInstructionStatusEnum.COMPLETE);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
		LOGGER.info("Pick completed: " + inWi);

		mActivePickWiList.remove(inWi);

		//Decrement count if this is a non-HK WI
		if (!inWi.isHousekeeping()) {
			String containerId = inWi.getContainerId();
			WorkInstructionCount count = this.mContainerToWorkInstructionCountMap.get(containerId);
			count.decrementGoodCountAndIncrementCompleteCount();

			//We can optionally change the containers map to a BiMap to avoid this reverse lookup
			Byte position = null;
			for (Entry<String, String> containerMapEntry : mPositionToContainerMap.entrySet()) {
				if (containerMapEntry.getValue().equals(containerId)) {
					position = Byte.valueOf(containerMapEntry.getKey());
					break;
				}
			}
			this.showCartRunFeedbackIfNeeded(position);
		}

		clearLedControllersForWi(inWi);

		if (mActivePickWiList.size() > 0) {
			// If there's more active picks then show them.
			LOGGER.error("Simulataneous work instructions turned off currently, so unexpected case in processNormalPick");
			showActivePicks();
		} else {
			// There's no more active picks, so move to the next set.
			doNextPick();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inButtonNum
	 * @return
	 */
	private String getContainerIdFromButtonNum(Integer inButtonNum) {
		return mPositionToContainerMap.get(Integer.toString(inButtonNum));
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inContainerId
	 * @return
	 */
	private WorkInstruction getWorkInstructionForContainerId(String inContainerId) {
		WorkInstruction result = null;
		Iterator<WorkInstruction> wiIter = mActivePickWiList.iterator();
		while (wiIter.hasNext()) {
			WorkInstruction wi = wiIter.next();
			if (wi.getContainerId().equals(inContainerId)) {
				result = wi;
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Complete the active WI at the selected position.
	 * @param inButtonNum
	 * @param inQuantity
	 * @param buttonPosition 
	 */
	protected void processButtonPress(Integer inButtonNum, Integer inQuantity, Byte buttonPosition) {
		String containerId = getContainerIdFromButtonNum(inButtonNum);
		if (containerId == null) {
			// Simply ignore button presses when there is no container.
			//invalidScanMsg(mCheStateEnum);
		} else {
			WorkInstruction wi = getWorkInstructionForContainerId(containerId);
			if (wi == null) {
				// Simply ignore button presses when there is no work instruction.
				//invalidScanMsg(mCheStateEnum);
			} else {
				clearOnePositionController(buttonPosition);
				String itemId = wi.getItemId();
				LOGGER.info("Button #" + inButtonNum + " for " + containerId + " / " + itemId);
				if (inQuantity >= wi.getPlanMinQuantity()) {
					processNormalPick(wi, inQuantity);
				} else {
					// More kludge for count > 99 case
					Integer planQuantity = wi.getPlanQuantity();
					if (inQuantity == maxCountForPositionControllerDisplay && planQuantity > maxCountForPositionControllerDisplay)
						processNormalPick(wi, planQuantity); // Assume all were picked. No way for user to tell if more than 98 given.
					else
						processShortPick(wi, inQuantity);
				}
			}
		}
	}


	// --------------------------------------------------------------------------
	/**
	 * Determine if the mActivePickWiList represents a housekeeping move. If so, display it and return true
	 */
	@Override
	protected boolean sendHousekeepingDisplay() {
		boolean returnBool = false;
		if (mActivePickWiList.size() == 1) {
			WorkInstruction wi = mActivePickWiList.get(0);
			if (wi == null)
				LOGGER.error("misunderstanding in sendHousekeepingDisplay");
			else {
				WorkInstructionTypeEnum theEnum = wi.getType();
				if (theEnum == WorkInstructionTypeEnum.HK_BAYCOMPLETE) {
					returnBool = true;
					showSpecialPositionCode(PosControllerInstr.BAY_COMPLETE_CODE, wi.getContainerId());
				} else if (theEnum == WorkInstructionTypeEnum.HK_REPEATPOS) {
					returnBool = true;
					showSpecialPositionCode(PosControllerInstr.REPEAT_CONTAINER_CODE, wi.getContainerId());
				}
			}
		}
		return returnBool;
	}

	/**
	 * Determine if the mActivePickWiList represents a housekeeping move. If so, display it and return true
	 */
	@Override
	protected void doPosConDisplaysforWi(WorkInstruction firstWi) {

		// Housekeeping moves will result in a single work instruction in the active pickes. Enum tells if housekeeping.
		if (!sendHousekeepingDisplay()) {
			byte planQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanQuantity());
			byte minQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanMinQuantity());
			byte maxQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanMaxQuantity());
			if (getCheStateEnum() == CheStateEnum.SHORT_PICK)
				minQuantityForPositionController = byteValueForPositionDisplay(0); // allow shorts to decrement on position controller down to zero

			// Also pretty easy. Light the position controllers on this CHE
			byte freq = PosControllerInstr.SOLID_FREQ;
			byte brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
			// blink is a weak indicator that decrement button is active, usually as a consequence of short pick. (Max difference is also possible for discretionary picks)
			if (planQuantityForPositionController != minQuantityForPositionController
					|| planQuantityForPositionController != maxQuantityForPositionController) {
				freq = PosControllerInstr.BRIGHT_DUTYCYCLE;
				brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
			}

			List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
			for (WorkInstruction wi : mActivePickWiList) {
				for (Entry<String, String> mapEntry : mPositionToContainerMap.entrySet()) {
					if (mapEntry.getValue().equals(wi.getContainerId())) {
						Byte posconIndex = Byte.valueOf(mapEntry.getKey());
						PosControllerInstr instruction = new PosControllerInstr(posconIndex,
							planQuantityForPositionController,
							minQuantityForPositionController,
							maxQuantityForPositionController,
							freq,
							brightness);
						instructions.add(instruction);
					}
				}
			}
			sendPositionControllerInstructions(instructions);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void showSpecialPositionCode(Byte inSpecialQuantityCode, String inContainerId) {
		boolean codeUnderstood = false;
		Byte codeToSend = inSpecialQuantityCode;
		if (inSpecialQuantityCode == PosControllerInstr.BAY_COMPLETE_CODE)
			codeUnderstood = true;
		else if (inSpecialQuantityCode == PosControllerInstr.REPEAT_CONTAINER_CODE)
			codeUnderstood = true;

		if (!codeUnderstood) {
			LOGGER.error("showSpecialPositionCode: unknown quantity code={}; containerId={}", inSpecialQuantityCode, inContainerId);
			return;
		}
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		for (Entry<String, String> mapEntry : mPositionToContainerMap.entrySet()) {
			if (mapEntry.getValue().equals(inContainerId)) {
				PosControllerInstr instruction = new PosControllerInstr(Byte.valueOf(mapEntry.getKey()),
					codeToSend,
					codeToSend,
					codeToSend,
					PosControllerInstr.SOLID_FREQ, // change from SOLID_FREQ
					PosControllerInstr.MED_DUTYCYCLE); // change from BRIGHT_DUTYCYCLE v6
				instructions.add(instruction);
			}
		}
		if (instructions.size() > 0)
			sendPositionControllerInstructions(instructions);
		else
			LOGGER.error("container match not found in showSpecialPositionCode");
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	protected void processShortPick(WorkInstruction inWi, Integer inQuantity) {
		setState(CheStateEnum.SHORT_PICK_CONFIRM);
		mShortPickWi = inWi;
		mShortPickQty = inQuantity;
	}

	// --------------------------------------------------------------------------
	/**
	 * return the button for this container ID. Mostly private use, but public for unit test convenience
	 */
	public byte buttonFromContainer(String inContainerId) {
		// must we do linear search? The code does throughout. Seems like map direct lookup would be fine.
		for (Entry<String, String> mapEntry : mPositionToContainerMap.entrySet()) {
			if (mapEntry.getValue().equals(inContainerId)) {
				return Byte.valueOf(mapEntry.getKey());
			}
		}
		return 0;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void logout() {
		super.logout();
		mPositionToContainerMap.clear();
		mContainerToWorkInstructionCountMap = null;
		mContainerInSetup = "";

	}

	/**
	 * Setup the CHE by clearing all the datastructures
	 */
	protected void setupChe() {
		super.setupChe();
		mPositionToContainerMap.clear();
		mContainerToWorkInstructionCountMap = null;
		mContainerInSetup = "";

	}

}
