package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.AisleDeviceLogic;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.testframework.IntegrationTest;
import com.codeshelf.testframework.ServerTest;

public class PosConControllerTest extends ServerTest{
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PosConControllerTest.class);
	
	@Test
	public final void changeLedControllerType() throws IOException{
		UUID facilityId = null;
		String defControllerId = "99999999";
		String newControllerId = "10000001";
		
		//Create facility
		TenantPersistenceService.getInstance().beginTransaction();
		Facility facility = setUpOneAisleFourBaysFlatFacilityWithOrders();
		facilityId = facility.getPersistentId();
		TenantPersistenceService.getInstance().commitTransaction();
		
		super.startSiteController();
		waitAndGetAisleDeviceLogic(this, new NetGuid(defControllerId));
		
		//Get and modify controller
		TenantPersistenceService.getInstance().beginTransaction();
		LedController controller = getController(facilityId, defControllerId);
		Assert.assertEquals(controller.getDomainId(), defControllerId);
		Assert.assertEquals(DeviceType.Lights, controller.getDeviceType());
		controller.updateFromUI(newControllerId, "Poscons");
		TenantPersistenceService.getInstance().commitTransaction();
		
		//Confirm the change through DB access
		TenantPersistenceService.getInstance().beginTransaction();
		controller = getController(facilityId, newControllerId);
		Assert.assertEquals(controller.getDomainId(), newControllerId);
		Assert.assertEquals(DeviceType.Poscons, controller.getDeviceType());
		TenantPersistenceService.getInstance().commitTransaction();
		
		//Confirm the change through site controller
		waitAndGetPosConController(this, new NetGuid(newControllerId));
	}
	
	private LedController getController(UUID facilityId, String controllerId) {
		Facility facility = Facility.staticGetDao().findByPersistentId(facilityId);
		List<CodeshelfNetwork> networks = facility.getNetworks();
		Assert.assertFalse(networks.isEmpty());
		Map<String, LedController> ledControllers = networks.get(0).getLedControllers();
		Assert.assertFalse(ledControllers.isEmpty());
		LedController controller = ledControllers.get(controllerId);
		return controller;
	}
	
	protected PosManagerSimulator waitAndGetPosConController(final IntegrationTest test, final NetGuid deviceGuid) {
		Callable<PosManagerSimulator> createPosManagerSimulator = new Callable<PosManagerSimulator> () {
			@Override
			public PosManagerSimulator call() throws Exception {
				PosManagerSimulator managerSimulator = new PosManagerSimulator(test, deviceGuid);
				return (managerSimulator.getControllerLogic() != null)? managerSimulator : null;
			}
		};
		
		PosManagerSimulator managerSimulator = new WaitForResult<PosManagerSimulator>(createPosManagerSimulator).waitForResult();
		return managerSimulator; 
	}
	
	protected AisleDeviceLogic waitAndGetAisleDeviceLogic(final IntegrationTest test, final NetGuid deviceGuid) {
		Callable<AisleDeviceLogic> getAisleLogic = new Callable<AisleDeviceLogic> () {
			@Override
			public AisleDeviceLogic call() throws Exception {
				INetworkDevice deviceLogic = test.getDeviceManager().getDeviceByGuid(deviceGuid);
				return (deviceLogic instanceof AisleDeviceLogic) ? (AisleDeviceLogic)deviceLogic : null;
			}
		};
		
		AisleDeviceLogic aisleLogic = new WaitForResult<AisleDeviceLogic>(getAisleLogic).waitForResult();
		return aisleLogic; 
	}
}