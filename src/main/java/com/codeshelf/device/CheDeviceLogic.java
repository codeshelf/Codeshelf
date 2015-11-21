/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceLogic.java,v 1.12 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.AisleDeviceLogic.LedCmd;
import com.codeshelf.device.PosControllerInstr.PosConInstrGroupSerializer;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlClearDisplay;
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.CommandControlDisplaySingleLineMessage;
import com.codeshelf.flyweight.command.EffectEnum;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ScannerTypeEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

/**
 * CheDeviceLogic is now an abstract base class for CHE programs with different state machines.
 * See SetupOrdersDeviceLogic to get what CheDeviceLogic used to be all by itself.
 */
public class CheDeviceLogic extends PosConDeviceABC {
	// This code runs on the site controller, not the CHE.
	// The goal is to convert data and instructions to something that the CHE controller can consume and act on with minimal logic.

	private static final Logger						LOGGER									= LoggerFactory.getLogger(CheDeviceLogic.class);

	protected static final String					COMMAND_PREFIX							= "X%";
	protected static final String					USER_PREFIX								= "U%";
	protected static final String					CONTAINER_PREFIX						= "C%";
	protected static final String					LOCATION_PREFIX							= "L%";
	protected static final String					ITEMID_PREFIX							= "I%";
	protected static final String					POSITION_PREFIX							= "P%";
	public static final String						TAPE_PREFIX								= "%";												// save a character for tape, allowing tighter resolution
	protected static final String					CHE_NAME_PREFIX							= "H%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 20 characters.
	// "SCAN START LOCATION" is at the 20 limit. If you change to "SCAN STARTING LOCATION", you get very bad behavior. The class loader will not find the CheDeviceLogic. Repeating throws.	
	protected static final String					EMPTY_MSG								= cheLine("");
	protected static final String					INVALID_SCAN_MSG						= cheLine("INVALID SCAN");
	protected static final String					SCAN_USERID_MSG							= cheLine("SCAN BADGE");
	protected static final String					SCAN_START_LOCATION_MSG					= cheLine("SCAN START LOCATION");
	protected static final String					OR_START_WORK_MSG						= cheLine("OR START WORK");
	protected static final String					SELECT_POSITION_MSG						= cheLine("SELECT POSITION");
	protected static final String					SHORT_PICK_CONFIRM_MSG					= cheLine("CONFIRM SHORT");
	public static final String						YES_NO_MSG								= cheLine("SCAN YES OR NO");						// public for test
	protected static final String					NO_CONTAINERS_SETUP_MSG					= cheLine("NO SETUP CONTAINERS");
	protected static final String					POSITION_IN_USE_MSG						= cheLine("POSITION IN USE");
	protected static final String					FINISH_SETUP_MSG						= cheLine("PLS SETUP CONTAINERS");
	protected static final String					COMPUTE_WORK_MSG						= cheLine("COMPUTING WORK");
	protected static final String					GET_WORK_MSG							= cheLine("GETTING WORK");
	protected static final String					SHOWING_ORDER_IDS_MSG					= cheLine("SHOWING ORDER IDS");
	protected static final String					SHOWING_WI_COUNTS						= cheLine("SHOWING WI COUNTS");
	protected static final String					VERIFYING_BADGE_MSG						= cheLine("VERIFYING BADGE");
	protected static final String					UNKNOWN_BADGE_MSG						= cheLine("UNKNOWN BADGE");

	protected static final String					INVALID_POSITION_MSG					= cheLine("INVALID POSITION");
	protected static final String					INVALID_CONTAINER_MSG					= cheLine("INVALID CONTAINER");
	protected static final String					CANCEL_TO_CONTINUE_MSG					= cheLine("CANCEL TO CONTINUE");

	protected static final String					SCAN_GTIN								= "SCAN %s UPC";
	protected static final String					SCAN_GTIN_OR_LOCATION					= "SCAN %s/LOCATION";

	// Newer messages only used in Line_Scan mode. Some portion of the above are used for both Setup_Orders and Line_Scan, so keeping them all here.
	protected static final String					SCAN_LINE_MSG							= cheLine("SCAN ORDER LINE");
	protected static final String					GO_TO_LOCATION_MSG						= cheLine("GO TO LOCATION");
	protected static final String					ABANDON_CHECK_MSG						= cheLine("ABANDON CURRENT JOB");
	protected static final String					ONE_JOB_MSG								= cheLine("DO THIS JOB (FIXME)");					// remove this later

	// For Put wall
	protected static final String					SCAN_PUTWALL_ORDER_MSG					= cheLine("SCAN ORDER FOR");
	protected static final String					SCAN_PUTWALL_LOCATION_MSG				= cheLine("SCAN LOCATION IN");
	protected static final String					PUT_WALL_MSG							= cheLine("PUT WALL");
	protected static final String					SCAN_PUTWALL_ITEM_MSG					= cheLine("SCAN ITEM/UPC");
	protected static final String					SCAN_PUTWALL_NAME_MSG					= cheLine("SCAN WALL NAME");
	protected static final String					NO_WORK_FOR								= cheLine("NO WORK FOR");
	protected static final String					SCAN_ITEM_OR_CANCEL						= cheLine("SCAN ITEM OR CANCEL");

	//For Sku wall
	protected static final String					SCAN_LOCATION_MSG						= cheLine("SCAN LOCATION");
	public static final String						CANCEL_TO_EXIT_MSG						= cheLine("CANCEL to exit");

	//For Remove command
	protected static final String					REMOVE_CONTAINER_MSG					= cheLine("To remove order");
	protected static final String					REMOVE_NOTHING_MSG						= cheLine("NOTHING TO REMOVE");

	//For Palletizer
	protected static final String					PALL_NEW_ORDER_1_MSG					= cheLine("Scan New Location");
	protected static final String					PALL_NEW_ORDER_2_MSG					= cheLine("For Store ");
	protected static final String					PALL_NEW_ORDER_3_MSG					= cheLine("Or Scan Another Item");
	protected static final String					PALL_SCAN_NEXT_ITEM_MSG					= cheLine("Scan Next Item");
	protected static final String					PALL_DAMAGED_CONFIRM_MSG				= cheLine("CONFIRM DAMAGED");
	protected static final String					PALL_REMOVE_1_MSG						= cheLine("Scan License");
	protected static final String					PALL_REMOVE_2_MSG						= cheLine("Or Location");
	protected static final String					PALL_REMOVE_3_MSG						= cheLine("To Close Pallet");

	//To repeat: !!!DO NOT CREATE LINES LONGER THAN 20 CHARACTERS!!! using cheLine()
	//Causes untraceable error during Site initialization

	//For Poscon Busy
	protected static final String					POSCON_BUSY_LINE_1						= "Poscon for %s busy";
	protected static final String					POSCON_BUSY_LINE_3						= "Scan YES after they come";
	protected static final String					POSCON_BUSY_LINE_4						= "or if they will never come";

	public static final String						STARTWORK_COMMAND						= "START";
	public static final String						REVERSE_COMMAND							= "REVERSE";
	protected static final String					SETUP_COMMAND							= "SETUP";
	protected static final String					SHORT_COMMAND							= "SHORT";
	public static final String						LOGOUT_COMMAND							= "LOGOUT";
	protected static final String					YES_COMMAND								= "YES";
	protected static final String					NO_COMMAND								= "NO";
	protected static final String					CLEAR_COMMAND							= "CLEAR";
	protected static final String					CANCEL_COMMAND							= "CANCEL";
	protected static final String					INVENTORY_COMMAND						= "INVENTORY";
	protected static final String					ORDER_WALL_COMMAND						= "ORDER_WALL";
	protected static final String					PUT_WALL_COMMAND						= "PUT_WALL";
	protected static final String					REMOTE_COMMAND							= "REMOTE";
	protected static final String					POSCON_COMMAND							= "POSCON";
	protected static final String					INFO_COMMAND							= "INFO";
	protected static final String					REMOVE_COMMAND							= "REMOVE";
	protected static final String					LOW_COMMAND								= "LOW";

	// With WORKSEQR = "WorkSequence", work may scan start instead of scanning a location. 
	// LOCATION_SELECT_REVIEW, we want "REVIEW MISSING WORK" "OR SCAN LOCATION" "OR SCAN START"
	protected static final String					OR_SCAN_START							= cheLine("OR SCAN START");
	protected static final String					OR_SCAN_LOCATION						= cheLine("OR SCAN LOCATION");

	// If used to check if the user wants to skip SCANPICK UPC/SKU/LCN verification
	protected static final String					SCAN_SKIP								= "SCANSKIP";
	protected static final String					SKIP_SCAN								= "SKIPSCAN";

	protected static final Integer					maxCountForPositionControllerDisplay	= 99;

	protected static boolean						kLogAsWarn								= true;
	protected static boolean						kLogAsInfo								= false;

	// The CHE's current state.
	@Accessors(prefix = "m")
	@Getter
	protected CheStateEnum							mCheStateEnum;

	// The CHE's current user. no lomboc because getUserId defined on base class
	protected String								mUserId;

	// All WIs for all containers on the CHE.
	@Accessors(prefix = "m")
	@Getter
	protected List<WorkInstruction>					mAllPicksWiList;

	// Remember the first string in last display message sent
	@Accessors(prefix = "m")
	ArrayList<String>								mLastScreenDisplayLines;

	// The active pick WIs.
	@Accessors(prefix = "m")
	@Getter
	protected List<WorkInstruction>					mActivePickWiList;

	private Table<String, Integer, WorkInstruction>	mWiNonChePoscons;

	private NetGuid									mLastLedControllerGuid;
	private boolean									mMultipleLastLedControllerGuids;

	protected WorkInstruction						mShortPickWi;
	protected Integer								mShortPickQty;

	protected boolean								connectedToServer						= true;
	@Accessors(prefix = "m")
	@Getter
	@Setter
	protected int									mSetStateStackCount						= 0;

	//private ScanNeededToVerifyPick					mScanNeededToVerifyPick;

	@Getter
	@Setter
	protected Boolean								mReversePickOrder						= false;

	@Getter
	@Setter
	protected String								lastScannedGTIN;

	@Getter
	@Setter
	private String									lastPutWallOrderScan;

	// Fields for REMOTE linked CHEs
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String									mLinkedToCheName						= null;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetGuid									mLinkedFromCheGuid						= null;

	private WorkInstruction							verifyWi								= null;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private ScannerTypeEnum							mScannerTypeEnum						= ScannerTypeEnum.ORIGINALSERIAL;

	/**
	 * We have only one inventory state, not two. Essentially another state by whether or not we think we have a valid
	 * gtin or item id in lastScanedGTIN.  This is fairly complicated. We desire:
	 * - scan a gtin or item ID: flash where we think it is, and remember it for later update.
	 * - scan codeshelf tape. If lastScannedGtin, do the update. If not, perhaps just flash it.
	 * By all means, do not remember a tape or location string as lastScanedGTIN
	 */
	protected void processGtinStateScan(final String inScanPrefixStr, final String inScanStr) {

		boolean isTape = TAPE_PREFIX.equals(inScanPrefixStr);
		// Let's separate out the logic by the kind of scan we got, and then handle by whether we have a stored gtin or not.

		// Location scan is fairly unlikely
		if (LOCATION_PREFIX.equals(inScanPrefixStr)) {
			if (lastScannedGTIN != null) {
				// Updating location of an item
				notifyScanInventoryUpdate(inScanStr, lastScannedGTIN);
				mDeviceManager.inventoryUpdateScan(this.getPersistentId(), inScanStr, lastScannedGTIN, null);
			} else {
				// just a location ID scan. light it.
				mDeviceManager.inventoryLightLocationScan(getPersistentId(), inScanStr, isTape);
			}
		}

		// tape scan is likely
		else if (TAPE_PREFIX.equals(inScanPrefixStr)) {
			String tapeScan = TAPE_PREFIX + inScanStr;
			if (lastScannedGTIN != null) {
				// Updating location of an item
				// Let's pass with the tape prefix to the server. Otherwise, it has to query one way, and then again for tape
				notifyScanInventoryUpdate(tapeScan, lastScannedGTIN);
				mDeviceManager.inventoryUpdateScan(this.getPersistentId(), tapeScan, lastScannedGTIN, null);
			} else {
				// just a location ID scan. light it. Also pass the % first.
				mDeviceManager.inventoryLightLocationScan(getPersistentId(), tapeScan, isTape);
			}
		}

		// Other special scans not valid, such as user, position, container
		else if (inScanPrefixStr != null && !inScanPrefixStr.isEmpty()) {
			LOGGER.warn("Recieved invalid scan: {}. Expected tape or location scan, or GTIN.", inScanStr);
		} else {
			// An unadorned string should be gtin/UPC. Store it, replacing what we had.
			mDeviceManager.inventoryLightItemScan(this.getPersistentId(), inScanStr);
			lastScannedGTIN = inScanStr;
		}
		setState(CheStateEnum.SCAN_GTIN);
	}

	protected enum ScanNeededToVerifyPick {
		NO_SCAN_TO_VERIFY("disabled"),
		UPC_SCAN_TO_VERIFY("UPC"),
		SKU_SCAN_TO_VERIFY("SKU"),
		LPN_SCAN_TO_VERIFY("LPN");
		private String	mInternal;

		private ScanNeededToVerifyPick(String inString) {
			mInternal = inString;
		}

		public static ScanNeededToVerifyPick stringToScanPickEnum(String inScanPickValue) {
			ScanNeededToVerifyPick returnValue = NO_SCAN_TO_VERIFY;
			for (ScanNeededToVerifyPick onValue : ScanNeededToVerifyPick.values()) {
				if (onValue.mInternal.equalsIgnoreCase(inScanPickValue))
					return onValue;
			}
			return returnValue;
		}

		public static String scanPickEnumToString(ScanNeededToVerifyPick inValue) {
			return inValue.mInternal;
		}
	}

	@Override
	public String getUserId() {
		return mUserId;
	}

	public void setUserId(String user) {
		mUserId = user;
	}

	public boolean usesNewCheScreen() {
		return false; // SetupOrderDeviceLogic will override
	}

	public void testOnlySetState(final CheStateEnum inCheState) {
		setState(inCheState);
	}

	protected boolean alreadyScannedSkuOrUpcOrLpnThisWi(WorkInstruction inWi) {
		return false;
	}

	protected boolean isScanNeededToVerifyPick() {
		WorkInstruction wi = this.getOneActiveWorkInstruction();

		if (wi.isHousekeeping()) {
			return false;
		}

		// /*
		else if (wi.getNeedsScan()) {
			return !alreadyScannedSkuOrUpcOrLpnThisWi(wi);
		}
		// */

		/*
		else if (mScanNeededToVerifyPick != ScanNeededToVerifyPick.NO_SCAN_TO_VERIFY) {
			return !alreadyScannedSkuOrUpcOrLpnThisWi(wi);
			// See if we can skip this scan because we already scanned.
		}
		*/

		return false;
	}

	public ScanNeededToVerifyPick getScanVerificationTypeEnum() {
		String scanPickValue = mDeviceManager.getScanTypeValue();
		return ScanNeededToVerifyPick.stringToScanPickEnum(scanPickValue);
	}

	public String getScanVerificationTypeUI() {
		String type = mDeviceManager.getScanTypeValue();
		if ("Disabled".equalsIgnoreCase(type)) {
			type = "UPC";
		}
		return type;
	}

	public String getScanVerificationType() {
		return ScanNeededToVerifyPick.scanPickEnumToString(getScanVerificationTypeEnum());
	}

	public void updateConfigurationFromManager() {
		// We might want this, as if workSequence, then start location is far less relevant
		@SuppressWarnings("unused")
		String mSequenceKind = mDeviceManager.getSequenceKind();
	}

	public CheDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController,
		Che che) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mCheStateEnum = CheStateEnum.IDLE;
		mAllPicksWiList = new ArrayList<WorkInstruction>();
		mActivePickWiList = new ArrayList<WorkInstruction>();
		mLastScreenDisplayLines = new ArrayList<String>(); // and preinitialize to lines 1-4
		if (che != null && che.getScannerType() != null)
			setScannerTypeEnum(che.getScannerType());
		for (int n = 0; n <= 3; n++) {
			mLastScreenDisplayLines.add(" ");
		}
		mWiNonChePoscons = HashBasedTable.create();
	}

	@Override
	public final short getSleepSeconds() {
		return 180;
	}

	public boolean inSetState() {
		// Are we currently in setState?  Only test code should ever call this.
		return mSetStateStackCount > 0;
	}

	public String getDeviceType() {
		LOGGER.error("getDeviceType(): Should have specific instance of this abstract type");
		return CsDeviceManager.DEVICETYPE_CHE;
	}

	// The last-aisle-controller-for-this-CHE package.
	private void setLastLedControllerGuid(NetGuid inAisleControllerGuid) {
		if (mLastLedControllerGuid == null)
			mLastLedControllerGuid = inAisleControllerGuid;
		else if (mLastLedControllerGuid != inAisleControllerGuid) {
			mLastLedControllerGuid = inAisleControllerGuid;
			mMultipleLastLedControllerGuids = true;
		}
	}

	private NetGuid getLastLedControllerGuid() {
		return mLastLedControllerGuid;
	}

	private boolean wereMultipleLastLedControllerGuids() {
		return mMultipleLastLedControllerGuids;
	}

	private void clearLastLedControllerGuids() {
		mLastLedControllerGuid = null;
		mMultipleLastLedControllerGuids = false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a display message to the CHE's embedded control device.
	 * @param inLine1Message
	 * @param inLine2Message
	 */
	protected void sendDisplayCommand(final String inLine1Message, final String inLine2Message) {
		sendDisplayCommand(inLine1Message, inLine2Message, "", "");
	}

	protected void setState(final CheStateEnum inCheState) {

	}

	public String getRecentCheDisplayString(int oneBasedLineIndex) {
		if (oneBasedLineIndex < 1 || oneBasedLineIndex > 4) {
			LOGGER.error("boundary error in getRecentCheDisplayString");
			return "";
		}
		return mLastScreenDisplayLines.get(oneBasedLineIndex - 1);
	}

	protected void doSetRecentCheDisplayString(int oneBasedLineIndex, String lineValue) {
		if (oneBasedLineIndex < 1 || oneBasedLineIndex > 4) {
			LOGGER.error("boundary error in doSetRecentCheDisplayString");
			return;
		}
		mLastScreenDisplayLines.set(oneBasedLineIndex - 1, lineValue);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the primitive that sends out a single line command
	 *  Documented at https://codeshelf.atlassian.net/wiki/display/TD/KW2+CHE+Displays
	 */
	private void sendSingleLineDisplayMessage(final String inLineMessageStr, final byte fontType, final short posX, final short posY) {
		if (inLineMessageStr != null && !inLineMessageStr.isEmpty()) {
			ICommand command = new CommandControlDisplaySingleLineMessage(NetEndpoint.PRIMARY_ENDPOINT,
				inLineMessageStr,
				fontType,
				posX,
				posY);
			sendScreenCommandToMyChe(command);
			sendScreenCommandToLinkFromChe(command);
		}
		// for this, just don't send anything if the line is blank.
	}

	// --------------------------------------------------------------------------
	/**
	 *  Test function to explore how it works
	 *  Findings on the 2.7 inch CHE: 20 is a comfortable x value for left side of screen.
	 *  40 is comfortable y value for first line for smaller fonts.
	 *  ARIAL16 displays about 35 digits.
	 *  ARIAL26 displays about 22 digits
	 *  ARIALMONOBOLD20 gives us 22 characters. To center 22 characters, x offset 18. For 20 chars, x = 26. For 4 y lines, use 35, 90, 145, 200
	 *  ARIALMONOBOLD24 gives us 19 characters. To center 19, use x offset 12.
	 *  ARIALMONOBOLD16 gives us 27 characters. Unpleasingly small.
	 *  ARIALMONOBOLD26 gives us 17 characters

	 */
	protected void testSingleLineDisplays(final int whichTest) {
		clearDisplay();
		String testString40 = "1234567890123456789012345678901234567890";
		String testString20 = "12345678901234567890";
		if (whichTest == 1) {
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIAL16, (byte) 0, (byte) 0);
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIAL16, (byte) 5, (byte) 40);
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIAL26, (byte) 10, (byte) 100);
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIAL26, (byte) 20, (byte) 180);
		} else if (whichTest == 2) {
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 20, (byte) 40);
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 20, (byte) 95);
			sendSingleLineDisplayMessage(testString40,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD24,
				(byte) 20,
				(byte) 150);
			sendSingleLineDisplayMessage(testString40,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD24,
				(byte) 20,
				(byte) 205);
		} else if (whichTest == 3) {
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 18, (byte) 35);
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 18, (byte) 90);
			sendSingleLineDisplayMessage(testString40,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20,
				(byte) 18,
				(byte) 145);
			sendSingleLineDisplayMessage(testString40,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20,
				(byte) 18,
				(byte) 200);
		} else if (whichTest == 4) { // This is our prototype for monospace corollary of old screens
			sendSingleLineDisplayMessage(testString20, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 26, (byte) 35);
			sendSingleLineDisplayMessage(testString20, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 26, (byte) 90);
			sendSingleLineDisplayMessage(testString20,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20,
				(byte) 26,
				(byte) 145);
			sendSingleLineDisplayMessage(testString20,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20,
				(byte) 26,
				(byte) 200);
		} else if (whichTest == 5) {
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD16, (byte) 20, (byte) 35);
			sendSingleLineDisplayMessage(testString40, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD16, (byte) 20, (byte) 90);
			sendSingleLineDisplayMessage(testString40,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD26,
				(byte) 20,
				(byte) 145);
			sendSingleLineDisplayMessage(testString40,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD26,
				(byte) 20,
				(byte) 200);
		} else if (whichTest == 6) { // Try to get mono 24. Does not quite do it. 19 is good
			sendSingleLineDisplayMessage(testString20, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD24, (byte) 12, (byte) 35);
			sendSingleLineDisplayMessage(testString20, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD24, (byte) 12, (byte) 90);
			sendSingleLineDisplayMessage(testString20,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD24,
				(byte) 12,
				(byte) 145);
			sendSingleLineDisplayMessage(testString20,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD24,
				(byte) 12,
				(byte) 200);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Helper function called by two display functions.
	 */
	private void rememberLinesSent(final String inLine1Message,
		final String inLine2Message,
		final String inLine3Message,
		final String inLine4Message) {
		doSetRecentCheDisplayString(1, inLine1Message);
		doSetRecentCheDisplayString(2, inLine2Message);
		doSetRecentCheDisplayString(3, inLine3Message);
		doSetRecentCheDisplayString(4, inLine4Message);
	}

	// --------------------------------------------------------------------------
	/**
	 * Helper function called by two display functions.
	 */
	private void logLinesSent(final String inLine1Message,
		final String inLine2Message,
		final String inLine3Message,
		final String inLine4Message) {
		String displayString = "Display message for " + getMyGuidStrForLog();
		if (!inLine1Message.isEmpty())
			displayString += " line1: " + inLine1Message;
		if (!inLine2Message.isEmpty())
			displayString += " line2: " + inLine2Message;
		if (!inLine3Message.isEmpty())
			displayString += " line3: " + inLine3Message;
		if (!inLine4Message.isEmpty())
			displayString += " line4: " + inLine4Message;
		notifyScreenDisplay(displayString);
	}

	// --------------------------------------------------------------------------
	/**
	 * A corollary to the original full screen message function. It remembers the lines,
	 * then sends a clear and several CommandControlDisplaySingleLineMessages.
	 * x offset = 26, which centers 20-character lines on the 400 pixel 2.7 inch display.
	 */
	protected void sendMonospaceDisplayScreen(final String inLine1Message,
		final String inLine2Message,
		final String inLine3Message,
		final String inLine4Message,
		final boolean largerBottomLine) {

		// Remember that we are trying to send, even before the association check. Want this to work in unit tests.
		rememberLinesSent(inLine1Message, inLine2Message, inLine3Message, inLine4Message);

		logLinesSent(inLine1Message, inLine2Message, inLine3Message, inLine4Message); // log even if no association. This is the only logging for remote linked CHE

		clearDisplay();

		sendSingleLineDisplayMessage(inLine1Message, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 26, (byte) 35);
		sendSingleLineDisplayMessage(inLine2Message, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 26, (byte) 90);
		sendSingleLineDisplayMessage(inLine3Message, CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20, (byte) 26, (byte) 145);
		if (largerBottomLine)
			sendSingleLineDisplayMessage(inLine4Message,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD24,
				(byte) 12,
				(byte) 200);
		else
			sendSingleLineDisplayMessage(inLine4Message,
				CommandControlDisplaySingleLineMessage.ARIALMONOBOLD20,
				(byte) 26,
				(byte) 200);
	}

	// --------------------------------------------------------------------------
	/**
	 * This was our original full screen message function. It remembers the lines,
	 * then sends a CommandControlDisplayMessage.
	 * The KW2 firmware automatically displays line 1 in ARIAL26, and lines 2-4 in ARIAL16.
	 */
	protected void sendDisplayCommand(final String inLine1Message,
		final String inLine2Message,
		final String inLine3Message,
		final String inLine4Message) {

		// Remember that we are trying to send, even before the association check. Want this to work in unit tests.
		rememberLinesSent(inLine1Message, inLine2Message, inLine3Message, inLine4Message);

		logLinesSent(inLine1Message, inLine2Message, inLine3Message, inLine4Message); // log the lines even if no association. This is the only logging for remote che

		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT,
			inLine1Message,
			inLine2Message,
			inLine3Message,
			inLine4Message);

		sendScreenCommandToMyChe(command);
		sendScreenCommandToLinkFromChe(command);

	}

	protected void clearDisplay() {
		ICommand command = new CommandControlClearDisplay(NetEndpoint.PRIMARY_ENDPOINT);
		sendScreenCommandToMyChe(command);
		sendScreenCommandToLinkFromChe(command);
	}

	protected boolean wiMatchesItemLocation(String matchItem, String matchPickLocation, WorkInstruction wiToCheck) { // used for DEV-691, DEV-692
		// decide not to also do uom check here. Would be very strange if a customer had different UOMs in the same pick location.

		if (matchItem == null || matchPickLocation == null) // this clause used for DEV-451 in selectNextActivePicks()
			return false;

		if (wiToCheck.isHousekeeping()) // they actually may match the getItemId and getPickInstruction values
			return false;

		if (matchItem.equals(wiToCheck.getItemId()))
			if (matchPickLocation.equals(wiToCheck.getPickInstruction()))
				return true;
		return false;
	}

	protected boolean isAPutState() {
		CheStateEnum state = getCheStateEnum();
		return state.equals(CheStateEnum.DO_PUT) || state.equals(CheStateEnum.SHORT_PUT)
				|| state.equals(CheStateEnum.SHORT_PUT_CONFIRM);
	}

	// --------------------------------------------------------------------------
	/**
	 * The answer comes from mActivePickWiList, as this can work also for putwall.
	 * For simultaneous picks, it might come from active picks list to avoid the possibility of failure cases below. But we still need to 
	 * work from mActivePickWiList if PICKMULT is off.
	 */
	private int getTotalCountSameSkuLocation(WorkInstruction inWi) {
		if (inWi == null) {
			LOGGER.error("null wi in getTotalCountSameSku");
			return 0;
		}

		String pickSku = inWi.getItemId();
		String pickLocation = inWi.getPickInstruction();
		int totalQty = 0;

		boolean putwallCase = isAPutState();

		// using mAllPicksWiList, we expect same item, same pick location, uncompleted, unshorted.
		// this will find and match the inWi also.
		// for put wall case, we started with item scan, so all match the item. The locations are different for put wall.
		for (WorkInstruction wi : mAllPicksWiList) {
			WorkInstructionStatusEnum theStatus = wi.getStatus();
			if (theStatus == WorkInstructionStatusEnum.INPROGRESS || theStatus == WorkInstructionStatusEnum.NEW)

				if (putwallCase || wiMatchesItemLocation(pickSku, pickLocation, wi)) {
					totalQty += wi.getPlanQuantity();
				}
			/* This code makes the huge assumption that the work sequencer is very rational. A case that would fail is:
			 * Pick item from slot A, sequence 1. Count 5.
			 * Pick item from slot A, sequence 2. Count 1.
			 * Pick other item from slot B, sequence 3. Count 2
			 * Back to slot A, sequence 4 for the same item. Count 3.
			 * This would fail by showing total count 9 for the first pick, even though the count for that group of pick from that location is only 6
			*/
		}
		return totalQty;
	}

	// --------------------------------------------------------------------------
	/**
	 * What "count" shall we show for a job? The answer is surprisingly tricky.
	 * If there are 3 jobs in a row for the same SKU for 2 each, multipick or not, should it show as "2" or "6" or "2 of 6".
	 * For picking, we want the total count of that sku in that location, whether multipick or not. That is "6"
	 * For put wall, we never have simultaneous puts. We want to show "2 of 6"
	 */
	protected String getWICountStringForCheDisplay(WorkInstruction inWi) {
		if (inWi.isHousekeeping()) {
			return "";
		}
		// If multi-pick, we are showing the first of active picks list, but all active picks have poscons lit. We want the total pick count.
		// If not multi-pick, one CHE poscon at a time is lit, but the screen shows the pick location. Do we prefer "QTY 9" or "QTY 3 of 9".  The worker will take 9 from that location.
		// For a put wall, if we showed "P15"  "QTY 9" , it would be misleading if only 3 are supposed to go to P15. We definitely want "QTY 3 of 9" for put wall.
		// Back to picking. If PICKMULT, "QTY 3 of 9" is not so good because all poscons are lit, and the the one we represent as first needing only 3 is arbitrary.
		// If not PICKMULT "QTY 3 of 9" is arguably better than "9", except for the inconsistency. For now, not doing it.

		Integer planQty = inWi.getPlanQuantity();
		Integer totalQtyThisSku = getTotalCountSameSkuLocation(inWi);
		String returnStr;

		if (planQty >= totalQtyThisSku)
			returnStr = planQty.toString();
		else {
			if (isAPutState())
				returnStr = String.format("%d of %d", planQty, totalQtyThisSku);
			else
				returnStr = totalQtyThisSku.toString();
		}

		return returnStr;
	}

	// --------------------------------------------------------------------------
	/**
	 * Breakup the description into three static lines no longer than 20 characters.
	 * Except the last line can be up to 40 characters (since it scrolls).
	 * Important change from v3. If quantity > 98, then tweak the description adding the count to the start.
	 * @param inPickInstructions
	 * @param inDescription
	 */
	private void sendDisplayWorkInstruction(WorkInstruction wi) {
		String planQtyStr = getWICountStringForCheDisplay(wi);
		boolean skipQtyDisplay = WiPurpose.WiPurposeSkuWallPut.equals(wi.getPurpose());

		String[] pickInfoLines = { "", "", "" };

		if ("Both".equalsIgnoreCase(mDeviceManager.getPickInfoValue())) {
			//First line is SKU, 2nd line is desc + qty if >= 99
			String info = wi.getItemId();

			//Make sure we do not exceed 40 chars
			if (info.length() > 40) {
				LOGGER.warn("Truncating WI SKU that exceeds 40 chars {}", wi);
				info = info.substring(0, 40);
			}

			pickInfoLines[0] = info;

			String displayDescription = wi.getDescription();
			if (!planQtyStr.isEmpty() && !skipQtyDisplay) {
				displayDescription = planQtyStr + " " + displayDescription;
			}

			//Add description
			int charPos = 0;
			for (int line = 1; line < 3; line++) {
				if (charPos < displayDescription.length()) {
					int toGet = Math.min(20, displayDescription.length() - charPos);
					pickInfoLines[line] = displayDescription.substring(charPos, charPos + toGet);
					charPos += toGet;
				}
			}

		} else if ("Description".equalsIgnoreCase(mDeviceManager.getPickInfoValue())) {

			String displayDescription = wi.getDescription();
			if (displayDescription == null) {
				displayDescription = "";
			}
			if (!planQtyStr.isEmpty() && !skipQtyDisplay) {
				displayDescription = planQtyStr + " " + displayDescription;
			}

			int pos = 0;
			for (int line = 0; line < 3; line++) {
				if (pos < displayDescription.length()) {
					int toGet = Math.min(20, displayDescription.length() - pos);
					pickInfoLines[line] = displayDescription.substring(pos, pos + toGet);
					pos += toGet;
				}
			}

			// Check if there is more description to add to the last line.
			if (pos < displayDescription.length()) {
				int toGet = Math.min(20, displayDescription.length() - pos);
				pickInfoLines[2] += displayDescription.substring(pos, pos + toGet);
			}
		} else {
			//DEFAULT TO SKU
			//First line is SKU, 2nd line is QTY if >= 99
			String info = wi.getItemId();

			//Make sure we do not exceed 40 chars
			if (info.length() > 40) {
				LOGGER.warn("Truncating WI SKU that exceeds 40 chars {}", wi);
				info = info.substring(0, 40);
			}

			pickInfoLines[0] = info;

			String quantity = "";
			if (!planQtyStr.isEmpty() && !skipQtyDisplay) {
				quantity = "QTY " + planQtyStr;
			}

			//Make sure we do not exceed 40 chars
			if (quantity.length() > 40) {
				LOGGER.warn("Truncating WI Qty that exceeds 40 chars {}", wi);
				quantity = quantity.substring(0, 40);
			}

			pickInfoLines[1] = quantity;
		}

		// get "DECREMENT POSITION" or other instruction
		pickInfoLines[2] = getFourthLineDisplay(wi);

		// Note: pickInstruction is more or less a location. Commonly a location alias, but may be a locationId or DDcId.
		// GoodEggs many locations orders hitting too long case
		String cleanedPickInstructions = wi.getPickInstruction();
		if (cleanedPickInstructions.length() > 19) {
			cleanedPickInstructions = cleanedPickInstructions.substring(0, 19);
		}

		sendDisplayCommand(cleanedPickInstructions, pickInfoLines[0], pickInfoLines[1], pickInfoLines[2]);

	}

	// --------------------------------------------------------------------------
	/**
	 * trying to have the fourth line only depend on the state. We might throw additional messaging here if necessary.
	 */
	private String getFourthLineDisplay(WorkInstruction wi) {
		if (CheStateEnum.SHORT_PICK == mCheStateEnum || CheStateEnum.SHORT_PUT == mCheStateEnum) {
			return "DECREMENT POSITION";
		} else if (CheStateEnum.SCAN_SOMETHING == mCheStateEnum) {
			// kind of funny. States are uniformly defined, so this works even from wrong object
			ScanNeededToVerifyPick scanVerification = getScanVerificationTypeEnum();
			if (wi.equals(verifyWi)) {
				switch (scanVerification) {
					case SKU_SCAN_TO_VERIFY:
						return "SCAN " + wi.getItemId();
					default:
						String gtin = wi.getGtin();

						return "SCAN " + ((gtin == null || gtin.isEmpty()) ? wi.getItemId() : gtin);
				}
			} else {
				switch (scanVerification) {
					case SKU_SCAN_TO_VERIFY:
						return "SCAN SKU NEEDED";
					case UPC_SCAN_TO_VERIFY:
						return "SCAN UPC NEEDED";
					case LPN_SCAN_TO_VERIFY: // not implemented
						return "SCAN LPN NEEDED";
					default:
						return "SCAN ITEM NEEDED";
				}
			}
		}
		return "";
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a light command for this CHE on the specified LED controller.
	 * @param inPosition
	 */
	protected void ledControllerSetLed(final NetGuid inControllerGuid,
		final Short inChannel,
		final LedSample inLedSample,
		final EffectEnum inEffect) {

		// The caller logs more concisely. This would log each LED separately.
		// LOGGER.info("Light position: " + inLedSample.getPosition() + " color: " + inLedSample.getColor());
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			LedCmd cmd = aisleDevice.getLedCmdFor(getGuid(), inChannel, inLedSample.getPosition());
			if (cmd == null) {
				aisleDevice.addLedCmdFor(getGuid(), inChannel, inLedSample, inEffect);
			}
		}

		// Remember any aisle controller we sent messages with lights to.
		if (!(device instanceof CheDeviceLogic)) {
			setLastLedControllerGuid(inControllerGuid);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * After we've set all of the LEDs, tell the controller to broadcast them updates.
	 * @param inControllerGuid
	 */
	protected void ledControllerShowLeds(final NetGuid inControllerGuid) {
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inControllerGuid);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			aisleDevice.updateLeds();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Called at logout. (or the multiple case below). Trying to provide a means to self-correct.
	 * Clears aisle controllers only, not position controllers on this CHE
	 * Side effect: clears its information on what was previously sent to.
	 */
	private void forceClearAllLedsForThisCheDevice() {
		List<AisleDeviceLogic> aList = mDeviceManager.getAisleControllers();
		for (AisleDeviceLogic theAisleDevice : aList) {
			theAisleDevice.removeLedCmdsForCheAndSend(getGuid());
		}
		clearLastLedControllerGuids(); // Setting the state that we have nothing more to clear for this CHE.		
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear poscons controlled by other devices set by original request from this device.
	 * This does nothing for this CHE's own poscons.
	 */
	protected void forceClearOtherPosConControllersForThisCheDevice() {
		List<PosManagerDeviceLogic> controllers = mDeviceManager.getPosConControllers();
		for (PosManagerDeviceLogic controller : controllers) {
			controller.removePosConInstrsForSource(getGuid());
			controller.updatePosCons();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear the LEDs for this CHE one whatever LED controllers it last sent to.
	 * Side effect: clears its information on what was previously sent to.
	 * @param inGuid
	 */
	protected void ledControllerClearLeds() {
		// Clear the LEDs for the last location(s) the CHE worked.
		NetGuid aisleControllerGuid = getLastLedControllerGuid();
		if (aisleControllerGuid == null)
			return;

		if (wereMultipleLastLedControllerGuids()) {
			// check all
			LOGGER.warn("Needing to clear multiple aisle controllers for one CHE device clear. This case should be unusual.");
			forceClearAllLedsForThisCheDevice();
		}
		// Normal case. Just clear the one aisle device we know we sent to last.
		else {
			INetworkDevice device = mDeviceManager.getDeviceByGuid(aisleControllerGuid);
			if (device instanceof AisleDeviceLogic) {
				AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
				aisleDevice.removeLedCmdsForCheAndSend(getGuid());
			}
			clearLastLedControllerGuids(); // Setting the state that we have nothing more to clear for this CHE.
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Clear the LEDs for this CHE on this specified LED (aisle) controller.
	 * No side effect: does not clears its information on what was previously sent to.
	 * This is called on completing one work instruction only. The work instruction knows its aisle controller (in theory)
	 * Would only be necessary for simultaneous multiple WI dispatch. Otherwise, could just as well call the above
	 * @param inGuid
	 */
	protected void ledControllerClearLeds(final NetGuid inLedControllerId) {
		// Clear the LEDs for the last location the CHE worked.
		INetworkDevice device = mDeviceManager.getDeviceByGuid(inLedControllerId);
		if (device instanceof AisleDeviceLogic) {
			AisleDeviceLogic aisleDevice = (AisleDeviceLogic) device;
			aisleDevice.removeLedCmdsForCheAndSend(getGuid());
		}

		clearLastLedControllerGuids(); // Not sure this is right!!! Need tests in CD_0043		
	}

	// --------------------------------------------------------------------------
	/* 
	 * This happens after reassociate. Warning: restartEnum may be null!
	 */
	@Override
	public final void startDevice(DeviceRestartCauseEnum restartEnum) {
		LOGGER.info("Start CHE controller (after association) ");

		// setState(mCheStateEnum);  Always, after start, there is the device associate chain and redisplay which will call setState(mCheStateEnum);
		setLastIncomingAckId((byte) 0);

		// Let's force a short wait after associate
		setLastRadioCommandSendForThisDevice(System.currentTimeMillis());

		if (restartEnum != null && restartEnum == DeviceRestartCauseEnum.USER_RESTART) {
			adjustStateForUserReset();
		}
	}

	// --------------------------------------------------------------------------
	/* 
	 * Called by CheDeviceLogic.startDevice() if the cause was a user reset
	 */
	protected void adjustStateForUserReset() {
		// Do nothing, as this is really an abstract class. 
		// Could handle verifying badge state here I suppose
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
				PosControllerInstr.BITENCODED_SEGMENTS_CODE,
				PosControllerInstr.BITENCODED_LED_DASH,
				PosControllerInstr.BITENCODED_LED_DASH,
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
	/* 
	 */
	public void processNonCommandScan(String inScanPrefixStr, String inContent) {
		LOGGER.error("processNonCommandScan needs override");
	}

	// --------------------------------------------------------------------------
	/* 
	 */
	@Override
	public void scanCommandReceived(String inCommandStr) {
		// TODO if passed from linked CHE, process it anyway.
		if (!connectedToServer) {
			LOGGER.debug("NotConnectedToServer: Ignoring scan command: " + inCommandStr);
			return;
		}

		// Clean up any potential newline or carriage returns.
		inCommandStr = inCommandStr.replaceAll("[\n\r]", "");

		String scanPrefixStr = getScanPrefix(inCommandStr);
		String scanStr = getScanContents(inCommandStr, scanPrefixStr);

		// If this (mobile) CHE is linked to another CHE, then pass through scans to that CHE. Except for REMOTE and LOGOUT
		boolean passToOtherChe = false;
		if (CheStateEnum.REMOTE_LINKED.equals(getCheStateEnum())) {
			passToOtherChe = true;
			if (COMMAND_PREFIX.equals(scanPrefixStr)) {
				if (REMOTE_COMMAND.equals(scanStr) || LOGOUT_COMMAND.equals(scanStr)) {
					passToOtherChe = false;
				}
			}
		}
		if (passToOtherChe) {
			passScanToLinkedChe(inCommandStr);
			return;
		}

		notifyScan(inCommandStr); // logs

		// Command scans actions are determined by the scan content (the command issued) then state because they are more likely to be state independent
		if (inCommandStr.startsWith(COMMAND_PREFIX)) {
			processCommandScan(scanStr);
		} else {
			processNonCommandScan(scanPrefixStr, scanStr);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.INetworkDevice#buttonCommandReceived(com.codeshelf.flyweight.command.CommandControlButton)
	 */
	@Override
	public void buttonCommandReceived(CommandControlButton inButtonCommand) {
		if (connectedToServer) {
			// Send a command to clear the position, so the controller knows we've gotten the button press.
			processButtonPress((int) inButtonCommand.getPosNum(), (int) inButtonCommand.getValue());
		} else {
			LOGGER.debug("NotConnectedToServer: Ignoring button command: " + inButtonCommand);
		}
	}

	protected void processNormalPick(WorkInstruction inWi, Integer inQuantity) {
		LOGGER.error("processNormalPick needs override");
	}

	// --------------------------------------------------------------------------
	/* 
	 * We call this when posting a poscon lighting message for an active WI.
	 * Note: may want to save the persistentID or something on the WI and not the reference itself.
	 * By convention, we are remembering the representation without the 0x (Change if necessary.)
	 */
	private void rememberOffChePosconWorkInstruction(String controllerId, int posconIndex, WorkInstruction inFirstWi) {
		if (controllerId == null || inFirstWi == null) {
			LOGGER.error(" null input to rememberOffChePosconWorkInstruction");
			return;
		}
		if (controllerId.length() < 2) { // not enough for a real business case, but avoids stringOutOfRange exception
			LOGGER.error(" bad input to rememberOffChePosconWorkInstruction");
			return;
		}
		String theSub = controllerId.substring(0, 2);
		if ("0x".equals(theSub)) {
			LOGGER.error("Probable coding error. See comments in rememberOffChePosconWorkInstruction()");
			// By convention, use the non- Ox form, but continue
		}

		WorkInstruction existingWi = mWiNonChePoscons.get(controllerId, posconIndex);
		if (existingWi == null) {
			mWiNonChePoscons.put(controllerId, posconIndex, inFirstWi);
		} else if (!existingWi.equals(inFirstWi)) {
			mWiNonChePoscons.remove(controllerId, posconIndex);
			mWiNonChePoscons.put(controllerId, posconIndex, inFirstWi);
		}
	}

	private WorkInstruction retrieveOffChePosconWorkInstruction(String controllerId, int posconIndex) {
		// By convention, we are remembering/retrieving the representation without the 0x. (Change if necessary.)
		if (controllerId == null || controllerId.length() < 2) {
			LOGGER.error(" bad input to retrieveOffChePosconWorkInstruction");
			return null;
		}
		if ("0x".equals(controllerId.substring(0, 2))) {
			LOGGER.error("Probable coding error. See comments in retrieveOffChePosconWorkInstruction()");
			// By convention, use the non- Ox form, but continue
		}
		return mWiNonChePoscons.get(controllerId, posconIndex);
	}

	// --------------------------------------------------------------------------
	/* 
	 * Test method callable by unit test
	 * But the functions we want to test are private. That is why this mess is here.
	 */
	public void testOffChePosconWorkInstructions() {
		LOGGER.info("START testOffChePosconWorkInstructions");
		if (mWiNonChePoscons == null) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 1");
			return;
		}

		final String controller1 = "00006661"; // by convention, we are storing the form without leading "0x in the map.
		final String controller2 = "00006662";
		final String controller3 = "0x00006663"; // this will induce and ERROR in log.
		WorkInstruction wi1 = new WorkInstruction();
		WorkInstruction wi2 = new WorkInstruction();
		WorkInstruction wi3 = new WorkInstruction();
		WorkInstruction wi4 = new WorkInstruction();

		WorkInstruction testWi = retrieveOffChePosconWorkInstruction(controller1, 4);
		if (testWi != null)
			LOGGER.error("FAIL testOffChePosconWorkInstructions 2");

		rememberOffChePosconWorkInstruction(controller1, 4, wi1);
		testWi = retrieveOffChePosconWorkInstruction(controller1, 4);
		if (!wi1.equals(testWi)) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 3");
		}
		// setting a second  to same keys should remove the first and add the second
		rememberOffChePosconWorkInstruction(controller1, 4, wi2);
		testWi = retrieveOffChePosconWorkInstruction(controller1, 4);
		if (!wi2.equals(testWi)) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 4a");
		}
		if (wi1.equals(testWi)) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 4b");
		}
		rememberOffChePosconWorkInstruction(controller1, 5, wi3);
		rememberOffChePosconWorkInstruction(controller2, 2, wi4);
		testWi = retrieveOffChePosconWorkInstruction(controller2, 5);
		if (testWi != null) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 5");
		}
		testWi = retrieveOffChePosconWorkInstruction(controller1, 5);
		if (!wi3.equals(testWi)) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 6");
		}
		testWi = retrieveOffChePosconWorkInstruction(controller2, 2);
		if (!wi4.equals(testWi)) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 7");
		}
		LOGGER.info("intentional error cases in rememberOffChePosconWorkInstruction/retrieveOffChePosconWorkInstruction");
		rememberOffChePosconWorkInstruction(null, 4, wi2);
		testWi = retrieveOffChePosconWorkInstruction(null, 4);
		if (testWi != null) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 8");
		}
		rememberOffChePosconWorkInstruction("", 4, wi2);
		testWi = retrieveOffChePosconWorkInstruction("", 4);
		if (testWi != null) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 9");
		}
		rememberOffChePosconWorkInstruction(controller1, 9, null);
		testWi = retrieveOffChePosconWorkInstruction(controller1, 9);
		if (testWi != null) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 10");
		}
		rememberOffChePosconWorkInstruction("0x1234", 9, wi2); // logs probable error, but works
		testWi = retrieveOffChePosconWorkInstruction("0x1234", 9); // logs probable error, but works
		if (testWi == null) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 11");
		}
		rememberOffChePosconWorkInstruction(controller3, 7, wi3); // logs probable error, but works
		testWi = retrieveOffChePosconWorkInstruction(controller3, 7); // logs probable error, but works
		if (testWi == null) {
			LOGGER.error("FAIL testOffChePosconWorkInstructions 12");
		}

		rememberOffChePosconWorkInstruction("0x", 9, wi2); // Just checking that we avoid the string range exception
		rememberOffChePosconWorkInstruction("0", 9, wi2); // Just checking that we avoid the string range exception

		LOGGER.info("END testOffChePosconWorkInstructions");
	}

	// --------------------------------------------------------------------------
	/* 
	 * An "off-CHE" button press came from a poscon in a location. It was routed to this CheDeviceLogic because we think this was being lit by an
	 * active work instruction for this CheDeviceLogic
	 */
	public void processOffCheButtonPress(String sourceGuid, CommandControlButton inButtonCommand) {
		if (connectedToServer) {
			WorkInstruction wi = null;

			int posconIndex = inButtonCommand.getPosNum();
			int quantity = inButtonCommand.getValue();
			notifyOffCheButton(posconIndex, quantity, sourceGuid); // This merely logs

			// find the work instruction this button was displaying?							
			wi = retrieveOffChePosconWorkInstruction(sourceGuid, posconIndex);

			if (wi != null) {
				if (quantity >= wi.getPlanMinQuantity()) {
					processNormalPick(wi, quantity);
				} else {
					// More kludge for count > 99 case
					Integer planQuantity = wi.getPlanQuantity();
					if (quantity == maxCountForPositionControllerDisplay && planQuantity > maxCountForPositionControllerDisplay)
						processNormalPick(wi, planQuantity); // Assume all were picked. No way for user to tell if more than 98 given.
					else {
						processShortPickOrPut(wi, quantity);
					}
				}
			}

		} else {
			LOGGER.debug("NotConnectedToServer: Ignoring off-CHE button command: " + inButtonCommand);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inScanStr
	 * @return
	 */
	protected String getScanPrefix(String inScanStr) {

		String result = "";

		if (inScanStr.startsWith(COMMAND_PREFIX)) {
			result = COMMAND_PREFIX;
		} else if (inScanStr.startsWith(USER_PREFIX)) {
			result = USER_PREFIX;
		} else if (inScanStr.startsWith(CONTAINER_PREFIX)) {
			result = CONTAINER_PREFIX;
		} else if (inScanStr.startsWith(LOCATION_PREFIX)) {
			result = LOCATION_PREFIX;
		} else if (inScanStr.startsWith(ITEMID_PREFIX)) {
			result = ITEMID_PREFIX;
		} else if (inScanStr.startsWith(POSITION_PREFIX)) {
			result = POSITION_PREFIX;
		} else if (inScanStr.startsWith(TAPE_PREFIX)) {
			result = TAPE_PREFIX;
		} else if (inScanStr.startsWith(CHE_NAME_PREFIX)) {
			result = CHE_NAME_PREFIX;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inScanStr
	 * @param inPrefix
	 * @return
	 */
	protected String getScanContents(String inScanStr, String inPrefix) {
		return inScanStr.substring(inPrefix.length());
	}

	// --------------------------------------------------------------------------
	/**
	 * Change state and display error message
	 * Send the LED error status as well (color: red effect: error channel: 0).
	 * 
	 */
	protected void invalidScanMsg(final CheStateEnum inCheState) {
		LOGGER.error("invalidScanMsg() override needed");
		// guessing at a start
		// mCheStateEnum = inCheState;
		//sendErrorCodeToAllPosCons();
	}

	// --------------------------------------------------------------------------
	/**
	 * Stay in the same state, but make the status invalid.
	 * Send the LED error status as well (color: red effect: error channel: 0).
	 * 
	 */
	protected void invalidScanMsg(String lineOne, String lineTwo, String lineThree, String lineFour) {
		sendDisplayCommand(lineOne, lineTwo, lineThree, lineFour);
		sendErrorCodeToAllPosCons();
	}

	/**
	 * Send Error Code to all PosCons
	 */
	protected void sendErrorCodeToAllPosCons() {
		List<PosControllerInstr> instructions = Lists.newArrayList(new PosControllerInstr(PosControllerInstr.POSITION_ALL,
			PosControllerInstr.BITENCODED_SEGMENTS_CODE,
			PosControllerInstr.BITENCODED_LED_E,
			PosControllerInstr.BITENCODED_LED_BLANK,
			PosControllerInstr.SOLID_FREQ, // change from BLINK_FREQ
			PosControllerInstr.MED_DUTYCYCLE)); // change from BRIGHT_DUTYCYCLE v6
		sendPositionControllerInstructions(instructions);
	}

	/** 
	 * Command scans are split out by command then state because they are more likely to be state independent
	 */
	protected void processCommandScan(final String inScanStr) {
		LOGGER.error("processCommandScan() needs override");
	}

	// --------------------------------------------------------------------------
	/** 
	 * logout() is the normal API
	 */
	protected void logout() {
		notifyCheWorkerVerb("LOG OUT", "");

		// if this CHE is being remotely controlled, we want to break the link.
		NetGuid linkedMobileGuid = this.getLinkedFromCheGuid();
		if (linkedMobileGuid != null) {
			breakLinkDueToLocalCheActivity(linkedMobileGuid);
		}

		//Send logout notification to server
		mDeviceManager.sendLogoutRequest(getGuid().getHexStringNoPrefix(), getPersistentId(), getUserId());

		// many side effects. Primarily clearing leds and poscons and setting state to idle
		_logoutSideEffects();
		mDeviceManager.setWorkerNameFromGuid(getGuid(), null);
	}

	// --------------------------------------------------------------------------
	/**
	 * Called by normal logout, but also called when remote mobile che unlinks or logs out.
	 */
	private void _logoutSideEffects() {
		this.setUserId(null);
		mActivePickWiList.clear();
		mAllPicksWiList.clear();
		setState(CheStateEnum.IDLE);

		clearAllPosconsOnThisDevice();
		forceClearAllLedsForThisCheDevice();

		//Clear PosConControllers. (Put walls showing order feedback.)
		forceClearOtherPosConControllersForThisCheDevice();
	}

	/**
	 * Setup the CHE by clearing all the data structures
	 */
	protected void setupChe() {
		clearAllPosconsOnThisDevice();
		setState(CheStateEnum.CONTAINER_SELECT);
	}

	protected void setupCommandReceived() {
		LOGGER.error("setupCommandReceived() needs override");
	}

	protected void processCommandCancel() {
		LOGGER.error("processCommandCancel() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned the SHORT_PICK command to tell us there is no product to pick.
	 */
	protected void shortPickCommandReceived() {
		LOGGER.error("shortPickCommandReceived() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * The user scanned YES or NO.
	 * @param inScanStr
	 */
	protected void yesOrNoCommandReceived(final String inScanStr) {
		LOGGER.error("yesOrNoCommandReceived() needs override");
	}

	protected void posconSetupCommandReveived() {
		CheStateEnum currentState = getCheStateEnum();

		switch (currentState) {
			case IDLE:
			case SETUP_SUMMARY:
			case READY:
				mDeviceManager.putPosConsInSetupMode(this);
				break;
			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The guts of the short transaction
	 * Update the WI fields, and call out to mDeviceManager to share it back to the server.
	 */
	protected void doShortTransaction(final WorkInstruction inWi, final Integer inActualPickQuantity) {
		inWi.setShortState(mUserId, inActualPickQuantity);

		// normal short will be in mActivePickWiList.
		// short-aheads will not be.
		if (mActivePickWiList.contains(inWi))
			mActivePickWiList.remove(inWi);

		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), inWi);
	}

	protected void showCartRunFeedbackIfNeeded(Byte inPosition) {
		LOGGER.error("showCartRunFeedbackIfNeeded() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * YES or NO confirm the short pick.
	 * @param inScanStr
	 */
	protected void confirmShortPick(final String inScanStr) {
		LOGGER.error("confirmShortPick() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * Our position controllers may only display up to 99. Integer overflows into bytes go negative, which have special meanings.
	 * So, any value larger than 99 shall display 99. For those cases, we show the count in the description.
	 */
	protected byte byteValueForPositionDisplay(Integer inInt) {
		if (inInt > maxCountForPositionControllerDisplay)
			return maxCountForPositionControllerDisplay.byteValue();
		else
			return inInt.byteValue();
	}

	// --------------------------------------------------------------------------
	/**
	 * Sort the command groups by their position. It is important that lower ones go out first.
	 */
	class CmdGroupComparator implements Comparator<LedCmdGroup> {

		@Override
		public int compare(LedCmdGroup inGroup1, LedCmdGroup inGroup2) {
			if (inGroup1 == null) {
				return -1;
			} else if (inGroup2 == null) {
				return 1;
			} else {
				return inGroup1.compareTo(inGroup2);
			}
		}
	};

	protected String getMyGuidStrForLog() {
		return getGuid().getHexStringNoPrefix();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 */
	protected void clearLedAndPosConControllersForWi(final WorkInstruction inWi) {
		//Clear LEDs
		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(inWi.getLedCmdStream());

		for (Iterator<LedCmdGroup> iterator = ledCmdGroups.iterator(); iterator.hasNext();) {
			LedCmdGroup ledCmdGroup = iterator.next();

			INetworkDevice ledController = mRadioController.getNetworkDevice(new NetGuid(ledCmdGroup.getControllerId()));
			if (ledController != null) {
				ledControllerClearLeds(ledController.getGuid());
			}
		}

		//Clear PutWall PosCons
		forceClearOtherPosConControllersForThisCheDevice();
	}

	// --------------------------------------------------------------------------
	/**
	 * Complete the active WI at the selected position.
	 * @param inButtonNum
	 * @param inQuantity
	 * @param buttonPosition 
	 */
	protected void processButtonPress(Integer inButtonNum, Integer inQuantity) {
		LOGGER.error("processButtonPress() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inWi
	 * @param inQuantity
	 */
	protected void processShortPickOrPut(WorkInstruction inWi, Integer inQuantity) {
		CheStateEnum state = getCheStateEnum();
		if (state == CheStateEnum.SHORT_PICK)
			setState(CheStateEnum.SHORT_PICK_CONFIRM);
		else if (state == CheStateEnum.SHORT_PUT)
			setState(CheStateEnum.SHORT_PUT_CONFIRM);
		else if (state == CheStateEnum.SHORT_PICK_CONFIRM || state == CheStateEnum.SHORT_PUT_CONFIRM) {
			LOGGER.info("extra button press without confirmation in state {}", state);
			setState(state); // forces out the usual side effects, if any. None so far, beyond redoing the CHE display and che poscons.
		} else {
			LOGGER.error("unanticipated state in processShortPickOrPut {}", state);
			setState(state); // because we want to force che and poscon redraw.  DEV-1318 (this part not in v24
		}
		mShortPickWi = inWi;
		mShortPickQty = inQuantity;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param totalWorkInstructionCount
	 * @param containerToWorkInstructionCountMap - Map containerIds to WorkInstructionCount objects
	 * Not final only because we let CsDeviceManager call this generically.
	 */
	public void processWorkInstructionCounts(final Integer totalWorkInstructionCount,
		final Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {
		LOGGER.error("Inappropriate call to processWorkInstructionCounts()");
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
	public void assignWork(final List<WorkInstruction> inWorkItemList, final String message) {
		LOGGER.error("Inappropriate call to assignWork()");
	}

	public void processStateSetup(HashMap<String, Integer> positionMap) {
		LOGGER.error("Inappropriate call to processStateSetup()");
	}

	public void assignWallPuts(final List<WorkInstruction> inWorkItemList, String wallType, String wallName) {
		LOGGER.error("Inappropriate call to assignWallPuts()");
	}

	// --------------------------------------------------------------------------
	/**
	 * return the button for this container ID. Mostly private use, but public for unit test convenience
	 * we let CsDeviceManager call this generically for CheDeviceLogic
	 */
	public byte getPosconIndexOfWi(WorkInstruction wi) {
		LOGGER.error("Inappropriate call to getPosconIndexOfWi()");
		return 0;
	}

	// --------------------------------------------------------------------------

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("netGuid", getGuid()).toString();
	}

	/**
	 *  Currently, these cannot be longer than 20 characters.
	 */
	protected static String cheLine(String message) {
		Preconditions.checkNotNull(message, "Message cannot be null");
		Preconditions.checkArgument(message.length() <= 20, "Message '%s' will not fit on che display", message);
		return Strings.padEnd(message, 20 - message.length(), ' ');
	}

	public void disconnectedFromServer() {
		connectedToServer = false;
		sendDisplayCommand("Server Connection", "Unavailable", "Please Wait...", "");
	}

	public void connectedToServer() {
		connectedToServer = true;
		this.clearAllPosconsOnThisDevice();
		redisplayState();
		resetStateCounter(); // because we may get several overlapping associate calls
	}

	private void resetStateCounter() {
		setSetStateStackCount(0);
	}

	private void redisplayState() {
		setState(mCheStateEnum);
	}

	// --------------------------------------------------------------------------
	/**
	 * Determine if the mActivePickWiList represents a housekeeping move. If so, display it and return true
	 */
	protected boolean sendHousekeepingDisplay() {
		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Send the LED controllers the active pick locations for current wi or wis.
	 */
	protected void lightWiLocations(WorkInstruction inFirstWi) {

		String wiCmdString = inFirstWi.getLedCmdStream();
		// If the location is not configured to be lit, all of the following is a noop.
		// an empty command string is "[]"
		if (wiCmdString.equals("[]")) {
			return;
		}

		final int kMaxLedSetsToLog = 20;
		LOGGER.info("deserialize and send out this WI cmd string: " + wiCmdString);
		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(wiCmdString);
		if (!LedCmdGroupSerializer.verifyLedCmdGroupList(ledCmdGroups))
			LOGGER.error("WI cmd string did not deserialize properly");

		// It is important to sort the CmdGroups.
		Collections.sort(ledCmdGroups, new CmdGroupComparator());

		INetworkDevice lastLedController = null;
		// This is not about clearing controllers/channels this CHE had lights on for.  Rather, it was about iterating the command groups and making sure
		// we do not clear out the first group when adding on a second. This is a concern for simultaneous multiple dispatch--not currently done.

		String myGuidStr = getMyGuidStrForLog();

		for (Iterator<LedCmdGroup> iterator = ledCmdGroups.iterator(); iterator.hasNext();) {
			LedCmdGroup ledCmdGroup = iterator.next();

			// The WI's ledCmdStream includes the controller ID. Usually only one command group per WI. So, we are setting ledController as the aisleDeviceLogic for the next WI's lights
			NetGuid nextControllerGuid = new NetGuid(ledCmdGroup.getControllerId());
			INetworkDevice ledController = mRadioController.getNetworkDevice(nextControllerGuid);

			if (ledController != null) {
				Short cmdGroupChannnel = ledCmdGroup.getChannelNum();
				if (cmdGroupChannnel == null || cmdGroupChannnel == 0) {
					String wiInfo = inFirstWi.getGroupAndSortCode() + "--  item: " + inFirstWi.getItemId() + "  cntr: "
							+ inFirstWi.getContainerId();
					LOGGER.error("Bad channel after deserializing LED command from the work instruction for sequence" + wiInfo);
					continue;
				}

				Short startLedNum = ledCmdGroup.getPosNum();
				Short currLedNum = startLedNum;

				// Clear the last LED commands to this controller if the last controller was different.
				if ((lastLedController != null) && (!ledController.equals(lastLedController))) {
					ledControllerClearLeds(nextControllerGuid);
					lastLedController = ledController;
				}

				NetGuid ledControllerGuid = ledController.getGuid();
				String controllerGuidStr = ledControllerGuid.getHexStringNoPrefix();
				// short cmdGroupChannnel = ledCmdGroup.getChannelNum();
				String toLogString = "CHE " + myGuidStr + " telling " + controllerGuidStr + " to set LEDs. " + EffectEnum.FLASH;
				Integer setCount = 0;
				for (LedSample ledSample : ledCmdGroup.getLedSampleList()) {

					// why are we doing this? Aren't the samples made correctly?
					ledSample.setPosition(currLedNum++);

					// Add this LED display to the aisleController. We are accumulating the log information here rather than logging separate in the called routine.
					ledControllerSetLed(ledControllerGuid, cmdGroupChannnel, ledSample, EffectEnum.FLASH);

					// Log concisely instead of each ledCmd individually
					setCount++;
					if (setCount <= kMaxLedSetsToLog)
						toLogString = toLogString + " " + ledSample.getPosition() + ":" + ledSample.getColor();
				}
				if (setCount > 0)
					notifyLeds(toLogString);
				if (setCount > kMaxLedSetsToLog)
					notifyLeds("And more LED not logged. Total LED Sets this update = " + setCount);

				if (ledController.isDeviceAssociated()) {
					ledControllerShowLeds(ledControllerGuid);
				}
			}
		}

	}

	/**
	 * This che is looking at the wi, and seeing a poscon lighting instruction. Send it to the appropriate controller.
	 */
	protected void lightWiPosConLocations(WorkInstruction inFirstWi) {
		String wiCmdString = inFirstWi.getPosConCmdStream();
		if (wiCmdString == null || wiCmdString.equals("[]")) {
			return;
		}

		/* Walmart palletizer, and later Walmart break pack sorter, light LEDs and the local CHE poscon,
		 * However, I suppose there could be a reduced poscon putwall configuration with a bay poscon
		String ledCmdString = inFirstWi.getLedCmdStream();
		if (ledCmdString != null && !ledCmdString.isEmpty() && !ledCmdString.equals("[]")) {
			LOGGER.error("lightWiPosConLocations: WI  at {} with both poscon and wi lighting instructions. How?",
				inFirstWi.getPickInstruction());
			LOGGER.error("poscon stream: {}", wiCmdString);
			LOGGER.error("led stream: {}", ledCmdString);
			// Note: someday we will light the bay with a ping pong ball, so then this will not be an error. For now, looks like a bug.
		}
		*/

		NetGuid cheGuid = getGuid();
		List<PosControllerInstr> instructions = PosConInstrGroupSerializer.deserializePosConInstrString(wiCmdString);
		HashSet<PosManagerDeviceLogic> controllers = new HashSet<>();
		int instructionCount = 0;
		int controllerCount = 0;
		for (PosControllerInstr instruction : instructions) {
			instructionCount++;
			if (instructionCount > 1) {
				LOGGER.warn("More than one poscon instruction for a single WI");
			}
			String controllerId = instruction.getControllerId();
			int posconIndex = instruction.getPosition();
			NetGuid thisGuid = new NetGuid(controllerId);
			String thisGuidStr = thisGuid.getHexStringNoPrefix();
			INetworkDevice device = mDeviceManager.getDeviceByGuid(thisGuid);
			if (device instanceof PosManagerDeviceLogic) {
				PosManagerDeviceLogic controller = (PosManagerDeviceLogic) device;

				// This is kind of confusing. The server set up the instruction and stored it in the work instruction. But now we may have a short state
				// So we have to modify the instruction. Only this CHE knows the state.
				if (getCheStateEnum().equals(CheStateEnum.SHORT_PUT)) {
					PosControllerInstr revisedInstruction = PosControllerInstr.getCorrespondingShortDisplay(instruction);
					controller.addPosConInstrFor(cheGuid, revisedInstruction);
				} else {
					controller.addPosConInstrFor(cheGuid, instruction);
				}
				// As this is the point of adding the information to the PosManagerDeviceLogic, this should be the point of 
				// remembering and associating this send to a specific active WI.
				notifyNonChePosconLight(thisGuidStr, posconIndex, inFirstWi);

				// LOGGER.info("remember {} {}", thisGuidStr, posconIndex); // useful debug
				rememberOffChePosconWorkInstruction(thisGuidStr, posconIndex, inFirstWi);

				controllerCount++;
				if (controllerCount > 1) {
					LOGGER.warn("More than one poscon controller for a single WI");
				}
				controllers.add(controller);
			}
		}

		for (PosManagerDeviceLogic controller : controllers) {
			controller.updatePosCons();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Get the work instruction that is active on the CHE now
	 * Note: after we implement simultaneous work instructions, this should still be valid. If mActivePickWiList.size() > 1, then
	 * all the the work instructions will be for the same SKU from the same location.
	 * It is only lighting the poscons where we need to look at all of the active pick list.
	 */
	protected WorkInstruction getOneActiveWorkInstruction() {
		WorkInstruction firstWi = null;
		if (mActivePickWiList.size() > 0) {
			// The first WI has the SKU and location info.
			firstWi = mActivePickWiList.get(0);
		}
		return firstWi;
	}

	// --------------------------------------------------------------------------
	/**
	 * For the work instruction that's active on the CHE now:
	 * 1) Clear out prior ledController lighting for this CHE
	 * 2) Send the CHE display
	 * 3) Send new ledController lighting instructions
	 * 4) Send poscon display instructions
	 */
	protected void showActivePicks() {

		// The first WI has the SKU and location info.
		WorkInstruction firstWi = getOneActiveWorkInstruction();
		if (firstWi != null) {
			// Send the CHE a display command (any of the WIs has the info we need).
			CheStateEnum currentState = getCheStateEnum();
			if (currentState != CheStateEnum.DO_PICK && currentState != CheStateEnum.SHORT_PICK
					&& currentState != CheStateEnum.SCAN_SOMETHING && currentState != CheStateEnum.DO_PUT
					&& currentState != CheStateEnum.SHORT_PUT) {
				LOGGER.error("unanticipated state in showActivePicks: {}", currentState);
				setState(CheStateEnum.DO_PICK);
				//return because setting state to DO_PICK will call this function again
				return;
			}

			// Tell the last aisle controller this device displayed on, to remove any led commands for this.
			ledControllerClearLeds();

			// This part is easy. Just display on the CHE controller
			sendDisplayWorkInstruction(firstWi);

			// Tell aisle controller(s) what to light next
			lightWiLocations(firstWi);

			forceClearOtherPosConControllersForThisCheDevice();
			lightWiPosConLocations(firstWi);

			// This can be elaborate. For setup_Orders work mode, as poscons complete their work, they show their status.
			doPosConDisplaysforActiveWis();
			// If and when we do simultaneous picks, we will deal with the entire mActivePickWiList instead of only firstWI.
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Get one poscon instruction for a Wi that does not need completed, feedback, type display. But does deal with short flashing display.
	 */
	protected PosControllerInstr getPosInstructionForWiAtIndex(WorkInstruction inWi, Byte inPosconIndex) {

		byte planQuantityForPositionController = byteValueForPositionDisplay(inWi.getPlanQuantity());
		byte minQuantityForPositionController = byteValueForPositionDisplay(inWi.getPlanMinQuantity());
		byte maxQuantityForPositionController = byteValueForPositionDisplay(inWi.getPlanMaxQuantity());
		if (getCheStateEnum() == CheStateEnum.SHORT_PICK)
			minQuantityForPositionController = byteValueForPositionDisplay(0); // allow shorts to decrement on position controller down to zero

		byte freq = PosControllerInstr.BLINK_FREQ;
		byte brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
		if (this.getCheStateEnum().equals(CheStateEnum.SCAN_SOMETHING)) { // a little weak feedback that the poscon button press will not work
			//brightness = PosControllerInstr.MIDDIM_DUTYCYCLE;
			freq = PosControllerInstr.RAPIDBLINK_FREQ;
		}

		// solid is an indicator that decrement button is active, usually as a consequence of short pick. (Max difference is also possible for discretionary picks)
		if (planQuantityForPositionController != minQuantityForPositionController
				|| planQuantityForPositionController != maxQuantityForPositionController) {
			freq = PosControllerInstr.SOLID_FREQ;
			brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
		}

		PosControllerInstr instruction = new PosControllerInstr(inPosconIndex,
			planQuantityForPositionController,
			minQuantityForPositionController,
			maxQuantityForPositionController,
			freq,
			brightness);

		return instruction;
	}

	// --------------------------------------------------------------------------
	/**
	 * Send to the LED controller the active picks for the work instruction that's active on the CHE now.
	 */
	protected void doPosConDisplaysforActiveWis() {
		LOGGER.error("doPosConDisplaysforActiveWis() needs override");
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	private String verifyWiField(final WorkInstruction inWi, String inScanStr) {
		// If the user scanned SKIPSCAN return true
		if (inScanStr.equals(SCAN_SKIP) || inScanStr.equals(SKIP_SCAN)) {
			notifyWiVerb(inWi, WorkerEvent.EventType.SKIP_ITEM_SCAN, kLogAsWarn);
			return "";
		}
		String sku = inWi.getItemId();
		String gtin = inWi.getGtin();
		String pickInstructionLocation = inWi.getPickInstruction();
		if (inScanStr.equals(sku)) {
			return "";
		}
		if (gtin == null || gtin.isEmpty()) {
			return String.format("Scan mismatch at %s: expected sku %s (no upc found), received %s",
				pickInstructionLocation,
				sku,
				inScanStr);
		}
		if (inScanStr.equals(gtin)) {
			return "";
		}
		return String.format("Scan mismatch at %s: expected sku %s or upc %s, received %s",
			pickInstructionLocation,
			sku,
			gtin,
			inScanStr);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	protected void processVerifyScan(final String inScanPrefixStr, String inScanStr) {
		WorkInstruction wi = getOneActiveWorkInstruction();
		if (inScanPrefixStr.isEmpty()) {
			if (wi == null) {
				LOGGER.error("unanticipated no active WI in processVerifyScan");
				invalidScanMsg(mCheStateEnum);
				return;
			}
			String errorStr = verifyWiField(wi, inScanStr);
			if (errorStr == null || errorStr.isEmpty()) {
				// clear usually not needed. Only after correcting a bad scan
				clearAllPosconsOnThisDevice();
				setState(CheStateEnum.DO_PICK);
			} else {
				// user "errors" are logged as warn. Progamming errors are logged as error
				LOGGER.warn(errorStr); // TODO get this to the CHE display
				invalidScanMsg(mCheStateEnum);
				verifyWi = wi;
				sendDisplayWorkInstruction(getOneActiveWorkInstruction());
			}
		} else {
			// Want some feedback here. Tell the user to scan something
			LOGGER.info("Need to confirm by scanning the UPC "); // TODO later look at the class enum and decide on SKU or UPC or LPN or ....
			invalidScanMsg(mCheStateEnum);
			verifyWi = wi;
			sendDisplayWorkInstruction(getOneActiveWorkInstruction());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * This is a scan while in DO_PICK state. Usual case is this came after an unnecessary scan
	 */
	protected void processPickVerifyScan(final String inScanPrefixStr, String inScanStr) {
		WorkInstruction wi = getOneActiveWorkInstruction();
		if (inScanPrefixStr.isEmpty()) {
			if (wi == null) {
				LOGGER.error("unanticipated no active WI in processPickVerifyScan");
				invalidScanMsg(mCheStateEnum);
				return;
			}
			// only reevaluate if the wi needs a scan
			if (!wi.getNeedsScan()) {
				setState(getCheStateEnum()); // forces redraw
				return;
			}

			String errorStr = verifyWiField(wi, inScanStr);
			if (errorStr == null || errorStr.isEmpty()) {
				// clear usually not needed. Only after correcting a bad scan
				notifyExtraInfo("Unnecessary extra scan of correct item/UPC", kLogAsInfo);
				clearAllPosconsOnThisDevice();
				setState(CheStateEnum.DO_PICK);
			} else {
				notifyExtraInfo("Unanticipated extra scan; incorrect item/UPC. Changing state back", kLogAsWarn);

				// Still a problem here. Worker had done a scan and did not need another, then scanned wrong one. We want to basically forget
				// The worker had done the good scan. However, that is remembered by the complete work instruction, so it cannot be forgotten.
				setState(CheStateEnum.SCAN_SOMETHING);

			}
		} else {
			// Just redraw current screen?
			setState(getCheStateEnum());
		}
	}

	public void processResultOfVerifyBadge(Boolean verified, String workerId) {
		// To be overridden by SetupOrderDeviceLogic and LineScanDeviceLogic
	}

	/**
	 * Implement some or most of the remote business at CheDeviceLogic level so that future CHE applications can automatically remote.
	 */
	@Override
	public boolean needUpdateCheDetails(NetGuid cheDeviceGuid, String cheName, byte[] linkedToCheGuid) {
		// TODO update internals

		return false;
	}

	/**
	 * Show if we are linked, and give instructions on how to link. Screen will show as
	 * Linked to: (none)
	 * Scan Che name to link
	 * 
	 * CANCEL to exit
	 * 
	 * or
	 * Linked to: CHE3
	 * Scan Che name to link
	 * or REMOTE to unlink
	 * CANCEL to exit
	 */
	protected void sendRemoteStateScreen() {
		String cheName = getLinkedToCheName();
		boolean wasNull = false;
		if (cheName == null) {
			cheName = "(none)";
			wasNull = true;
		}
		// TODO localize
		String line1 = String.format("Linked to: %s", cheName);
		String line2 = "Scan CHE name to link";
		String line3 = "";
		String line4 = "";
		if (!wasNull) {
			line3 = "or CANCEL to unlink";
			line4 = "REMOTE to keep link";
		} else {
			line4 = "CANCEL to exit";
		}

		sendDisplayCommand(line1, line2, line3, line4);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send the websocket message to clear
	 * For DEV-843, 844.
	 */
	protected void unlinkRemoteCheAssociation() {
		// Not so great. Does the cheDeviceLogic locals in advance. Pretty valid for unlink as it is the unlikely the server will not comply.
		// There is no good place to put it on the associateRemoteChe response as by then the values are changed and we can not tell the linkee to clear itself.
		unLinkLocalVariables();

		setState(CheStateEnum.REMOTE_PENDING);

		mDeviceManager.linkRemoteChe(getGuid().getHexStringNoPrefix(), getPersistentId(), null);
	}

	// --------------------------------------------------------------------------
	/**
	 * Send the websocket message to set
	 * For DEV-843, 844.
	 */
	private void linkRemoteCheAssociation(String cheName) {
		if (cheName == null) {
			LOGGER.error("Bug? Or use unlinkRemoteCheAssociation");
		}
		setState(CheStateEnum.REMOTE_PENDING); // forces screen redraw. Later, send the message and go to REMOTE_PENDING state.

		mDeviceManager.linkRemoteChe(getGuid().getHexStringNoPrefix(), getPersistentId(), cheName);
		// sends a command. Ultimately returns back the newly linked che, or old one if there was a validation failure

	}

	protected void processCheLinkScan(String inScanPrefixStr, String inContent) {
		if (CHE_NAME_PREFIX.equals(inScanPrefixStr)) {
			linkRemoteCheAssociation(inContent);
		} else {
			LOGGER.warn("Not a CHE scan:{}{} ", inScanPrefixStr, inContent);
			invalidScanMsg(mCheStateEnum);
		}

	}

	/**
	 * This is called as a result of AssociateRemoteCheResponse. We need to transition off of remote_Pending state,
	 * and update our local variables.
	 * One complexity: If we were linked to one CHE then
	 * - Now linked to none: make sure the cart CHE goes to idle
	 * - Now linked to different cart CHE: make sure first cart CHE goes to idle. (See test remoteToDifferentCart).
	 */
	public void maintainLink(String linkCheName) {
		LOGGER.debug("maintainLink called with {}", linkCheName);
		if (!CheStateEnum.REMOTE_PENDING.equals(this.getCheStateEnum())) {
			LOGGER.error("Incorrect state in maintainLink. How? State is {}", getCheStateEnum());
		}

		String priorLinkCheName = getLinkedToCheName();
		LOGGER.info("CheDeviceLogic.maintainLink set link name: {}. Old link name was:{}", linkCheName, priorLinkCheName);

		this.setLinkedToCheName(linkCheName); // null is ok here. Means no association.
		if (linkCheName == null)
			setState(CheStateEnum.REMOTE);
		else {
			setState(CheStateEnum.REMOTE_LINKED);
		}
	}

	/**
	 * Get the CheDevice logic that this remote (mobile) CHE is linked to.
	 */
	public CheDeviceLogic getLinkedCheDevice() {
		CheDeviceLogic linkedDevice = null;
		NetGuid linkedGuid = getDeviceManager().getLinkedCheGuidFromGuid(getGuid());
		if (linkedGuid != null) {
			linkedDevice = getDeviceManager().getCheDeviceByNetGuid(linkedGuid);
		}
		return linkedDevice;
	}

	/**
	 * This is the receiving side of pass-through to the linked CHE. At this time only scans are passed through.
	 * The scanning CHE directly calls this.
	 */
	void scanReceivedFrom(NetGuid remoteCheGuid, String scanStr) {
		LOGGER.info("{} recieved from {}", scanStr, remoteCheGuid);
		// TODO: make sure this is processed, even if this CHE is offline
		scanCommandReceived(scanStr);
	}

	/**
	 * This is the sending  side of pass-through to the linked CHE. At this time only scans are passed through.
	 */
	public void passScanToLinkedChe(String scanStr) {
		LOGGER.info("passScanToLinkedChe {}", scanStr);
		CheDeviceLogic linkedDevice = getLinkedCheDevice();
		if (linkedDevice == null) {
			LOGGER.warn("passScanToLinkedChe failed to find the device. Setting state to unlinked.");
			setLinkedToCheName(null);
			setState(CheStateEnum.REMOTE);
			return;
		}
		linkedDevice.scanReceivedFrom(getGuid(), scanStr);
	}

	/**
	 * Called on entry to REMOTE_LINKED state. This is the sending side to get the screen from linked device.
	 */
	public void enterLinkedState() {
		CheDeviceLogic linkedDevice = getLinkedCheDevice();
		if (linkedDevice == null) {
			LOGGER.error("enterLinkedState failed to find the device");
			return;
		}
		// Is this is primary "link" transaction? Place the notify here.
		notifyLink(linkedDevice.getGuid());

		linkedDevice.processLink(getGuid(), getUserId());
	}

	/**
	 * Called on entry to REMOTE_LINKED state. This is the receiving side to get the screen from linked device.
	 */
	void processLink(NetGuid sourceDeviceGuid, String sourceUserName) {
		// We want to store the source device or guid
		// We want to claim the sourceUserName as our own.
		// If in idle state, we want to transition to SETUP_SUMMARY, acting as if we are logged in.
		// finally, draw our screen onto source device. But that may happen as a side effect.

		if (sourceDeviceGuid == null) {
			LOGGER.error("null guid in processLink");
			return;
		}

		CheDeviceLogic sourceDevice = getDeviceManager().getCheDeviceByNetGuid(sourceDeviceGuid);
		if (sourceDevice == null) {
			LOGGER.error("processLink failed to find the device");
			return;
		}

		String currentUserId = getUserId();
		if (currentUserId != null && !currentUserId.equals(sourceUserName)) {
			LOGGER.warn("linking {}. Replacing user {} with {}",
				sourceDeviceGuid.getHexStringNoPrefix(),
				currentUserId,
				sourceUserName);
		} else {
			LOGGER.info("linking {} with user {}", sourceDeviceGuid.getHexStringNoPrefix(), sourceUserName);
		}

		setLinkedFromCheGuid(sourceDeviceGuid);
		setUserId(sourceUserName);
		if (CheStateEnum.IDLE.equals(getCheStateEnum())) {
			// we want to log in "generically"
			finishLogin();
		} else {
			// finishLogin resulted in new screen draw, which was also sent to the linked CHE. But if connecting after cart is logged in
			// we still need a screen redraw or else the remote CHE is left on the "Linking..." screen. 
			// Achieve in the usual way: just setState to current state. setState() forces a redraw.
			setState(getCheStateEnum());
		}
	}

	/**
	 * When the controlling CHE logs out without unlinking, we want to keep the links for easy scan badge and resume.
	 * But we need the cart CHE to go back to idle state.
	 * This is the sending (remote CHE) side of the transaction
	 */
	void disconnectRemoteDueToLogout() {
		CheDeviceLogic linkedDevice = getLinkedCheDevice();
		if (linkedDevice == null) {
			return;
		}
		LOGGER.info("{} logged out, so setting remote screen back.", this.getGuidNoPrefix());
		linkedDevice.processDisconnectRemoteDueToLogout(this.getGuidNoPrefix());
	}

	/**
	 * When the controlling CHE clears the link or logs out when linked, the cart CHE should return to its base state.
	 * This is the receiving (cart CHE) side of the transaction.
	 */
	void processDisconnectRemoteDueToLogout(String sourceString) {
		setLinkedFromCheGuid(null);

		LOGGER.info("{} inactive link from {} due to logout. Back to base state.", this.getGuidNoPrefix(), sourceString);
		_logoutSideEffects();
	}

	/**
	 * If the controlled CHE logs out while mobile CHE is controlling it, we want the controlling CHE to know it.
	 * This is the sending (cart CHE) side of the transaction
	 */
	void breakLinkDueToLocalCheActivity(NetGuid linkedMobileGuid) {
		LOGGER.info("Breaking link from {} due to local logout", linkedMobileGuid.getHexStringNoPrefix());
		CheDeviceLogic linkedMobileDevice = getLinkFromCheDevice(); // should correspond to linkedMobileGuid
		if (linkedMobileDevice != null) {
			linkedMobileDevice.processBreakLinkDueToLocalCheActivity();
		}
	}

	/**
	 * If the controlled CHE logs out while mobile CHE is controlling it, we want the controlling CHE to know it.
	 * This is the receiving (mobile CHE) side of the transaction. The result we want is transition to REMOTE state showing no link
	 * 
	 */
	void processBreakLinkDueToLocalCheActivity() {
		if (CheStateEnum.REMOTE_LINKED.equals(this.getCheStateEnum())) {
			// This is a rather large side effect. To remote pending state, waiting for response
			// This achieves the unlink happening all the way up at the server.
			unlinkRemoteCheAssociation();
		}
	}

	/**
	 * When the controlling CHE clears the link or logs out when linked, the cart CHE should return to its base state.
	 * This is the sending (remote CHE) side of the transaction
	 */
	void unLinkLocalVariables() {
		CheDeviceLogic linkedDevice = getLinkedCheDevice();
		if (linkedDevice == null) {
			return;
		}
		// This is our best single unlink spot. Call notify here. Misses some forced unlinks.
		notifyUnlink(linkedDevice.getGuid());

		linkedDevice.processUnLinkLocalVariables(this.getGuidNoPrefix());
	}

	/**
	 * When the controlling CHE clears the link or logs out when linked, the cart CHE should return to its base state.
	 * This is the receiving (cart CHE) side of the transaction.
	 */
	void processUnLinkLocalVariables(String sourceString) {
		LOGGER.info("{} unlinking from {}. Resuming local control.", this.getGuidNoPrefix(), sourceString);
		setLinkedFromCheGuid(null);
		setUserId(null);
		setState(CheStateEnum.IDLE);
	}

	/**
	 * This is rarely used. Only if a CHE was remote to another, and then itself gets controlled. It should not be in linked state.
	 * Or one mobile was in linked state, then another mobile takes over control of the same cart.
	 * Note the database and csDeviceLogic structures should be good when this called. This is only about the cheDeviceLogic member variables
	 */
	void forceFromLinkedState(CheStateEnum newState) {
		LOGGER.warn("{} forced from state {} to {}", this.getGuidNoPrefix(), getCheStateEnum(), newState);
		this.setLinkedToCheName(null);
		setState(newState);
	}

	public void finishLogin() {
		LOGGER.error("override finishLogin needed");
	}

	/**
	 * If a remote CHE is linked to me, get its device.
	 */
	CheDeviceLogic getLinkFromCheDevice() {
		NetGuid sourceGuid = getLinkedFromCheGuid();
		if (sourceGuid == null)
			return null;
		return getDeviceManager().getCheDeviceByNetGuid(sourceGuid);
	}

	/**
	 * Called by the three screen command primitives. Therefore, just after drawing the local screen, will draw the remote screen
	 * This is the sending side (the cart CHE) as its state machine decided to redraw its own screen and send a clone out to the mobile che screen.
	 */
	void sendScreenCommandToLinkFromChe(ICommand inCommand) {
		CheDeviceLogic linkFromDevice = getLinkFromCheDevice();
		if (linkFromDevice != null) {
			quickSleep(); // slight separation before sending to other device
			// We do not want to call directly. Rather, pass back, and let that device decide if it is in in the right state for remote screen redraws.			
			linkFromDevice.processScreenCommandAtLinkFromChe(inCommand);
		}
	}

	/**
	 * Called by the three screen command primitives. Therefore, just after drawing the local screen, will draw the remote screen
	 * This is the sending side (the cart CHE) as its state machine decided to redraw its own screen and send a clone out to the mobile che screen.
	 */
	void sendScreenCommandToMyChe(ICommand inCommand) {
		sendRadioControllerCommand(inCommand, true);
	}

	/**
	 * This is the receiving side (the mobile CHE) getting the screen from the cart CHE.
	 * This will log a warn if not in the REMOTE_LINKED state and will not do anything.
	 */
	void processScreenCommandAtLinkFromChe(ICommand inCommand) {
		CheStateEnum state = getCheStateEnum();
		if (CheStateEnum.REMOTE_LINKED.equals(state)) {
			sendScreenCommandToMyChe(inCommand);
		} else {
			LOGGER.warn("processScreenCommandFromLinkedChe called from state:{}. Not drawing", state);
		}
	}

	protected void displayDeviceInfo() {
		String deviceType = "UNKNOWN";
		if (this instanceof SetupOrdersDeviceLogic) {
			deviceType = "SETUP ORDERS";
		} else if (this instanceof LineScanDeviceLogic) {
			deviceType = "LINE SCAN";
		} else if (this instanceof ChePalletizerDeviceLogic) {
			deviceType = "PALLETIZER";
		}
		sendDisplayCommand("Site " + mDeviceManager.getUsername(), "Device: " + deviceType, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
	}

	@Override
	public byte getScannerTypeCode() {
		return getScannerTypeEnum().getValue();
	}
}
