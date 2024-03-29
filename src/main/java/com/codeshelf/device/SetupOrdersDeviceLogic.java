/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.BitSet;
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

import com.codeshelf.behavior.InfoBehavior.InfoPackage;
import com.codeshelf.flyweight.command.CommandControlPosconBroadcast;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.CodeshelfTape;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.CheLightingEnum;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.util.CompareNullChecker;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.protocol.request.InfoRequest.InfoRequestType;
import com.codeshelf.ws.protocol.request.PutWallPlacementRequest;

/**
 * @author jonranstrom
 *
 */
public class SetupOrdersDeviceLogic extends CheDeviceLogic {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger					LOGGER									= LoggerFactory.getLogger(SetupOrdersDeviceLogic.class);

	// The CHE's container map.
	private Map<String, String>					mPositionToContainerMap;
	// This always exists, but map may be empty if PUT_WALL prior to container setup, or reduced meaning if PUT_WALL after work complete on this path.

	//Map of containers to work instruction counts
	@Getter
	private Map<String, WorkInstructionCount>	mContainerToWorkInstructionCountMap;
	// Careful: this initializes as null, and only exists if there was successful return of the response from server. It must always be null checked.

	private List<String>						mStolenCntrs;

	// Transient. The CHE has scanned this container, and will add to container map if it learns the poscon position.
	private String								mContainerInSetup;

	// The location the CHE scanned as starting point. Note: this initializes from che.getLastScannedLocation(), but then is maintained locally.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mLocationId;

	// If the CHE is in PUT_WALL process, the wall currently getting work instructions for
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mPutWallName;

	// When putting items into Sku wall, save here if another wall holds the required item
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mAlternatePutWall;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mLastPutWallItemScan;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private CheStateEnum						mRememberEnteringWallOrInventoryState	= CheStateEnum.CONTAINER_SELECT;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private CheStateEnum						mRememberEnteringInfoState				= CheStateEnum.PUT_WALL_SCAN_ITEM;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private CheStateEnum						mRememberEnteringLowState				= CheStateEnum.SCAN_SOMETHING;

	//Save result of the last INFO request
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private InfoPackage							mInfo;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mLastScannedInfoLocation				= null;

	// When we  START or location change again, the server does not give us what we completed already.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private int									mRememberPriorCompletes					= 0;

	// When we  START or location change again, the server does not give us what we shorted already.
	// Warning: server will make new plan for what you shorted.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private int									mRememberPriorShorts					= 0;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String								mSubstitutionScan						= null;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private CheStateEnum						mRememberPreSubstitutionState			= CheStateEnum.SCAN_SOMETHING;

	private String								mLastAssignedPoscon						= null;

	private final boolean						useNewCheScreen							= true;

	private String								mBusyPosconHolders						= null;
	private String								mIgnoreExistingHoldersOnThesePoscons	= null;

	private boolean								mSetupMixHasPutwall						= false;
	private boolean								mSetupMixHasCntrOrder					= false;

	//This field indicates whether SETUP_SUMMARY was reached from container setup or from elsewhere
	private boolean								mPostSetupSummary						= false;

	private static int							BAY_COMPLETE_CODE						= 1;
	private static int							REPEAT_CONTAINER_CODE					= 2;

	public SetupOrdersDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController,
		final Che che) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController, che);

		mPositionToContainerMap = new HashMap<String, String>();
		mStolenCntrs = new ArrayList<String>();

		updateConfigurationFromManager();
		// For DEV-776, 778, we want to initialize to know the path location the CHE last scanned onto.
		if (che != null) { // many tests do not have the che available, so just leave mLocationId null
			mLocationId = che.getLastScannedLocation();
		}
	}

	public boolean usesNewCheScreen() {
		return useNewCheScreen;
	}

	@Override
	public String getDeviceType() {
		return CsDeviceManager.DEVICETYPE_CHE_SETUPORDERS;
	}

	private String getForWallMessageLine() {
		return String.format("FOR %s", getPutWallName());
	}

	// --------------------------------------------------------------------------
	/* 
	 * Called by CheDeviceLogic.startDevice() if the cause was a user reset
	 * We use this to break out of "half-states" where we are waiting for a response from the server that will likely never come
	 */
	@Override
	protected void adjustStateForCheconReset() {
		CheStateEnum currentState = getCheStateEnum();
		switch (currentState) {
			case VERIFYING_BADGE:
				setState(CheStateEnum.IDLE);
				LOGGER.warn("Breaking {} out of verify badge state", this.getGuidNoPrefix());
				break;
			case GET_WORK:
			case COMPUTE_WORK:
				setState(CheStateEnum.SETUP_SUMMARY);
				LOGGER.warn("Breaking {} out of compute work state", this.getGuidNoPrefix());
				break;
			default:
				// Are there more half-states?
				// After scan UPC?
				// After putwall or sku wall scan waiting for plan?
		}

	}

	// --------------------------------------------------------------------------
	/**
	 */
	@Override
	protected void setState(final CheStateEnum inCheState) {
		int priorCount = getSetStateStackCount();
		try {
			String line1, line2, line3, line4;
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

				case SETUP_SUMMARY:
					if (mStolenCntrs.size() > 0) {
						adjustForStolenCntrs(); // DEV-1347
					}
					sendSummaryScreen();
					// We also want cart feedback, including active work instruction counts per poscon
					this.showCartSetupFeedback();
					break;

				case COMPUTE_WORK:
					sendDisplayCommand(COMPUTE_WORK_MSG, EMPTY_MSG);
					clearAllPosconsOnThisDevice();
					break;

				case GET_WORK:
					sendDisplayCommand(GET_WORK_MSG, EMPTY_MSG);
					clearAllPosconsOnThisDevice();
					break;

				case CONTAINER_SELECT:
					if (mPositionToContainerMap.size() < 1) {
						sendDisplayCommand(getContainerSetupMsg(), EMPTY_MSG);
					} else {
						sendDisplayCommand(getContainerSetupMsg(), OR_START_WORK_MSG, EMPTY_MSG, SHOWING_ORDER_IDS_MSG);
					}
					if (previousState != CheStateEnum.CONTAINER_POSITION) {
						showContainerAssignments();
					} else {
						showLatestContainerAssignment();
					}
					break;

				case CONTAINER_POSITION:
					sendDisplayCommand(SELECT_POSITION_MSG, EMPTY_MSG);
					if (previousState != CheStateEnum.CONTAINER_SELECT) {
						showContainerAssignments();
					}
					break;

				case CONTAINER_POSITION_INVALID:
					invalidScanMsg(INVALID_POSITION_MSG, EMPTY_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
					break;

				case CONTAINER_POSITION_IN_USE:
					invalidScanMsg(POSITION_IN_USE_MSG, EMPTY_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
					break;

				case CONTAINER_SELECTION_INVALID:
					// This deals with DEV-836, 837.
					if (!this.isSetupMixOk()) {
						invalidScanMsg(INVALID_CONTAINER_MSG, "Use START to begin", EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
					} else {
						invalidScanMsg(INVALID_CONTAINER_MSG, EMPTY_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
					}
					break;

				case NO_CONTAINERS_SETUP:
					invalidScanMsg(NO_CONTAINERS_SETUP_MSG, FINISH_SETUP_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
					break;

				case SHORT_PUT_CONFIRM:
					sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
					break;

				case SHORT_PICK_CONFIRM:
					clearAllPosconsOnThisDevice();
					if (isSameState) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					WorkInstruction currentWi = getOneActiveWorkInstruction();
					if (currentWi.getSubstitution() == null) {
						sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
					} else {
						sendDisplayCommand(AMOUNT_CONFIRM_MSG, YES_NO_MSG);
					}
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
					if (isSameState || previousState == CheStateEnum.GET_WORK || previousState == CheStateEnum.SCAN_SOMETHING
							|| previousState == CheStateEnum.SHORT_PICK_CONFIRM) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					showActivePicks(); // if setState(DO_PICK) is called, it always calls showActivePicks. fewer direct calls to showActivePicks elsewhere.
					break;

				case SCAN_SOMETHING:
					if (isSameState || previousState == CheStateEnum.GET_WORK || previousState == CheStateEnum.SCAN_SOMETHING_SHORT) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					showActivePicks(); // change this? DEV-653
					break;

				case SCAN_SOMETHING_SHORT: // this is like a short confirm.
					clearAllPosconsOnThisDevice();
					if (isSameState) {
						this.showCartRunFeedbackIfNeeded(PosControllerInstr.POSITION_ALL);
					}
					sendDisplayCommand(SHORT_PICK_CONFIRM_MSG, YES_NO_MSG);
					break;

				case SCAN_GTIN:
					sendGtinScreen(lastScannedGTIN);
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

				case SKU_WALL_SCAN_GTIN_LOCATION:
					line2 = "For " + mLastPutWallItemScan;
					line3 = "Into wall: " + mPutWallName;
					sendDisplayCommand(SCAN_LOCATION_MSG, line2, line3, CANCEL_TO_EXIT_MSG);
					break;

				case SKU_WALL_ALTERNATE_WALL_AVAILABLE:
					if (mAlternatePutWall.equalsIgnoreCase("other walls")) {
						line1 = "In other walls";
						line2 = "Scan other wall";
					} else {
						line1 = "Item in " + mAlternatePutWall;
						line2 = "Scan to " + mAlternatePutWall;
					}
					line3 = "Or tape in " + mPutWallName;
					line4 = "Or CANCEL " + mLastPutWallItemScan;
					sendDisplayCommand(line1, line2, line3, line4);
					break;

				case NO_PUT_WORK:
					// we would like to say "No work for item in wall2"
					String itemID = getLastPutWallItemScan();
					String thirdLine = String.format("IN %s", getPutWallName());
					sendDisplayCommand(NO_WORK_FOR, itemID, thirdLine, SCAN_ITEM_OR_CANCEL);
					break;

				case DO_PUT:
					showActivePicks();
					break;

				case PUT_WALL_POSCON_BUSY:
					showPosconBusyScreen();
					break;

				case INFO_PROMPT:
					sendDisplayCommand("SCAN LOCATION", "For content INFO", EMPTY_MSG, CANCEL_TO_EXIT_MSG);
					break;

				case INFO_RETRIEVAL:
					sendDisplayCommand("RETRIEVING INFO", EMPTY_MSG);
					break;

				case INFO_DISPLAY:
					displayInfo();
					break;

				case REMOVE_CONFIRMATION:
					displayRemoveInfo();
					break;

				case REMOVE_CHE_CONTAINER:
					sendDisplayCommand(SELECT_POSITION_MSG, REMOVE_CONTAINER_MSG, EMPTY_MSG, CANCEL_TO_EXIT_MSG);
					showContainerAssignments();
					break;

				case REMOTE:
					sendRemoteStateScreen();
					break;

				case REMOTE_PENDING:
					sendDisplayCommand("Linking...", EMPTY_MSG);
					break;

				case REMOTE_LINKED:
					LOGGER.info("Transition to linked state");
					// sendRemoteStateScreen();
					// This user is logged in. As we transition into REMOTE_LINKED state, ask other CHE device to draw our screen.
					enterLinkedState();
					break;

				case LOW_CONFIRM:
					sendDisplayCommand(LOW_CONFIRM_MSG, YES_NO_MSG);
					break;
				default:
					break;

				case SUBSTITUTION_CONFIRM:
					clearAllPosconsOnThisDevice();
					line1 = StringUtils.substring("Substitute " + getSubstitutionScan(), 0, 40);
					WorkInstruction activeWi = getOneActiveWorkInstruction();
					if (activeWi == null) {
						LOGGER.warn("Somehow got null from getOneActiveWorkInstruction() when entering the SUBSTITUTION_CONFIRM state");
						sendDisplayCommand(line1, "?");
					} else {
						sendDisplayCommand(line1, "For " + getOneActiveWorkInstruction().getItemId() + "?");
					}
					break;
			}
		} finally {
			setSetStateStackCount(priorCount);
		}
	}

	/**
	 * Command scans are split out by command then state because they are more likely to be state independent
	 */
	@Override
	protected void processCommandScan(final String inScanStr) {
		mPostSetupSummary = false;

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
				processCommandYesOrNo(inScanStr);
				break;

			case CLEAR_COMMAND:
			case CANCEL_COMMAND:
				processCommandCancel();
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

			case REMOTE_COMMAND:
				remoteCommandReceived();
				break;

			case POSCON_COMMAND:
				posconSetupCommandReveived();
				break;

			case INFO_COMMAND:
				infoCommandReceived();
				break;

			case REMOVE_COMMAND:
				removeCommandReceived();
				break;

			case LOW_COMMAND:
				processCommandLow();
				break;

			default:

				//Legacy Behavior
				if (mCheStateEnum != CheStateEnum.SHORT_PICK_CONFIRM) {
					clearAllPosconsOnThisDevice();
				}
				break;
		}
	}

	protected void remoteCommandReceived() {
		// state sensitive. Only allow at start and finish for now.
		switch (mCheStateEnum) {
			case SETUP_SUMMARY:
				// One tricky case. If this CHE is being remote controlled by another, and then there is a local remote scan
				// we need to do something to remain consistent. Notice, we are only logged in as a result of the controlling CHE's log in, so
				// if we do anything at all, we would have to log out. Probably best to do nothing. Local logout scan is the way out of this if the
				// mobile has disappeared
				if (this.getLinkedFromCheGuid() != null) {
					LOGGER.warn("{}: REMOTE scan from controlled CHE not allowed. (Would have to log out to do anything.) User should LOGOUT to use this CHE independently",
						getGuidNoPrefix());
				} else {
					// The usual case is to go straight to remote state.
					setState(CheStateEnum.REMOTE);
				}
				break;

			case REMOTE_LINKED:
				setState(CheStateEnum.REMOTE);
				break;

			case REMOTE:
				// If there is a link, the screen says "Remote to keep link"
				// So, we need to know if there is a link. If so, transition to REMOTE_LINKED
				// If not, just stay here.
				String cheName = getLinkedToCheName();
				if (cheName != null) {
					setState(CheStateEnum.REMOTE_LINKED);
				}
				break;

			default:
				break;
		}
	}

	protected void orderWallCommandReceived() {
		// state sensitive. Only allow at start and finish for now.
		switch (mCheStateEnum) {
			case CONTAINER_SELECT:
				// only if no container/orders at all have been set up
				if (mPositionToContainerMap.size() == 0) {
					setRememberEnteringWallOrInventoryState(mCheStateEnum);
					setState(CheStateEnum.PUT_WALL_SCAN_ORDER);
				} else {
					LOGGER.warn("User: {} attempted to do ORDER_WALL after having some pick orders set up", this.getUserId());
				}
				break;

			case SETUP_SUMMARY:
				setRememberEnteringWallOrInventoryState(mCheStateEnum);
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
					setRememberEnteringWallOrInventoryState(mCheStateEnum);
					setState(CheStateEnum.PUT_WALL_SCAN_WALL);
				} else {
					LOGGER.warn("User: {} attempted to do PUT_WALL after having some pick orders set up", this.getUserId());
				}
				break;

			case SETUP_SUMMARY:
				setRememberEnteringWallOrInventoryState(mCheStateEnum);
				setState(CheStateEnum.PUT_WALL_SCAN_WALL);
				break;

			default:
				break;
		}

	}

	/**
	 * Inventory command received. Worker might do this at any time. Within this, controlled by state, we want to allow, or simply ignore the scan.
	 * Inventory allowed (for now) from basically the same places put wall is allowed, at the start or end of process, but not within.
	 * SETUP_SUMMARY state mostly.
	 */
	protected void inventoryCommandReceived() {

		switch (mCheStateEnum) {

			case CONTAINER_SELECT:
				// only if no container/orders at all have been set up. Consistent with putwall/order wall
				if (mPositionToContainerMap.size() == 0) {
					setRememberEnteringWallOrInventoryState(mCheStateEnum);
					setState(CheStateEnum.SCAN_GTIN);
				} else {
					LOGGER.warn("User: {} attempted to do INVENTORY after having some pick orders set up", this.getUserId());
				}
				break;

			case SETUP_SUMMARY:
				setRememberEnteringWallOrInventoryState(mCheStateEnum);
				setState(CheStateEnum.SCAN_GTIN);
				break;

			case SCAN_GTIN:
				// Inventory got us to this state. Rescan of inventory just clears the GTIN so that it will not accidentally inventory.
				this.lastScannedGTIN = null;
				setState(CheStateEnum.SCAN_GTIN);
				break;

			default:
				LOGGER.warn("User: {} attempted inventory scan in invalid state: {}", this.getUserId(), mCheStateEnum);
				break;
		}

	}

	protected void infoCommandReceived() {
		switch (mCheStateEnum) {
			case IDLE:
				displayDeviceInfo();
				break;
			case SCAN_GTIN: //Inventory scan
			case PUT_WALL_SCAN_WALL:
			case PUT_WALL_SCAN_ITEM:
				setInfo(null);
				setRememberEnteringInfoState(mCheStateEnum);
				setState(CheStateEnum.INFO_PROMPT);
				break;
			case SETUP_SUMMARY:
				setState(CheStateEnum.CONTAINER_SELECT);
				break;
			default:
		}
	}

	protected void removeCommandReceived() {
		switch (mCheStateEnum) {
			case INFO_DISPLAY:
				InfoPackage info = getInfo();
				if (info == null || !info.isSomethingToRemove()) {
					sendDisplayCommand(REMOVE_NOTHING_MSG, EMPTY_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
					break;
				}
				switch (getRememberEnteringInfoState()) {
					case SCAN_GTIN: //Inventory->Info
						setState(CheStateEnum.REMOVE_CONFIRMATION);
						break;
					case PUT_WALL_SCAN_WALL: //PutWall->Info
					case PUT_WALL_SCAN_ITEM:
						setState(CheStateEnum.REMOVE_CONFIRMATION);
						break;
					default:
				}
				break;
			case CONTAINER_SELECT:
				setState(CheStateEnum.REMOVE_CHE_CONTAINER);
				break;
			default:
		}
	}

	private void processCommandLow() {
		switch (mCheStateEnum) {
			case SCAN_SOMETHING:
			case SCAN_SOMETHING_SHORT:
			case SCAN_GTIN:
			case DO_PUT:
			case DO_PICK:
				setRememberEnteringLowState(mCheStateEnum);
				setState(CheStateEnum.LOW_CONFIRM);
				break;
			default:
				LOGGER.warn("LOW command is not available from state {}", mCheStateEnum);
		}
	}

	@Override
	protected void processCommandCancel() {
		//Split it out by state
		switch (mCheStateEnum) {

		//Clear the error
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				clearAllPosconsOnThisDevice();
				setState(CheStateEnum.CONTAINER_SELECT);
				break;
			case SCAN_GTIN:
				lastScannedGTIN = null;
				CheStateEnum priorToInventoryState = getRememberEnteringWallOrInventoryState();
				setState(priorToInventoryState);
				break;

			case NO_PUT_WORK:
				setState(CheStateEnum.PUT_WALL_SCAN_ITEM);
				break;

			case REMOTE:
				//  If linked, the screen says clear to unlink. So we need to know if we are linked
				String cheName = getLinkedToCheName();
				if (cheName != null) {
					// This triggers our clear link action
					unlinkRemoteCheAssociation();
					// will go to REMOTE_PENDING
				} else {
					setState(CheStateEnum.SETUP_SUMMARY);
				}
				break;

			// just a note: clear command is passed through in REMOTE_LINKED state

			case REMOTE_PENDING: // State is transitory unless the server failed to respond
				LOGGER.error("Probable bug. Clear from REMOTE_PENDING state.");
				setState(CheStateEnum.SETUP_SUMMARY);
				break;

			case GET_PUT_INSTRUCTION: // State is transitory unless the server failed to respond
				LOGGER.error("Probable bug. Clear from GET_PUT_INSTRUCTION state.");
				CheStateEnum priorState = getRememberEnteringWallOrInventoryState();
				setState(priorState);
				break;

			case PUT_WALL_SCAN_ORDER:
			case PUT_WALL_SCAN_LOCATION:
			case PUT_WALL_SCAN_ITEM:
			case PUT_WALL_SCAN_WALL:
				// DEV-708, 712 specification. We want to return the state we started from: CONTAINER_SELECT or PICK_COMPLETE
				priorState = getRememberEnteringWallOrInventoryState();
				setState(priorState);
				break;

			case INFO_PROMPT:
				priorState = getRememberEnteringInfoState();
				setState(priorState);
				break;

			case INFO_RETRIEVAL:
			case INFO_DISPLAY:
				setInfo(null);
				setState(CheStateEnum.INFO_PROMPT);
				break;

			case REMOVE_CONFIRMATION:
				setState(CheStateEnum.INFO_DISPLAY);
				break;

			case REMOVE_CHE_CONTAINER:
				setState(CheStateEnum.CONTAINER_SELECT);
				break;

			case SKU_WALL_SCAN_GTIN_LOCATION:
			case SKU_WALL_ALTERNATE_WALL_AVAILABLE:
				setState(CheStateEnum.PUT_WALL_SCAN_ITEM);
				break;

			case DO_PUT:
			case SHORT_PUT:
			case SHORT_PUT_CONFIRM:
				// DEV-713 : Worker is on a job, wants to abandon it.
				// Allow at all? If we did nothing it would force worker to complete or short it.
				WorkInstruction wi = this.getOneActiveWorkInstruction();
				if (wi != null) {
					notifyWiVerb(wi, WorkerEvent.EventType.CANCEL_PUT, false);
					clearLedAndPosConControllersForWi(wi);
				}
				setState(CheStateEnum.PUT_WALL_SCAN_ITEM);
				// Might be nice to send message to server to delete the work instruction, but we leave the wi hanging around for many use cases.
				// When that item/GTIN is scanned again, the work instruction will "recycle", so we are not creating a bigger and bigger mess.

				break;

			case LOW_CONFIRM:
				setState(getRememberEnteringLowState());
				break;

			case SUBSTITUTION_CONFIRM:
				setSubstitutionScan(null);
				setState(getRememberPreSubstitutionState());
				break;

			case SHORT_PICK:
				setState(CheStateEnum.DO_PICK);
				break;

			default:
				//Reset ourselves
				//Ideally we shouldn't have to clear poscons here
				clearAllPosconsOnThisDevice();
				setState(mCheStateEnum);
				break;

		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned YES or NO.
	 * @param inScanStr
	 */
	@Override
	protected void processCommandYesOrNo(final String inScanStr) {

		switch (mCheStateEnum) {
		//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do nothing. Only a "Cancel Error" will get you out
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

			case PUT_WALL_POSCON_BUSY:
				//mActivePickWiList.clear();
				WorkInstruction nextWi = getOneActiveWorkInstruction();
				if (nextWi != null) {
					mIgnoreExistingHoldersOnThesePoscons = nextWi.getPosConCmdStream();
				}
				processPutWallItemScan("", getLastPutWallItemScan());
				break;

			case INFO_DISPLAY:
				InfoRequestType type = YES_COMMAND.equals(inScanStr) ? InfoRequestType.LIGHT_COMPLETE_ORDERS
						: InfoRequestType.LIGHT_INCOMPLETE_ORDERS;
				mDeviceManager.performInfoOrRemoveAction(type,
					getLastScannedInfoLocation(),
					getGuidNoPrefix(),
					getPersistentId().toString());
				break;

			case REMOVE_CONFIRMATION:
				if (YES_COMMAND.equalsIgnoreCase(inScanStr)) {
					if (getRememberEnteringInfoState() == CheStateEnum.SCAN_GTIN) {
						InfoPackage info = getInfo();
						UUID removeItemId = info == null ? null : info.getRemoveItemId();
						mDeviceManager.performInfoOrRemoveAction(InfoRequestType.REMOVE_INVENTORY,
							getLastScannedInfoLocation(),
							getGuidNoPrefix(),
							getPersistentId().toString(),
							removeItemId);
					} else {
						mDeviceManager.performInfoOrRemoveAction(InfoRequestType.REMOVE_WALL_ORDERS,
							getLastScannedInfoLocation(),
							getGuidNoPrefix(),
							getPersistentId().toString());
					}
					setInfo(null);
					setState(CheStateEnum.INFO_PROMPT);
				} else {
					setState(CheStateEnum.INFO_DISPLAY);
				}
				break;

			case LOW_CONFIRM:
				if (YES_COMMAND.equalsIgnoreCase(inScanStr)) {
					notifyWiVerb(getOneActiveWorkInstruction(), EventType.LOW, false);
				}
				setState(getRememberEnteringLowState());
				break;

			case SUBSTITUTION_CONFIRM:
				if (YES_COMMAND.equalsIgnoreCase(inScanStr)) {
					doShortsAndSubstitutionsAheads(getOneActiveWorkInstruction(), getSubstitutionScan());
					if (mActivePickWiList.isEmpty()) {
						WorkInstruction upcomingWi = mAllPicksWiList.get(0);
						doNextItemWithPossibleFlashingThread(upcomingWi.isHousekeeping());
					} else {
						showActivePicks();
					}
					//setState(CheStateEnum.DO_PICK);
				} else {
					setState(getRememberPreSubstitutionState());
				}
				setSubstitutionScan(null);
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
	@Override
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
		if (inWi.isAlreadyShorted()) {
			//Prevent multiple shorting of the same WI on a single "SHORT" scan
			return;
		}
		inWi.setAlreadyShorted(true);

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

	protected void processShortPickYes(final List<WorkInstruction> inWiList, int inPicked) {
		boolean autoShortOn = mDeviceManager.getAutoShortValue();
		boolean housekeeping = true;
		for (WorkInstruction wi : inWiList) {
			if (!wi.isHousekeeping()) {
				housekeeping = false;
			}

			doShortTransaction(wi, inPicked);

			EventType eventType = EventType.SHORT;
			notifyWiVerb(wi, eventType, kLogAsWarn);

			clearLedAndPosConControllersForWi(wi); // wrong? What about any short aheads?

			// Depends on AUTOSHRT parameter
			if (autoShortOn) {
				doShortsAheads(wi); // Jobs for the same product on the cart should automatically short, and not subject the user to them.
			}
		}
		// If AUTOSHRT if off, there still might be other jobs in active pick list. If on, any remaining there would be shorted and removed.
		int afterActiveCount = mActivePickWiList.size();
		if (afterActiveCount > 0) {
			// If there's more active picks then show them.
			if (autoShortOn) {
				/* JR says: DEV-1234 we used to error here, and probably should. Our short-ahead logic is wrong. See comments in 
				 * SetupOrdersDeviceLogic.selectNextActivePicks(). The only reason this is not an error is because this got called multiple
				 * times for short ahead situation. On the first call, mActivePickWiList.size is zero as expected, and falls through below to doNextPick()
				 * which populates the next active pick list state. The subsequent call to this then looks like an error.
				WorkInstruction firstWiInList = mActivePickWiList.get(0);
				LOGGER.error("Unexpected situation in processShortPickYes. state:{} active_count:{} first_active_wi:{}",
					state, afterActiveCount, firstWiInList, new RuntimeException());
				*/

			}
			// Related to DEV-1234 silliness. This showActivePicks() is not needed if we actually shorted ahead. It causes superfluous drawing instructions.
			// But it is needed if not shorting ahead. Then we need to show the rest of the active jobs as there is no other state transition and redraw. 
			// Cannot add an else without doing other kludgy checks above. Don't. If you embark on this, but the LOGGER.error() back and then fix short aheads properly.
			showActivePicks();
		} else {
			doNextItemWithPossibleFlashingThread(housekeeping);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Process the Yes after a short. Very complicated for simultaneous picks
	 */
	protected void processShortPickYes(final WorkInstruction inWi, int inPicked) {
		List<WorkInstruction> wiList = new ArrayList<>();
		wiList.add(inWi);
		processShortPickYes(wiList, inPicked);
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
				if (wi.getSubstitution() != null && mShortPickQty != 0) {
					processNormalPick(wi, mShortPickQty);
				} else {
					processShortPickYes(wi, mShortPickQty);
				}
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
			} else {
				//The getActivePickWiList() list may be modified during processShortPickYes(). That's why we are creating a copy of it here. to iterate through.
				List<WorkInstruction> activeInstructions = new ArrayList<>(getActivePickWiList());
				processShortPickYes(activeInstructions, 0);
			}
		} else {
			// Just return to showing the active picks.
			setState(CheStateEnum.SCAN_SOMETHING);
		}
	}

	// --------------------------------------------------------------------------
	/**  DEV-1347
	 *   Remove the containers known to have been stolen, then go to summary state so all work is recomputed without those
	 *   OR if the mStolenCntrs has value WI_COMPLETE_FAIL, then don't remove any orders, but still signal for the recompute, etc.
	 */
	private void adjustForStolenCntrs() {
		if (mStolenCntrs.size() == 0) {
			LOGGER.error("adjustForStolenCntrs() not called as expected");
		}
		// adjust all. We will be entering SETUP_SUMMARY now, so all will be recomputed

		//   Java 8 this approach is cool!   mPositionToContainerMap.entrySet().removeIf(e-> <boolean expression> );
		if (mStolenCntrs.size() == 1 && mStolenCntrs.get(0).equals(CsDeviceManager.WI_COMPLETE_FAIL)) {
			LOGGER.warn("Signaling the need to recompute because a work instruction did not complete at the server.");
		} else {
			LOGGER.warn("removing containers stolen by other cart(s):{}. Will then need to recompute.", mStolenCntrs);
			for (Iterator<Map.Entry<String, String>> it = mPositionToContainerMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, String> entry = it.next();
				if (mStolenCntrs.contains(entry.getValue())) {
					it.remove();
				}
			}
		}

		// pretty kludgy flag. Test START from SETUP_SUMMARY it needs to ask all from the server again. Necessary to get our feedback right.
		mContainerToWorkInstructionCountMap = null;

		// then clear the stolen containers
		mStolenCntrs.clear();
	}

	// --------------------------------------------------------------------------
	/**  doNextPick has a major side effect of setState(DO_PICK) if there is more work.
	 *   Then setState(DO_PICK) calls showActivePicks()
	 */
	private void doNextPick() {
		// DEV-1347 do not proceed is some other CHE has stolen a container
		if (mStolenCntrs.size() > 0) {
			setState(CheStateEnum.SETUP_SUMMARY);
			return;
		}

		// We might call doNextPick after a normal complete, or a short pick confirm.
		// We should not call it for any put wall cases; call doNextWallPut instead
		mShortPickWi = null;
		mShortPickQty = 0;
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
				int uncompletedInstructionsOnOtherPathsSum = getCountJobsOnOtherPaths();
				processPickComplete(uncompletedInstructionsOnOtherPathsSum > 0);
			}
		}
	}

	/**
	 * Is this useful to linescan?  If not, move as private function to SetupOrdersDeviceLogic
	 */
	private void processPickComplete(boolean isWorkOnOtherPaths) {
		// There are no more WIs, so the pick is complete.
		String otherInformation = "";
		if (isWorkOnOtherPaths)
			otherInformation = "Some of these orders need picks from other paths"; //  should be localized
		notifyCheWorkerVerb("PATH COMPLETE", otherInformation);

		// Clear the existing LEDs.
		ledControllerClearLeds(); // this checks getLastLedControllerGuid(), and bails if null.

		//Clear PosConControllers
		forceClearOtherPosConControllersForThisCheDevice();

		// CD_0041 is there a need for this?
		ledControllerShowLeds(getGuid());

		setState(CheStateEnum.SETUP_SUMMARY);
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
				//result of getOneActiveWorkInstruction() is guaranteed by selectNextActivePicks() not to be null
				WorkInstruction nextWi = getOneActiveWorkInstruction();
				String posconStream = nextWi.getPosConCmdStream();
				if (posconStream != null && posconStream.equals(mIgnoreExistingHoldersOnThesePoscons)) {
					//CHE arrived here from scanning YES on the POSCON_BUSY screen.
					//Continue without checking if needed Poscons are in use by another CHE
					mIgnoreExistingHoldersOnThesePoscons = null;
					setState(CheStateEnum.DO_PUT);
				} else {
					//Check if the required Poscons are in use by another CHE. If yes, go to POSCON_BUSY screen
					String posconHolders = mDeviceManager.getPosconHolders(getGuid(), posconStream);
					if (posconHolders == null) {
						setState(CheStateEnum.DO_PUT); // This will cause showActivePicks();
					} else {
						mBusyPosconHolders = posconHolders;
						setState(CheStateEnum.PUT_WALL_POSCON_BUSY);
					}
				}
			} else {
				// no side effects needed? processPickComplete() is the corollary
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
		boolean doMultipleWiPicks = mDeviceManager.getPickMultValue(getCheLightingEnum()); // DEV-451

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
			if (state != CheStateEnum.GET_PUT_INSTRUCTION && state != CheStateEnum.DO_PUT
					&& state != CheStateEnum.SHORT_PUT_CONFIRM && state != CheStateEnum.COMPUTE_WORK)
				if (getPosconIndexOfWi(wi) == 0) {
					LOGGER.error("{} not in container map. State is {}. In selectNextActivePicks()", wi.getContainerId(), state);
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
		return result;
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
		inWi.setCompleteState(mUserId, 0);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
	}

	private void doShortsAheads(final WorkInstruction inChangedWi) {
		doShortsAndSubstitutionsAheads(inChangedWi, null);
	}

	/**
	 * wiAlreadyDone() returns true for short or complete or substitute. If so, should not consider this for substitute ahead or short ahead
	 */
	private boolean wiAlreadyDone(WorkInstruction inWi) {
		if (inWi == null) {
			LOGGER.error("null value in wiAlreadyDone");
			return true; // The safe return value
		}
		WorkInstructionStatusEnum status = inWi.getStatus();
		if (status.equals(WorkInstructionStatusEnum.COMPLETE) || status.equals(WorkInstructionStatusEnum.SHORT)
				| status.equals(WorkInstructionStatusEnum.SUBSTITUTION))
			return true;
		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * This function is called in 2 situations:
	 * 
	 * 1)The inChangedWi was just shorted. Leave "substitution" param null.
	 *   Compare later wis to that. If the same product, short those also, removing unnecessary housekeeping work instructions
	 * 
	 * 2)The inChangedWi is being substituted. Provide "substitution" param.
	 *   For it and all upcoming wis with matching itemId:
	 *     *If the wi has substitutionAllowed, set the "substitution" value
	 *     *Otherwise, short the wi
	 */
	private void doShortsAndSubstitutionsAheads(final WorkInstruction inChangedWi, String substitution) {
		// Look for more wis in the CHE's job list that must short also if this one did.
		// Short and notify them, and remove so the worker will not encounter them.
		// Also remove unnecessary repeats and bay changes. The algorithm for housekeep removal is:
		// - There may be bay changes or repeat containers. And only one housekeep between at a time (for now--this may break)
		// - So track the previous work instruction as we iterate. If there is a housekeep right before a short ahead, we assume it is not needed.
		// - Repeat: definitely not needed. Baychange: maybe not needed. Better to over-remove than leave a final bay change at the end of the list.

		// Warning: for simultaneous work instructions, "short ahead" might actually be behind in the sequence. So we need to iterate the activePickList always.

		Integer toShortCount = 0;
		Integer substitutedCount = 0;
		Integer removeHousekeepCount = 0;

		// Algorithm. Assemble what we want to short
		List<WorkInstruction> toShortList = new ArrayList<WorkInstruction>();

		// Find short aheads from the active picks first, which might have lower sort values.
		// If we are shorting the inShortWi, there should be no housekeeps in the active pick list.
		for (WorkInstruction wi : getActivePickWiList()) {
			if (wiAlreadyDone(wi)) {
				continue;
			}
			if (wi.isHousekeeping()) {
				LOGGER.error("unanticipated housekeeping WI in mActivePickWiList in doShortAheads");
			}
			boolean substituteThisWi = substitution != null && wi.getSubstituteAllowed();
			//if (!wi.equals(inChangedWi) && sameProductLotEtc(wi, inChangedWi) && !substituteThisWi){
			if (sameProductLotEtc(wi, inChangedWi) && !substituteThisWi) {
				//Short the WI
				toShortCount++;
				toShortList.add(wi);
			}
		}

		// Now look for later work instructions to short, and remove housekeeps as necessary.
		WorkInstruction prevWi = null;
		for (WorkInstruction wi : mAllPicksWiList) {
			if (wiAlreadyDone(wi)) {
				prevWi = wi; // must do this which is normally done at the end of the loop
				continue;
			}
			if (sameProductLotEtc(wi, inChangedWi)) {
				if (substitution != null && wi.getSubstituteAllowed()) {
					substitutedCount++;
					wi.setSubstitution(substitution);
				} else if (laterWi(wi, inChangedWi)) {
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
			notifyWiVerb(wi, WorkerEvent.EventType.SHORT_AHEAD, kLogAsWarn);
			//Clear the substitution field, if any for WIs with 0 items picked
			wi.setSubstitution(null);
			doShortTransaction(wi, 0);
		}

		// Let's only report all this if there is a short ahead. We do not need to see in the log that we considered after every short.
		if (toShortCount > 0) {
			String reportShortAheadDetails = "";
			if (toShortCount > 0)
				reportShortAheadDetails += " Shorted " + toShortCount + " more.";
			if (removeHousekeepCount > 0)
				reportShortAheadDetails += " Also removed " + removeHousekeepCount + " housekeeping instructions.";
			LOGGER.info(reportShortAheadDetails);
		}
		if (substitutedCount > 0) {
			LOGGER.info("Set substitution field to " + substitution + " in " + substitutedCount + " upcoming jobs");
		}

	}

	/*
	 * For setup mix, see notes about DEV-836 in processContainerSelectScan
	*/
	private boolean recordSetupMix(boolean possiblePutWallScan) {
		// This is inelegant. We record in our internals if we are returning false even though we will not actually record the container/wall.
		// Just to make isSetupMixOk() work, which signals the appropriate user message.  Barely ok because no other side effects are tied to this.
		// Hopefully the feedback will train the user to not do this too often.
		if (possiblePutWallScan)
			mSetupMixHasPutwall = true;
		else
			mSetupMixHasCntrOrder = true;
		return isSetupMixOk();
	}

	private boolean isSetupMixOk() {
		// ok as long as not both
		return (!(mSetupMixHasPutwall && mSetupMixHasCntrOrder));
	}

	private void clearSetupMix() {
		mSetupMixHasPutwall = false;
		mSetupMixHasCntrOrder = false;

	}

	// --------------------------------------------------------------------------
	/**
	 * During Setup_Orders process, the user has scanned the thing to be set up.  That is:
	 * - C% container ID, as for Good Eggs cross batch order setup.
	 * - a plain order ID. Almost any pick operation. (as of v16, still requires the order has preassignedContainerID matching the order.)
	 * - from v16 L% put wall name. The plain put wall name also works, but only because here we think it is an order ID. The
	 *   back end does a search to determine what it is.
	 */
	private void processContainerSelectScan(final String inScanPrefixStr, String inScanStr) {
		processContainerSelectScan(inScanPrefixStr, inScanStr, false);
	}

	private void processContainerSelectScan(final String inScanPrefixStr, String inScanStr, boolean sendPosConBroadcast) {
		boolean possiblePutWallScan = LOCATION_PREFIX.equals(inScanPrefixStr);
		// Since we enforce "all orders/containers" or "all putwall" per cart setup, we can track that here.
		boolean setupMixOk = recordSetupMix(possiblePutWallScan);
		if (!setupMixOk) {
			setState(CheStateEnum.CONTAINER_SELECTION_INVALID);

		}

		else if (inScanPrefixStr.isEmpty() || CONTAINER_PREFIX.equals(inScanPrefixStr) || LOCATION_PREFIX.equals(inScanPrefixStr)) {

			mContainerInSetup = extractContainerIdFromScan(inScanStr);
			// Check to see if this container is already setup in a position.

			byte currentAssignment = getPosconIndexOfContainerId(mContainerInSetup);
			if (currentAssignment != 0) {
				// careful: 0 also equals PosControllerInstr.POSITION_ALL
				clearContainerAssignmentAtIndex(currentAssignment);
				this.clearOnePosconOnThisDevice(currentAssignment);
			}

			// trouble here. Cause of DEV-836 bug. If the scan was of a putwall name, the next correct state is container_position to give it a position on
			// cart. But if some generic position that the user wanted to start at, we want to either "error, please START", or go directly to processLocationScan
			// in order to compute the work. Site controller cannot know which case it is.
			setState(CheStateEnum.CONTAINER_POSITION);

			if (sendPosConBroadcast) {
				CommandControlPosconBroadcast broadcast = new CommandControlPosconBroadcast(NetEndpoint.PRIMARY_ENDPOINT, CommandControlPosconBroadcast.POSCON_DSP_ADDRESS);
				broadcast.setIncludeMapBytes(getUsedPositionsByteArray());
				sendRadioControllerCommand(broadcast, true);
			}

		} else {
			setState(CheStateEnum.CONTAINER_SELECTION_INVALID);
		}
	}

	/**
	 * Returns a portion of the Container scan is the ORDERSUB property is set
	 * Start and End indexies work the following way: "1-1" returns the first char in the scan. "2-4" returns 3 chars starting with the second, etc.
	 */
	private String extractContainerIdFromScan(String scan) {
		String orderSubProp = mDeviceManager.getOrdersubValue();
		if (orderSubProp == null || orderSubProp.isEmpty() || "Disabled".equalsIgnoreCase(orderSubProp)) {
			return scan;
		}
		if (scan.contains("%")) {
			return scan;
		}
		String parts[] = orderSubProp.split("-");
		if (parts.length != 2) {
			LOGGER.warn("Invalid ORDERSUB {}. Using full order scan.", orderSubProp);
			return scan;
		}
		int start = 0, end = 0;
		try {
			start = Integer.parseInt(parts[0].trim());
			end = Integer.parseInt(parts[1].trim());
		} catch (NumberFormatException e) {
			LOGGER.warn("Invalid ORDERSUB {}. Unable to parse start and end. Using full order scan.", orderSubProp);
			return scan;
		}
		if (start > end) {
			LOGGER.warn("Invalid ORDERSUB {}. Start is larger than end. Using full order scan.", orderSubProp);
			return scan;
		}
		if (start < 1) {
			LOGGER.warn("Invalid ORDERSUB {}. Start is less than 1. Using full order scan.", orderSubProp);
			return scan;
		}
		//Adjust for 1-indexed properties
		start--;
		int scanLen = scan.length();
		if (start > scanLen) {
			start = scanLen;
		}
		if (end > scanLen) {
			end = scanLen;
		}
		return scan.substring(start, end);
	}

	/**
	 * This de-clones lots of code. Assumes it is called from mPositionToContainerMap. This will log the error if there is bad data in the system so caller does not need to.
	 * We think that is prevented now. Part of DEV-1222
	 * A return code of 0 is basically an error. Calling code should silently not process this case and move on.
	 */
	private byte getPositionValue(Entry<String, String> entry) {
		return getPositionValue(entry.getKey());
	}

	private byte getPositionValue(String value) {
		byte position = 0;
		try {
			position = Byte.valueOf(value);
		} catch (NumberFormatException e) {
			LOGGER.info("getPositionValue found bad value in position map", e);
			// DEV-1222 catch and move on. Better than skipping feedback on all poscons just because one position got bad data.
		}
		return position;
	}

	private byte[] getUsedPositionsByteArray() {
		BitSet usedPositions = new BitSet();
		for (Entry<String, String> entry : mPositionToContainerMap.entrySet()) {
			byte position = getPositionValue(entry);
			if (position != 0)
				usedPositions.set(position);
		}
		byte[] data = usedPositions.toByteArray();
		return data;
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
				//Do nothing. Only a "Cancel Error" will get you out
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
					if (wi.isHousekeeping()) {
						LOGGER.warn("Probable test error. Don't short a housekeeping. User error if happening in production");
						invalidScanMsg(mCheStateEnum); // Invalid to short a housekeep
					} else {
						if (getCheLightingEnum() == CheLightingEnum.LABEL_V1) {
							setState(CheStateEnum.SCAN_SOMETHING_SHORT);//With Label-lighting CHEs, auto-short by 0
						} else {
							setState(CheStateEnum.SHORT_PICK); // flashes all poscons with active jobs
						}
					}
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
			case SETUP_SUMMARY:
				//Setup the CHE
				setupChe();
				break;

			//In the error states we must go to CLEAR_ERROR_SCAN_INVALID
			case CONTAINER_POSITION_IN_USE:
			case CONTAINER_POSITION_INVALID:
			case CONTAINER_SELECTION_INVALID:
			case NO_CONTAINERS_SETUP:
				//Do nothing. Only a "Cancel Error" will get you out
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
			clearAllPosconsOnThisDevice();
			this.setUserId(inScanStr);
			setState(CheStateEnum.VERIFYING_BADGE);
			mDeviceManager.verifyBadge(getGuid().getHexStringNoPrefix(), getPersistentId(), inScanStr);
		} else {
			LOGGER.info("Not a user ID: " + inScanStr);
			invalidScanMsg(CheStateEnum.IDLE);
		}
	}

	/**
	* This is the essence of the log in result after verify badge. Factored out so that remote linked CHE may get this CHE to the right state.
	*/
	@Override
	public void finishLogin() {
		clearAllPosconsOnThisDevice();
		setState(CheStateEnum.SETUP_SUMMARY);
	}

	@Override
	public void processResultOfVerifyBadge(Boolean verified, String workerId) {
		if (mCheStateEnum.equals(CheStateEnum.VERIFYING_BADGE) || mCheStateEnum.equals(CheStateEnum.IDLE)) {
			if (verified) {
				// finishLogin();
				clearAllPosconsOnThisDevice();
				setUserId(workerId);
				notifyCheWorkerVerb("LOG IN", "");

				// If I am linked, and I just logged in, let's go to the REMOTE screen to show the worker what she is linked to.
				// Better than going directly to REMOTE_LINKED state.
				String cheName = getLinkedToCheName();
				if (cheName != null) {
					setState(CheStateEnum.REMOTE);
				} else {
					setState(CheStateEnum.SETUP_SUMMARY); // the normal case
				}
			} else {
				setState(CheStateEnum.IDLE);
				invalidScanMsg(UNKNOWN_BADGE_MSG, EMPTY_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
				notifyCheWorkerVerb("LOG IN", "Credential Denied");
			}
		} else {
			LOGGER.error("unexpected verifyBadge response in state {}", mCheStateEnum);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Factor this out as it is called from two places. The normal processLocationScan, and skipping the location scan if just going by sequence.
	 * WARNING: The parameter is the scanned location, or "START", or "REVERSE"
	 * @param inLocationStr
	 */
	private void requestWorkAndSetGetWorkState(final String inLocationStr, final Boolean reverseOrderFromLastTime) {
		// by protocol, inLocationStr may be null for START or Reverse. Do not overwrite mLocationId which is perfectly good.
		clearAllPosconsOnThisDevice();

		rememberCompletesAndShorts();

		Map<String, String> positionToContainerMapCopy = new HashMap<String, String>(mPositionToContainerMap);
		LOGGER.info("Sending {} positions to server in getCheWork", positionToContainerMapCopy.size());
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
		if (LOCATION_PREFIX.equals(inScanPrefixStr) || TAPE_PREFIX.equals(inScanPrefixStr)) {
			if (TAPE_PREFIX.equals(inScanPrefixStr)) {
				inScanStr = inScanPrefixStr + inScanStr;
				CodeshelfTape tape = CodeshelfTape.scan(inScanStr);
				if (tape == null) {
					LOGGER.info("Tried to parse " + inScanStr + " as a Codeshelf Tape scan, but failed. Ignoring it.");
					return;
				}
			}

			// DEV-836, 837. From what states shall we allow this?
			ledControllerClearLeds();
			mLocationId = inScanStr; // let's remember where user scanned.

			// Careful. Later, codeshelf tape scan. Need to get the interpretted position back from server.
			LOGGER.info("processLocationScan {}. About to call requestWorkAndSetGetWorkState", inScanStr);
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

			case CONTAINER_SELECT:
				processContainerSelectScan(inScanPrefixStr, inContent, true);
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
				//Do Nothing if you are in an error state and you scan something that's not "Cancel Error"
				break;

			case SETUP_SUMMARY:
				// At summary, we can change location/path
				if (inScanPrefixStr.equals(LOCATION_PREFIX) || inScanPrefixStr.equals(TAPE_PREFIX)) {
					processLocationScan(inScanPrefixStr, inContent);
				}
				break;

			case DO_PICK:
				// At any time during the pick we can change locations.
				if (inScanPrefixStr.equals(LOCATION_PREFIX) || inScanPrefixStr.equals(TAPE_PREFIX)) {
					processLocationScan(inScanPrefixStr, inContent);
				} else if (inScanPrefixStr.equals(POSITION_PREFIX) && getCheLightingEnum() == CheLightingEnum.LABEL_V1) {
					processPosconScanToPick(inScanPrefixStr, inContent);
				} else {
					// DEV-1295.  If the user scanned something, at minimum redraw stuff.
					// But more importantly, what this is multi-pick not needing another scan, but worker scans a different item?
					processPickVerifyScan(inScanPrefixStr, inContent);
				}
				break;

			case SCAN_SOMETHING:
				// At any time during the pick we can change locations.
				if (inScanPrefixStr.equals(LOCATION_PREFIX) || inScanPrefixStr.equals(TAPE_PREFIX)) {
					processLocationScan(inScanPrefixStr, inContent);
				}
				// If SCANPICK parameter is set, then the scan is SKU or UPC or LPN or .... Process it.
				processVerifyScan(inScanPrefixStr, inContent);
				break;

			case SCAN_GTIN:
				processGtinStateScan(inScanPrefixStr, inContent);
				break;

			case PUT_WALL_SCAN_LOCATION:
				processPutWallLocationScan(inScanPrefixStr, inContent);
				break;

			case PUT_WALL_SCAN_ORDER:
				processPutWallOrderScan(inScanPrefixStr, inContent);
				break;

			case NO_PUT_WORK:
				// If one item scan did not work, let user scan another directly without first having to CANCEL.
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

			case SKU_WALL_SCAN_GTIN_LOCATION:
				processSkuWallNewLocationScan(inScanPrefixStr, inContent);
				break;

			case SKU_WALL_ALTERNATE_WALL_AVAILABLE:
				processSkuWallLocationDisambiguation(inScanPrefixStr, inContent);
				break;

			case INFO_PROMPT:
			case INFO_DISPLAY:
				processInfoLocationScan(inScanPrefixStr, inContent);
				break;

			case REMOVE_CHE_CONTAINER:
				if ("P%".equalsIgnoreCase(inScanPrefixStr)) {
					byte position = Byte.parseByte(inContent);
					clearContainerAssignmentAtIndex(position);
					clearOnePosconOnThisDevice(position);
					setState(CheStateEnum.CONTAINER_SELECT);
				}
				break;

			case REMOTE:
				processCheLinkScan(inScanPrefixStr, inContent);
				break;

			case REMOTE_LINKED:
				LOGGER.error("Should not have gotten here");
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
	private void showContainerAssignments() {
		if (mPositionToContainerMap.isEmpty()) {
			LOGGER.debug("No Container Assaigments to send");
			return;
		}
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		for (Entry<String, String> entry : mPositionToContainerMap.entrySet()) {

			byte position = getPositionValue(entry);
			if (position == 0) {
				continue;
			}
			String containerId = entry.getValue();
			PosControllerInstr instruction = generatePosconInstruction(position, containerId);
			instructions.add(instruction);
		}
		LOGGER.debug("Sending Container Assaignments {}", instructions);

		//Adding the "clear" code below to clear PosCons from the new button-placement mode.
		CommandControlPosconBroadcast broadcast = new CommandControlPosconBroadcast(NetEndpoint.PRIMARY_ENDPOINT, CommandControlPosconBroadcast.CLEAR_DISPLAY);
		broadcast.setIncludeMapBytes(getUsedPositionsByteArray());
		sendRadioControllerCommand(broadcast, true);

		sendPositionControllerInstructions(instructions);
	}

	private void showLatestContainerAssignment() {
		if (mLastAssignedPoscon == null) {
			return;
		}
		String containerId = mPositionToContainerMap.get(mLastAssignedPoscon);
		byte position = getPositionValue(mLastAssignedPoscon);
		mLastAssignedPoscon = null;
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		PosControllerInstr instruction = generatePosconInstruction(position, containerId);
		instructions.add(instruction);
		LOGGER.debug("Sending Latest Container Assaignment {}", instructions);
		sendPositionControllerInstructions(instructions);
	}

	private PosControllerInstr generatePosconInstruction(byte position, String containerId) {
		Byte value = 0;
		boolean needBitEncodedA = false;
		//Use the last 1-2 characters of the containerId if the container is numeric.
		//Otherwise stick to the default character "a"

		if (!StringUtils.isEmpty(containerId) && StringUtils.isNumeric(containerId)) {
			if (containerId.length() == 1) {
				value = Byte.valueOf(containerId);
			} else {
				value = Byte.valueOf(containerId.substring(containerId.length() - 2));
			}
		} else {
			needBitEncodedA = true; // "a"
		}

		if (needBitEncodedA) {
			return new PosControllerInstr(position,
				PosControllerInstr.BITENCODED_SEGMENTS_CODE,
				PosControllerInstr.BITENCODED_LED_A,
				PosControllerInstr.BITENCODED_LED_BLANK,
				PosControllerInstr.SOLID_FREQ,
				PosControllerInstr.MED_DUTYCYCLE);
		} else if (value >= 0 && value < 10) {
			//If we are going to pass a single 0 <= digit < 10 like 9, then we must show "09" instead of just 9.
			return new PosControllerInstr(position,
				PosControllerInstr.BITENCODED_SEGMENTS_CODE,
				PosControllerInstr.BITENCODED_DIGITS[value],
				PosControllerInstr.BITENCODED_DIGITS[0],
				PosControllerInstr.SOLID_FREQ,
				PosControllerInstr.MED_DUTYCYCLE);
		} else {
			return new PosControllerInstr(position,
				value,
				value,
				value,
				PosControllerInstr.SOLID_FREQ,
				PosControllerInstr.MED_DUTYCYCLE);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param totalWorkInstructionCount
	 * @param containerToWorkInstructionCountMap - Map containerIds to WorkInstructionCount objects
	 * Not final only because we let CsDeviceManager call this generically.
	 */
	public void processWorkInstructionCounts(final Integer totalWorkInstructionCount,
		final Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {
		// DEV-1257 do not accept if in wrong state. Should only only accept from one state?
		CheStateEnum currentState = this.getCheStateEnum();
		if (currentState == CheStateEnum.IDLE || currentState == CheStateEnum.VERIFYING_BADGE) {
			LOGGER.error("Late WorkInstructionCounts response from server. Current state is {}. Doing nothing.", currentState);
			// We certainly do not want to transition to SETUP_SUMMARY state after clearing our badge ID. Should we redo the container map anyway? Not sure.
			return;
		}
		// DEV-1331 part 3 If we are already picking, and we get this, we are in grave danger. Server has changed out its work instructions.
		// We have to go back to a state where the user must scan start again.
		if (currentState == CheStateEnum.DO_PICK) {
			LOGGER.error("Late WorkInstructionCounts response from server. Current state is {}. Transition back to summary.",
				currentState);
			setState(CheStateEnum.SETUP_SUMMARY);
			return;
		}

		//Store counts
		this.mContainerToWorkInstructionCountMap = containerToWorkInstructionCountMap;

		// The back-end returned the work instruction count.
		if (totalWorkInstructionCount > 0 && mContainerToWorkInstructionCountMap != null
				&& !mContainerToWorkInstructionCountMap.isEmpty()) {

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
			// It is not so clear, should server give us completed work instructions? Should we clear those out?
		}
		setState(CheStateEnum.SETUP_SUMMARY);
	}

	private void processPosconScanToPick(String inScanPrefixStr, String inContent) {
		try {
			if (inContent.equals(POSITION_AUTO)){
				inContent = getActivePoscon();
			}
			byte position = Byte.parseByte(inContent);
			//Try to retrieve the displayed value to allow for shorts
			Integer value = null;
			//If the CHE is in LABEL light mode, Poscons will be blank, so read planed quantity from the WI
			String containerId = mPositionToContainerMap.get(inContent);
			WorkInstruction wi = getWorkInstructionForContainerId(containerId);
			if (wi == null) {
				LOGGER.warn("Ignoring {}{} scan, as could not retrieve the work instruction at that position",
					inScanPrefixStr,
					inContent);
				return;
			}
			value = wi.getPlanQuantity();
			//Simulate button press
			processButtonPress((int) position, value);
		} catch (NumberFormatException e) {
			LOGGER.error("Ignoring {}{} scan as could not parse it as byte", inScanPrefixStr, inContent);
		}
	}
	
	private String getActivePoscon(){
		WorkInstruction activeWi = getOneActiveWorkInstruction();
		if (activeWi == null) {
			LOGGER.warn("Could not find active WI");
			return null;
		}
		byte positionId = getPosconIndexOfContainerId(activeWi.getContainerId());
		return positionId + "";
	}

	/**
	 * A series of private functions giving the overall state of the setup
	 * How many jobs on not being done for this setup? Includes uncompleted wi other paths, and details with no wi made
	 */
	private int getCountJobsOnOtherPaths() {
		if (mContainerToWorkInstructionCountMap == null)
			return 0;
		else {
			int uncompletedInstructionsOnOtherPathsCounter = 0;
			for (WorkInstructionCount count : mContainerToWorkInstructionCountMap.values()) {
				uncompletedInstructionsOnOtherPathsCounter += count.getOtherWorkTotal();
			}
			return uncompletedInstructionsOnOtherPathsCounter;
		}
	}

	/**
	 * How many container/orderId are setup?
	 */
	private int getCountOfSetupOrderContainers() {
		if (mContainerToWorkInstructionCountMap == null) {
			// special case for initialization of persisted cart setup
			return this.mPositionToContainerMap.size();
		} else
			return mContainerToWorkInstructionCountMap.size();
		// huge assumption that all WorkInstructionCounts in the map are valid. See ComputeWorkCommand.computeContainerWorkInstructionCounts
		// which filtered out some "None" counts. Not sure if that is correct or not.
	}

	/**
	 * How many jobs for the mLocationId path?
	 */
	private int getCountOfGoodJobsOnSetupPath() {
		if (mContainerToWorkInstructionCountMap == null) {
			// special case for initialization of persisted cart setup
			if (this.mPositionToContainerMap.size() > 0)
				return -1; // This is a goofy flag that will display as ?
			else
				return 0;
		} else {
			int goodJobsCounter = 0;
			for (WorkInstructionCount count : mContainerToWorkInstructionCountMap.values()) {
				goodJobsCounter += count.getGoodCount();
			}
			return goodJobsCounter;
		}
	}

	/**
	 * How many completed jobs are currently in the map.
	 */
	private void removeCompletesAndShortsInCountMap() {
		if (mContainerToWorkInstructionCountMap == null)
			return;
		else {
			for (WorkInstructionCount count : mContainerToWorkInstructionCountMap.values()) {
				count.setCompleteCount(0);
				count.setShortCount(0);
			}
		}
	}

	/**
	 * How many completed jobs are currently in the map.
	 */
	private int getCountOfCompletedJobsInCountMap() {
		if (mContainerToWorkInstructionCountMap == null)
			return 0;
		else {
			int completeJobsCounter = 0;
			for (WorkInstructionCount count : mContainerToWorkInstructionCountMap.values()) {
				completeJobsCounter += count.getCompleteCount();
			}
			return completeJobsCounter;
		}
	}

	/**
	 * How many completed jobs are currently in the map.
	 */
	private int getCountOfShortedJobsInCountMap() {
		if (mContainerToWorkInstructionCountMap == null)
			return 0;
		else {
			int shortedJobsCounter = 0;
			for (WorkInstructionCount count : mContainerToWorkInstructionCountMap.values()) {
				shortedJobsCounter += count.getShortCount();
			}
			return shortedJobsCounter;
		}
	}

	/**
	 * How many jobs were completed? Give the value of this feedback cycle, plus whatever we accumulated before.
	 */
	private int getCountOfCompletedJobsThisSetup() {
		return getCountOfCompletedJobsInCountMap() + getRememberPriorCompletes();
	}

	/**
	 * How many shorts for the mLocationId path? Give the value of this feedback cycle, plus whatever we accumulated before.
	 */
	private int getCountOfShortsThisSetup() {
		return getCountOfShortedJobsInCountMap() + getRememberPriorShorts();
	}

	/**
	 * Server will come back with new counts for a new START or new path, so we remember completes and shorts for this cart setup.
	 * When we dump the cart (SETUP), we set these back to zero.
	 * Since we are remembering a summary value, we clear the completes and shorts in the counts map, while leaving the container wi map alone.
	 */
	private void rememberCompletesAndShorts() {
		/*
		int priorCompleteValue = getRememberPriorCompletes();
		int completesInCountMap = getCountOfCompletedJobsInCountMap();
		LOGGER.debug("Entering rememberCompletesAndShorts, prior completes={}. in map = {}", priorCompleteValue, completesInCountMap);
		*/

		setRememberPriorShorts(getCountOfShortsThisSetup());
		setRememberPriorCompletes(getCountOfCompletedJobsThisSetup());

		// Remove the completes and shorts in the map since we are now remembering the summary count elsewhere. DEV-812 bug if not.
		removeCompletesAndShortsInCountMap();
	}

	/**
	 * When we are changing path or starting again, and get feedback from server, we need to remember how many shorts and completes we have done.
	 */
	private void resetRememberCompletesAndShorts() {
		setRememberPriorShorts(0);
		setRememberPriorCompletes(0);
	}

	/**
	 * Help the user know what scans will do
	 */
	private void sendGtinScreen(String inLastScannedGTIN) {
		String scanType = getScanVerificationTypeUI();
		String line1 = cheLine(String.format(SCAN_GTIN_OR_LOCATION, scanType));
		String line2 = "";
		String line3 = "";
		String line4 = "CANCEL to exit";

		if (inLastScannedGTIN == null) {
			line2 = scanType + " to move inventory";
			line3 = "Location to test light";

		} else {
			// A GTIN was scanned. It will move on a location scan
			line2 = String.format("%s will move if", inLastScannedGTIN);
			line3 = "you scan a location.";
			line4 = "Or other " + scanType + " or CANCEL";

		}
		sendDisplayCommand(line1, line2, line3, line4);
	}

	/**
	 * Show status for this setup in our restrictive 4 x 20 manner.
	 * Trying to align the counts, allow for 3 digit counts.
	 */
	private void sendSummaryScreen() {
		int orderCount = getCountOfSetupOrderContainers();
		String orderCountStr = Integer.toString(orderCount);
		orderCountStr = StringUtils.leftPad(orderCountStr, 3);
		String locStr = getLocationId(); // this might be null the very first time.
		String line1;
		String ordersOrReplen = getReplenishRun() ? "replen" : (orderCount == 1) ? "order " : "orders";
		if (locStr == null) {
			line1 = String.format("%s %s ", orderCountStr, ordersOrReplen);
		} else {
			if (locStr.startsWith(TAPE_PREFIX)) {
				mDeviceManager.requestTapeDecoding(getGuid().getHexStringNoPrefix(), getPersistentId(), locStr);
			}
			locStr = StringUtils.leftPad(locStr, 9); // Always right justifying the location
			line1 = String.format("%s %s %s", orderCountStr, ordersOrReplen, locStr);
		}

		int pickCount = getCountOfGoodJobsOnSetupPath();
		// Goofy flag for the persisted initialization case. If -1, display as ?
		String pickCountStr;
		if (pickCount >= 0)
			pickCountStr = Integer.toString(pickCount);
		else
			pickCountStr = "?";
		pickCountStr = StringUtils.leftPad(pickCountStr, 3);
		// Too clever?  only show other path counts if there are any
		String line2;
		int otherCount = getCountJobsOnOtherPaths();
		if (otherCount > 0) {
			String otherCountStr = Integer.toString(otherCount);
			otherCountStr = StringUtils.leftPad(otherCountStr, 3);
			if (pickCount == 1)
				line2 = String.format("%s job    %s other", pickCountStr, otherCountStr);
			else
				line2 = String.format("%s jobs   %s other", pickCountStr, otherCountStr);
		} else {
			if (pickCount == 1)
				line2 = String.format("%s job  ", pickCountStr);
			else
				line2 = String.format("%s jobs ", pickCountStr);
		}

		int doneCount = getCountOfCompletedJobsThisSetup();
		String doneCountStr = Integer.toString(doneCount);
		doneCountStr = StringUtils.leftPad(doneCountStr, 3);
		int shortCount = getCountOfShortsThisSetup();
		// We want to show completed jobs and shorts upon completion only. The reason is the server is not
		// handing these to us usefully in the computeWorkInstructions process. If we showed 0 done as the user scans onto a new location or reverses
		// part way through, users would complain about us "losing" their completed work.
		String line3 = "";
		if (doneCount > 0 || shortCount > 0) {
			if (shortCount > 0) {
				String shortCountStr = Integer.toString(shortCount);
				shortCountStr = StringUtils.leftPad(shortCountStr, 3);
				line3 = String.format("%s done   %s short", doneCountStr, shortCountStr);
			} else {
				line3 = String.format("%s done", doneCountStr);
			}
		}

		// Try to be a little clever and context sensitive here
		String line4;
		if (pickCount == 0 && otherCount == 0)
			line4 = "SETUP"; // START provides no useful functionality
		else if (pickCount == 0 && otherCount > 0)
			line4 = "Scan Other Location"; // Or setup to nuke the cart. Not enough space
		else if (pickCount == 0)
			line4 = "SETUP (or START)"; // you might start again to redo any shorts
		else
			// pickcount > 0. Usually just want to start
			line4 = "START (or SETUP)"; // or other location

		// This screen needs monospace
		if (this.usesNewCheScreen())
			this.sendMonospaceDisplayScreen(line1, line2, line3, line4, true); // larger bottom line
		else
			this.sendDisplayCommand(line1, line2, line3, line4);
	}

	/** Shows the count feedback on the position controller
	 * This returns without error if the feedback counters are not valid.
	 * This routine has "grown" from v16. Used to not be called at the end of a run. Now is. So we need to show more state
	 * from what happened on the run. But only show that stuff if there is not a good WI count.
	 */
	protected void showCartSetupFeedback() {
		//make sure mContainerToWorkInstructionCountMap exists
		if (!feedbackCountersValid()) {
			return;
		}

		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();

		// v24 DEV-1261 lets log a single large line instead of multiple lines for many poscons. Hint: may want to summarize wiCount further in the future.
		String toLogStr = "";

		for (Entry<String, String> containerMapEntry : mPositionToContainerMap.entrySet()) {
			String containerId = containerMapEntry.getValue();
			byte position = getPositionValue(containerMapEntry);
			if (position == 0) {
				continue;
			}

			WorkInstructionCount wiCount = mContainerToWorkInstructionCountMap.get(containerId);

			//if wiCount is 0 then the server did have any WIs for the order.
			//this is an "unknown" order id
			if (wiCount == null) {
				//TODO send a special code for this?
				//Right now it matches "done for now" feedback
				if (mPostSetupSummary) {
					instructions.add(new PosControllerInstr(position,
						(byte) 0,
						(byte) 0,
						(byte) 0,
						PosControllerInstr.BLINK_FREQ.byteValue(),
						PosControllerInstr.BRIGHT_DUTYCYCLE.byteValue()));
				} else {
					instructions.add(new PosControllerInstr(position,
						(byte) 0,
						(byte) 0,
						(byte) 0,
						PosControllerInstr.SOLID_FREQ.byteValue(),
						PosControllerInstr.DIM_DUTYCYCLE.byteValue()));
				}
				LOGGER.info("Position {} got no WIs. Causes: no path defined, unknown container id, no inventory", position);
			} else {
				byte count = (byte) wiCount.getGoodCount();
				if (toLogStr.isEmpty())
					toLogStr = "Position values: ";
				toLogStr += String.format("%n%s at pos:%d -- %s", containerId, position, wiCount.toString());
				// LOGGER.info("{} at pos:{} -- {}",containerId, position, wiCount);
				if (count == 0) {
					//0 good WI's

					// Fairly significant change from v16 to let this routine do the odd stuff
					instructions.add(getCartRunFeedbackInstructionForCount(wiCount, position)); // this should do shorts, otherpath, orderComplete
					// used to have code here to do the dash and "oc".

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
		if (!toLogStr.isEmpty())
			LOGGER.info(toLogStr);

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
				byte position = getPositionValue(containerMapEntry);
				if (position == 0) {
					continue;
				}

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
			clearOnePosconOnThisDevice(inPosition);
		}
	}

	/**
	 * @return - Returns the PosControllerInstr for the position given the count if any is warranted. Null otherwise.
	 */
	public PosControllerInstr getCartRunFeedbackInstructionForCount(WorkInstructionCount wiCount, byte position) {
		//if wiCount is null then the server did have any WIs for the order.
		//this is an "unknown" order id
		if (wiCount == null) {
			//Unknown order id matches "done for now" - dim, solid, dashes
			return new PosControllerInstr(position,
				(byte) 0,
				(byte) 0,
				(byte) 0,
				PosControllerInstr.SOLID_FREQ.byteValue(),
				PosControllerInstr.DIM_DUTYCYCLE.byteValue());
		} else {
			byte count = (byte) wiCount.getGoodCount();
			// Caller (in SetupOrdersDeviceLogic) just logged, so we do not need to log again here. Caller also logged the pick count.
			if (count == 0) { // nothing else to do from this cart setup.
				// Indicate short this path, work other paths, or both.
				if (wiCount.hasShortsThisPath() && wiCount.hasWorkOtherPaths()) {
					//If there any bad counts then we are "done for now" - dim, solid, dashes
					return new PosControllerInstr(position,
						PosControllerInstr.BITENCODED_SEGMENTS_CODE,
						PosControllerInstr.BITENCODED_TRIPLE_DASH,
						PosControllerInstr.BITENCODED_TRIPLE_DASH,
						PosControllerInstr.SOLID_FREQ.byteValue(),
						PosControllerInstr.DIM_DUTYCYCLE.byteValue());
				} else if (wiCount.hasShortsThisPath()) {
					return new PosControllerInstr(position,
						PosControllerInstr.BITENCODED_SEGMENTS_CODE,
						PosControllerInstr.BITENCODED_TOP_BOTTOM,
						PosControllerInstr.BITENCODED_TOP_BOTTOM,
						PosControllerInstr.SOLID_FREQ.byteValue(),
						PosControllerInstr.DIM_DUTYCYCLE.byteValue());
				} else if (wiCount.hasWorkOtherPaths()) {
					return new PosControllerInstr(position,
						PosControllerInstr.BITENCODED_SEGMENTS_CODE,
						PosControllerInstr.BITENCODED_LED_DASH,
						PosControllerInstr.BITENCODED_LED_DASH,
						PosControllerInstr.SOLID_FREQ.byteValue(),
						PosControllerInstr.DIM_DUTYCYCLE.byteValue());
				} else {
					if (wiCount.getCompleteCount() == 0) {
						// This should not be possible (unless we only had a single HK WI, which would be a bug)
						// However, restart on a route after completing all work for an order comes back this way. Server could return the count
						// but does not. Treat it as order complete. The corresponding case  in setupOrdersDeviceLogic is demonstrated 
						// in cheProcessPutWall.orderWallRemoveOrder(); Don't know if any case hits this in CheDeviceLogic.
						LOGGER.debug("WorkInstructionCount has no counts {};", wiCount);
					}
					//Ready for packout - solid, dim, "oc"
					return new PosControllerInstr(position,
						PosControllerInstr.BITENCODED_SEGMENTS_CODE,
						PosControllerInstr.BITENCODED_LED_C,
						PosControllerInstr.BITENCODED_LED_O,
						PosControllerInstr.SOLID_FREQ.byteValue(),
						PosControllerInstr.DIM_DUTYCYCLE.byteValue());
				}
			} else {
				//No feedback is count > 0
				return null;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *
	 */
	private void processContainerPosition(final String inScanPrefixStr, String inScanStr) {
		if (POSITION_PREFIX.equals(inScanPrefixStr)) {
			//DEV-1222 make sure we have a good position before adding it to our map
			if (StringUtils.isEmpty(inScanStr) || !StringUtils.isNumeric(inScanStr)) {
				LOGGER.warn("Bad position scan: {}{}", inScanPrefixStr, inScanStr);
				setState(CheStateEnum.CONTAINER_POSITION_INVALID);
				return;
			}

			if (inScanStr.equalsIgnoreCase(POSITION_AUTO)) {
				inScanStr = getNextAvailablePosition();
			}
			
			if (mPositionToContainerMap.get(inScanStr) == null) {
				mPositionToContainerMap.put(inScanStr, mContainerInSetup);
				mLastAssignedPoscon = inScanStr;

				// This one is kind of funny. We go straight from scan badge to setup, or after setup command. We will choose as our event
				// the first order association to poscon.
				if (mPositionToContainerMap.values().size() == 1) {
					String otherInformation = String.format("First order/container: %s", mContainerInSetup);
					notifyCheWorkerVerb("SETUP STARTED", otherInformation);
				}
				mContainerInSetup = "";
				setState(CheStateEnum.CONTAINER_SELECT);
			} else {
				setState(CheStateEnum.CONTAINER_POSITION_IN_USE);
			}
		} else {
			setState(CheStateEnum.CONTAINER_POSITION_INVALID);
		}
	}
	
	private String getNextAvailablePosition(){
		int maxUsedPosition = 0, position;
		for (String positionStr : mPositionToContainerMap.keySet()) {
			try {
				position = Integer.parseInt(positionStr);
				if (position > maxUsedPosition) {
					maxUsedPosition = position;
				}
			} catch (NumberFormatException e) {
				LOGGER.warn("Unexpected error pasting position id {}", positionStr);
			}
		}
		return (maxUsedPosition + 1) + "";
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
				//Do nothing. Only a "Cancel Error" will get you out
				break;
			case IDLE:
			case VERIFYING_BADGE:
				//Do nothing while still verifying badge
				break;

			case CONTAINER_POSITION:
				processContainerPosition(COMMAND_PREFIX, inScanStr);
				break;

			case SETUP_SUMMARY:
				// Normally, start work here would hit the default case below, calling start work() which queries to server again
				// ultimately coming back to SETUP_SUMMARY state. However, if okToStartWithoutLocation, then start scan moves us forward
				boolean reverseOrderFromLastTime = getMReversePickOrder() != reverse;
				//Remember the selected pick direction
				setMReversePickOrder(reverse);

				// For DEV-784 Odd case. If we just initialized our setup state, then we may be in SETUP_SUMMARY state even though
				// We never did the work instruction count thing. We need to call startWork instead of requestWorkAndSetGetWorkState
				if (this.mContainerToWorkInstructionCountMap == null) {
					if (mPositionToContainerMap.values().size() > 0) {
						startWork(inScanStr);
					} else {
						LOGGER.error("Not sending request work because container map is empty"); // DEV-1370
						// new error in v25. Don't know how often we get here in production. Only one unit test in CheProcessPickExceptions hits this.
					}

				} else { // normal case
					requestWorkAndSetGetWorkState(null, reverseOrderFromLastTime);
				}
				break;

			case SCAN_GTIN:
				break;

			//Anywhere else we can start work if there's anything setup
			case CONTAINER_SELECT:
				mPostSetupSummary = true;
				//Continue on to default:
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

		clearAllPosconsOnThisDevice();
		mContainerInSetup = "";

		rememberCompletesAndShorts(); // is this right?

		// New late v24 DEV-1331 part 2
		if (mAllPicksWiList.size() > 0) {
			LOGGER.info("Clearing work instructions as asking server for update");
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
		}

		//Duplicate map to avoid later changes
		Map<String, String> positionToContainerMapCopy = new HashMap<String, String>(mPositionToContainerMap);
		LOGGER.info("Sending {} positions to server in computeCheWork", positionToContainerMapCopy.size());
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

		// DEV-1257 do not accept if in wrong state. Should only only accept from one state?
		CheStateEnum currentState = this.getCheStateEnum();
		if (currentState == CheStateEnum.IDLE || currentState == CheStateEnum.VERIFYING_BADGE) {
			LOGGER.error("Late assignWork response from server. Current state is {}. Doing nothing.", currentState);
			// We certainly do not want to transition to SETUP_SUMMARY state if we logged out and do not have a badge ID any more.
			return;
		}
		// DEV-1331 If we are already picking, and we get this, we are in grave danger. Server has changed out its work instructions
		// We have to go back to a state where the user must scan start again.
		if (currentState == CheStateEnum.DO_PICK) {
			LOGGER.error("Late assignWork response from server. Current state is {}. Transition back to summary.", currentState);
			// We certainly do not want to transition to SETUP_SUMMARY state if we logged out and do not have a badge ID any more.
			setState(CheStateEnum.SETUP_SUMMARY);
			return;
		}

		if (inWorkItemList == null || inWorkItemList.size() == 0) {
			setState(CheStateEnum.SETUP_SUMMARY);
		} else {
			WorkInstruction wi1 = inWorkItemList.get(0);
			String otherInformation = String.format("First pick at %s", wi1.getPickInstruction());
			for (WorkInstruction wi : inWorkItemList) {
				LOGGER.debug("WI: Loc: {}  SKU: {}  Instr: {}", wi.getLocationId(), wi.getItemId(), wi.getPickInstruction());
			}
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
			mAllPicksWiList.addAll(inWorkItemList);

			//  This is the best trigger for "start" on a path. But this also triggers for location change or reverse on a path.
			notifyCheWorkerVerb("PATH STARTED", otherInformation);

			doNextPick();
			// doNextPick will set the state.
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Almost the same as assignWork(), but some state transitions differ
	 */
	@Override
	public void assignWallPuts(final List<WorkInstruction> inWorkItemList, String wallType, String wallName) {
		notifyPutWallResponse(inWorkItemList, wallType);
		boolean noWork = inWorkItemList == null || inWorkItemList.size() == 0;
		if (noWork) {
			if (Location.PUTWALL_USAGE.equals(wallType)) {
				setState(CheStateEnum.NO_PUT_WORK);
			} else if (Location.SKUWALL_USAGE.equals(wallType)) {
				if (wallName == null) {
					setState(CheStateEnum.SKU_WALL_SCAN_GTIN_LOCATION);
				} else {
					mAlternatePutWall = wallName;
					setState(CheStateEnum.SKU_WALL_ALTERNATE_WALL_AVAILABLE);
				}
			}
		} else {
			mActivePickWiList.clear();
			mAllPicksWiList.clear();
			mAllPicksWiList.addAll(inWorkItemList);
			//When putting into SKU walls, workers can switch walls, if their items is in another wall.
			//Not applicable for PUT walls, so ignore this unless wallName is explicitly provided
			if (wallName != null) {
				mPutWallName = wallName;
			}
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
		inWi.setCompleteState(mUserId, inQuantity);

		EventType eventType = EventType.COMPLETE;
		if (getCheStateEnum() == CheStateEnum.DO_PUT) {
			if (inWi.getOrderDetail() == null) {
				eventType = EventType.COMPLETE;
			} else {
				eventType = EventType.COMPLETE;
			}
		}

		if (inWi.getSubstitution() != null) {
			inWi.setStatus(WorkInstructionStatusEnum.SUBSTITUTION);
			eventType = EventType.SUBSTITUTION;
		}

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
		notifyWiVerb(inWi, eventType, kLogAsInfo);

		mActivePickWiList.remove(inWi);

		if (feedbackCountersValid()) { // makes sure mContainerToWorkInstructionCountMap not null
			// maintain the CHE feedback, but not for put wall puts. Not DO_PUT. And not SHORT_PUT state.
			//Decrement count if this is a non-HK WI
			String containerId = inWi.getContainerId();
			if (!inWi.isHousekeeping() && !"None".equalsIgnoreCase(containerId)) {
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
			if (!mDeviceManager.getPickMultValue(getCheLightingEnum())) {
				LOGGER.error("Simultaneous work instructions turned off currently, so unexpected case in processNormalPick");
				// let's log the first just for fun
				LOGGER.error("first wi from processNormalPick error jsut before is: {}", mActivePickWiList.get(0));
			}
			showActivePicks();
		} else {
			// There's no more active picks, so move to the next set.
			doNextItemWithPossibleFlashingThread(inWi.isHousekeeping());
		}
	}

	private void doNextItemWithPossibleFlashingThread(boolean housekeeping) {
		if (mDeviceManager.getPickMultValue(getCheLightingEnum()) && !housekeeping) {
			Thread doNextItemThread = new Thread(new Runnable() {
				@Override
				public void run() {
					List<PosControllerInstr> instructions = new ArrayList<>();
					PosControllerInstr instr = new PosControllerInstr((byte) 0,
						PosControllerInstr.BITENCODED_SEGMENTS_CODE,
						PosControllerInstr.BITENCODED_TRIPLE_DASH,
						PosControllerInstr.BITENCODED_TRIPLE_DASH,
						PosControllerInstr.BLINK_FREQ.byteValue(),
						PosControllerInstr.BRIGHT_DUTYCYCLE.byteValue());
					instructions.add(instr);
					sendPositionControllerInstructions(instructions);
					ThreadUtils.sleep(800);
					clearAllPosconsOnThisDevice();
					doNextItem();
				}
			});
			doNextItemThread.start();
		} else {
			doNextItem();
		}
	}

	private void doNextItem() {
		CheStateEnum state = getCheStateEnum();
		if (state == CheStateEnum.DO_PUT || state.equals(CheStateEnum.SHORT_PUT_CONFIRM)) {
			doNextWallPut();
		} else {
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

	@Override
	protected String getButtonPurpose(int buttonNum) {
		return getContainerIdFromButtonNum(buttonNum);
	}

	/**
	 * override this if your che device sets poscon to a state where they can send, but shouldn't, and you know if it is a bad send
	 * For our purpose, the easy case is when we need a scan. 
	 * Could get more elaborate about button presses on "oc", etc, but that is handled elsewhere
	 */
	@Override
	protected String tellIfNotLegitimateButtonPress(int buttonNum, int showingQuantity) {
		CheStateEnum state = getCheStateEnum();
		switch (state) {
			case SCAN_SOMETHING:
			case SCAN_SOMETHING_SHORT:
				return String.format("Need to scan, so ignoring button #%d quantity %d", buttonNum, showingQuantity);

			default:
				return ""; // Return empty string for normal non-error condition
		}
	}

	private void clearContainerAssignmentAtIndex(byte posconIndex) {
		// careful: POSITION_ALL is zero
		if (PosControllerInstr.POSITION_ALL.equals(posconIndex))
			LOGGER.error("Incorrect use of clearContainerAssignmentAtIndex"); // did you really intend mPositionToContainerMap.clear()?
		else {
			String containerId = mPositionToContainerMap.remove(Integer.toString(posconIndex));
			// The count map may or may not be set up, depending on what happened before this clear is called. However, it is certain
			// that if we do not have the position mapped, we should not have work instruction count.
			if (containerId != null) {
				// careful. Unlike the mPositionToContainerMap, this map is null until populated
				if (mContainerToWorkInstructionCountMap != null)
					mContainerToWorkInstructionCountMap.remove(containerId);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * DEV-1347	This device log could be in any state. What we generally want to accomplish is remove the container from the map, and recompute the work instructions
	 * without that order. But we do not want to interrupt this che in the middle of a pick, especially in a multi-pick.
	 * Let's post this to a queue, then upon completion of the SKU (where we multi-flash), instead of next pick, remove the container and go back to summary state.
	 */
	@Override
	protected void removeStolenCntr(String inCntr) {
		// Remember, this is called by other cheDeviceLogic during it's setup. Does it log in the other's context?
		LOGGER.debug("queue to remove {} from {}", inCntr, this.getGuidNoPrefix());
		mStolenCntrs.add(inCntr);
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
	@Override
	protected void processButtonPress(Integer inButtonNum, Integer inQuantity) {
		// In general, this can only come if the poscon was set in a way that prepared it to be able to send.
		// However, pickSimulator.pick() can be called in any context, which simulates the button press command coming in.
		notifyButton(inButtonNum, inQuantity);

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
			case REMOVE_CHE_CONTAINER:
				clearContainerAssignmentAtIndex(inButtonNum.byteValue());
				clearOnePosconOnThisDevice(inButtonNum.byteValue());
				setState(CheStateEnum.CONTAINER_SELECT);
				return;
			default: {
				// See a few of these if the poscon clear did not take, the user presses that button again.
				// But most are the user pressing "oc" or dashes, etc. Just noise.
				// Try to log the ones distinctively that might show an error. Filter for "ignored with value"
				if (inQuantity != null && inQuantity > 0 && inQuantity < 10)
					LOGGER.warn("Unexpected button press ignored with value {}", inQuantity);
				else
					LOGGER.warn("Unexpected button press ignored."); // don't represent the value here. Complicated if a bit encoded one.
				// We want to ignore the button press, but force out starting poscon situation again.
				setState(mCheStateEnum);
				return;
			}
		}

		String containerId = getContainerIdFromButtonNum(inButtonNum);
		if (containerId == null) {
			// DEV-1318 don't just  ignore button presses when there is no container.
			LOGGER.warn("Ignore button because no order/container associated to button. But refreshing the displays."); // DEV-1318
			setState(mCheStateEnum);
			return;
		} else if (this.mStolenCntrs.contains(containerId) || this.mStolenCntrs.contains(CsDeviceManager.WI_COMPLETE_FAIL)) {
			// DEV-1347. Refuse to complete this job is the container was just stolen by another CHE, as the server's work instruction will not match
			// This is the worst case of being in the middle of a pick concerning the stolen container. Usually dealt with between picks without confusing the worker much.
			// Or the unfortunately common case of the pick just after a pick did not complete on the server. DEV-1331
			LOGGER.warn("Not completing job for {} because it was taken by another cart, or job is stale", containerId);
			// Recover is not too elegant.
			for (WorkInstruction wi : mActivePickWiList) {
				clearLedAndPosConControllersForWi(wi);
			}
			setState(CheStateEnum.SETUP_SUMMARY);
		} else {
			WorkInstruction wi = getWorkInstructionForContainerId(containerId);
			if (wi == null) {
				// DEV-1318 Most likely cause of no work instruction is it was just completed, but the poscon message did not get through to the poscon
				// Therefore, user is pressing the button again. Let's make sure we send out the poscon again and not just silently ignore button press
				// Most are the user pressing "oc" or dashes, etc. Just noise.
				// Try to log the ones distinctively that might show an error. Filter for "ignored with value"
				if (inQuantity != null && inQuantity > 0 && inQuantity < 10) {
					LOGGER.warn("Button {} has no workinstruction. Press ignored with value {}", inButtonNum, inQuantity);
					// This is the likely bug of poscon clear did not happen. Should be no way a poscon has a count without a work instruction.
				} else {
					LOGGER.warn("Not processing button because no work instruction remains at that position. But refreshing the displays.");
				}
				setState(mCheStateEnum);
				return;
			} else if (wi.isHousekeeping()) {
				// for housekeeping, any button press count. We will plug in a value of 0.  Version 3.0 poscons send value of -1.
				// version 2.0 sends value of 0.
				processNormalPick(wi, 0);
			} else {
				if (inQuantity == wi.getPlanMinQuantity()) {
					processNormalPick(wi, inQuantity);
				} else {
					// More kludge for count > 99 case
					Integer planQuantity = wi.getPlanQuantity();
					if (inQuantity == maxCountForPositionControllerDisplay && planQuantity > maxCountForPositionControllerDisplay)
						processNormalPick(wi, planQuantity); // Assume all were picked. No way for user to tell if more than 98 given.
					else {
						// DEV-1287 See other part of the fix in notifyButton(). If that does not throw, this will hit.
						// Is it legitimate? Just someone pushing a button on "--" or "oc"? If so, change to a warn, or perhaps,
						// Improve getLastSentPositionControllerDisplayValue to not send null for a legitimate case.
						if (inQuantity < 0) {
							LOGGER.error("Button #{}: Unnexpected value {} in processButtonPress", inButtonNum, inQuantity);
						} else {
							processShortPickOrPut(wi, inQuantity);
						}
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
				clearOnePosconOnThisDevice(position);
		}

		// Then the inherited shorts part is the same
		super.processShortPickOrPut(inWi, inQuantity);
	}

	protected void processPutWallOrderScan(final String inScanPrefixStr, final String inScanStr) {
		setLastPutWallOrderScan(inScanStr);
		setState(CheStateEnum.PUT_WALL_SCAN_LOCATION);
	}

	protected void processPutWallLocationScan(final String inScanPrefixStr, String inScanStr) {
		String orderId = getLastPutWallOrderScan();
		if (TAPE_PREFIX.equals(inScanPrefixStr)) {
			inScanStr = TAPE_PREFIX + inScanStr;
		}
		sendOrderPlacementMessage(orderId, inScanStr);
		// DEV-766. If we just put this order to the wall, we should remove it from our cart.
		// Otherwise, if we START or change location, we will still get that order work.

		// TODO later. if we remove from container map, need to clear CHE state after the server maintains that.
		Byte orderPositionOnChe = getPosconIndexOfContainerId(orderId);
		if (orderPositionOnChe != 0) {
			clearContainerAssignmentAtIndex(orderPositionOnChe);
			notifyRemoveOrderFromChe(orderId, orderPositionOnChe);
		}

		setState(CheStateEnum.PUT_WALL_SCAN_ORDER);
	}

	private void sendOrderPlacementMessage(String orderId, String locationName) {
		// This will form the command and send to server. If successful, the putwall poscon feedback will be apparent.
		PutWallPlacementRequest message = new PutWallPlacementRequest(getPersistentId().toString(), orderId, locationName);
		mDeviceManager.clientEndpoint.sendMessage(message);

		notifyOrderToPutWall(orderId, locationName);
	}

	protected void processPutWallItemScan(final String inScanPrefixStr, final String inScanStr) {
		// This should be a clean scan of itemId or GTIN/UPC. If there is a scan prefix, don't bother asking the server.
		if (!inScanPrefixStr.isEmpty()) {
			LOGGER.warn("Ignoring inappropriate scan");
			setState(getCheStateEnum()); // looks like noop, but force screen redraw
			return;
		}

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
					showHouseKeepingDisplayOnPoscon(BAY_COMPLETE_CODE, wi.getContainerId());
				} else if (WorkInstructionTypeEnum.HK_REPEATPOS.equals(theEnum)) {
					returnBool = true;
					showHouseKeepingDisplayOnPoscon(REPEAT_CONTAINER_CODE, wi.getContainerId());
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
	 * We used to have value code kludge for baychange and repeat container.
	 */
	private void showHouseKeepingDisplayOnPoscon(int inSpecialCode, String inContainerId) {

		boolean codeUnderstood = false;

		Byte valueToSend = PosControllerInstr.BITENCODED_SEGMENTS_CODE;
		Byte minToSend = 0;
		Byte maxToSend = 0;
		if (inSpecialCode == BAY_COMPLETE_CODE) {
			codeUnderstood = true;
			minToSend = PosControllerInstr.BITENCODED_LED_C; // this is the right one, so it spells "bc"
			maxToSend = PosControllerInstr.BITENCODED_LED_B;
		} else if (inSpecialCode == REPEAT_CONTAINER_CODE) {
			codeUnderstood = true;
			minToSend = PosControllerInstr.BITENCODED_LED_R; // this is the right one
			maxToSend = PosControllerInstr.BITENCODED_LED_BLANK;
		}

		if (!codeUnderstood) {
			LOGGER.error("showHouseKeepingDisplayOnPoscon: called with unknown code={}; containerId={}",
				inSpecialCode,
				inContainerId);
			return;
		}
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		for (Entry<String, String> mapEntry : mPositionToContainerMap.entrySet()) {
			if (mapEntry.getValue().equals(inContainerId)) {
				byte position = getPositionValue(mapEntry);
				if (position == 0) {
					continue;
				}

				PosControllerInstr instruction = new PosControllerInstr(position,
					valueToSend,
					minToSend,
					maxToSend,
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
				byte position = getPositionValue(mapEntry);
				return position; // no need to bail on return value zero, as this function is supposed to return zero in that case.
			}
		}
		return 0;
	}

	private void showPosconBusyScreen() {
		String line1 = String.format(POSCON_BUSY_LINE_1, getOneActiveWorkInstruction().getPickInstruction());
		String line2 = "By " + mBusyPosconHolders;
		sendDisplayCommand(line1, line2, POSCON_BUSY_LINE_3, POSCON_BUSY_LINE_4);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void logout() {
		super.logout(); // this calls the notifyXXX
		lastScannedGTIN = null;
		mContainerInSetup = "";

		// if this che had remote control of another, release the screen of the other
		disconnectRemoteDueToLogout();
	}

	/**
	 * Setup the CHE by clearing all the data structures
	 */
	protected void setupChe() {
		mPositionToContainerMap.clear();
		mContainerToWorkInstructionCountMap = null;
		mContainerInSetup = "";
		resetRememberCompletesAndShorts();
		clearSetupMix();
		super.setupChe();
	}

	// --------------------------------------------------------------------------
	/**
	 * Attempt to guess if we already must have scanned this one.
	 * For DEV-692.  Our allPicksList should be sorted and still have recently completed work.
	 * DEV-1295 complexity. What if worker scanned once correctly, then although did not need to scan, then scanned incorrectly?
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
					if (theStatus == WorkInstructionStatusEnum.COMPLETE || theStatus == WorkInstructionStatusEnum.SHORT
							|| theStatus == WorkInstructionStatusEnum.SUBSTITUTION)
						return true;
				}
		}
		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Initialize from server to know what containers are setup on this CHE
	 * For DEV-784.
	 */
	@Override
	public void processStateSetup(HashMap<String, Integer> positionMap) {
		if (positionMap == null) {
			LOGGER.error("Null map inprocessStateSetup");
			return;
		}
		CheStateEnum state = getCheStateEnum();

		// DEV-1261.  We now realize that if site controller loses connection to server and reconnects, then the server triggers its
		// che state initialization just as if site controller had quit and restarted. Anyway, these messages may come at any time, so
		// it is not an error if the device logic is not in setup phase. However, let's keep the site controller's state and not replace it
		// with the server state. Hence the returns.
		if (!state.equals(CheStateEnum.IDLE)) {
			LOGGER.info("Received processStateSetup in state {}. Normal occurrence only if site controller just reconnected to server.",
				state);
			// errors here through v24. Then returns as here.
			return;
		}
		if (!mPositionToContainerMap.isEmpty()) {
			LOGGER.error("Received processStateSetup in IDLE state when map is not empty. Normal occurrence only if site controller just reconnected to server.");
			// errors here through v24. Then returns as here.
			return;
		}
		for (Map.Entry<String, Integer> entry : positionMap.entrySet()) {
			String container = entry.getKey();
			Integer position = entry.getValue();
			if (container == null || position == null)
				LOGGER.error("null value in processStateSetup map");
			else if (container.isEmpty() || position < 1 || position > 255)
				LOGGER.error("bad value in processStateSetup map. container:{} position:{}", container, position);
			else {
				LOGGER.info("{} initialize setup; container:{} position:{}", this.getMyGuidStrForLog(), container, position);
				mPositionToContainerMap.put(position.toString(), container);
			}
		}
	}

	protected void processSkuWallNewLocationScan(String inScanPrefixStr, String inScanStr) {
		if (TAPE_PREFIX.equals(inScanPrefixStr)) {
			inScanStr = TAPE_PREFIX + inScanStr;
		}
		mDeviceManager.inventoryUpdateScan(this.getPersistentId(), inScanStr, mLastPutWallItemScan, mPutWallName);
		setState(CheStateEnum.COMPUTE_WORK);
	}

	protected void processSkuWallLocationDisambiguation(String inScanPrefixStr, String inScanStr) {
		if (TAPE_PREFIX.equals(inScanPrefixStr)) {
			inScanStr = TAPE_PREFIX + inScanStr;
		}
		mDeviceManager.skuWallLocationDisambiguation(this.getPersistentId(), inScanStr, mLastPutWallItemScan, mPutWallName);
		setState(CheStateEnum.COMPUTE_WORK);
	}

	private void processInfoLocationScan(String prefix, String location) {
		if (CheDeviceLogic.TAPE_PREFIX.equals(prefix)) {
			location = prefix + location;
		}
		setLastScannedInfoLocation(location);
		CheStateEnum infoSourceState = getRememberEnteringInfoState();
		switch (infoSourceState) {
			case PUT_WALL_SCAN_WALL:
			case PUT_WALL_SCAN_ITEM:
				setState(CheStateEnum.INFO_RETRIEVAL);
				mDeviceManager.performInfoOrRemoveAction(InfoRequestType.GET_WALL_LOCATION_INFO,
					location,
					getGuidNoPrefix(),
					getPersistentId().toString());
				break;
			case SCAN_GTIN: //Inventory mode
				setState(CheStateEnum.INFO_RETRIEVAL);
				mDeviceManager.performInfoOrRemoveAction(InfoRequestType.GET_INVENTORY_INFO,
					location,
					getGuidNoPrefix(),
					getPersistentId().toString());
				break;
			default:
		}
	}

	private void displayInfo() {
		InfoPackage info = getInfo();
		if (info == null) {
			sendDisplayCommand("NO INFO RECEIVED", "CANCEL to exit");
		} else {
			sendDisplayCommand(info.getDisplayInfoLine(0),
				info.getDisplayInfoLine(1),
				info.getDisplayInfoLine(2),
				info.getDisplayInfoLine(3));
		}
	}

	private void displayRemoveInfo() {
		InfoPackage info = getInfo();
		if (info == null) {
			sendDisplayCommand("NO INFO RECEIVED", "CANCEL to exit");
		} else {
			sendDisplayCommand(info.getDisplayRemoveLine(0),
				info.getDisplayRemoveLine(1),
				info.getDisplayRemoveLine(2),
				info.getDisplayRemoveLine(3));
		}
	}
}
