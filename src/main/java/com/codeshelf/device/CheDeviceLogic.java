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
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.EffectEnum;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.ThreadUtils;
import com.google.common.base.Objects;
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
	protected static final String					TAPE_PREFIX								= "%";

	// These are the message strings we send to the remote CHE.
	// Currently, these cannot be longer than 20 characters.
	// "SCAN START LOCATION" is at the 20 limit. If you change to "SCAN STARTING LOCATION", you get very bad behavior. The class loader will not find the CheDeviceLogic. Repeating throws.	
	protected static final String					EMPTY_MSG								= cheLine("");
	protected static final String					INVALID_SCAN_MSG						= cheLine("INVALID");
	protected static final String					SCAN_USERID_MSG							= cheLine("SCAN BADGE");
	protected static final String					SCAN_LOCATION_MSG						= cheLine("SCAN START LOCATION");
	protected static final String					OR_START_WORK_MSG						= cheLine("OR START WORK");
	protected static final String					SELECT_POSITION_MSG						= cheLine("SELECT POSITION");
	protected static final String					SHORT_PICK_CONFIRM_MSG					= cheLine("CONFIRM SHORT");
	protected static final String					PICK_COMPLETE_MSG						= cheLine("ALL WORK COMPLETE");
	public static final String						YES_NO_MSG								= cheLine("SCAN YES OR NO");						// public for test
	protected static final String					NO_CONTAINERS_SETUP_MSG					= cheLine("NO SETUP CONTAINERS");
	protected static final String					POSITION_IN_USE_MSG						= cheLine("POSITION IN USE");
	protected static final String					FINISH_SETUP_MSG						= cheLine("PLS SETUP CONTAINERS");
	protected static final String					COMPUTE_WORK_MSG						= cheLine("COMPUTING WORK");
	protected static final String					GET_WORK_MSG							= cheLine("GETTING WORK");
	protected static final String					NO_WORK_MSG								= cheLine("NO WORK TO DO");
	protected static final String					ON_CURR_PATH_MSG						= cheLine("ON CURRENT PATH");
	protected static final String					LOCATION_SELECT_REVIEW_MSG_LINE_1		= cheLine("REVIEW MISSING WORK");
	protected static final String					LOCATION_SELECT_REVIEW_MSG_LINE_2		= cheLine("OR SCAN LOCATION");
	protected static final String					LOCATION_SELECT_REVIEW_MSG_LINE_3		= cheLine("TO CONTINUE AS IS");
	protected static final String					SHOWING_ORDER_IDS_MSG					= cheLine("SHOWING ORDER IDS");
	protected static final String					SHOWING_WI_COUNTS						= cheLine("SHOWING WI COUNTS");
	protected static final String					PATH_COMPLETE_MSG						= cheLine("PATH COMPLETE");
	protected static final String					SCAN_NEW_LOCATION_MSG					= cheLine("SCAN NEW LOCATION");
	protected static final String					OR_SETUP_NEW_CART_MSG					= cheLine("OR SETUP NEW CART");

	protected static final String					INVALID_POSITION_MSG					= cheLine("INVALID POSITION");
	protected static final String					INVALID_CONTAINER_MSG					= cheLine("INVALID CONTAINER");
	protected static final String					CLEAR_ERROR_MSG_LINE_1					= cheLine("CLEAR ERROR TO");
	protected static final String					CLEAR_ERROR_MSG_LINE_2					= cheLine("CONTINUE");

	protected static final String					SCAN_GTIN								= cheLine("SCAN GTIN");
	protected static final String					SCAN_GTIN_OR_LOCATION					= cheLine("SCAN GTIN/LOCATION");

	// Newer messages only used in Line_Scan mode. Some portion of the above are used for both Setup_Orders and Line_Scan, so keeping them all here.
	protected static final String					SCAN_LINE_MSG							= cheLine("SCAN ORDER LINE");
	protected static final String					GO_TO_LOCATION_MSG						= cheLine("GO TO LOCATION");
	protected static final String					ABANDON_CHECK_MSG						= cheLine("ABANDON CURRENT JOB");
	protected static final String					ONE_JOB_MSG								= cheLine("DO THIS JOB (FIXME)");					// remove this later

	// For Put wall
	protected static final String					SCAN_PUTWALL_ORDER_MSG					= cheLine("SCAN ORDER FOR");
	protected static final String					SCAN_PUTWALL_LOCATION_MSG				= cheLine("SCAN LOCATION IN");
	protected static final String					SCAN_PUTWALL_ITEM_MSG					= cheLine("SCAN ITEM/UPC FOR");
	protected static final String					SCAN_PUTWALL_LINE2_MSG					= cheLine("THE PUT WALL");
	protected static final String					SCAN_PUTWALL_NAME_MSG					= cheLine("SCAN PUT WALL NAME");

	public static final String						STARTWORK_COMMAND						= "START";
	public static final String						REVERSE_COMMAND							= "REVERSE";
	protected static final String					SETUP_COMMAND							= "SETUP";
	protected static final String					SHORT_COMMAND							= "SHORT";
	public static final String						LOGOUT_COMMAND							= "LOGOUT";
	protected static final String					YES_COMMAND								= "YES";
	protected static final String					NO_COMMAND								= "NO";
	protected static final String					CLEAR_ERROR_COMMAND						= "CLEAR";
	protected static final String					INVENTORY_COMMAND						= "INVENTORY";
	protected static final String					ORDER_WALL_COMMAND						= "ORDER_WALL";
	protected static final String					PUT_WALL_COMMAND						= "PUT_WALL";

	// With WORKSEQR = "WorkSequence", work may scan start instead of scanning a location. 
	// LOCATION_SELECT, we want "SCAN START LOCATION" "OR SCAN START"
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

	// The CHE's current user.
	@Accessors(prefix = "m")
	@Getter
	@Setter
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

	@Accessors(prefix = "m")
	@Getter
	@Setter
	boolean											mOkToStartWithoutLocation				= true;

	private NetGuid									mLastLedControllerGuid;
	private boolean									mMultipleLastLedControllerGuids;

	protected WorkInstruction						mShortPickWi;
	protected Integer								mShortPickQty;

	protected boolean								connectedToServer						= true;
	@Accessors(prefix = "m")
	@Getter
	@Setter
	protected int									mSetStateStackCount						= 0;

	protected ScanNeededToVerifyPick				mScanNeededToVerifyPick;

	@Getter
	@Setter
	protected Boolean								mReversePickOrder						= false;

	@Getter
	@Setter
	protected String								lastScanedGTIN;

	@Getter
	@Setter
	private String									lastPutWallOrderScan;

	protected void processGtinScan(final String inScanPrefixStr, final String inScanStr) {
		boolean isTape = false;

		if (LOCATION_PREFIX.equals(inScanPrefixStr) && lastScanedGTIN != null) {
			// Updating location of an item
			notifyScanInventoryUpdate(inScanStr, lastScanedGTIN);
			mDeviceManager.inventoryUpdateScan(this.getPersistentId(), inScanStr, lastScanedGTIN);
		} else if (LOCATION_PREFIX.equals(inScanPrefixStr) && lastScanedGTIN == null) {
			// Lighting a location based on location
			mDeviceManager.inventoryLightLocationScan(getPersistentId(), inScanStr, isTape);
		} else if (TAPE_PREFIX.equals(inScanPrefixStr) && lastScanedGTIN == null) {
			// Lighting a location based on tape
			isTape = true;
			mDeviceManager.inventoryLightLocationScan(getPersistentId(), inScanStr, isTape);
		} else if (USER_PREFIX.equals(inScanPrefixStr)) {
			LOGGER.warn("Recieved invalid USER scan: {}. Expected location or GTIN.", inScanStr);
		} else {
			mDeviceManager.inventoryLightItemScan(this.getPersistentId(), inScanStr);
			lastScanedGTIN = inScanStr;
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

	protected boolean alreadyScannedSkuOrUpcOrLpnThisWi(WorkInstruction inWi) {
		return false;
	}

	protected boolean isScanNeededToVerifyPick() {
		WorkInstruction wi = this.getOneActiveWorkInstruction();

		if (wi.isHousekeeping())
			return false;
		else if (mScanNeededToVerifyPick != ScanNeededToVerifyPick.NO_SCAN_TO_VERIFY) {
			return !alreadyScannedSkuOrUpcOrLpnThisWi(wi);
			// See if we can skip this scan because we already scanned.
		}
		return false;
	}

	protected void setScanNeededToVerifyPick(ScanNeededToVerifyPick inValue) {
		mScanNeededToVerifyPick = inValue;
	}

	public String getScanVerificationType() {
		return ScanNeededToVerifyPick.scanPickEnumToString(mScanNeededToVerifyPick);
	}

	public void updateConfigurationFromManager() {
		mScanNeededToVerifyPick = ScanNeededToVerifyPick.NO_SCAN_TO_VERIFY;
		String scanPickValue = mDeviceManager.getScanTypeValue();
		ScanNeededToVerifyPick theEnum = ScanNeededToVerifyPick.stringToScanPickEnum(scanPickValue);
		setScanNeededToVerifyPick(theEnum);

		@SuppressWarnings("unused")
		String mSequenceKind = mDeviceManager.getSequenceKind();
		//setOkToStartWithoutLocation("WorkSequence".equalsIgnoreCase(mSequenceKind));
		//As part of DEV-670 work, we are always enabling scanning of "start" or "reverse" on the "scan location" screen
		setOkToStartWithoutLocation(true);

	}

	public CheDeviceLogic(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

		mCheStateEnum = CheStateEnum.IDLE;
		mAllPicksWiList = new ArrayList<WorkInstruction>();
		mActivePickWiList = new ArrayList<WorkInstruction>();
		mLastScreenDisplayLines = new ArrayList<String>(); // and preinitialize to lines 1-4
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
	 * @param inLine1Message
	 * @param inLine2Message
	 * @param inLine3Message
	 * @param inLine4Message
	 */
	protected void sendDisplayCommand(final String inLine1Message,
		final String inLine2Message,
		final String inLine3Message,
		final String inLine4Message) {
		// Remember that we are trying to send, even before the association check. Want this to work in unit tests.
		doSetRecentCheDisplayString(1, inLine1Message);
		doSetRecentCheDisplayString(2, inLine2Message);
		doSetRecentCheDisplayString(3, inLine3Message);
		doSetRecentCheDisplayString(4, inLine4Message);

		// DEV-459 if this CHE is not associated, there is no point in sending out a display.
		// Lots of upstream code generates display messages.

		if (!this.isDeviceAssociated()) {
			LOGGER.debug("skipping send display for unassociated " + this.getMyGuidStrForLog());
			// This is far less logging than if the command actually goes, so might as well say what is going on.
			return;
		}

		String displayString = "Display message for " + getMyGuidStrForLog();
		if (!inLine1Message.isEmpty())
			displayString += " line1: " + inLine1Message;
		if (!inLine2Message.isEmpty())
			displayString += " line2: " + inLine2Message;
		if (!inLine3Message.isEmpty())
			displayString += " line3: " + inLine3Message;
		if (!inLine4Message.isEmpty())
			displayString += " line4: " + inLine4Message;
		LOGGER.info(displayString);
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT,
			inLine1Message,
			inLine2Message,
			inLine3Message,
			inLine4Message);
		mRadioController.sendCommand(command, getAddress(), true);
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

	// --------------------------------------------------------------------------
	/**
	 * After simultaneous work instruction enhancement, the answer will come from mActivePickWiList.
	 * For our v13 kludge, the answer comes from mAllPicksWiList
	 */
	private int getTotalCountSameSkuLocation(WorkInstruction inWi) {
		if (inWi == null) {
			LOGGER.error("null wi in getTotalCountSameSku");
			return 0;
		}

		String pickSku = inWi.getItemId();
		String pickLocation = inWi.getPickInstruction();
		int totalQty = 0;

		// using mAllPicksWiList, we expect same item, same pick location, uncompleted, unshorted.
		// this will find and match the inWi also.
		for (WorkInstruction wi : mAllPicksWiList) {
			WorkInstructionStatusEnum theStatus = wi.getStatus();
			if (theStatus == WorkInstructionStatusEnum.INPROGRESS || theStatus == WorkInstructionStatusEnum.NEW)
				if (wiMatchesItemLocation(pickSku, pickLocation, wi)) {
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
	 * Today, return just the simple string numeral.
	 * Suggested enhancement: part of a multi-work instruction pick, return as +5+.  If a single, then -5-
	 * For our v13 kludge, the answer comes from mAllPicksWiList
	 */
	protected String getWICountStringForCheDisplay(WorkInstruction inWi) {
		if (inWi.isHousekeeping()) {
			return "";
		}
		Integer planQty = inWi.getPlanQuantity();
		Integer totalQtyThisSku = getTotalCountSameSkuLocation(inWi);
		String returnStr;
		/* better
		if (planQty >= totalQtyThisSku)
			returnStr = "-" + planQty + "-";
		else
			returnStr = "+" + totalQtyThisSku + "+";
		*/
		// As Zach specifies
		if (planQty >= totalQtyThisSku)
			returnStr = planQty.toString();
		else
			returnStr = totalQtyThisSku.toString();
		// >=?  We do not really know that all future deviceLogic classes will use mAllPicksWiList which is where getTotalCountSameSku comes from.

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
	protected void sendDisplayWorkInstruction(WorkInstruction wi) {
		String planQtyStr = getWICountStringForCheDisplay(wi);

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
			if (!planQtyStr.isEmpty()) {
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
			if (!planQtyStr.isEmpty()) {
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
			if (!planQtyStr.isEmpty()) {
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
		pickInfoLines[2] = getFourthLineDisplay();

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
	protected String getFourthLineDisplay() {
		String returnString = "";
		if (CheStateEnum.SHORT_PICK == mCheStateEnum) {
			returnString = "DECREMENT POSITION";
		}
		// kind of funny. States are uniformly defined, so this works even from wrong object
		else if (CheStateEnum.SCAN_SOMETHING == mCheStateEnum) {
			returnString = "SCAN UPC NEEDED";
			// TODO: UPC or LPN or SKU
		}
		return returnString;
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

	private void forceClearAllPosConControllersForThisCheDevice() {
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
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.INetworkDevice#start()
	 */
	@Override
	public final void startDevice() {
		LOGGER.info("Start CHE controller (after association) ");

		// setState(mCheStateEnum);  Always, after start, there is the device associate chain and redisplay which will call setState(mCheStateEnum);
		setLastAckId((byte) 0);
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
			LOGGER.info("Position Feedback: Poscon {} -- {}", position, wiCount);
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
						//This should not be possible (unless we only had a single HK WI, which would be a bug)
						//We will log this for now and treat it as a completed WI
						LOGGER.error("WorkInstructionCount has no counts {};", wiCount);
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
		if (!connectedToServer) {
			LOGGER.debug("NotConnectedToServer: Ignoring scan command: " + inCommandStr);
			return;
		}

		// Clean up any potential newline or carriage returns.
		inCommandStr = inCommandStr.replaceAll("[\n\r]", "");

		notifyScan(inCommandStr); // logs

		String scanPrefixStr = getScanPrefix(inCommandStr);
		String scanStr = getScanContents(inCommandStr, scanPrefixStr);

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
		@SuppressWarnings("unused")
		String theSub = controllerId.substring(0, 2);
		if ("0x".equals(controllerId.substring(0, 2))) {
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
			// TODO how do we find the work instruction this button was displaying?							

			int posconIndex = inButtonCommand.getPosNum();
			int quantity = inButtonCommand.getValue();
			notifyOffCheButton(posconIndex, quantity, sourceGuid); // This merely logs

			// LOGGER.info("retrieve {} {}", sourceGuid, posconIndex); // useful to debug
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
						processShortPick(wi, quantity);
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
	 */
	protected void logout() {
		LOGGER.info("User logut");
		// Clear all of the container IDs we were tracking.
		this.setUserId("");
		clearAllPositionControllers();
		mActivePickWiList.clear();
		mAllPicksWiList.clear();
		setState(CheStateEnum.IDLE);

		forceClearAllLedsForThisCheDevice();

		//Clear PosConControllers
		forceClearAllPosConControllersForThisCheDevice();

		clearAllPositionControllers();
	}

	/**
	 * Setup the CHE by clearing all the data structures
	 */
	protected void setupChe() {
		clearAllPositionControllers();
		clearAllPositionControllers();
		setState(CheStateEnum.CONTAINER_SELECT);
	}

	protected void setupCommandReceived() {
		LOGGER.error("setupCommandReceived() needs override");
	}

	protected void clearErrorCommandReceived() {
		LOGGER.error("clearErrorCommandReceived() needs override");
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
	 * Is this useful to linescan?  If not, move as private function to SetupOrdersDeviceLogic
	 */
	protected void processPickComplete(boolean isWorkOnOtherPaths) {
		// There are no more WIs, so the pick is complete.

		// Clear the existing LEDs.
		ledControllerClearLeds(); // this checks getLastLedControllerGuid(), and bails if null.

		//Clear PosConControllers
		forceClearAllPosConControllersForThisCheDevice();

		// CD_0041 is there a need for this?
		ledControllerShowLeds(getGuid());

		if (isWorkOnOtherPaths) {
			setState(CheStateEnum.PICK_COMPLETE_CURR_PATH);
		} else {
			setState(CheStateEnum.PICK_COMPLETE);
		}
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
		forceClearAllPosConControllersForThisCheDevice();
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
	protected void processShortPick(WorkInstruction inWi, Integer inQuantity) {
		setState(CheStateEnum.SHORT_PICK_CONFIRM);
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

	public void assignWallPuts(final List<WorkInstruction> inWorkItemList, final String message) {
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
		return Objects.toStringHelper(this).add("netGuid", getGuid()).toString();
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
		redisplayState();

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
					LOGGER.info(toLogString);
				if (setCount > kMaxLedSetsToLog)
					LOGGER.info("And more LED not logged. Total LED Sets this update = " + setCount);

				if (ledController.isDeviceAssociated()) {
					ledControllerShowLeds(ledControllerGuid);
				}
			}
		}

	}

	private void lightWiPosConLocations(WorkInstruction inFirstWi) {
		String wiCmdString = inFirstWi.getPosConCmdStream();
		if (wiCmdString == null || wiCmdString.equals("[]")) {
			return;
		}
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
				controller.addPosConInstrFor(cheGuid, instruction);
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
					&& currentState != CheStateEnum.SCAN_SOMETHING && currentState != CheStateEnum.DO_PUT) {
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

			forceClearAllPosConControllersForThisCheDevice();
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

		byte freq = PosControllerInstr.SOLID_FREQ;
		byte brightness = PosControllerInstr.BRIGHT_DUTYCYCLE;
		if (this.getCheStateEnum().equals(CheStateEnum.SCAN_SOMETHING)) { // a little weak feedback that the poscon button press will not work
			brightness = PosControllerInstr.MIDDIM_DUTYCYCLE;
		}

		// blink is an indicator that decrement button is active, usually as a consequence of short pick. (Max difference is also possible for discretionary picks)
		if (planQuantityForPositionController != minQuantityForPositionController
				|| planQuantityForPositionController != maxQuantityForPositionController) {
			freq = PosControllerInstr.BRIGHT_DUTYCYCLE; // Bug?  should be BLINK_FREQ
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
	 * These notifyXXX functions  with warn parameter might get hooked up to Codeshelf Companion tables someday. 
	 * These log from the site controller extremely consistently. Companion should mostly log back end effects.
	 * However, something like SKIPSCAN can only be learned of here.
	 * 
	 * By convention, start these string with something recognizable, to tell these notifies apart from the rest that is going on.
	 */
	protected void notifyWiVerb(final WorkInstruction inWi, String inVerb, boolean needWarn) {
		if (inWi == null) {
			LOGGER.error("bad call to notifyWarnWi"); // want stack trace?
			return;
		}
		String orderId = inWi.getContainerId(); // We really want order ID, but site controller only has this denormalized

		// Pretty goofy code duplication, but can avoid some run time execution if loglevel would not result in this logging
		if (needWarn)
			LOGGER.warn("*{} for order/cntr:{} item:{} location:{} by picker:{} device:{}",
				inVerb,
				orderId,
				inWi.getItemId(),
				inWi.getPickInstruction(),
				getUserId(),
				getMyGuidStr());
		else
			LOGGER.info("*{} for order/cntr:{} item:{} location:{} by picker:{} device:{}",
				inVerb,
				orderId,
				inWi.getItemId(),
				inWi.getPickInstruction(),
				getUserId(),
				getMyGuidStr());
	}

	protected void notifyOrderToPutWall(String orderId, String locationName) {
		LOGGER.info("*Put order/cntr:{} into put wall location:{} by picker:{} device:{}",
			orderId,
			locationName,
			getUserId(),
			getMyGuidStr());
	}

protected void notifyPutWallResponse(final List<WorkInstruction> inWorkItemList){
	int listsize = 0;
	if (inWorkItemList != null)
		listsize = inWorkItemList.size();
	LOGGER.info("*{} work instructions in put wall response:{} by picker:{} device:{}",
		listsize,
		getUserId(),
		getMyGuidStr());	
}

protected void notifyPutWallItem(String itemOrUpd, String wallname) {
		LOGGER.info("*Request plans for item:{} in put wall:{} by picker:{} device:{}",
			itemOrUpd,
			wallname,
			getUserId(),
			getMyGuidStr());
	}

	protected void notifyScanInventoryUpdate(String locationStr, String itemOrGtin) {
		LOGGER.info("*Inventory update for item/gtin:{} to location:{} by picker:{} device:{}",
			itemOrGtin,
			locationStr,
			getUserId(),
			getMyGuidStr());
	}

	protected void notifyButton(int buttonNum, int showingQuantity) {
		LOGGER.info("*Button #{} pressed with quantity {} by picker:{} device:{}",
			buttonNum,
			showingQuantity,
			getUserId(),
			getMyGuidStr());
	}

	protected void notifyOffCheButton(int buttonNum, int showingQuantity, String fromGuidId) {
		LOGGER.info("*Wall Button #{} device:{} pressed with quantity {}. Inferred picker:{} inferred device:{}",
			buttonNum,
			fromGuidId,
			showingQuantity,
			getUserId(),
			getMyGuidStr());
	}

	private void notifyNonChePosconLight(String controllerId, int posconIndex, WorkInstruction wi) {
		if (wi == null) {
			LOGGER.error("null work instruction in notifyNonChePosconLight");
			return;
		}
		int displayCount = wi.getPlanQuantity();
		LOGGER.info("*Wall Button #{} device:{} will show count:{}. Active job for picker:{} device:{}",
			posconIndex,
			controllerId,
			displayCount,
			getUserId(),
			getMyGuidStr());
	}

	protected void notifyScan(String theScan) {
		LOGGER.info("*Scan {} by picker:{} device:{}", theScan, getUserId(), getMyGuidStr());
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	protected String verifyWiField(final WorkInstruction inWi, String inScanStr) {

		String returnString = "";
		// TODO
		// If the user scanned SKIPSCAN return true
		if (inScanStr.equals(SCAN_SKIP) || inScanStr.equals(SKIP_SCAN)) {
			notifyWiVerb(inWi, "SKIPSCAN", kLogAsWarn);
			return returnString;
		}

		String wiVerifyValue = "";
		switch (mScanNeededToVerifyPick) {
			case SKU_SCAN_TO_VERIFY:
				wiVerifyValue = inWi.getItemId();
				break;

			// TODO change this when we capture UPC. Need to pass it through to site controller in serialized WI to get it here.
			case UPC_SCAN_TO_VERIFY:
				wiVerifyValue = inWi.getItemId(); // for now, only works if the SKU is the UPC
				break;

			case LPN_SCAN_TO_VERIFY: // not implemented
				LOGGER.error("LPN scan not implemented yet");
				break;

			default:

		}
		if (wiVerifyValue == null || wiVerifyValue.isEmpty())
			returnString = "Data error in WI"; // try to keep to 20 characters
		else if (!wiVerifyValue.equals(inScanStr))
			returnString = "Scan mismatch";

		return returnString;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param insScanPrefixStr
	 * @param inScanStr
	 */
	protected void processVerifyScan(final String inScanPrefixStr, String inScanStr) {
		if (inScanPrefixStr.isEmpty()) {

			WorkInstruction wi = getOneActiveWorkInstruction();
			if (wi == null) {
				LOGGER.error("unanticipated no active WI in processVerifyScan");
				invalidScanMsg(mCheStateEnum);
				return;
			}
			String errorStr = verifyWiField(wi, inScanStr);
			if (errorStr.isEmpty()) {
				// clear usually not needed. Only after correcting a bad scan
				clearAllPositionControllers();
				setState(CheStateEnum.DO_PICK);

			} else {
				LOGGER.info("errorStr = {}", errorStr); // TODO get this to the CHE display
				invalidScanMsg(mCheStateEnum);
			}

		} else {
			// Want some feedback here. Tell the user to scan something
			LOGGER.info("Need to confirm by scanning the UPC "); // TODO later look at the class enum and decide on SKU or UPC or LPN or ....
			invalidScanMsg(mCheStateEnum);
		}
	}

	public CheStateEnum waitForCheState(CheStateEnum state, int timeoutInMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeoutInMillis) {
			// retry every 100ms
			ThreadUtils.sleep(100);
			CheStateEnum currentState = getCheStateEnum();
			// we are waiting for the expected CheStateEnum, AND the indicator that we are out of the setState() routine.
			// Typically, the state is set first, then some side effects are called that depend on the state.  The picker is usually checking on
			// some of the side effects after this call.
			if (currentState.equals(state) && !inSetState()) {
				// expected state found - all good
				break;
			}
		}
		CheStateEnum existingState = getCheStateEnum();
		return existingState;
	}

}
