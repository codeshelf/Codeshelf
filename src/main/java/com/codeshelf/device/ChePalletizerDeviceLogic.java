package com.codeshelf.device;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.service.WorkService.PalletizerInfo;

import lombok.Getter;
import lombok.Setter;
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
	@Setter
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
					PalletizerInfo info = getInfo();
					sendDisplayCommand(info.getLocation(), "Item: " + info.getItem(), "Store: " + info.getOrderId(), PALL_SCAN_NEXT_ITEM_MSG);
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
				setInfo(new PalletizerInfo());
				logout();
				break;
				
			case CLEAR_COMMAND:
			case CANCEL_COMMAND:
				processCommandCancel();
				break;

			case INFO_COMMAND:
				processCommandInfo();
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
		mDeviceManager.palletizerNewLocationRequest(getGuidNoPrefix(), getPersistentId().toString(), getInfo().getItem(), scanBody);
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
		setInfo(info);
		if (info.isOrderFound()) {
			setState(CheStateEnum.PALLETIZER_PUT_ITEM);
		} else {
			setState(CheStateEnum.PALLETIZER_NEW_ORDER);
		}
		String error = info.getErrorMessage();
		if (error != null) {
			sendDisplayCommand(error, EMPTY_MSG, EMPTY_MSG, CANCEL_TO_CONTINUE_MSG);
		}
	}
	
	private boolean isEmpty(String str){
		return (str == null) ? true : str.isEmpty();
	}
}