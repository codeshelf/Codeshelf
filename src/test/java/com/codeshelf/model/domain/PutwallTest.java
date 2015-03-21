/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.LedSample;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.device.PosControllerInstr.Brightness;
import com.codeshelf.device.PosControllerInstr.Frequency;
import com.codeshelf.device.PosManagerDeviceLogic;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.AislesFileCsvImporter.ControllerLayout;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.DeviceType;
import com.codeshelf.testframework.MockDaoTest;

public class PutwallTest extends MockDaoTest {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PutwallTest.class);
	
	private PosManagerDeviceLogic mController;
	private NetGuid mControllerGuid;
	
	@Before
	public void initTest(){
		IRadioController radioController = mock(IRadioController.class);
		mController = new PosManagerDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"), mock(CsDeviceManager.class), radioController);
		mControllerGuid = mController.getGuid();
	}

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
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());		
		Facility facility= Facility.createFacility(getDefaultTenant(),fName, "TEST", Point.getZeroPoint());
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(getDefaultTenant(),new StringReader(csvString), facility, ediProcessTime);
		CodeshelfNetwork network = facility.getNetworks().get(0);
		LedController controller1 = network.findOrCreateLedController(getDefaultTenant(),fName, new NetGuid("0x00000011"));
		String uuid1 = controller1.getPersistentId().toString();
		Tier tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		tier.setControllerChannel(getDefaultTenant(),uuid1, "1", "aisle");
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

				
		// LedController should be in light mode initially
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Aisle aisle = Aisle.staticGetDao().findByDomainId(getDefaultTenant(),facility, "A1");
		Assert.assertNotNull(aisle);
		tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		List<Slot> slots = aisle.getActiveChildrenAtLevel(Slot.class);
		Slot slot = slots.get(0);
		LedController ledController = tier.getLedController();
		ledController = LedController.staticGetDao().findByPersistentId(getDefaultTenant(),ledController.getPersistentId());
		Assert.assertEquals(DeviceType.Lights, ledController.getDeviceType());
		Assert.assertTrue(tier.isLightableAisleController());
		Assert.assertTrue(slot.isLightableAisleController());
		Assert.assertFalse(tier.isLightablePoscon());
		Assert.assertFalse(slot.isLightablePoscon());	
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

		// switch ledcontroller to poscon mode and set poscons on slots
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		tier = (Tier) facility.findSubLocationById("A1.B1.T1");
		ledController = tier.getLedController();
		ledController.setDeviceType(DeviceType.Poscons);
		LedController.staticGetDao().store(getDefaultTenant(),ledController);
		tier.setPoscons(getDefaultTenant(),5);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		
		// check controller, tier and slots
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		
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

		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}

	@Test
	public void testInstructionsOperations() {
		NetGuid che1Guid = new NetGuid("0x00000001");
		NetGuid che2Guid = new NetGuid("0x00000002");
		//Use commands sent to controller's own device to take a deeper look at PosCon displays
		
		//SEND INSTRUCTIONS (from CHE's and from the PosCon controller itself)
		mController.addPosConInstrFor(che1Guid, null, (byte)2, (byte)50, (byte)40, (byte)60, Frequency.SOLID.toByte(), Brightness.BRIGHT.toByte());
		mController.addPosConInstrFor(che1Guid, null, (byte)4, (byte)6, (byte)3, (byte)9, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		mController.addPosConInstrFor(che2Guid, null, (byte)4, (byte)60, (byte)30, (byte)90, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		mController.addPosConInstrFor(mControllerGuid, null, (byte)3, (byte)65, (byte)35, (byte)95, Frequency.SOLID.toByte(), Brightness.MEDIUM.toByte());
		mController.updatePosCons(true);
		
		//Test what was saved
		PosControllerInstr instr1 = mController.getPosConInstrFor(che1Guid, (byte)2);
		PosControllerInstr instr2 = mController.getPosConInstrFor(che1Guid, (byte)4);
		PosControllerInstr instr3 = mController.getPosConInstrFor(che2Guid, (byte)4);
		PosControllerInstr instr4 = mController.getPosConInstrFor(mControllerGuid, (byte)3);
		assertInstruction(instr1, null, (byte)2, (byte)50, (byte)40, (byte)60, Frequency.SOLID.toByte(), Brightness.BRIGHT.toByte());
		assertInstruction(instr2, null, (byte)4, (byte)6, (byte)3, (byte)9, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		assertInstruction(instr3, null, (byte)4, (byte)60, (byte)30, (byte)90, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		assertInstruction(instr4, null, (byte)3, (byte)65, (byte)35, (byte)95, Frequency.SOLID.toByte(), Brightness.MEDIUM.toByte());
				
		//Test what is being displyaed
		Map<Byte, PosControllerInstr> displayed = mController.getPosToLastSetIntrMap();
		PosControllerInstr dispOn2 = displayed.get((byte)2);
		PosControllerInstr dispOn3 = displayed.get((byte)3);
		PosControllerInstr dispOn4 = displayed.get((byte)4);
		compareInstructions(dispOn2, instr1);
		compareInstructions(dispOn3, instr4);
		compareInstructions(dispOn4, instr3);
		
		//CLEAR INSTRUCTIONS
		
		//Remove instruction that is incidentally currently being displayed on PosCon 4 
		mController.removePosConInstrsForSourceAndPositions(che2Guid, Arrays.asList(new Byte[]{4}));
		mController.updatePosCons(true);
		//Assert that the instruction is gone
		instr3 = mController.getPosConInstrFor(che2Guid, (byte)4);
		Assert.assertNull(instr3);
		//Assert that an earlier instruction is now displayed
		displayed = mController.getPosToLastSetIntrMap();
		dispOn4 = displayed.get((byte)4);
		compareInstructions(dispOn4, instr2);
		
		//Remove all instructions from a PosCon
		mController.removePosConInstrs((byte)2);
		mController.updatePosCons(true);
		//Assert that the instruction is gone
		instr1 = mController.getPosConInstrFor(che1Guid, (byte)2);
		Assert.assertNull(instr1);
		//Assert that nothing is displayed on the PosCon
		displayed = mController.getPosToLastSetIntrMap();
		dispOn2 = displayed.get((byte)2);
		Assert.assertNull(dispOn2);
		
		//Remove all instructions for a source device
		mController.removePosConInstrsForSource(mControllerGuid);
		mController.updatePosCons(true);
		//Assert that the instruction is gone
		instr4 = mController.getPosConInstrFor(mControllerGuid, (byte)3);
		Assert.assertNull(instr4);
		//Assert that nothing is displayed on the PosCon
		displayed = mController.getPosToLastSetIntrMap();
		dispOn3 = displayed.get((byte)3);
		Assert.assertNull(dispOn3);

		//Remove all remaining instructions
		mController.removeAllPosConInstrsAndSend();
		//Assert that the instruction is gone
		instr3 = mController.getPosConInstrFor(che2Guid, (byte)4);
		Assert.assertNull(instr3);
		//Assert that an earlier instruction is now displayed
		displayed = mController.getPosToLastSetIntrMap();
		Assert.assertTrue(displayed.isEmpty());
	}
	
	@Test
	public void testSerializer() {
		String message = 
				"[\n" + 
				"    {\n" + 
				"      \"controllerId\":\"0x0000002D\",\n" + 
				"      \"position\":1,\n" + 
				"      \"reqQty\":47,\n" + 
				"      \"brightness\":\"DIM\",\n" + 
				"      \"frequency\":\"BLINK\"\n" + 
				"    },\n" + 
				"    {\n" + 
				"      \"controllerId\":\"0x0000002D\",\n" + 
				"      \"position\":2,\n" + 
				"      \"reqQty\":10,\n" + 
				"      \"maxQty\":60,\n" + 
				"      \"brightness\":\"DIM\",\n" + 
				"      \"frequency\":\"SOLID\"\n" + 
				"    },\n" +
				"    {\n" + 
				"      \"controllerId\":\"0x0000002D\",\n" + 
				"      \"position\":2,\n" + 
				"      \"reqQty\":20,\n" + 
				"      \"maxQty\":70\n" + 
				"    }\n" + 
				"]";
		mController.lightExtraPosCons(message);
		mController.updatePosCons(true);
		
		PosControllerInstr instr1 = mController.getPosConInstrFor(mControllerGuid, (byte)1);
		PosControllerInstr instr2 = mController.getPosConInstrFor(mControllerGuid, (byte)2);
		
		assertInstruction(instr1, null, (byte)1, (byte)47, (byte)47, (byte)47, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		assertInstruction(instr2, null, (byte)2, (byte)20, (byte)20, (byte)70, Frequency.SOLID.toByte(), Brightness.BRIGHT.toByte());
	}
	
	private void assertInstruction(PosControllerInstr instruction, LedSample inLedSample, Byte position, Byte quantity, Byte min, Byte max, Byte frequency, Byte brightness) {
		Assert.assertNotNull(instruction);
		Assert.assertEquals(position, instruction.getPosition());
		Assert.assertEquals(quantity, instruction.getReqQty());
		Assert.assertEquals(min, instruction.getMinQty());
		Assert.assertEquals(max, instruction.getMaxQty());
		Assert.assertEquals(frequency, instruction.getFreq());
		Assert.assertEquals(brightness, instruction.getDutyCycle());
	}
	
	private void compareInstructions(PosControllerInstr instruction1, PosControllerInstr instruction2) {
		Assert.assertNotNull(instruction1);
		Assert.assertNotNull(instruction2);
		Assert.assertEquals(instruction1.getPosition(), instruction2.getPosition());
		Assert.assertEquals(instruction1.getReqQty(), instruction2.getReqQty());
		Assert.assertEquals(instruction1.getMinQty(), instruction2.getMinQty());
		Assert.assertEquals(instruction1.getMaxQty(), instruction2.getMaxQty());
		Assert.assertEquals(instruction1.getFreq(), instruction2.getFreq());
		Assert.assertEquals(instruction1.getDutyCycle(), instruction2.getDutyCycle());
	}

}
