/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
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

	@Getter
	@Setter
	private String				lastScanedDetailId;

	@Getter
	@Setter
	private String				readyMsg;

	public LineScanDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final ICsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

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
			case GET_WORK:
				processGetWorkStateScan(inScanPrefixStr, inContent);
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
			setState(CheStateEnum.READY);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.IDLE);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the GET_WORK state. 
	 * Should only happen if the get work answer did not come back from server before user scanned another detailId
	 */
	private void processGetWorkStateScan(final String inScanPrefixStr, final String inScanStr) {
		if (!inScanPrefixStr.isEmpty()) {
			LOGGER.info("processReadyStateScan: Expecting order detail ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.READY);
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
			invalidScanMsg(CheStateEnum.READY);
			return;
		}
		// if an apparently good order detail ID, send that off to the backend, but transition to a "querying" state.
		setLastScanedDetailId(inScanStr); // not needed so far, but do it for completion
		sendDetailWIRequest(inScanStr);
		setState(CheStateEnum.GET_WORK);

	}

	// --------------------------------------------------------------------------
	/**
	 * The CHE is in the PICK state. For non-command scans, we only expect new order detail
	 */
	private void processPickStateScan(final String inScanPrefixStr, final String inScanStr) {
		// if on one job, and new detail scan comes in, then we ask about aborting the current job.

		if (!inScanPrefixStr.isEmpty()) {
			LOGGER.info("processPickStateScan: Expecting order detail ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.READY);
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
	 * Usually only one uncompleted WI back
	 */
	public void assignWork(final List<WorkInstruction> inWorkItemList) {
		int wiCount = inWorkItemList.size();
		LOGGER.info("assignWork returned " + wiCount + "work instructions");
		// Implement the success case first. Then worry about the rest.
		if (wiCount == 1) {
			WorkInstruction wi = inWorkItemList.get(0);
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
			mActivePickWiList.add(wi);
			setState(CheStateEnum.DO_PICK);
		} else {
			// if 0 or more than 1, we want to transition back to ready, but with a message

			if (wiCount == 0)
				setReadyMsg("NO JOBS FOR SCANNED");
			else if (wiCount == 0)
				setReadyMsg(wiCount + " JOBS FOR SCANNED");
			setState(CheStateEnum.READY);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * If on a job, abandon it and go back to ready state
	 */
	@Override
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
	 * Two uses: confirm scan, or abandon job check 
	 */
	@Override
	protected void yesOrNoCommandReceived(final String inScanStr) {
		boolean theValue = Boolean.parseBoolean(inScanStr);

		switch (mCheStateEnum) {
			case ABANDON_CHECK:
				if (theValue) {
					String lastDetailId = getLastScanedDetailId();
					sendDetailWIRequest(lastDetailId);
					setState(CheStateEnum.GET_WORK);
				} else {
					// If no, we want to remain on current job
					setState(CheStateEnum.DO_PICK);
					// or on GET_WORK if we never got past get work?
				}
				break;

			default:
				break;

		}
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
				sendDisplayCommand(SCAN_LINE_MSG, getReadyMsg());
				break;

			case GET_WORK:
				sendDisplayCommand(COMPUTE_WORK_MSG, GO_TO_LOCATION_MSG);
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

			case ABANDON_CHECK:
				sendDisplayCommand(ABANDON_CHECK_MSG, YES_NO_MSG);
				break;

			default:
				break;
		}
	}

	private void showTheActivePick() {
		// needs implementation. Roughly corresponds to showActivePicks
		sendDisplayCommand(ONE_JOB_MSG, EMPTY_MSG);

	}

}
