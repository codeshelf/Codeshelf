/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandControlClearPosController;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.NotificationService.EventType;

/**
 * @author jonranstrom
 *
 */
public class LineScanDeviceLogic extends CheDeviceLogic {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LineScanDeviceLogic.class);

	@Getter
	@Setter
	private String				lastScanedDetailId;

	@Getter
	@Setter
	private String				readyMsg;
	
	

	public LineScanDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		updateConfigurationFromManager();
		setLastScanedDetailId("");
		setReadyMsg("");
	}

	@Override
	public String getDeviceType() {
		return CsDeviceManager.DEVICETYPE_CHE_LINESCAN;
	}

	/** 
	 * Command scans are split out by command then state because they are more likely to be state independent
	 */
	protected void processCommandScan(final String inScanStr) {

		switch (inScanStr) {

			case LOGOUT_COMMAND:
				//You can logout at any time
				setReadyMsg("");
				logout();
				break;

			case SETUP_COMMAND:
			case STARTWORK_COMMAND:
				// does nothing in this workflow
				break;

			case SHORT_COMMAND:
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
				inventoryScanCommandReveived();
				break;

			default:
				break;
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
			case READY:
				processReadyStateScan(inScanPrefixStr, inContent);
				break;
			case GET_WORK:
				processGetWorkStateScan(inScanPrefixStr, inContent);
				break;
			case DO_PICK:
			case SHORT_PICK:
			case SHORT_PICK_CONFIRM:
				processPickStateScan(inScanPrefixStr, inContent);
				break;
			case SCAN_SOMETHING:
				// If SCANPICK parameter is set, then the scan is SKU or UPC or LPN or .... Process it.
				processVerifyScan(inScanPrefixStr, inContent);
				break;
			case SCAN_GTIN:
				processGtinScan(inScanPrefixStr, inContent);
				break;
			default:
				break;
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the Idle state (not logged in), so if we get a user scan then move to ready state.
	 */
	private void processIdleStateScan(final String inScanPrefixStr, final String inScanStr) {

		if (USER_PREFIX.equals(inScanPrefixStr) || "".equals(inScanPrefixStr) || inScanPrefixStr == null) {
			setReadyMsg("");
			this.setUserId(inScanStr);
			mDeviceManager.verifyBadge(getGuid().getHexStringNoPrefix(), getPersistentId(), inScanStr);
			setState(CheStateEnum.VERIFYING_BADGE);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			setReadyMsg("Invalid scan");
		}
	}
	
	@Override
	public void processResultOfVerifyBadge(Boolean verified) {
		if (mCheStateEnum == CheStateEnum.VERIFYING_BADGE) {
			if (verified) {
				clearAllPositionControllers();
				setState(CheStateEnum.READY);
			} else {
				setState(CheStateEnum.IDLE);
				invalidScanMsg(UNKNOWN_BADGE_MSG, EMPTY_MSG, CLEAR_ERROR_MSG_LINE_1, CLEAR_ERROR_MSG_LINE_2);
			}
		}
	}


	/**
	 * The CHE is in the GET_WORK state. 
	 * Should only happen if the get work answer did not come back from server before user scanned another detailId
	 */
	private void processGetWorkStateScan(final String inScanPrefixStr, final String inScanStr) {
		if (!inScanPrefixStr.isEmpty()) {
			LOGGER.info("processReadyStateScan: Expecting order detail ID: " + inScanStr);
			setReadyMsg("Invalid scan");
			return;
		}
		setLastScanedDetailId(inScanStr); // not needed so far, but do it for completion
		setState(CheStateEnum.ABANDON_CHECK);
	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the READY state. We normally expect an order detail ID with no scan prefix
	 */
	private void processReadyStateScan(final String inScanPrefixStr, final String inScanStr) {
		if (!inScanPrefixStr.isEmpty()) {
			LOGGER.info("processReadyStateScan: Expecting order detail ID: " + inScanStr);
			setReadyMsg("Invalid scan");
			return;
		}
		// if an apparently good order detail ID, send that off to the backend, but transition to a "querying" state.
		setLastScanedDetailId(inScanStr); // not needed so far, but do it for completion
		LOGGER.info("set state GET_WORK in processReadyStateScan");

		setState(CheStateEnum.GET_WORK); // need to this before sending the command, or else timing is odd in unit test
		sendDetailWIRequest(inScanStr);

	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the PICK state. For non-command scans, we only expect new order detail
	 */
	private void processPickStateScan(final String inScanPrefixStr, final String inScanStr) {
		// if on one job, and new detail scan comes in, then we ask about aborting the current job.

		if (!inScanPrefixStr.isEmpty()) {
			LOGGER.info("processPickStateScan: Expecting order detail ID: " + inScanStr);
			setReadyMsg("Invalid scan");
			return;
		}

		// need to save new detail ID that was scanned in.
		setLastScanedDetailId(inScanStr); // needed
		setState(CheStateEnum.ABANDON_CHECK);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send this in a command to the server. We expect it is an order detail Id.
	 */
	private void sendDetailWIRequest(final String inScanStr) {
		LOGGER.info("sendDetailWIRequest for detail:" + inScanStr);
		mDeviceManager.computeCheWork(getGuid().getHexStringNoPrefix(), getPersistentId(), inScanStr);
		// sends a command. Ultimately returns back the work instruction list below in assignWork
	}

	// --------------------------------------------------------------------------
	/**
	 * This will be enhanced to have the object parameter that includes the work instruction list and other return information.
	 * Currently handled: 1 uncompleted, 1 completed, 0 returned, more than one returned.
	 */
	public void assignWork(final List<WorkInstruction> inWorkItemList, String message) {
		LOGGER.info("LineScanDeviceLogic.assignWork() entered");

		// only honor the response if we are in the state where we sent and are waiting for the response.
		CheStateEnum currentState = this.getCheStateEnum();
		if (!currentState.equals(CheStateEnum.GET_WORK)) {
			LOGGER.info("LineScanDeviceLogic.assignWork(): not in GET_WORK. In " + currentState);
			return;
		}

		int wiCount = inWorkItemList.size();
		LOGGER.info("assignWork returned " + wiCount + " work instruction(s)");
		// Implement the success case first. Then worry about the rest.
		if (wiCount == 1) {
			WorkInstruction wi = inWorkItemList.get(0);
			if (wi.getStatus().equals(WorkInstructionStatusEnum.COMPLETE)) {
				setReadyMsg("Already completed");
				LOGGER.info("LineScanDeviceLogic.assignWork(): Already completed");
				setState(CheStateEnum.READY);
			} else {
				

				mActivePickWiList.clear();
				mAllPicksWiList.clear();
				mActivePickWiList.add(wi);

				if (isScanNeededToVerifyPick()){
					LOGGER.info("LineScanDeviceLogic.assignWork(): transitioning to SCAN_SOMETHING");
					LOGGER.info("calling set state SCAN_SOMETHING in assignWork");
					setState(CheStateEnum.SCAN_SOMETHING); // This will cause showActivePicks();
				} else {
					LOGGER.info("LineScanDeviceLogic.assignWork(): transitioning to DO_PICK");
					LOGGER.info("calling set state DO_PICK in assignWork");
					setState(CheStateEnum.DO_PICK); // This will cause showActivePicks();
				}
			}
		} else {
			// if 0 or more than 1, we want to transition back to ready, but with a message
			if (wiCount == 0){
				if (message == null) {
					setReadyMsg("No jobs last scan");					
				} else {
					setReadyMsg(message);
				}
			} else {
				// more than 1. Perhaps 1 complete and we should find the uncompleted one. See what the new object brings us
				setReadyMsg(wiCount + " jobs last scan");
			}
			LOGGER.info("LineScanDeviceLogic.assignWork(): not 1 job");
			setState(CheStateEnum.READY);
		}
	}
	
	protected void inventoryScanCommandReveived() {
		CheStateEnum currentState = getCheStateEnum();
		
		switch(currentState) {
			case READY:
				// In setup orders. Only want to respect this just after login
				setState(CheStateEnum.SCAN_GTIN);
				break;
			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * If on a job, abandon it and go back to ready state
	 */
	@Override
	protected void clearErrorCommandReceived() {
		clearAllPositionControllers();
		// needs implementation
		CheStateEnum currentState = getCheStateEnum();
		switch (currentState) {
			case DO_PICK:
			case GET_WORK:
				// get_work is a funny one. If stuck there (no response from server to the request), we want clear to get us out of trouble.
				// however, if the response is merely slow, we might go back to ready state before the response comes. We only honor the response from 
				// get_work state.
			case SHORT_PICK:
			case ABANDON_CHECK:
			case SHORT_PICK_CONFIRM:
				setReadyMsg("Abandoned last job");
				setState(CheStateEnum.READY); // clears off job and poscon
				break;

			case READY:
			case IDLE:
				setReadyMsg(""); // If user is looking at message on ready state, clear it. But do not change state.
				// however, call the setState as it forces the CHE update out, which actually clears the message.
				setState(currentState);
				break;
			case SCAN_SOMETHING:
				setState(currentState);
				break;
			case SCAN_GTIN:
				lastScanedGTIN = null;
				setReadyMsg("");
				setState(CheStateEnum.READY);
				break;
			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SHORT_PICK command to tell us there is no product to pick.
	 */
	@Override
	protected void shortPickCommandReceived() {
		WorkInstruction wi = getActiveWorkInstruction();
		if (wi == null) {
			setReadyMsg("Invalid scan");
			return;
		}

		//Split it out by state
		switch (mCheStateEnum) {

			case DO_PICK:
				setState(CheStateEnum.SHORT_PICK); // Used to be SHORT_PICK_CONFIRM
				break;

			case SCAN_SOMETHING:
				setState(CheStateEnum.SCAN_SOMETHING_SHORT); 
				break;
			case SCAN_SOMETHING_SHORT:
				break;
				
			default:
				setReadyMsg("No job to short");
				break;

		}

	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned YES or NO.
	 * Two uses: confirm scan, or abandon job check 
	 */
	@Override
	protected void yesOrNoCommandReceived(final String inScanStr) {
		boolean answeredYes = inScanStr.equals(YES_COMMAND);
		// do not use Boolean.parseBoolean(inScanStr); as it returns false for "YES".

		switch (mCheStateEnum) {
			case ABANDON_CHECK:
				if (answeredYes) {
					String lastDetailId = getLastScanedDetailId();
					setState(CheStateEnum.GET_WORK); // do this before sending the command
					sendDetailWIRequest(lastDetailId);
				} else {
					// If no, we want to remain on current job
					LOGGER.info("calling setState DO_PICK in yesOrNoCommandReceived");
					setState(CheStateEnum.DO_PICK);
					// or on GET_WORK if we never got past get work?
				}
				break;

			case SHORT_PICK_CONFIRM:
				confirmShortPick(inScanStr);
				break;
			
			case SCAN_SOMETHING_SHORT:
				confirmSomethingShortPick(inScanStr);
				break;

			default:
				break;

		}
	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick.
	 * @param inScanStr
	 */
	protected void confirmShortPick(final String inScanStr) {
		if (inScanStr.equals(YES_COMMAND)) {
			WorkInstruction wi = this.getActiveWorkInstruction();
			if (wi != null) {
				notifyWiVerb(wi, EventType.SHORT, kLogAsWarn);
				doShortTransaction(wi, mShortPickQty);

				clearLedAndPosConControllersForWi(wi);
				setReadyMsg("");
				setState(CheStateEnum.READY);
			}
		} else {
			// NO will go back to the job, with the full count again
			setState(CheStateEnum.DO_PICK);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick from the SCAN_SOMETHING state.
	 * @param inScanStr
	 */
	protected void confirmSomethingShortPick(final String inScanStr) {
		if (inScanStr.equals(YES_COMMAND)) {
			List<WorkInstruction> wiList = this.getActivePickWiList();
			WorkInstruction wi = null;
			if (wiList.size() > 0)
				wi = wiList.get(0);
	
			if (wi != null) {
				doShortTransaction(wi, 0);
			}
			
			clearLedAndPosConControllersForWi(wi);
			setReadyMsg("");
			setState(CheStateEnum.READY);
		} else {
			// Just return to showing the active picks.
			setState(CheStateEnum.SCAN_SOMETHING);
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
		notifyWiVerb(inWi, EventType.COMPLETE, kLogAsInfo);

		mActivePickWiList.remove(inWi);

		// Skip the count stuff that setup_Orders process has

		// Clear off any lit aisles for last job
		clearLedAndPosConControllersForWi(inWi);

		if (mActivePickWiList.size() > 0) {
			// If there's more active picks then show them.
			LOGGER.error("Simulataneous work instructions turned off currently, so unexpected case in processNormalPick");
			// certainly not expected in Line_Scan process
			showActivePicks();
		} else {
			// There's no more active picks, so transition to READY state
			setReadyMsg("");
			setState(CheStateEnum.READY);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * trivial helper function
	 */

	private WorkInstruction getActiveWorkInstruction() {
		if (mActivePickWiList.size() == 0)
			return null;
		else
			return mActivePickWiList.get(0);
	}

	// --------------------------------------------------------------------------
	/**
	 * Complete the active WI. For this process mode, only one poscon is there.
	 * @param inButtonNum
	 * @param inQuantity
	 */
	protected void processButtonPress(Integer inButtonNum, Integer inQuantity) {

		// The point is, let's check our state
		switch (mCheStateEnum) {
			case DO_PICK:
			case SHORT_PICK:
				break;
				
			case SCAN_SOMETHING:
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
		
		WorkInstruction wi = getActiveWorkInstruction();
		if (wi == null) {
			// Simply ignore button presses when there is no work instruction.
		} else {
			String itemId = wi.getItemId();
			String orderDetailId = wi.getOrderDetailId();
			LOGGER.info("Button for " + orderDetailId + " / " + itemId);
			if (inQuantity >= wi.getPlanMinQuantity()) {
				processNormalPick(wi, inQuantity);
			} else {
				// Kludge for count > 99 case
				Integer planQuantity = wi.getPlanQuantity();
				if (inQuantity == maxCountForPositionControllerDisplay && planQuantity > maxCountForPositionControllerDisplay)
					processNormalPick(wi, planQuantity); // Assume all were picked. No way for user to tell if more than 98 given.
				else
					processShortPickOrPut(wi, inQuantity);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Very simple for line scan CHE. Send the one WI count to poscon #1.
	 */
	@Override
	protected void doPosConDisplaysforActiveWis() {

		/*
		byte planQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanQuantity());
		byte minQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanMinQuantity());
		byte maxQuantityForPositionController = byteValueForPositionDisplay(firstWi.getPlanMaxQuantity());
		if (getCheStateEnum() == CheStateEnum.SHORT_PICK)
			minQuantityForPositionController = byteValueForPositionDisplay(0); // allow shorts to decrement on position controller down to zero

		byte freq = PosControllerInstr.SOLID_FREQ;
		byte brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
		// blink is an indicator that decrement button is active, usually as a consequence of short pick. (Max difference is also possible for discretionary picks)
		if (planQuantityForPositionController != minQuantityForPositionController
				|| planQuantityForPositionController != maxQuantityForPositionController) {
			freq = PosControllerInstr.BRIGHT_DUTYCYCLE;
			brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
		}

		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

		PosControllerInstr instruction = new PosControllerInstr(getPosconIndex(),
			planQuantityForPositionController,
			minQuantityForPositionController,
			maxQuantityForPositionController,
			freq,
			brightness);
		*/
		WorkInstruction firstWi = getOneActiveWorkInstruction();
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		PosControllerInstr instruction = getPosInstructionForWiAtIndex(firstWi, getPosconIndex());		
		instructions.add(instruction);
		sendPositionControllerInstructions(instructions);
	}

	// --------------------------------------------------------------------------
	/** Hardcode to return 1 for Line_Scan process mode
	 */
	private Byte getPosconIndex() {
		Byte returnValue = 1;
		return returnValue;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void clearThePoscon() {
		ICommand command = new CommandControlClearPosController(NetEndpoint.PRIMARY_ENDPOINT, getPosconIndex());
		mRadioController.sendCommand(command, getAddress(), true);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void clearOutCurrentJob() {
		mActivePickWiList.clear();
		clearThePoscon();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	@Override
	protected void setState(final CheStateEnum inCheState) {

		int priorCount = getSetStateStackCount();
		try {
			// This is tricky. setState() may have side effects that call setState. So even as the internal setState is done, the first one may not be done.
			// Therefore, a counter instead of a boolean.
			setSetStateStackCount(priorCount + 1);
			CheStateEnum previousState = mCheStateEnum;
			boolean isSameState = previousState == inCheState;
			mCheStateEnum = inCheState;
			LOGGER.info("Switching to state: {} isSameState: {}", inCheState, isSameState);

			switch (inCheState) {
				case IDLE:
					sendDisplayCommand(SCAN_USERID_MSG, EMPTY_MSG);
					break;

				case VERIFYING_BADGE:
					sendDisplayCommand(VERIFYING_BADGE_MSG, EMPTY_MSG);
					break;

				case READY:
					// We jump back to ready from various places. Do not clear the ready message, but do clear any job that was on before.
					clearOutCurrentJob();
					sendDisplayCommand(SCAN_LINE_MSG, getReadyMsg());
					break;

				case GET_WORK:
					setReadyMsg("");
					sendDisplayCommand(COMPUTE_WORK_MSG, GO_TO_LOCATION_MSG);
					break;

				case SHORT_PICK_CONFIRM:
					clearThePoscon(); // Clear so it does not look like you can press the button to finish the job. It will come back on NO.
					sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
					break;

				case SHORT_PICK:
					showTheActivePick();
					break;

				case DO_PICK:
					showTheActivePick();
					break;

				case ABANDON_CHECK:
					clearThePoscon(); // Clear so it does not look like you can press the button to finish the job. It will come back on NO.
					sendDisplayCommand(ABANDON_CHECK_MSG, YES_NO_MSG);
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
				
				case SCAN_GTIN:
					if (lastScanedGTIN == null) {
						sendDisplayCommand(SCAN_GTIN, EMPTY_MSG);
					} else {
						sendDisplayCommand(SCAN_GTIN_OR_LOCATION, EMPTY_MSG);
					}
					break;
					
				default:
					break;
			}
		} finally {
			setSetStateStackCount(priorCount);
		}
	}

	private void showTheActivePick() {
		showActivePicks(); // ancestor method works
	}
	
	protected void invalidScanMsg(final CheStateEnum inCheState) {
		mCheStateEnum = inCheState;

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, INVALID_SCAN_MSG);
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
}
