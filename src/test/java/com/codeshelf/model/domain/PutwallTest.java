/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.AislesFileCsvImporter.ControllerLayout;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.DeviceType;
import com.codeshelf.testframework.MockDaoTest;

public class PutwallTest extends MockDaoTest {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PutwallTest.class);

	@Test
	public final void posconModeTest() {

		String controllerLayoutStr = ControllerLayout.tierLeft.name();

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,"+ controllerLayoutStr +",12.85,43.45,X,40,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Aisle,A2,,,,,"+ controllerLayoutStr +",12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n"; //

		String fName = "F-Foobar";
		
		// initial data setup
		this.getTenantPersistenceService().beginTransaction();		
		Facility facility= Facility.createFacility(fName, "TEST", Point.getZeroPoint());
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		CodeshelfNetwork network = facility.getNetworks().get(0);
		LedController controller1 = network.findOrCreateLedController(fName, new NetGuid("0x00000011"));
		String uuid1 = controller1.getPersistentId().toString();
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		tier.setControllerChannel(uuid1, "1", "aisle");
		this.getTenantPersistenceService().commitTransaction();

				
		// LedController should be in light mode initially
		this.getTenantPersistenceService().beginTransaction();
		Aisle aisle = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle);
		tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		List<Slot> slots = aisle.getActiveChildrenAtLevel(Slot.class);
		Slot slot = slots.get(0);
		LedController ledController = tier.getLedController();
		ledController = LedController.DAO.findByPersistentId(ledController.getPersistentId());
		Assert.assertEquals(DeviceType.Lights, ledController.getDeviceType());
		Assert.assertTrue(tier.isLightableAisleController());
		Assert.assertTrue(slot.isLightableAisleController());
		Assert.assertFalse(tier.isLightablePoscon());
		Assert.assertFalse(slot.isLightablePoscon());	
		this.getTenantPersistenceService().commitTransaction();

		// switch ledcontroller to poscon mode and set poscons on slots
		this.getTenantPersistenceService().beginTransaction();
		tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ledController = tier.getLedController();
		ledController.setDeviceType(DeviceType.Poscons);
		LedController.DAO.store(ledController);
		tier.setPoscons(5);
		this.getTenantPersistenceService().commitTransaction();
		
		// check controller, tier and slots
		this.getTenantPersistenceService().beginTransaction();
		
		tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		Assert.assertFalse(tier.isLightableAisleController());
		Assert.assertFalse(tier.isLightablePoscon());
		
		slot = (Slot) facility.findSubLocationById("A1.B1.T1.S1");
		Assert.assertFalse(slot.isLightableAisleController());
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertEquals((Integer)5, slot.getPosconIndex());

		slot = (Slot) facility.findSubLocationById("A1.B1.T1.S2");
		Assert.assertFalse(slot.isLightableAisleController());
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertEquals((Integer)6, slot.getPosconIndex());

		slot = (Slot) facility.findSubLocationById("A1.B1.T1.S3");
		Assert.assertFalse(slot.isLightableAisleController());
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertEquals((Integer)7, slot.getPosconIndex());

		slot = (Slot) facility.findSubLocationById("A1.B1.T1.S4");
		Assert.assertFalse(slot.isLightableAisleController());
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertEquals((Integer)8, slot.getPosconIndex());

		slot = (Slot) facility.findSubLocationById("A1.B1.T1.S5");
		Assert.assertFalse(slot.isLightableAisleController());
		Assert.assertTrue(slot.isLightablePoscon());
		Assert.assertEquals((Integer)9, slot.getPosconIndex());

		this.getTenantPersistenceService().commitTransaction();
	}


}
