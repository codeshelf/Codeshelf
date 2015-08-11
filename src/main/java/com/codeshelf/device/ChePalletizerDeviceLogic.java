package com.codeshelf.device;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.service.PalletizerService.PalletizerInfo;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * This CHE type is being developed for WallMart.
 * At the moment, it is meant to sort items into bins based on the first few digits of item id.
 * See: 
 * https://codeshelf.atlassian.net/wiki/display/TD/CD_0105+Walmart+Putwall+Pallet+License+Plate
 * https://codeshelf.atlassian.net/wiki/display/TD/CD_0105C+Walmart+Palletizer+Design
 * @author Ilya
 */
public class ChePalletizerDeviceLogic extends CheDeviceLogic{
	private static final Logger					LOGGER		= LoggerFactory.getLogger(ChePalletizerDeviceLogic.class);
	
	@Accessors(prefix = "m")
	@Getter
	private PalletizerInfo						mInfo		 = new PalletizerInfo();
	
	public ChePalletizerDeviceLogic(UUID inPersistentId,
		NetGuid inGuid,
		CsDeviceManager inDeviceManager,
		IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
	}
	
	@Override
	public String getDeviceType() {
		return CsDeviceManager.DEVICETYPE_CHE_PALLETIZER;
	}

	private void setItemInfo(PalletizerInfo info){
		if (info == null) {
			clearItemInfo();
		} else {
			mInfo = info;
		}
	}

	private void clearItemInfo(){
		mInfo = new PalletizerInfo();
	}
	
	@Override
	protected void invalidScanMsg(final CheStateEnum inCheState) {
		mCheStateEnum = inCheState;

		switch (inCheState) {
			case IDLE:
				sendDisplayCommand(SCAN_USERID_MSG, INVALID_SCAN_MSG);
				break;
			default:
				break;
		}
		sendErrorCodeToAllPosCons();
	}
	
	@Override
	protected void setState(final CheStateEnum inCheState) {
		clearAffectedLedAndPoscons();
		int priorCount = getSetStateStackCount();
		try {
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

				case PALLETIZER_SCAN_ITEM:
					sendDisplayCommand("Scan Item", EMPTY_MSG);
					break;
					
				case PALLETIZER_PROCESSING:
					sendDisplayCommand("Processing...", EMPTY_MSG);
					break;
					
				case PALLETIZER_NEW_ORDER:
					sendDisplayCommand(PALL_NEW_ORDER_1_MSG, PALL_NEW_ORDER_2_MSG + getInfo().getOrderId(), EMPTY_MSG, PALL_NEW_ORDER_3_MSG);
					break;
					
				case PALLETIZER_PUT_ITEM:
					displayPutScreen();
					break;
					
				case PALLETIZER_DAMAGED:
					sendDisplayCommand(DAMAGED_PUT_CONFIRM_MSG, YES_NO_MSG);
					break;
					
				case PALLETIZER_LICENSE:
					sendDisplayCommand(PALL_REMOVE_LICENSE_1_MSG, PALL_REMOVE_LICENSE_1_MSG, EMPTY_MSG, CANCEL_TO_EXIT_MSG);
					break;
					
				default:
					LOGGER.warn("Unknown Palletizer State {}", inCheState);
					break;
			}
		} finally {
			setSetStateStackCount(priorCount);
		}
	}
	
	@Override
	protected void processCommandScan(final String inScanStr) {
		switch (inScanStr) {
			case LOGOUT_COMMAND:
				clearItemInfo();
				logout();
				break;
				
			case CLEAR_COMMAND:
			case CANCEL_COMMAND:
				processCommandCancel();
				break;

			case SHORT_COMMAND:
				if (mCheStateEnum == CheStateEnum.PALLETIZER_PUT_ITEM) {
					setState(CheStateEnum.PALLETIZER_DAMAGED);
				}
				break;
				
			case YES_COMMAND:
			case NO_COMMAND:
				yesOrNoCommandReceived(inScanStr);
				break;

			case REMOVE_COMMAND:
				clearItemInfo();
				setState(CheStateEnum.PALLETIZER_LICENSE);
				break;
				
			case INFO_COMMAND:
				processCommandInfo();
				break;
				
			default:
				break;
		}
	}
	
	@Override
	protected void yesOrNoCommandReceived(final String inScanStr) {
		switch (mCheStateEnum) {
			case PALLETIZER_DAMAGED:
				if (YES_COMMAND.equalsIgnoreCase(inScanStr)) {
					notifyWiVerb(getInfo().getWi(), WorkerEvent.EventType.SHORT, kLogAsWarn);
					setState(CheStateEnum.PALLETIZER_SCAN_ITEM);
				} else {
					setState(CheStateEnum.PALLETIZER_PUT_ITEM);
				}
				break;
				
			default:
				break;
		}
	}
	
	@Override
	public void processNonCommandScan(String scanPrefix, String scanBody) {
		// Non-command scans are split out by state then the scan content
		switch (mCheStateEnum) {
			case IDLE:
				processIdleStateScan(scanPrefix, scanBody);
				break;
				
			case PALLETIZER_SCAN_ITEM:
				if (isEmpty(scanPrefix)){
					processItemScan(scanPrefix, scanBody);
				}
				break;
				
			case PALLETIZER_PUT_ITEM:
				completeCurrentWi();
				if (isEmpty(scanPrefix)){
					processItemScan(scanPrefix, scanBody);
				}
				break;
				
			case PALLETIZER_NEW_ORDER:
				if (LOCATION_PREFIX.equalsIgnoreCase(scanPrefix) || TAPE_PREFIX.equalsIgnoreCase(scanPrefix)) {
					processNewOrderLocationScan(scanPrefix, scanBody);
				} else if (isEmpty(scanPrefix)){
					processItemScan(scanPrefix, scanBody);
				}
				break;
				
			case PALLETIZER_LICENSE:
				if (isEmpty(scanPrefix)) {
					setState(CheStateEnum.PALLETIZER_PROCESSING);
					mDeviceManager.palletizerRemoveOrderRequest(getGuidNoPrefix(), getPersistentId().toString(), scanBody);
				}
				break;
				
			default:
		}
	}
	
	protected void processCommandInfo() {
		switch (mCheStateEnum) {
			case IDLE:
				displayDeviceInfo();
				break;
			default:
				break;
		}
	}
	
	@Override
	protected void processCommandCancel() {
		switch (mCheStateEnum) {
			case PALLETIZER_PROCESSING:
				setState(CheStateEnum.PALLETIZER_SCAN_ITEM);
				break;
				
			case PALLETIZER_PUT_ITEM:
				setState(CheStateEnum.PALLETIZER_SCAN_ITEM);
				break;
				
			case PALLETIZER_DAMAGED:
				setState(CheStateEnum.PALLETIZER_PUT_ITEM);
				break;
				
			case PALLETIZER_LICENSE:
				setState(CheStateEnum.PALLETIZER_SCAN_ITEM);
				break;
				
			default:
				setState(mCheStateEnum);
				break;
		}
	}
	
	private void processItemScan(String scanPrefix, String scanBody) {
		if (isEmpty(scanPrefix)){
			setState(CheStateEnum.PALLETIZER_PROCESSING);
			mDeviceManager.palletizerItemRequest(getGuidNoPrefix(), getPersistentId().toString(), scanBody);
		} else {
			String scan = scanPrefix + scanBody;
			LOGGER.warn("Item scan {} must not have a prefix", scan);
			sendDisplayCommand("Invalid Item", scan, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
		}
	}
	
	private void processIdleStateScan(String scanPrefix, String scanBody) {
		if (USER_PREFIX.equals(scanPrefix) || isEmpty(scanPrefix)) {
			clearAllPosconsOnThisDevice();
			this.setUserId(scanPrefix);
			setState(CheStateEnum.VERIFYING_BADGE);
			mDeviceManager.verifyBadge(getGuid().getHexStringNoPrefix(), getPersistentId(), scanBody);
		} else {
			LOGGER.info("Not a user ID: " + scanBody);
			invalidScanMsg(CheStateEnum.IDLE);
		}
	}
	
	private void processNewOrderLocationScan(String scanPrefix, String scanBody){
		setState(CheStateEnum.PALLETIZER_PROCESSING);
		if (TAPE_PREFIX.equalsIgnoreCase(scanPrefix)) {
			scanBody = scanPrefix + scanBody;
		}
		mDeviceManager.palletizerNewOrderRequest(getGuidNoPrefix(), getPersistentId().toString(), getInfo().getItem(), scanBody);
	}
	
	@Override
	protected void processButtonPress(Integer inButtonNum, Integer inQuantity) {
		notifyButton(inButtonNum, inQuantity);

		// The point is, let's check our state
		switch (mCheStateEnum) {
			case PALLETIZER_PUT_ITEM:
				completeCurrentWi();
				clearItemInfo();
				setState(CheStateEnum.PALLETIZER_SCAN_ITEM);
				break;

			default:
				LOGGER.warn("Unexpected button press ignored.");
				// We want to ignore the button press, but force out starting poscon situation again.
				setState(mCheStateEnum);
				return;
		}
	}
	
	private void completeCurrentWi(){
		WorkInstruction wi = getInfo().getWi();
		if (wi == null) {
			LOGGER.warn("Palletizer PUT button press without a saved WI");
			return;
		}
		wi.setActualQuantity(1);
		wi.setPickerId(mUserId);
		wi.setCompleted(new Timestamp(System.currentTimeMillis()));
		wi.setStatus(WorkInstructionStatusEnum.COMPLETE);
		mDeviceManager.completeWi(getGuid().getHexStringNoPrefix(), getPersistentId(), wi);
		notifyWiVerb(wi, WorkerEvent.EventType.COMPLETE, kLogAsInfo);
	}
	
	private void clearAffectedLedAndPoscons(){
		WorkInstruction wi = getInfo().getWi();
		if (wi != null) {
			clearLedAndPosConControllersForWi(wi);
		}
		clearAllPosconsOnThisDevice();
	}
	
	@Override
	public void processResultOfVerifyBadge(Boolean verified) {
		if (mCheStateEnum.equals(CheStateEnum.VERIFYING_BADGE) || mCheStateEnum.equals(CheStateEnum.IDLE)) {
			if (verified) {
				clearAllPosconsOnThisDevice();
				notifyCheWorkerVerb("LOG IN", "");
				setState(CheStateEnum.PALLETIZER_SCAN_ITEM);
			} else {
				setState(CheStateEnum.IDLE);
				invalidScanMsg(UNKNOWN_BADGE_MSG, EMPTY_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
				notifyCheWorkerVerb("LOG IN", "Credential Denied");
			}
		} else {
			LOGGER.warn("unexpected verifyBadge response in state {}", mCheStateEnum);
		}
	}
	
	protected void processItemResponse(PalletizerInfo info){
		if (mCheStateEnum != CheStateEnum.PALLETIZER_PROCESSING){
			LOGGER.warn("Unexpected state {} when receiving Palletizer Item Response", mCheStateEnum);
			return;
		}
		setItemInfo(info);
		if (info.isOrderFound()) {
			setState(CheStateEnum.PALLETIZER_PUT_ITEM);
		} else {
			setState(CheStateEnum.PALLETIZER_NEW_ORDER);
		}
		String error1 = info.getErrorMessage1();
		if (error1 != null) {
			String error2 = info.getErrorMessage2();
			if (error2 == null){
				error2 = "";
			}
			sendDisplayCommand(error1, error2, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
		}
	}
	
	protected void processRemoveResponse(String error){
		if (mCheStateEnum != CheStateEnum.PALLETIZER_PROCESSING){
			LOGGER.warn("Unexpected state {} when receiving Palletizer Remove Response", mCheStateEnum);
			return;
		}
		if (error == null) {
			setState(CheStateEnum.PALLETIZER_SCAN_ITEM);
		} else {
			setState(CheStateEnum.PALLETIZER_LICENSE);
			sendDisplayCommand(error, CANCEL_TO_CONTINUE_MSG);
		}
	}
	
	private void displayPutScreen(){
		PalletizerInfo info = getInfo();
		sendDisplayCommand(info.getLocation(), "Item: " + info.getItem(), "Store: " + info.getOrderId(), PALL_SCAN_NEXT_ITEM_MSG);
		WorkInstruction wi = info.getWi();
		forceClearOtherPosConControllersForThisCheDevice();
		if (wi != null) {
			lightWiLocations(wi);
			lightWiPosConLocations(wi);
		} else {
			LOGGER.warn("Arrived at Palletizer's PUT screen without a work instruction");
		}
		byte qty = 1;
		List<PosControllerInstr> instructions = Lists.newArrayList();
		instructions.add(new PosControllerInstr((byte)1,qty,qty,qty,PosControllerInstr.SOLID_FREQ, PosControllerInstr.BRIGHT_DUTYCYCLE));
		sendPositionControllerInstructions(instructions);
	}
		
	private boolean isEmpty(String str){
		return (str == null) ? true : str.isEmpty();
	}
}