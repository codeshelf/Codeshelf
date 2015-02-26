package com.codeshelf.integration;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.ThreadUtils;

public class CheProcessScanPickReversable extends EndToEndIntegrationTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheProcessScanPickReversable.class);
	private static final int WAIT_TIME = 400000;

	private Facility setUpOneAisleFourBaysFlatFacilityWithOrders() throws IOException{
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" + 
				"Aisle,A1,,,,,zigzagB1S1Side,2.85,5,X,20\r\n" + 
				"Bay,B1,50,,,,,,,,\r\n" + 
				"Tier,T1,50,0,16,0,,,,,\r\n" + 
				"Bay,B2,50,,,,,,,,\r\n" + 
				"Tier,T1,50,0,16,0,,,,,\r\n" + 
				"Bay,B3,50,,,,,,,,\r\n" + 
				"Tier,T1,50,0,16,0,,,,,\r\n" + 
				"Bay,B4,50,,,,,,,,\r\n" + 
				"Tier,T1,50,0,16,0,,,,,\r\n"; //

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(new StringReader(aislesCsvString), getFacility(), ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 3d, 6d, 5d, 6d);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		String csvLocationAliases = "mappedLocationId,locationAlias\r\n" +
				"A1.B1.T1,LocX24\r\n" + 
				"A1.B2.T1,LocX25\r\n" + 
				"A1.B3.T1,LocX26\r\n" + 
				"A1.B4.T1,LocX27\r\n";//

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter locationAliasImporter = createLocationAliasImporter();
		locationAliasImporter.importLocationAliasesFromCsvStream(new StringReader(csvLocationAliases), getFacility(), ediProcessTime2);

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController(organizationId, new NetGuid("0x00000011"));

		Short channel1 = 1;
		Location tier = getFacility().findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		
		mPropertyService.changePropertyValue(getFacility(), DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		
		String inventory = "itemId,locationId,description,quantity,uom,inventoryDate,lotId,cmFromLeft\r\n" + 
				"Item1,LocX24,Item Desc 1,1000,a,12/03/14 12:00,,0\r\n" + 
				"Item2,LocX24,Item Desc 2,1000,a,12/03/14 12:00,,12\r\n" + 
				"Item3,LocX24,Item Desc 3,1000,a,12/03/14 12:00,,24\r\n" + 
				"Item4,LocX24,Item Desc 4,1000,a,12/03/14 12:00,,36\r\n" + 
				"Item5,LocX25,Item Desc 5,1000,a,12/03/14 12:00,,0\r\n" + 
				"Item6,LocX25,Item Desc 6,1000,a,12/03/14 12:00,,12\r\n" + 
				"Item7,LocX25,Item Desc 7,1000,a,12/03/14 12:00,,24\r\n" + 
				"Item8,LocX25,Item Desc 8,1000,a,12/03/14 12:00,,36\r\n" + 
				"Item9,LocX26,Item Desc 9,1000,a,12/03/14 12:00,,0\r\n" + 
				"Item10,LocX26,Item Desc 10,1000,a,12/03/14 12:00,,12\r\n" + 
				"Item11,LocX26,Item Desc 11,1000,a,12/03/14 12:00,,24\r\n" + 
				"Item12,LocX26,Item Desc 12,1000,a,12/03/14 12:00,,36\r\n" + 
				"Item13,LocX27,Item Desc 13,1000,a,12/03/14 12:00,,0\r\n" + 
				"Item14,LocX27,Item Desc 14,1000,a,12/03/14 12:00,,12\r\n" + 
				"Item15,LocX27,Item Desc 15,1000,a,12/03/14 12:00,,24\r\n" + 
				"Item16,LocX27,Item Desc 16,1000,a,12/03/14 12:00,,36\r\n";
		importInventoryData(getFacility(), inventory);
		
		String orders = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,workSequence,locationId\r\n" + 
				"1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1,,\r\n" + 
				"1,1,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1,,\r\n" + 
				"1,1,347,12/03/14 12:00,12/31/14 12:00,Item11,,120,a,Group1,,\r\n" + 
				"1,1,348,12/03/14 12:00,12/31/14 12:00,Item9,,11,a,Group1,,\r\n" + 
				"1,1,349,12/03/14 12:00,12/31/14 12:00,Item2,,22,a,Group1,,\r\n" + 
				"1,1,350,12/03/14 12:00,12/31/14 12:00,Item5,,33,a,Group1,,\r\n" + 
				"1,1,351,12/03/14 12:00,12/31/14 12:00,Item3,,22,a,Group1,5,LocX24\r\n" + 
				"1,1,352,12/03/14 12:00,12/31/14 12:00,Item6,,33,a,Group1,3,LocX26\r\n" + 
				"2,2,353,12/03/14 12:00,12/31/14 12:00,Item3,,44,a,Group1,,\r\n" + 
				"2,2,354,12/03/14 12:00,12/31/14 12:00,Item15,,55,a,Group1,,\r\n" + 
				"2,2,355,12/03/14 12:00,12/31/14 12:00,Item2,,66,a,Group1,,\r\n" + 
				"2,2,356,12/03/14 12:00,12/31/14 12:00,Item8,,77,a,Group1,,\r\n" + 
				"2,2,357,12/03/14 12:00,12/31/14 12:00,Item14,,77,a,Group1,,\r\n";
		importOrdersData(getFacility(), orders);
		return getFacility();
	}
	
	private PickSimulator waitAndGetPickerForProcessType(EndToEndIntegrationTest test, NetGuid cheGuid, String inProcessType) {
		// took over 250 ms on JR's fast macbook pro. Hence the initial wait, then checking more frequently in the loop
		ThreadUtils.sleep(250);
		long start = System.currentTimeMillis();
		final long maxTimeToWaitMillis = 5000;
		String existingType = "";
		int count = 0;
		while (System.currentTimeMillis() - start < maxTimeToWaitMillis) {
			count++;
			PickSimulator picker = new PickSimulator(test, cheGuid);
			existingType = picker.getProcessType();
			if (existingType.equals(inProcessType)) {
				LOGGER.info(count + " pickers made in waitAndGetPickerForProcessType before getting it right");
				return picker;
			}
			ThreadUtils.sleep(100); // retry every 100ms
		}
		Assert.fail("Process type " + inProcessType + " not encountered in " + maxTimeToWaitMillis + "ms. Process type is "
				+ existingType);
		return null;
	}

	private PickSimulator setupTestPicker() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpOneAisleFourBaysFlatFacilityWithOrders();

		this.getTenantPersistenceService().commitTransaction();

		PickSimulator picker = waitAndGetPickerForProcessType(this, cheGuid1, "CHE_SETUPORDERS");

		Assert.assertEquals(CheStateEnum.IDLE, picker.currentCheState());

		
		LOGGER.info("1a: leave LOCAPICK off, set SCANPICK, set BayDistance");

		
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Assert.assertNotNull(facility);
		mPropertyService.changePropertyValue(facility, DomainObjectProperty.LOCAPICK, Boolean.toString(false));
		mPropertyService.changePropertyValue(facility, DomainObjectProperty.SCANPICK, "SKU");
		mPropertyService.changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());

		mPropertyService.turnOffHK(facility);
		this.getTenantPersistenceService().commitTransaction();	
		
		CsDeviceManager manager = this.getDeviceManager();
		Assert.assertNotNull(manager);
		
		// We would rather have the device manager know from parameter updates, but that does not happen yet in the integration test.
		manager.setSequenceKind(WorkInstructionSequencerType.BayDistance.toString());
		Assert.assertEquals(WorkInstructionSequencerType.BayDistance.toString(), manager.getSequenceKind());
		manager.setScanTypeValue("SKU");
		Assert.assertEquals("SKU", manager.getScanTypeValue());
		picker.forceDeviceToMatchManagerConfiguration();
		
		return picker;
	}
	
	private void compareList(List<WorkInstruction> instructions, String[] expectations) {
		Assert.assertEquals(instructions.size(), expectations.length);
		for (int i = 0; i < expectations.length; i++) {
			WorkInstruction instruction = instructions.get(i);
			Assert.assertEquals(String.format("Mismatch in item %d. Expected list %s, got [%s]", i, Arrays.toString(expectations), printInstructionsList(instructions)),
				instruction.getItemId(), expectations[i]);
		}
	}
	
	private String printInstructionsList(List<WorkInstruction> instructions) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < instructions.size(); i++) {
			result.append(instructions.get(i).getItemId());
			if (i < instructions.size() - 1) {
				result.append(",");
			}
		}
		return result.toString();
	}

	
	@Test
	public void testPickForwardForward() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item2","Item3","Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations);
	}
	
	@Test
	public void testPickReverseReverse() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations);
	}
	
	@Test
	public void testPickForwardReverse() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations);
	}

	@Test
	public void testPickReverseForward() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item2","Item3","Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations);
	}
	
	@Test
	public void testPickMultipleDoubleStarts() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);
		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations1 = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations1);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);
		scWiList = picker.getAllPicksList();
		String[] expectations2 = {"Item2","Item3","Item5","Item7","Item9","Item11","Item15"};
		compareList(scWiList, expectations2);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);
		scWiList = picker.getAllPicksList();
		String[] expectations3 = {"Item15","Item11","Item9","Item7","Item5","Item3","Item2"};
		compareList(scWiList, expectations3);
	}
	
	@Test
	public void testPickForwardAndLocation() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item9","Item11","Item15","Item2","Item3","Item5","Item7"};
		compareList(scWiList, expectations);
	}

	@Test
	public void testPickReverseAndLocation() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item7","Item5","Item3","Item2","Item15","Item11","Item9"};
		compareList(scWiList, expectations);
	}

	@Test
	public void testPickForwardForwardThenJump() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);
		
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item9","Item11","Item15","Item2","Item3","Item5","Item7"};
		compareList(scWiList, expectations);
	}
	
	@Test
	public void testPickReverseReverseThenJump() throws IOException{
		PickSimulator picker = setupTestPicker();
		
		picker.loginAndCheckState("Picker #1", CheStateEnum.CONTAINER_SELECT);
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.setupContainer("1", "1"); 
		picker.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		picker.scanCommand(CheDeviceLogic.REVERSE_COMMAND);
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);
		
		picker.scanLocation("LocX26");
		picker.waitForCheState(CheStateEnum.SCAN_SOMETHING, WAIT_TIME);

		List<WorkInstruction> scWiList = picker.getAllPicksList();
		String[] expectations = {"Item7","Item5","Item3","Item2","Item15","Item11","Item9"};
		compareList(scWiList, expectations);
	}


}
