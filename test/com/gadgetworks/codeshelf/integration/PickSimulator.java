package com.gadgetworks.codeshelf.integration;

import java.util.List;

import lombok.Getter;

import org.junit.Assert;

import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.device.CheStateEnum;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.ThreadUtils;
import com.gadgetworks.flyweight.command.CommandControlButton;
import com.gadgetworks.flyweight.command.NetGuid;

public class PickSimulator {
	
	EndToEndIntegrationTest test;
	
	@Getter
	CheDeviceLogic cheDeviceLogic;
	
	public PickSimulator(EndToEndIntegrationTest test, NetGuid cheGuid) {
		this.test = test;
		// verify that che is in site controller's device list
		cheDeviceLogic = (CheDeviceLogic) test.getSiteController().getDeviceManager().getDeviceByGuid(cheGuid);
		Assert.assertNotNull(cheDeviceLogic);
	}

	public void login(String pickerId) {
		// Set up a cart for order 12345, which will generate work instructions
		cheDeviceLogic.scanCommandReceived("U%"+pickerId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT,1000);
	}

	public void setupContainer(String containerId, String positionId) {
		cheDeviceLogic.scanCommandReceived("C%"+containerId);
		waitForCheState(CheStateEnum.CONTAINER_POSITION,1000);

		cheDeviceLogic.scanCommandReceived("P%"+positionId);
		waitForCheState(CheStateEnum.CONTAINER_SELECT,1000);
	}

	public List<WorkInstruction> start(String location) {
		cheDeviceLogic.scanCommandReceived("X%START");
		if (location==null) {
			// perform start without location scan, if location is undefined
			return null;
		}
		waitForCheState(CheStateEnum.LOCATION_SELECT,5000);
		cheDeviceLogic.scanCommandReceived("L%"+location);
		waitForCheState(CheStateEnum.DO_PICK,1000);
		
		List<WorkInstruction> activeList = cheDeviceLogic.getActivePickWiList();
		return activeList;
	}	
	
	public List<WorkInstruction> pick(int position, int quantity) {
		CommandControlButton buttonCommand = new CommandControlButton();
		buttonCommand.setPosNum((byte) position);
		buttonCommand.setValue((byte) quantity);
		cheDeviceLogic.buttonCommandReceived(buttonCommand);
		List<WorkInstruction> activeList = cheDeviceLogic.getActivePickWiList();
		return activeList;
	}
	
	public void logout() {
		cheDeviceLogic.scanCommandReceived("X%LOGOUT");		
		waitForCheState(CheStateEnum.IDLE,1000);
	}

	public void waitForCheState(CheStateEnum state, int timeoutInMillis) {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start<timeoutInMillis) {
			// retry every 100ms
			ThreadUtils.sleep(100);
			if (cheDeviceLogic.getCheStateEnum()==state) {
				// expected state found - all good
				return;
			}
		}
		Assert.fail("Che state "+state+" not encountered in "+timeoutInMillis+"ms");
	}
}
