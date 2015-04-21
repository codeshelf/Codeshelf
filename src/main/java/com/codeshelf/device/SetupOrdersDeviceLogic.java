/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.CompareNullChecker;
import com.codeshelf.ws.protocol.request.PutWallPlacementRequest;

/**
 * @author jonranstrom
 *
 */
public class SetupOrdersDeviceLogic extends CheDeviceLogic {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger					LOGGER						= LoggerFactory.getLogger(SetupOrdersDeviceLogic.class);

	// The CHE's container map.
	private Map<String, String>					mPositionToContainerMap;
	// This always exists, but map may be empty if PUT_WALL prior to container setup, or reduced meaning if PUT_WALL after work complete on this path.

	//Map of containers to work instruction counts
	private Map<String, WorkInstructionCount>	mContainerToWorkInstructionCountMap;
	// Careful: this initializes as null, and only exists if there was successful return of the response from server. It must always be null checked.

	// Transient. The CHE has scanned this container, and will add to container map if it learns the poscon position.
	private String								mContainerInSetup;

	// The CHE's current location.
	@Accessors(prefix = "m")
	@Getter
	private String								mLocationId;

	// If the CHE is in PUT_WALL process, the wall currently getting work instructions for
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mPutWallName;

	// knowing this allows better CHE feedback
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mLastPutWallItemScan;

	@Getter
	@Setter
	private boolean								mInventoryCommandAllowed	= true;

	public SetupOrdersDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mPositionToContainerMap = new HashMap<String, String>();

		updateConfigurationFromManager();
	}

	public String getDeviceType() {
		return CsDeviceManager.DEVICETYPE_CHE_SETUPORDERS;
	}

	private String getForWallMessageLine() {
		return String.format("FOR %s", getPutWallName());
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void setState(final CheStateEnum inCheState) {
		int priorCount = getSetStateStackCount();
		try {
			// This is tricky. setState() may have side effects that call setState. So even as the internal setState is done, the first one may not be done.
			// Therefore, a counter instead of a boolean.
			setSetStateStackCount(priorCount + 1);
			CheStateEnum previousState = mCheStateEnum;
			boolean isSameState = previousState == inCheState;
			mCheStateEnum = inCheState;
			LOGGER.debug("Switching to state: {} isSameState: {}", inCheState, isSameState);

			switch (inCheState) {
				case IDLE:
					sendDisplayCommand(SCAN_USERID_MSG, EMPTY_MSG);
					break;

				case VERIFYING_BADGE:
					sendDisplayCommand(VERIFYING_BADGE_MSG, EMPTY_MSG);
					break;

				case COMPUTE_WORK:
					sendDisplayCommand(COMPUTE_WORK_MSG, EMPTY_MSG);
					break;

				case GET_WORK:
					sendDisplayCommand(GET_WORK_MSG, EMPTY_MSG);
					break;

				case LOCATION_SELECT:
					if (isOkToStartWithoutLocation())
						sendDisplayCommand(SCAN_LOCATION_MSG, OR_SCAN_START, EMPTY_MSG, SHOWING_WI_COUNTS);
					else
						sendDisplayCommand(SCAN_LOCATION_MSG, EMPTY_MSG, EMPTY_MSG, SHOWING_WI_COUNTS);
					this.showCartSetupFeedback();
					break;

				case LOCATION_SELECT_REVIEW:
					if (isOkToStartWithoutLocation())
						sendDisplayCommand(LOCATION_SELECT_REVIEW_MSG_LINE_1, OR_SCAN_LOCATION, OR_SCAN_START, SHOWING_WI_COUNTS);
					else
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

				case SHORT_PUT_CONFIRM:
					sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
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

				case SHORT_PUT:
					// first try. Show normally, but based on state, the wi min count will be set to zero.
					showActivePicks();
					break;

				case DO_PICK:
					if (isSameState || previousState == CheStateEnum.GET_WORK || previousState == CheStateEnum.SCAN_SOMETHING) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					showActivePicks(); // if setState(DO_PICK) is called, it always calls showActivePicks. fewer direct calls to showActivePicks elsewhere.
					break;

				case SCAN_SOMETHING:
					if (isSameState || previousState == CheStateEnum.GET_WORK) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					showActivePicks(); // change this? DEV-653
					break;

				case SCAN_SOMETHING_SHORT: // this is like a short confirm.
					if (isSameState) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
					break;

				case PICK_COMPLETE:
					if (isSameState) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					sendDisplayCommand(PICK_COMPLETE_MSG, EMPTY_MSG);
					break;

				case PICK_COMPLETE_CURR_PATH:
					if (isSameState) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					sendDisplayCommand(PATH_COMPLETE_MSG, SCAN_NEW_LOCATION_MSG, OR_SETUP_NEW_CART_MSG, EMPTY_MSG);
					break;

				case NO_WORK:
					sendDisplayCommand(NO_WORK_MSG, EMPTY_MSG, EMPTY_MSG, SHOWING_WI_COUNTS);
					this.showCartSetupFeedback();
					break;
				case NO_WORK_CURR_PATH:
					sendDisplayCommand(NO_WORK_MSG, ON_CURR_PATH_MSG, SCAN_LOCATION_MSG, SHOWING_WI_COUNTS);
					this.showCartSetupFeedback();
					break;
				case SCAN_GTIN:
					if (lastScanedGTIN == null) {
						sendDisplayCommand(SCAN_GTIN, EMPTY_MSG);
					} else {
						sendDisplayCommand(SCAN_GTIN_OR_LOCATION, EMPTY_MSG);
					}
					break;

				case PUT_WALL_SCAN_ORDER:
					sendDisplayCommand(SCAN_PUTWALL_ORDER_MSG, PUT_WALL_MSG); // could be for any wall in this state.
					break;

				case PUT_WALL_SCAN_LOCATION:
					sendDisplayCommand(SCAN_PUTWALL_LOCATION_MSG, PUT_WALL_MSG); // could be for any wall in this state.
					break;

				case PUT_WALL_SCAN_ITEM:
					sendDisplayCommand(SCAN_PUTWALL_ITEM_MSG, getForWallMessageLine()); // we know the wall the worker is doing in this state
					break;

				case PUT_WALL_SCAN_WALL:
					sendDisplayCommand(SCAN_PUTWALL_NAME_MSG, EMPTY_MSG);
					break;

				case GET_PUT_INSTRUCTION:
					sendDisplayCommand(GET_WORK_MSG, EMPTY_MSG);
					break;

				case NO_PUT_WORK:
					// we would like to say "No work for item in wall2"
					String itemID = getLastPutWallItemScan();
					String thirdLine = String.format("IN %s", getPutWallName());
					sendDisplayCommand(NO_WORK_FOR, itemID, thirdLine, SCAN_ITEM_OR_CLEAR);
					break;

				case DO_PUT:
					showActivePicks();
					break;

				default:
					break;
			}
		} finally {
			setSetStateStackCount(priorCount);
		}
	}

	/** 
	 * Command scans are split out by command then state because they are more likely to be state independent
	 */
	protected void processCommandScan(final String inScanStr) {

		updateInventoryCommandAccess(inScanStr);

		switch (inScanStr) {

			case LOGOUT_COMMAND:
				//You can logout at any time
				logout();
				break;

			case SETUP_COMMAND:
				setupCommandReceived();
				break;

			case STARTWORK_COMMAND:
			case REVERSE_COMMAND:
				startWorkCommandReceived(inScanStr);
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

			case INVENTORY_COMMAND:
				inventoryCommandReceived();
				break;

			case ORDER_WALL_COMMAND:
				orderWallCommandReceived();
				break;

			case PUT_WALL_COMMAND:
				putWallCommandReceived();
				break;

			default:

				//Legacy Behavior
				if (mCheStateEnum != CheStateEnum.SHORT_PICK_CONFIRM) {
					clearAllPositionControllers();
				}
				break;
		}
	}

	protected void orderWallCommandReceived() {
		// state sensitive. Only allow at start and finish for now.
		switch (mCheStateEnum) {
			case CONTAINER_SELECT:
				// only if no container/orders at all have been set up
				if (mPositionToContainerMap.size() == 0) {
					setState(CheStateEnum.PUT_WALL_SCAN_ORDER);
				} else {
					LOGGER.warn("User: {} attempted to do ORDER_WALL after having some pick orders set up", this.getUserId());
				}
				break;

			case PICK_COMPLETE:
			case PICK_COMPLETE_CURR_PATH:
				setState(CheStateEnum.PUT_WALL_SCAN_ORDER);
				break;

			default:
				break;
		}

	}

	protected void putWallCommandReceived() {
		// state sensitive. Only allow at start and finish for now.
		// state sensitive. Only allow at start and finish for now.
		switch (mCheStateEnum) {
			case CONTAINER_SELECT:
				// only if no container/orders at all have been set up
				if (mPositionToContainerMap.size() == 0) {
					setState(CheStateEnum.PUT_WALL_SCAN_WALL);
				} else {
					LOGGER.warn("User: {} attempted to do PUT_WALL after having some pick orders set up", this.getUserId());
				}
				break;

			case PICK_COMPLETE:
			case PICK_COMPLETE_CURR_PATH:
				setState(CheStateEnum.PUT_WALL_SCAN_WALL);
				break;

			default:
				break;
		}

	}

	protected void resetInventoryCommandAllowed() {
		lastScanedGTIN = null;
		setMInventoryCommandAllowed(true);
	}

	protected void updateInventoryCommandAccess(String inCommandStr) {
		if (!inCommandStr.equals(INVENTORY_COMMAND)) {
			setMInventoryCommandAllowed(false);
		}

		else if (inCommandStr.equals(LOGOUT_COMMAND)) {
			setMInventoryCommandAllowed(true);
		}
	}

	protected void inventoryCommandReceived() {

		switch (mCheStateEnum) {
			case CONTAINER_SELECT:
				if (isMInventoryCommandAllowed()) {
					setState(CheStateEnum.SCAN_GTIN);
				} else {
					LOGGER.warn("User: {} attempted inventory scan in invalid state: {}", this.getUserId(), mCheStateEnum);
				}
				break;
			default:
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
			case SCAN_GTIN:
				resetInventoryCommandAllowed();
				setState(CheStateEnum.CONTAINER_SELECT);
				break;

			case NO_PUT_WORK:
				setState(CheStateEnum.PUT_WALL_SCAN_ITEM);
				break;

			case PUT_WALL_SCAN_ORDER:
			case PUT_WALL_SCAN_LOCATION:
			case PUT_WALL_SCAN_ITEM:
			case GET_PUT_INSTRUCTION: // should never happen. State is transitory unless the server failed to respond
			case PUT_WALL_SCAN_WALL:
				// DEV-708, 712 specification. We want to return the state we started from: CONTAINER_SELECT or PICK_COMPLETE
				// Perhaps will need a member variable, but for now we can tell by the state of the container map
				if (mPositionToContainerMap.size() == 0) {
					setState(CheStateEnum.CONTAINER_SELECT);
				} else {
					setState(CheStateEnum.PICK_COMPLETE);
				}
				break;

			case DO_PUT:
			case SHORT_PUT:
			case SHORT_PUT_CONFIRM:
				// DEV-713 : more to do. This situation is on a job, and the user wants to abandon it.
				// Allow at all? set to PUT_WALL_SCAN_ITEM implies that. Or do nothing to force worker to complete or short it.

				setState(CheStateEnum.PUT_WALL_SCAN_ITEM);
				// TODO
				// If allowing, we would want to clear and refresh the poscon display, and clear activeWis.
				// Also a notifyXX()
				// Would be nice to send message to server to delete the work instruction, but we leave lots of wis hanging around.
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
			case SHORT_PUT_CONFIRM:
				confirmShortPick(inScanStr);
				break;

			case SCAN_SOMETHING_SHORT:
				confirmSomethingShortPick(inScanStr);
				break;

			case SCAN_GTIN:
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
	 * mContainerToWorkInstructionCountMap may be null in the PUT_WALL process
	 * Should be valid in our normal process states
	 */
	private boolean feedbackCountersValid() {
		return mContainerToWorkInstructionCountMap != null;
	}

	// --------------------------------------------------------------------------
	/**
	 * call the short transaction
	 * then update our local counts
	 */
	protected void doShortTransaction(final WorkInstruction inWi, final Integer inActualPickQuantity) {
		// CheDeviceLogic does the main shorting transactions
		super.doShortTransaction(inWi, inActualPickQuantity);

		// See that doNormalPick will remove from activePickList; therefore, any short should as well.
		if (mActivePickWiList.contains(inWi))
			mActivePickWiList.remove(inWi);

		// The proper setup orders flow will have this work instruction represented in this CHE's poscon data structures.
		// However, the PUT_WALL process (somewhat like LINE_SCAN) does not. Let's not NPE

		// Extra stuff for setup_orders is keeping track of poscon feedback information
		//Decrement count as short
		if (inWi.isHousekeeping()) {
			LOGGER.error("unexpected housekeep in doShortTransaction");
		} else if (feedbackCountersValid()) {
			//The HK check should never be false
			String containerId = inWi.getContainerId();
			WorkInstructionCount count = this.mContainerToWorkInstructionCountMap.get(containerId);
			count.decrementGoodCountAndIncrementShortCount();

			// TODO
			// Bug? This may have shorted ahead, so need to do more feedback

			Byte position = getPosconIndexOfContainerId(containerId);
			if (!position.equals(0)) {
				this.showCartRunFeedbackIfNeeded(position);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Process the Yes after a short. Very complicated for simultaneous picks
	 */
	protected void processShortPickYes(final WorkInstruction inWi, int inPicked) {

		notifyWiVerb(inWi, "SHORT", kLogAsWarn);
		doShortTransaction(inWi, inPicked);

		CheStateEnum state = getCheStateEnum();

		clearLedAndPosConControllersForWi(inWi); // wrong? What about any short aheads?

		// Depends on AUTOSHRT parameter
		boolean autoShortOn = mDeviceManager.getAutoShortValue();
		if (autoShortOn) {
			doShortAheads(inWi); // Jobs for the same product on the cart should automatically short, and not subject the user to them.
		}

		// If AUTOSHRT if off, there still might be other jobs in active pick list. If on, any remaining there would be shorted and removed.
		if (mActivePickWiList.size() > 0) {
			// If there's more active picks then show them.
			if (autoShortOn) {
				LOGGER.error("Simultaneous work instructions turned off currently, so unexpected case in confirmShortPick. state: {}",
					state);
				LOGGER.error("wi = {}", mActivePickWiList.get(0)); // log the first to help understand
			}
			showActivePicks();
		} else {
			// There's no more active picks, so move to the next set.
			if (state.equals(CheStateEnum.SHORT_PUT_CONFIRM))
				doNextWallPut();
			else
				doNextPick();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick.
	 * @param inScanStr
	 */
	protected void confirmShortPick(final String inScanStr) {
		if (inScanStr.equals(YES_COMMAND)) {
			WorkInstruction wi = mShortPickWi;
			if (wi != null) {
				processShortPickYes(wi, mShortPickQty);
			}
		} else {
			// Just return to showing the active picks or puts.
			CheStateEnum state = getCheStateEnum();
			if (state == CheStateEnum.SHORT_PICK_CONFIRM)
				setState(CheStateEnum.DO_PICK);
			else if (state == CheStateEnum.SHORT_PUT_CONFIRM)
				setState(CheStateEnum.DO_PUT);
			else {
				LOGGER.error("unexpected state in confirmShortPick: {}", state);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick from the SCAN_SOMETHING state.
	 * @param inScanStr
	 */
	protected void confirmSomethingShortPick(final String inScanStr) {
		if (inScanStr.equals(YES_COMMAND)) {
			WorkInstruction wi = mShortPickWi;
			if (wi != null) {
				processShortPickYes(wi, mShortPickQty);
			}
		} else {
			// Just return to showing the active picks.
			setState(CheStateEnum.SCAN_SOMETHING);
		}
	}

	// --------------------------------------------------------------------------
	/**  doNextPick has a major side effect of setState(DO_PICK) if there is more work.
	 *   Then setState(DO_PICK) calls showActivePicks()
	 */
	private void doNextPick() {
		// We might call doNextPick after a normal complete, or a short pick confirm.
		// We should not call it for any put wall cases; call doNextWallPut instead			
		CheStateEnum state = getCheStateEnum();
		if (state.equals(CheStateEnum.SHORT_PUT_CONFIRM) || state.equals(CheStateEnum.SHORT_PUT)
				|| state.equals(CheStateEnum.DO_PUT))
			LOGGER.error("unexpected call to doNextPick() state:{}", state);

		if (mActivePickWiList.size() > 0) {
			// There are still picks in the active list.
			LOGGER.error("Unexpected case in doNextPick. State:{}", state);
			showActivePicks();
			// each caller to doNextPick already checked mActivePickWiList.size(). Therefore new situation if found

		} else {

			if (selectNextActivePicks()) {
				if (isScanNeededToVerifyPick())
					setState(CheStateEnum.SCAN_SOMETHING); // This will cause showActivePicks();
				else
					setState(CheStateEnum.DO_PICK); // This will cause showActivePicks();
			} else {
				int uncompletedInstructionsOnOtherPathsSum = getUncompletedInstructionsOnOtherPathsSum();
				processPickComplete(uncompletedInstructionsOnOtherPathsSum > 0);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**  doNextWallPut side effects
	 */
	private void doNextWallPut() {
		LOGGER.debug(this + "doNextWallPut");

		if (mActivePickWiList.size() > 0) {
			// There are still picks in the active list.
			LOGGER.error("Unexpected case in doNextWallPut");
			showActivePicks();
			// each caller to doNextPick already checked mActivePickWiList.size(). Therefore new situation if found

		} else {

			if (selectNextActivePicks()) {
				setState(CheStateEnum.DO_PUT); // This will cause showActivePicks();
			} else {
				// no side effects needed? processPickComplete() is the corrolary
				setState(CheStateEnum.PUT_WALL_SCAN_ITEM);
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
		boolean doMultipleWiPicks = mDeviceManager.getPickMultValue(); // DEV-451

		boolean result = false;

		mActivePickWiList.clear(); // repopulate from mAllPicksWiList.

		// Loop through each container to see if there is a WI for that container at the next location.
		// The "next location" is the first location we find for the next pick.
		String firstLocationId = null;
		String firstItemId = null;
		Collections.sort(mAllPicksWiList, new WiGroupSortComparator());
		for (WorkInstruction wi : mAllPicksWiList) {

			// This check is valid for batch order pick. Invalid for put wall put.			
			CheStateEnum state = this.getCheStateEnum();
			if (state != CheStateEnum.GET_PUT_INSTRUCTION && state != CheStateEnum.DO_PUT && getPosconIndexOfWi(wi) == 0) {
				LOGGER.error("{} not in container map. State is {}", wi.getContainerId(), state);
				break;
			}

			// If the WI is INPROGRESS or NEW then consider it.
			if ((wi.getStatus().equals(WorkInstructionStatusEnum.NEW))
					|| (wi.getStatus().equals(WorkInstructionStatusEnum.INPROGRESS))) {
				// if we did not have one yet, make sure we add the first. Using firstItemId == null as our test
				if (firstItemId == null || wiMatchesItemLocation(firstItemId, firstLocationId, wi)) {
					firstLocationId = wi.getPickInstruction();
					firstItemId = wi.getItemId();
					wi.setStarted(new Timestamp(System.currentTimeMillis()));

					// Temporary
					if (mActivePickWiList.contains(wi)) {
						LOGGER.error("Not adding work instruction to active list again {}", wi);
					} else {
						mActivePickWiList.add(wi);
					}

					result = true;
					if (!doMultipleWiPicks)
						return true; // bail here instead of continuing to next wi in mAllPicksWiList, looking for location/item match
				}
			}
		}
		/*
			}
		} */

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

		// Warning: for simultaneous work instructions, "short ahead" might actually be behind in the sequence. So we need to iterate the activePickList always.

		Integer laterCount = 0;
		Integer toShortCount = 0;
		Integer removeHousekeepCount = 0;

		/* old Algorithm
		// Algorithm:  The all picks list is ordered by sequence. So consider anything in that list with later sequence.
		// One or two of these might also be in mActivePickWiList if it is a simultaneous work instruction pick that we are on.  Remove if found there.
		WorkInstruction prevWi = null;
		for (WorkInstruction wi : mAllPicksWiList) {
			if (laterWi(wi, inShortWi)) {
				laterCount++;
				if (sameProductLotEtc(wi, inShortWi)) {
					// When simultaneous pick work instructions return, we must not short ahead within the simultaneous pick. See CD_0043 for details.
					// if (!mActivePickWiList.contains(wi)) {
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
					// }
				}
			}
			prevWi = wi;
		}
		*/

		// New algorithm. Assemble what we want to short
		List<WorkInstruction> toShortList = new ArrayList<WorkInstruction>();
		// Find short aheads from the active picks first, which might have lower sort values.
		// If we are shorting the inShortWi, there should be no housekeeps in the active pick list.
		for (WorkInstruction wi : getActivePickWiList()) {
			if (wi.isHousekeeping()) {
				LOGGER.error("unanticipated housekeeping WI in mActivePickWiList in doShortAheads");
			}
			if (!wi.equals(inShortWi))
				if (sameProductLotEtc(wi, inShortWi)) {
					toShortCount++;
					toShortList.add(wi);
				}
		}
		// Now look for later work instructions to short, and remove housekeeps as necessary.
		WorkInstruction prevWi = null;
		for (WorkInstruction wi : mAllPicksWiList) {
			if (laterWi(wi, inShortWi)) {
				laterCount++;
				if (sameProductLotEtc(wi, inShortWi)) {
					// might be already in the list from above
					if (!toShortList.contains(wi)) {
						toShortCount++;
						toShortList.add(wi);
					}
					// housekeeps that are not the current job should not be in mActivePickWiList. Just check that possibility to confirm our understanding.
					if (unCompletedUnneededHousekeep(prevWi)) {
						if (mActivePickWiList.contains(prevWi))
							LOGGER.error("unanticipated housekeep in mActivePickWiList in doShortAheads");
						removeHousekeepCount++;
						doCompleteUnneededHousekeep(prevWi);
					}
				}
			}
			prevWi = wi;
		}
		// Finally, do all our shorts
		for (WorkInstruction wi : toShortList) {
			// Short aheads will always set the actual pick quantity to zero.
			notifyWiVerb(wi, "SHORT_AHEAD", kLogAsWarn);
			doShortTransaction(wi, 0);
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

			byte currentAssignment = getPosconIndexOfContainerId(mContainerInSetup);
			if (currentAssignment != 0) {
				// careful: 0 also equals PosControllerInstr.POSITION_ALL
				clearContainerAssignmentAtIndex(currentAssignment);
				this.clearOnePositionController(currentAssignment);
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

			case SCAN_SOMETHING:
				setState(CheStateEnum.SCAN_SOMETHING_SHORT);
				break;

			case SCAN_SOMETHING_SHORT:
				break;

			case SCAN_GTIN:
				break;

			case DO_PUT:
				setState(CheStateEnum.SHORT_PUT); // flashes the poscons with active jobs
				break;

			//Anywhere else we can start work if there's anything setup
			default:
				WorkInstruction wi = getOneActiveWorkInstruction();
				if (wi != null) {
					// short scan of housekeeping work instruction makes no sense
					if (wi.isHousekeeping())
						invalidScanMsg(mCheStateEnum); // Invalid to short a housekeep
					else
						setState(CheStateEnum.SHORT_PICK); // flashes all poscons with active jobs
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
			case PICK_COMPLETE_CURR_PATH:
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

			case SCAN_GTIN:
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

		if (USER_PREFIX.equals(inScanPrefixStr) || "".equals(inScanPrefixStr) || inScanPrefixStr == null) {
			clearAllPositionControllers();
			this.setUserId(inScanStr);
			mDeviceManager.verifyBadge(getGuid().getHexStringNoPrefix(), getPersistentId(), inScanStr);
			setState(CheStateEnum.VERIFYING_BADGE);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.IDLE);
		}
	}

	@Override
	public void processResultOfVerifyBadge(Boolean verified) {
		if (mCheStateEnum == CheStateEnum.VERIFYING_BADGE) {
			if (verified) {
				clearAllPositionControllers();
				setState(CheStateEnum.CONTAINER_SELECT);
			} else {
				setState(CheStateEnum.IDLE);
				invalidScanMsg(UNKNOWN_BADGE_MSG, EMPTY_MSG, CLEAR_ERROR_MSG_LINE_1, CLEAR_ERROR_MSG_LINE_2);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Factor this out as it is called from two places. The normal processLocationScan, and skipping the location scan if just going by sequence.
	 * WARNING: The parameter is the scanned location, or "START", or "REVERSE"
	 * @param inLocationStr
	 */
	private void requestWorkAndSetGetWorkState(final String inLocationStr, final Boolean reverseOrderFromLastTime) {
		clearAllPositionControllers();
		this.mLocationId = inLocationStr;
		Map<String, String> positionToContainerMapCopy = new HashMap<String, String>(mPositionToContainerMap);

		mDeviceManager.getCheWork(getGuid().getHexStringNoPrefix(),
			getPersistentId(),
			inLocationStr,
			positionToContainerMapCopy,
			getMReversePickOrder(),
			reverseOrderFromLastTime);
		setState(CheStateEnum.GET_WORK);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private void processLocationScan(final String inScanPrefixStr, String inScanStr) {
		if (LOCATION_PREFIX.equals(inScanPrefixStr)) {
			ledControllerClearLeds();
			requestWorkAndSetGetWorkState(inScanStr, false);
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
			case NO_WORK_CURR_PATH:
				processLocationScan(inScanPrefixStr, inContent);
				break;
			case LOCATION_SELECT:
				processLocationScan(inScanPrefixStr, inContent);
				break;

			case LOCATION_SELECT_REVIEW:
				processLocationScan(inScanPrefixStr, inContent);
				break;

			case PICK_COMPLETE_CURR_PATH:
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

			case SCAN_SOMETHING:
				// At any time during the pick we can change locations.
				if (inScanPrefixStr.equals(LOCATION_PREFIX)) {
					processLocationScan(inScanPrefixStr, inContent);
				}
				// If SCANPICK parameter is set, then the scan is SKU or UPC or LPN or .... Process it.
				processVerifyScan(inScanPrefixStr, inContent);
				break;

			case SCAN_GTIN:
				processGtinScan(inScanPrefixStr, inContent);
				break;

			case PUT_WALL_SCAN_LOCATION:
				processPutWallLocationScan(inScanPrefixStr, inContent);
				break;

			case PUT_WALL_SCAN_ORDER:
				processPutWallOrderScan(inScanPrefixStr, inContent);
				break;

			case NO_PUT_WORK:
				// If one item scan did not work, let user scan another directly without first having to CLEAR.
				// If user scans another location, let's assume it was a put wall change attempt.
				if ("L%".equals(inScanPrefixStr)) {
					processPutWallScanWall(inScanPrefixStr, inContent);
				} else {
					processPutWallItemScan(inScanPrefixStr, inContent);
				}
				break;

			case PUT_WALL_SCAN_ITEM:
				processPutWallItemScan(inScanPrefixStr, inContent);
				break;

			case PUT_WALL_SCAN_WALL:
				processPutWallScanWall(inScanPrefixStr, inContent);
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
		if (totalWorkInstructionCount > 0 && mContainerToWorkInstructionCountMap != null
				&& !mContainerToWorkInstructionCountMap.isEmpty()) {
			//Use the map to determine if we need to go to location_select or review

			//Check to see if we have any unknown containerIds. We must have a count for every container
			boolean doesNeedReview = !(mPositionToContainerMap.size() == mContainerToWorkInstructionCountMap.size());

			if (!doesNeedReview) {
				for (WorkInstructionCount wiCount : mContainerToWorkInstructionCountMap.values()) {
					if (wiCount.getGoodCount() == 0 || wiCount.hasBadCounts()) {
						doesNeedReview = true;
						break;
					}
				}
			}
			LOGGER.info("Got Counts {}", mContainerToWorkInstructionCountMap);

			if (doesNeedReview) {
				setState(CheStateEnum.LOCATION_SELECT_REVIEW);
			} else {
				setState(CheStateEnum.LOCATION_SELECT);
			}
		} else {
			int uncompletedInstructionsOnOtherPathsSum = getUncompletedInstructionsOnOtherPathsSum();
			if (uncompletedInstructionsOnOtherPathsSum == 0) {
				setState(CheStateEnum.NO_WORK);
			} else {
				setState(CheStateEnum.NO_WORK_CURR_PATH);
			}
		}
	}

	private int getUncompletedInstructionsOnOtherPathsSum() {
		if (!feedbackCountersValid()) {
			LOGGER.error("Inappropriate call to getUncompletedInstructionsOnOtherPathsSum. state:{}", getCheStateEnum());
			return 0;
		}
		// feedbackCountersValid() proves mContainerToWorkInstructionCountMap != null, so do not check that again.
		// why not just iterate the values?
		WorkInstructionCount[] counts = mContainerToWorkInstructionCountMap.values().toArray(new WorkInstructionCount[0]);
		int uncompletedInstructionsOnOtherPathsCounter = 0;
		for (WorkInstructionCount count : counts) {
			uncompletedInstructionsOnOtherPathsCounter += count.getUncompletedInstructionsOnOtherPaths();
		}
		return uncompletedInstructionsOnOtherPathsCounter;
	}

	/** Shows the count feedback on the position controller
	 * This returns without error if the feedback counters are not valid.
	 */
	protected void showCartSetupFeedback() {
		//make sure mContainerToWorkInstructionCountMap exists
		if (!feedbackCountersValid()) {
			return;
		}

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
				LOGGER.info("Position {} got no WIs. Causes: no path defined, unknown container id, no inventory", position);
			} else {
				byte count = (byte) wiCount.getGoodCount();
				LOGGER.info("Position Feedback_2: Poscon {} -- {}", position, wiCount);
				if (count == 0) {
					//0 good WI's
					if (wiCount.hasBadCounts() || wiCount.hasWorkOtherPaths()) {
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

	/** Shows the count feedback on the position controller during the cart run.
	 * If called for a specific position, will clear the position if there was no feedback. Avoids complex logic elsewhere.
	 */
	protected void showCartRunFeedbackIfNeeded(Byte inPosition) {
		if (inPosition == null) {
			LOGGER.error("showCartRunFeedbackIfNeeded was supplied a null position");
			return;
		}
		if (!feedbackCountersValid()) {
			LOGGER.error("incorrect call to showCartRunFeedbackIfNeeded");
			return;
		}

		// Temporary
		LOGGER.info("showCartRunFeedbackIfNeeded posconIndex {} called", inPosition);

		//Generate position controller commands
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

		boolean specificPositionCalled = false;
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
			specificPositionCalled = true;
			// TODO make sure the get is valid
			String containerId = mPositionToContainerMap.get(inPosition.toString());
			WorkInstructionCount wiCount = mContainerToWorkInstructionCountMap.get(containerId);
			PosControllerInstr instr = this.getCartRunFeedbackInstructionForCount(wiCount, inPosition);
			if (instr != null) {
				instructions.add(instr);
			}
		}

		//Show counts on position controllers. Or, if was called specifically, clears the poscon if there is no feedback to show.
		if (!instructions.isEmpty()) {
			sendPositionControllerInstructions(instructions);
		} else if (specificPositionCalled) {
			clearOnePositionController(inPosition);
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
	private void startWorkCommandReceived(final String inScanStr) {
		boolean reverse = REVERSE_COMMAND.equalsIgnoreCase(inScanStr);
		//Split it out by state
		switch (mCheStateEnum) {
		//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do nothing. Only a "Clear Error" will get you out 
				break;

			case VERIFYING_BADGE:
				//Do nothing while still verifying badge
				break;

			case CONTAINER_POSITION:
				processContainerPosition(COMMAND_PREFIX, inScanStr);
				break;

			case LOCATION_SELECT:
			case LOCATION_SELECT_REVIEW:
				// Normally, start work here would hit the default case below, calling start work() which queries to server again
				// ultimately coming back to LOCATION_SELECT state. However, if okToStartWithoutLocation, then start scan moves us forward
				if (isOkToStartWithoutLocation()) {
					LOGGER.info("starting without a start location");
					boolean reverseOrderFromLastTime = getMReversePickOrder() != reverse;
					//Remember the selected pick direction
					setMReversePickOrder(reverse);
					requestWorkAndSetGetWorkState(null, reverseOrderFromLastTime);
				} else { // do as we did before
					if (mPositionToContainerMap.values().size() > 0) {
						startWork(inScanStr);
					} else {
						setState(CheStateEnum.NO_CONTAINERS_SETUP);
					}
				}
				break;

			case SCAN_GTIN:
				break;

			//Anywhere else we can start work if there's anything setup
			case CONTAINER_SELECT:
			default:
				if (mPositionToContainerMap.values().size() > 0) {
					startWork(inScanStr);
					//Remember the selected pick direction
					setMReversePickOrder(reverse);
				} else {
					setState(CheStateEnum.NO_CONTAINERS_SETUP);
				}
				break;

		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Parameters currently are the string literals REVERSE_COMMAND or START_COMMAND
	 * Tricky process from here. Send the container/order list to server to compute work instructions.
	 * This goes to COMPUTE_WORK state which basically just waits for the server response.
	 * I think for this, we always want to computeCheWork uniformly, in the forward direction. Will get reversed later.
	 */
	private void startWork(final String inScanedPickDirections) {
		boolean isReverse = inScanedPickDirections.equals(REVERSE_COMMAND);

		clearAllPositionControllers();
		mContainerInSetup = "";
		//Duplicate map to avoid later changes
		Map<String, String> positionToContainerMapCopy = new HashMap<String, String>(mPositionToContainerMap);
		mDeviceManager.computeCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), positionToContainerMapCopy, isReverse);
		setState(CheStateEnum.COMPUTE_WORK);
	}

	// --------------------------------------------------------------------------
	/**
	 * Give the CHE the work it needs to do for a container.
	 * This is recomputed at the server for ALL containers on the CHE and returned in work-order.
	 * Whatever the CHE thought it needed to do before is now invalid and is replaced by what we send here.
	 * Only not final because we let CsDeviceManager call this generically.
	 */
	public void assignWork(final List<WorkInstruction> inWorkItemList, String message) {
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
			// doNextPick will set the state.
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Almost the same as assignWork(), but some state transitions differ
	 */
	public void assignWallPuts(final List<WorkInstruction> inWorkItemList, String message) {
		notifyPutWallResponse(inWorkItemList);
		if (inWorkItemList == null || inWorkItemList.size() == 0) {
			setState(CheStateEnum.NO_PUT_WORK);
		} else {
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
			mAllPicksWiList.addAll(inWorkItemList);
			doNextWallPut(); // should set the state
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	@Override
	protected void processNormalPick(WorkInstruction inWi, Integer inQuantity) {

		inWi.setActualQuantity(inQuantity);
		inWi.setPickerId(mUserId);
		inWi.setCompleted(new Timestamp(System.currentTimeMillis()));
		inWi.setStatus(WorkInstructionStatusEnum.COMPLETE);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
		notifyWiVerb(inWi, "COMPLETE by button", kLogAsInfo);

		mActivePickWiList.remove(inWi);

		CheStateEnum state = getCheStateEnum();

		if (feedbackCountersValid()) { // makes sure mContainerToWorkInstructionCountMap not null
			// maintain the CHE feedback, but not for put wall puts. Not DO_PUT. And not SHORT_PUT state.
			//Decrement count if this is a non-HK WI
			String containerId = inWi.getContainerId();
			if (!inWi.isHousekeeping()) {
				WorkInstructionCount count = this.mContainerToWorkInstructionCountMap.get(containerId);
				count.decrementGoodCountAndIncrementCompleteCount();
			}
			Byte position = getPosconIndexOfContainerId(containerId);
			if (!position.equals(0)) {
				this.showCartRunFeedbackIfNeeded(position); // handles the CHE poscons, including clearing this specific poscon if no feedback
			}

		}

		clearLedAndPosConControllersForWi(inWi); // includes putwall poscons, not CHE poscons

		// If PICKMULT if on, there still might be other jobs in active pick list. If off, there should not be
		if (mActivePickWiList.size() > 0) {
			// If there's more active picks then show them.
			if (!mDeviceManager.getPickMultValue()) {
				LOGGER.error("Simultaneous work instructions turned off currently, so unexpected case in processNormalPick");
				// let's log the first just for fun
				LOGGER.error("wi = {}", mActivePickWiList.get(0));
			}
			showActivePicks();
		} else {
			// There's no more active picks, so move to the next set.
			if (state == CheStateEnum.DO_PUT)
				doNextWallPut();
			else
				doNextPick();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inButtonNum
	 * @return
	 */
	private String getContainerIdFromButtonNum(Integer inButtonNum) {
		// TODO make the get safe
		return mPositionToContainerMap.get(Integer.toString(inButtonNum));
	}

	private void clearContainerAssignmentAtIndex(byte posconIndex) {
		// careful: POSITION_ALL is zero
		if (PosControllerInstr.POSITION_ALL.equals(posconIndex))
			LOGGER.error("Incorrect use of clearContainerAssignmentAtIndex"); // did you really intend mPositionToContainerMap.clear()?
		else
			mPositionToContainerMap.remove(Integer.toString(posconIndex));
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
	 */
	protected void processButtonPress(Integer inButtonNum, Integer inQuantity) {
		// In general, this can only come if the poscon was set in a way that prepared it to be able to send.
		// However, pickSimulator.pick() can be called in any context, which simulates the button press command coming in.

		// The point is, let's check our state
		switch (mCheStateEnum) {
			case DO_PICK:
			case SHORT_PICK:
				break;

			case SCAN_SOMETHING:
			case SCAN_SOMETHING_SHORT:
				// Do not allow button press in this state. We did display the count on poscon. User might get confused.
				setState(mCheStateEnum);
				return;
			default: {
				// We want to ignore the button press, but force out starting poscon situation again.
				setState(mCheStateEnum);
				LOGGER.warn("Unexpected button press ignored. OR invalid pick() call by some unit test.");
				return;
			}
		}

		String containerId = getContainerIdFromButtonNum(inButtonNum);
		if (containerId == null) {
			// Simply ignore button presses when there is no container.
		} else {
			WorkInstruction wi = getWorkInstructionForContainerId(containerId);
			if (wi == null) {
				// Simply ignore button presses when there is no work instruction.
			} else {
				notifyButton(inButtonNum, inQuantity);
				if (inQuantity >= wi.getPlanMinQuantity()) {
					processNormalPick(wi, inQuantity);
				} else {
					// More kludge for count > 99 case
					Integer planQuantity = wi.getPlanQuantity();
					if (inQuantity == maxCountForPositionControllerDisplay && planQuantity > maxCountForPositionControllerDisplay)
						processNormalPick(wi, planQuantity); // Assume all were picked. No way for user to tell if more than 98 given.
					else {
						processShortPickOrPut(wi, inQuantity);
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	@Override
	protected void processShortPickOrPut(WorkInstruction inWi, Integer inQuantity) {
		// This should be a corollary of processNormalPick, which has side effects of clearing the poscon for the pressed button if there is more work,
		// Or not clearing, and just going to feedback

		// For short, we are always going to go to shortPickConfirm stage. User must scan. We put out the poscon the user pressed as feedback that something happened.
		// If PICKMULT, there may be many lit poscons. All should be put out as we don't want to imply that the user can decrement and press others.
		for (WorkInstruction wi : this.getActivePickWiList()) {
			byte position = getPosconIndexOfWi(wi);
			if (position != 0)
				clearOnePositionController(position);
		}

		// Then the inherited shorts part is the same
		super.processShortPickOrPut(inWi, inQuantity);
	}

	protected void processPutWallOrderScan(final String inScanPrefixStr, final String inScanStr) {
		setLastPutWallOrderScan(inScanStr);
		setState(CheStateEnum.PUT_WALL_SCAN_LOCATION);
	}

	protected void processPutWallLocationScan(final String inScanPrefixStr, final String inScanStr) {
		String orderId = getLastPutWallOrderScan();
		PutWallPlacementRequest message = new PutWallPlacementRequest(getPersistentId().toString(), orderId, inScanStr);
		mDeviceManager.clientEndpoint.sendMessage(message);
		sendOrderPlacementMessage(getLastPutWallOrderScan(), inScanStr);
		setState(CheStateEnum.PUT_WALL_SCAN_ORDER);
	}

	private void sendOrderPlacementMessage(String orderId, String locationName) {
		// This will form the command and send to server. If successful, the putwall poscon feedback will be apparent.
		LOGGER.info("to do: send put wall setup msg {} at {}", orderId, locationName);

		notifyOrderToPutWall(orderId, locationName);
	}

	protected void processPutWallItemScan(final String inScanPrefixStr, final String inScanStr) {

		setState(CheStateEnum.GET_PUT_INSTRUCTION);
		// The response then returns the work instruction, and transitions to DO_PUT state. 
		// Note: or NO_PUT_WORK

		notifyPutWallItem(inScanStr, getPutWallName());
		setLastPutWallItemScan(inScanStr);
		sendWallItemWiRequest(inScanStr, getPutWallName());

	}

	protected void processPutWallScanWall(final String inScanPrefixStr, final String inScanStr) {
		// The goal here is to remember the wall name that was scanned, then transition to PUT_WALL_SCAN_ITEM
		setPutWallName(inScanStr);
		setState(CheStateEnum.PUT_WALL_SCAN_ITEM);
	}

	private void sendWallItemWiRequest(String itemOrUpc, String putWallName) {
		// DEV-713
		LOGGER.info("sendWallItemWiRequest for {} in wall {}", itemOrUpc, putWallName);

		mDeviceManager.computePutWallInstruction(getGuid().getHexStringNoPrefix(), getPersistentId(), itemOrUpc, putWallName);
		// sends a command. Ultimately returns back the work instruction list for assign work

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
				if (WorkInstructionTypeEnum.HK_BAYCOMPLETE.equals(theEnum)) {
					returnBool = true;
					showSpecialPositionCode(PosControllerInstr.BAY_COMPLETE_CODE, wi.getContainerId());
				} else if (WorkInstructionTypeEnum.HK_REPEATPOS.equals(theEnum)) {
					returnBool = true;
					showSpecialPositionCode(PosControllerInstr.REPEAT_CONTAINER_CODE, wi.getContainerId());
				}
			}
		} else if (mActivePickWiList.size() > 1) {
			// temporary debugging. Should never hit the error case.
			boolean foundHousekeep = false;
			for (WorkInstruction aWi : getActivePickWiList()) {
				if (aWi.isHousekeeping()) {
					foundHousekeep = true;
				}
			}
			// list out what is in the active list
			if (foundHousekeep) {
				LOGGER.error("+++++ ACTIVE WI LIST  +++++");
				for (WorkInstruction aWi : mActivePickWiList) {
					LOGGER.error("WI in active list {}", aWi);
				}
				LOGGER.error("+++++ FROM TOTAL LIST +++++");
				for (WorkInstruction aWi2 : getAllPicksWiList()) {
					LOGGER.error("WI in all picks list {}", aWi2);
				}
			}
		}
		return returnBool;
	}

	/**
	 * What do we show on the CHE for put wall put? Note that we have variable context. If slow move CHE came to do the put, user might like the 
	 * appropriate container corresponding to a wall to light somehow. (What count if displaying multiples?) If multiple, we definitely cannot allow
	 * button press on the CHE
	 */
	protected void doPosConDisplaysforActivePutWis() {
		// for now, nothing
	}

	/**
	* Make poscons reflect the current active wis. For MULTPICK, this may be several
	*/
	@Override
	protected void doPosConDisplaysforActiveWis() {

		CheStateEnum state = getCheStateEnum();

		if (state == CheStateEnum.DO_PUT || state == CheStateEnum.SHORT_PUT) {
			doPosConDisplaysforActivePutWis();
		} else {

			// Housekeeping moves will result in a single work instruction in the active pickes. Enum tells if housekeeping.
			if (!sendHousekeepingDisplay()) {

				List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

				for (WorkInstruction wi : mActivePickWiList) {
					Byte theIndex = getPosconIndexOfWi(wi);
					if (theIndex > 0) {
						PosControllerInstr instruction = getPosInstructionForWiAtIndex(wi, getPosconIndexOfWi(wi));
						instructions.add(instruction);
					} else
						LOGGER.error("unexpected missing poscon index for work instruction. State is {}", state);
					// This might be troublesome for the Lowe's case of poscons on pick slot, and not on the cart at all.
				}

				// check instructions size? The callee does check and errors
				sendPositionControllerInstructions(instructions);

			}
		}
	}

	// --------------------------------------------------------------------------
	/** What poscon does this wi belong to?
	 */
	public byte getPosconIndexOfWi(WorkInstruction wi) {
		String cntrId = wi.getContainerId();
		return getPosconIndexOfContainerId(cntrId);
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
	 * return the button for this container ID. Return 0 if not found, or for invalid input.
	 * This is the main lookup function. Many calling contexts
	 * careful: 0 also equals PosControllerInstr.POSITION_ALL
	 */
	private byte getPosconIndexOfContainerId(String inContainerId) {
		if (inContainerId == null || inContainerId.isEmpty())
			return 0;

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
		resetInventoryCommandAllowed();
		mPositionToContainerMap.clear();
		mContainerToWorkInstructionCountMap = null;
		mContainerInSetup = "";
	}

	/**
	 * Setup the CHE by clearing all the data structures
	 */
	protected void setupChe() {
		mPositionToContainerMap.clear();
		mContainerToWorkInstructionCountMap = null;
		mContainerInSetup = "";
		super.setupChe();
	}

	// --------------------------------------------------------------------------
	/**
	 * Attempt to guess if we already must have scanned this one.
	 * For DEV-692.  Our allPicksList should be sorted and still have recently completed work.
	 */
	@Override
	protected boolean alreadyScannedSkuOrUpcOrLpnThisWi(WorkInstruction inWi) {
		String matchSku = inWi.getItemId();
		String matchPickLocation = inWi.getPickInstruction();
		for (WorkInstruction wi : getAllPicksWiList()) {
			if (!wi.equals(inWi))
				if (wiMatchesItemLocation(matchSku, matchPickLocation, wi)) {
					WorkInstructionStatusEnum theStatus = wi.getStatus();
					// Short or complete must have been scanned.
					if (theStatus.equals(WorkInstructionStatusEnum.COMPLETE) || theStatus.equals(WorkInstructionStatusEnum.SHORT))
						return true;

				}
		}
		return false;
	}

}
