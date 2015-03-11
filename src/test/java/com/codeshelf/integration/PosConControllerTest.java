package com.codeshelf.integration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.edi.VirtualSlottedFacilityGenerator;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.testframework.ServerTest;

public class PosConControllerTest extends ServerTest{
	@Test
	public final void changeLedControllerType(){
		UUID facilityId = null;
		
		TenantPersistenceService.getInstance().beginTransaction();
		//Create facility
		VirtualSlottedFacilityGenerator generator = new VirtualSlottedFacilityGenerator(getDefaultTenant(),createAisleFileImporter(), createLocationAliasImporter(), createOrderImporter());
		Facility facility = generator.generateFacilityForVirtualSlotting(testName.getMethodName());
		facilityId = facility.getPersistentId();
		//Get controller
		LedController controller = getController(facilityId, "99999998");
		Assert.assertEquals(controller.getDomainId(), "99999998");
		Assert.assertEquals(DeviceType.Lights, controller.getDeviceType());
		//Modify controller
		controller.updateFromUI("00000011", "Poscons");
		TenantPersistenceService.getInstance().commitTransaction();
		
		TenantPersistenceService.getInstance().beginTransaction();
		controller = getController(facilityId, "00000011");
		Assert.assertEquals(controller.getDomainId(), "00000011");
		Assert.assertEquals(DeviceType.Poscons, controller.getDeviceType());
		TenantPersistenceService.getInstance().commitTransaction();
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
}
