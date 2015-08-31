package com.codeshelf.device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlClearPosController;
import com.codeshelf.flyweight.command.CommandControlSetPosController;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.ws.protocol.message.NotificationMessage;

public abstract class PosConDeviceABC extends DeviceLogicABC {
	private static final Logger				LOGGER								= LoggerFactory.getLogger(PosConDeviceABC.class);

	@Accessors(prefix = "m")
	@Getter
	private Map<Byte, PosControllerInstr>	mPosToLastSetIntrMap;

	public PosConDeviceABC(UUID inPersistentId, NetGuid inGuid, CsDeviceManager inDeviceManager, IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);
		mPosToLastSetIntrMap = new HashMap<Byte, PosControllerInstr>();
	}

	protected String getConciseInstructionsSummary() {
		return null;
	}

	private void logOnePosconBatch(final List<PosControllerInstr> inInstructions) {
		// Much better than LOGGER.info("{}: Sending PosCon Instructions {}", this.getMyGuidStr(), inInstructions);

		String header = "Sending PosCon Instructions";
		final int logGroupSize = 3;
		int intructionCount = 0;
		int totalCount = inInstructions.size();
		String toLogStr = "";
		for (PosControllerInstr instr : inInstructions) {
			intructionCount++;
			if (intructionCount == 1) {
				toLogStr = String.format("%s %s", header, instr.conciseDescription());
			} else {
				toLogStr = String.format("%s %s", toLogStr, instr.conciseDescription());
			}
			if ((intructionCount == totalCount) || (intructionCount % logGroupSize == 0)) {
				notifyPoscons(toLogStr);
				toLogStr = "";
			}
		}
	}

	protected void sendPositionControllerInstructions(List<PosControllerInstr> inInstructions) {

		if (inInstructions.isEmpty()) {
			LOGGER.error("sendPositionControllerInstructions called for empty instructions");
			return;
		}

		//Update the last sent posControllerInstr for the position 
		for (PosControllerInstr instr : inInstructions) {
			if (PosControllerInstr.POSITION_ALL.equals(instr.getPosition())) {
				//A POS_ALL instruction overrides all previous instructions
				mPosToLastSetIntrMap.clear();
			}
			mPosToLastSetIntrMap.put(instr.getPosition(), instr);
		}

		int batchStart = 0, size = inInstructions.size(), batchSize = 10;
		while (batchStart < size) {
			List<PosControllerInstr> batch = inInstructions.subList(batchStart, Math.min(batchStart + batchSize, size));
			// log these as we are really sending them out
			logOnePosconBatch(batch);
			ICommand command = new CommandControlSetPosController(NetEndpoint.PRIMARY_ENDPOINT, batch);
			sendRadioControllerCommand(command, true);
			batchStart += batchSize;
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Sleep briefly between repeating sends to two CHE. Especially in sendMonospaceDisplayScreen
	 */
	protected void quickSleep() {
		// with 3.1 will eliminate, or at least reduce to 5ms.
		int delayPeriodMills = 5;
		if (delayPeriodMills > 0) {
			try {
				Thread.sleep(delayPeriodMills);
			} catch (InterruptedException e) {
			}
		}
	}

	protected void clearAllPosconsOnThisDevice() {
		clearOnePosconOnThisDevice(PosControllerInstr.POSITION_ALL);
	}

	protected void clearOnePosconOnThisDevice(Byte inPosition) {
		String toWhere;
		if (inPosition == PosControllerInstr.POSITION_ALL)
			toWhere = "ALL";
		else {
			toWhere = inPosition.toString();
		}
		String toLog = String.format("Sending Clear PosCon command for %s", toWhere);
		notifyPoscons(toLog);

		//Remove lastSent Set Instr from map to indicate the clear
		if (PosControllerInstr.POSITION_ALL.equals(inPosition)) {
			mPosToLastSetIntrMap.clear();
		} else {
			mPosToLastSetIntrMap.remove(inPosition);
		}

		ICommand command = new CommandControlClearPosController(NetEndpoint.PRIMARY_ENDPOINT, inPosition);
		sendRadioControllerCommand(command, true);
	}

	public void simulateButtonPress(int inPosition, int inQuantity) {
		// Caller's responsibility to get the quantity correct. Normally match the planQuantity. Normally only lower after SHORT command.
		CommandControlButton buttonCommand = new CommandControlButton();
		buttonCommand.setPosNum((byte) inPosition);
		buttonCommand.setValue((byte) inQuantity);
		this.buttonCommandReceived(buttonCommand);
	}

	public Byte getLastSentPositionControllerDisplayValue(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getReqQty();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getReqQty();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerDisplayFreq(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getFreq();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getFreq();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerDisplayDutyCycle(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getDutyCycle();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getDutyCycle();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerMinQty(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getMinQty();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getMinQty();
		} else {
			return null;
		}
	}

	public Byte getLastSentPositionControllerMaxQty(byte position) {
		if (getPosToLastSetIntrMap().containsKey(position)) {
			return getPosToLastSetIntrMap().get(position).getMaxQty();
		} else if (getPosToLastSetIntrMap().containsKey(PosControllerInstr.POSITION_ALL)) {
			return getPosToLastSetIntrMap().get(PosControllerInstr.POSITION_ALL).getMaxQty();
		} else {
			return null;
		}
	}

	public Byte waitForControllerDisplayValue(byte position, Byte value, int timeoutInMillis) {
		long start = System.currentTimeMillis();
		Byte currentValue = null;
		while (System.currentTimeMillis() - start < timeoutInMillis) {
			// retry every 100ms
			ThreadUtils.sleep(100);
			currentValue = getLastSentPositionControllerDisplayValue(position);
			// either can be null
			if (currentValue == value) {
				// expected state found - all good
				break;
			}
		}
		return currentValue;
	}

	// --------------------------------------------------------------------------
	/**
	 * These notifyXXX functions  with warn parameter might get hooked up to Codeshelf Companion tables someday. 
	 * These log from the site controller extremely consistently. Companion should mostly log back end effects.
	 * However, something like SKIPSCAN can only be learned of here.
	 * 
	 * By convention, start these string with something recognizable, to tell these notifies apart from the rest that is going on.
	 * 
	 * Currently saved actions: CANCEL_PUT, SHORT, SHORT_AHEAD, COMPLETE, SCAN_SKIP
	 */
	protected void notifyWiVerb(final WorkInstruction inWi, WorkerEvent.EventType inVerb, boolean needWarn) {
		if (inWi == null) {
			LOGGER.error("bad call to notifyWarnWi"); // want stack trace?
			return;
		}
		String orderId = inWi.getContainerId(); // We really want order ID, but site controller only has this denormalized

		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Work_Instruction");
			// Pretty goofy code duplication, but can avoid some run time execution if loglevel would not result in this logging
			if (needWarn)
				LOGGER.info("{} for order/cntr:{} item:{} location:{}",
					inVerb,
					orderId,
					inWi.getItemId(),
					inWi.getPickInstruction());
			else
				LOGGER.info("{} for order/cntr:{} item:{} location:{}",
					inVerb,
					orderId,
					inWi.getItemId(),
					inWi.getPickInstruction());
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}

		NotificationMessage message = new NotificationMessage(Che.class, getPersistentId(), getMyGuidStr(), getUserId(), inVerb);
		if (!inWi.isHousekeeping()) {
			message.setWorkInstructionId(inWi.getPersistentId());
		}
		mDeviceManager.sendNotificationMessage(message);
	}

	protected void notifyOrderToPutWall(String orderId, String locationName) {
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Order_Into_Wall");
			LOGGER.info("Put order/cntr:{} into put wall location:{}", orderId, locationName);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	protected void notifyRemoveOrderFromChe(String orderId, Byte orderPositionOnChe) {
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Remove_Order_Che");
			LOGGER.info("Removed order/cntr:{} from position:{}", orderId, orderPositionOnChe);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	protected void notifyCheWorkerVerb(String inVerb, String otherInformation) {
		// VERBS initially are LOGIN, LOGOUT, BEGIN, SETUP, START_PATH, COMPLETE_PATH

		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Worker_Action");
			LOGGER.info(inVerb);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}

	}

	protected void notifyPutWallResponse(final List<WorkInstruction> inWorkItemList, String wallType) {
		int listsize = 0;
		if (inWorkItemList != null)
			listsize = inWorkItemList.size();

		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Wall_Plans_Response");
			LOGGER.info("{} work instructions in " + wallType + " response", listsize);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	protected void notifyPutWallItem(String itemOrUpd, String wallname) {
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Wall_Plans_Request");
			LOGGER.info("Request plans for item:{} in put wall:{}", itemOrUpd, wallname);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}

	}

	protected void notifyScanInventoryUpdate(String locationStr, String itemOrGtin) {
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Inventory_Update");
			LOGGER.info("Inventory update for item/gtin:{} to location:{}", itemOrGtin, locationStr);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	protected void notifyButton(int buttonNum, int showingQuantity) {
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Button");
			LOGGER.info("Button #{} pressed with quantity {}", buttonNum, showingQuantity);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}

		/* Remove--we do not need button presses in our database.
		NotificationMessage message = new NotificationMessage(Che.class,
			getPersistentId(),
			getMyGuidStr(),
			getUserId(),
			EventType.BUTTON);
		mDeviceManager.sendNotificationMessage(message);
		*/
	}

	protected void notifyOffCheButton(int buttonNum, int showingQuantity, String fromGuidId) {
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Wall_Button_Press");
			LOGGER.info("Wall Button #{} device:{} pressed with quantity {}", buttonNum, fromGuidId, showingQuantity);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	protected void notifyNonChePosconLight(String controllerId, int posconIndex, WorkInstruction wi) {
		if (wi == null) {
			LOGGER.error("null work instruction in notifyNonChePosconLight");
			return;
		}
		int displayCount = wi.getPlanQuantity();
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Wall_Button_Display");
			LOGGER.info("Button #{} device:{} will show count:{} for active job", posconIndex, controllerId, displayCount);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	protected void notifyScan(String theScan) {
		// LOGGER.info("*Scan {} by picker:{} device:{}", theScan, getUserId(), getMyGuidStr());

		// new
		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Scan");
			LOGGER.info(theScan);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
		}
	}

	//--------------------------
	/**
	 * CHE_DISPLAY notifies
	 */
	protected void notifyDisplayTag(String logStr, String tagName) {
		boolean guidChange = false;
		String loggerNetGuid = org.apache.logging.log4j.ThreadContext.get(THREAD_CONTEXT_NETGUID_KEY);

		try {
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_WORKER_KEY, getUserId());
			org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_TAGS_KEY, tagName);

			// A kludge to cover up some sloppiness of lack of logging context. And also, even without sloppiness, some cases happen
			// somewhat independent of a transaction context

			String myGuid = this.getMyGuidStr();
			if (!myGuid.equals(loggerNetGuid)) {
				org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_NETGUID_KEY, myGuid);
				guidChange = true;
			}
			LOGGER.info(logStr);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_WORKER_KEY);
			org.apache.logging.log4j.ThreadContext.remove(THREAD_CONTEXT_TAGS_KEY);
			if (guidChange)
				org.apache.logging.log4j.ThreadContext.put(THREAD_CONTEXT_NETGUID_KEY, loggerNetGuid);
		}
	}

	protected void notifyScreenDisplay(String screenSummary) {
		notifyDisplayTag(screenSummary, "CHE_DISPLAY Screen");
	}

	protected void notifyLeds(String ledSummary) {
		notifyDisplayTag(ledSummary, "CHE_DISPLAY Lights");
	}

	protected void notifyPoscons(String posconSummary) {
		notifyDisplayTag(posconSummary, "CHE_DISPLAY Poscons");
	}

	protected void notifyLink(NetGuid linkingTo) {
		if (linkingTo == null) {
			LOGGER.error("null guid in notifyLink");
			return;
		}
		String toNotify = String.format("Linking to %s. Taking remote control.", linkingTo.getHexStringNoPrefix());
		notifyDisplayTag(toNotify, "CHE_EVENT Link");
	}

	protected void notifyUnlink(NetGuid unlinkFrom) {
		if (unlinkFrom == null) {
			LOGGER.error("null guid in notifyUnLink");
			return;
		}
		String toNotify = String.format("Unlink from %s. Giving up remote control.", unlinkFrom.getHexStringNoPrefix());
		notifyDisplayTag(toNotify, "CHE_EVENT Unlink");
	}

	/*
	 * Helper functions
	 */

	public String getUserId() {
		// getUserId has an override. But notifyPoscons may be called from putwall which does not have an inherent user.");
		return "";
	}

}
