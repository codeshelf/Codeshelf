package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.testframework.IntegrationTest;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

public class PosConControllerTest extends ServerTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PosConControllerTest.class);
	
	@Test
	public final void changeLedControllerType() throws IOException{
		UUID facilityId = null;
		String newControllerId = "10000001";
		
		TenantPersistenceService.getInstance().beginTransaction();
		//Create facility
		Facility facility = setUpOneAisleFourBaysFlatFacilityWithOrders();
		facilityId = facility.getPersistentId();
		//Get controller
		LedController controller = getController(facilityId, "99999999");
		Assert.assertEquals(controller.getDomainId(), "99999999");
		Assert.assertEquals(DeviceType.Lights, controller.getDeviceType());
		//Modify controller
		controller.updateFromUI(newControllerId, "Poscons");
		TenantPersistenceService.getInstance().commitTransaction();
		
		//COnfirm the change through DB access
		TenantPersistenceService.getInstance().beginTransaction();
		controller = getController(facilityId, newControllerId);
		Assert.assertEquals(controller.getDomainId(), newControllerId);
		Assert.assertEquals(DeviceType.Poscons, controller.getDeviceType());
		TenantPersistenceService.getInstance().commitTransaction();
		
		//Confirm the change through site controller
		super.startSiteController();
		PosManagerSimulator controllerSimulator = waitAndGetController(this, new NetGuid(newControllerId));
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
	
	private PosManagerSimulator waitAndGetController(IntegrationTest test, NetGuid controllerGuid) {
		ThreadUtils.sleep(250);
		long start = System.currentTimeMillis();
		final long maxTimeToWaitMillis = 5000;
		int count = 0;
		while (System.currentTimeMillis() - start < maxTimeToWaitMillis) {
			count++;
			PosManagerSimulator controller = new PosManagerSimulator(test, controllerGuid);
			System.out.println(controller.getControllerLogic());
			if (controller.getControllerLogic() != null) {
				LOGGER.info(count + " PosManagers made in waitAndGetController before getting it right");
				return controller;
			}
			ThreadUtils.sleep(100); // retry every 100ms
		}
		Assert.fail(String.format("Did not encounter PosManager for device %s in %dms after %d checks.", controllerGuid, maxTimeToWaitMillis, count));
		return null;
	}
}