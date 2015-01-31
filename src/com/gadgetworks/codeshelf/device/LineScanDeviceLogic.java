/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;

/**
 * @author jonranstrom
 *
 */
public class LineScanDeviceLogic extends CheDeviceLogic {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LineScanDeviceLogic.class);

	public LineScanDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

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
				logout();
				break;

			case SETUP_COMMAND:
				// does nothing in this workflow
				break;

			case STARTWORK_COMMAND:
				// does nothing in this workflow
				break;

			case SHORT_COMMAND:
				shortPickCommandReceived();
				// needs implementation.
				break;

			case YES_COMMAND:
			case NO_COMMAND:
				yesOrNoCommandReceived(inScanStr);
				break;

			case CLEAR_ERROR_COMMAND:
				clearErrorCommandReceived();
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
			case DO_PICK:
				processPickStateScan(inScanPrefixStr, inContent);
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

		if (USER_PREFIX.equals(inScanPrefixStr)) {
			clearAllPositionControllers();
			setState(CheStateEnum.READY);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.IDLE);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the READY state. We normally expect an order detail ID with no scan prefix
	 */
	private void processReadyStateScan(final String inScanPrefixStr, final String inScanStr) {

	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the PICK state. For non-command scans, we only expect new order detail
	 */
	private void processPickStateScan(final String inScanPrefixStr, final String inScanStr) {

	}

	@Override
	// --------------------------------------------------------------------------
	/**
	 * If on a job, abandon it and go back to ready state
	 */
	protected void clearErrorCommandReceived() {
		// needs implementation
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SHORT_PICK command to tell us there is no product to pick.
	 */
	@Override
	protected void shortPickCommandReceived() {
		// needs implementation
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned YES or NO.
	 * @param inScanStr
	 */
	@Override
	protected void yesOrNoCommandReceived(final String inScanStr) {
		// needs implementation
	}

	// --------------------------------------------------------------------------
	/**
	 */
	@Override
	protected void setState(final CheStateEnum inCheState) {
		CheStateEnum previousState = mCheStateEnum;
		boolean isSameState = previousState == inCheState;
		mCheStateEnum = inCheState;
		LOGGER.debug("Switching to state: {} isSameState: {}", inCheState, isSameState);

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, EMPTY_MSG);
				break;

			case READY:
				sendDisplayCommand(SCAN_LINE_MSG, EMPTY_MSG);
				break;

			case SHORT_PICK_CONFIRM:
				this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
				sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
				break;

			case SHORT_PICK:
				this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
				showTheActivePick();
				break;

			case DO_PICK:
				this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
				showTheActivePick();
				break;

			default:
				break;
		}
	}

	private void showTheActivePick() {
		// needs implementation. Roughly corresponds to showActivePicks
	}

}
