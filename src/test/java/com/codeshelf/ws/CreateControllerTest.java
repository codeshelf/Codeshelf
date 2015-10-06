package com.codeshelf.ws;

import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.DeviceType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.service.UiUpdateBehavior;
import com.codeshelf.testframework.HibernateTest;

public class CreateControllerTest extends HibernateTest{
	@Test
	public final void testCreateController() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = getFacility();
		Facility.staticGetDao().store(facility);
		UiUpdateBehavior service = new UiUpdateBehavior();
		UUID controllerId = service.addController(facility.getPersistentId().toString(), "1a", "Poscons");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		LedController controller = LedController.staticGetDao().findByPersistentId(controllerId);
		Assert.assertEquals("0000001a", controller.getDomainId());
		Assert.assertEquals("0x0000001a", controller.getDeviceGuidStr());
		Assert.assertEquals(DeviceType.Poscons, controller.getDeviceType());
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testUpdateController() {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = createFacility();
		Facility.staticGetDao().store(facility);
		UiUpdateBehavior service = new UiUpdateBehavior();
		UUID controllerId = service.addController(facility.getPersistentId().toString(), "1a", "Poscons");
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		LedController controller = LedController.staticGetDao().findByPersistentId(controllerId);
		controller.updateFromUI("2b", "Lights");
		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();
		controller = LedController.staticGetDao().findByPersistentId(controllerId);
		Assert.assertEquals("0000002b", controller.getDomainId());
		Assert.assertEquals("0x0000002b", controller.getDeviceGuidStr());
		Assert.assertEquals(DeviceType.Lights, controller.getDeviceType());
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void testDeleteControllerWithDependencies() throws IOException {
		Short one = new Short("1"), two = new Short("2");
		
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpFacilityWithAisles();
		Facility.staticGetDao().store(facility);
		UiUpdateBehavior service = new UiUpdateBehavior();
		UUID controllerId = service.addController(facility.getPersistentId().toString(), "1a", "Poscons");
		LedController controller = LedController.staticGetDao().findByPersistentId(controllerId);
		//Retrieve an aisle and an unrelated tier
		Aisle a1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		Aisle a2 = Aisle.staticGetDao().findByDomainId(facility, "A2");
		Bay b2_1 = Bay.staticGetDao().findByDomainId(a2, "B1");
		Tier t2_1_1 = Tier.staticGetDao().findByDomainId(b2_1, "T1");
		UUID aisleId = a1.getPersistentId();
		UUID tierId = t2_1_1.getPersistentId();
		//Assign controller to aisle and a tier
		a1.setLedController(controller);
		a1.setLedChannel(one);
		t2_1_1.setLedController(controller);
		t2_1_1.setLedChannel(two);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		//Verify that controller was saved
		controller = LedController.staticGetDao().findByPersistentId(controllerId);
		a1 = Aisle.staticGetDao().findByPersistentId(aisleId);
		t2_1_1 = Tier.staticGetDao().findByPersistentId(tierId);
		Assert.assertEquals(controller, a1.getLedController());
		Assert.assertEquals(one, a1.getLedChannel());
		Assert.assertEquals(controller, t2_1_1.getLedController());
		Assert.assertEquals(two, t2_1_1.getLedChannel());
		//Delete controller
		service.deleteController(controllerId);

		//Verify that controller was saved
		a1 = Aisle.staticGetDao().findByPersistentId(aisleId);
		t2_1_1 = Tier.staticGetDao().findByPersistentId(tierId);
		Assert.assertNull(a1.getLedController());
		Assert.assertNull(a1.getLedChannel());
		Assert.assertNull(t2_1_1.getLedController());
		Assert.assertNull(t2_1_1.getLedChannel());

		this.getTenantPersistenceService().commitTransaction();
	}


	private Facility setUpFacilityWithAisles() throws IOException{
		//Import aisles
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\n" + 
				"Aisle,A1,,,,,zigzagB1S1Side,2.85,10,X,20\n" + 
				"Bay,B1,50,,,,,,,,\n" + 
				"Tier,T1,50,4,4,0,,,,,\n" + 
				"Bay,B2,50,,,,,,,,\n" + 
				"Tier,T1,50,4,4,0,,,,,\n" + 
				"Bay,B3,50,,,,,,,,\n" + 
				"Tier,T1,50,4,4,0,,,,,\n" + 
				"Bay,B4,50,,,,,,,,\n" + 
				"Tier,T1,50,4,4,0,,,,,\n"; //
		importAislesData(getFacility(), aislesCsvString);
		return getFacility();
	}
}
