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

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.AisleDeviceLogic;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.testframework.IntegrationTest;
import com.codeshelf.testframework.ServerTest;

public class PosConControllerTest extends ServerTest{
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(PosConControllerTest.class);
	private String DEF_CONTROLLER_ID = "99999999";
	

	@Test
	public final void changeLedControllerType() throws IOException{
		UUID facilityId = null;
		
		//Create facility
		//TenantPersistenceService.getInstance().beginTransaction();
		Facility facility = setUpOneAisleFourBaysFlatFacilityWithOrders();
		facilityId = facility.getPersistentId();
		//TenantPersistenceService.getInstance().commitTransaction();
		
		super.startSiteController();
		// Test framework had set up 3 ledcons. Use them
		
		//Get and modify controller
		TenantPersistenceService.getInstance().beginTransaction();
		LedController controller = getController(facilityId, ledconId1);
		Assert.assertEquals(DeviceType.Lights, controller.getDeviceType());
		controller.updateFromUI(ledconId1, "Poscons");
		TenantPersistenceService.getInstance().commitTransaction();
		
		//Confirm the change through DB access
		TenantPersistenceService.getInstance().beginTransaction();
		controller = getController(facilityId, ledconId1);
		Assert.assertEquals(controller.getDomainId(), ledconId1);
		Assert.assertEquals(DeviceType.Poscons, controller.getDeviceType());
		TenantPersistenceService.getInstance().commitTransaction();
		
		//Confirm the change through site controller
		waitAndGetPosConController(this, new NetGuid(ledconId1));
	}
	
	@Test
	public final void runPutWallProcess() throws IOException{
		setUpFacilityWithPutWallAndOrders();
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
	
	private Facility setUpFacilityWithPutWallAndOrders() throws IOException{
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
		beginTransaction();
		Facility facility = getFacility().reload();
		importAislesData(facility, aislesCsvString);
		commitTransaction();
		
		// Get the aisle
		beginTransaction();
		facility = facility.reload();
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);
		
		//Assign path to aisle
		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 3d, 6d, 5d, 6d);
		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);
		
		//Import locations
		String csvLocationAliases = "mappedLocationId,locationAlias\n" + 
				"A1.B1.T1.S1,LocP1\n" + 
				"A1.B1.T1.S2,LocP2\n" + 
				"A1.B1.T1.S3,LocP3\n" + 
				"A1.B1.T1.S4,LocP4\n" + 
				"A1.B2.T1.S1,LocP5\n" + 
				"A1.B2.T1.S2,LocP6\n" + 
				"A1.B2.T1.S3,LocP7\n" + 
				"A1.B2.T1.S4,LocP8\n" + 
				"A1.B3.T1.S1,LocP9\n" + 
				"A1.B3.T1.S2,LocP10\n" + 
				"A1.B3.T1.S3,LocP11\n" + 
				"A1.B3.T1.S4,LocP12\n" + 
				"A1.B4.T1.S1,LocP13\n" + 
				"A1.B4.T1.S2,LocP14\n" + 
				"A1.B4.T1.S3,LocP15\n" + 
				"A1.B4.T1.S4,LocP16";//
		importLocationAliasesData(facility, csvLocationAliases);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();	
		CodeshelfNetwork network = getNetwork();
		//Change LED controller to PosManager
		LedController controller = network.findOrCreateLedController(DEF_CONTROLLER_ID, new NetGuid(DEF_CONTROLLER_ID));
		controller.updateFromUI(DEF_CONTROLLER_ID, "Poscons");
		Assert.assertEquals(DeviceType.Poscons, controller.getDeviceType());

		//Assign PosCon controller and indexies to tiers
		String[] tierNames = {"A1.B1.T1", "A1.B2.T1", "A1.B3.T1", "A1.B4.T1"};
		int posconIndex = 1;
		for (String tierName : tierNames) {
			Location tier = facility.findSubLocationById(tierName);
			controller.addLocation(tier);
			tier.setLedChannel((short)1);
			tier.setPosconIndex(posconIndex);
			tier.getDao().store(tier);
			posconIndex += 4;
		}
		
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		commitTransaction();

		//Import Cross-Batch orders
		beginTransaction();
		facility = facility.reload();
		String orderCsvString = "orderGroupId,orderId,orderDetailId,itemId,description,quantity,uom\n" + 
				"Group1,Order1,5397af8406851f02000014d0,TrailMix,\"bag, nanajoesgranola, Gluten-Free Tony's Trail Mix\",11,bag\n" + 
				"Group1,Order1,5397af8406851f02000014af,PickleOfTheMonth,\"jar, emmys, Pickle of the Month\",22,jar\n" + 
				"Group1,Order1,5397af8406851f02000014d2,Bagels,\"1/2 dozen, sourflour, Assorted Bagels\",32,1/2 dozen\n" + 
				"Group1,Order1,5397af8406851f02000014b4,Bread,\"loaf, tartinebread, Porridge Bread\",89,loaf\n" + 
				"Group1,Order1,5397af8406851f02000014ce,TortillaChips,\"bag, primavera, Tortilla Chips\",99,bag\n" + 
				"Group1,Order1,5398ad2b583349020000007a,SeaSalt,\"jar, oaktownspiceshop, Pacific Fine Sea Salt\",1,jar\n" + 
				"Group1,Order1,5398ad2b583349020000007b,FlakeSalt,\"jar, oaktownspiceshop, Cyprus White Flake Sea Salt\",1,jar\n" + 
				"Group1,Order1,5398ad2b5833490200000077,ChocolateCupcakes,\"box, nattycakes, Spring Chocolate Cupcakes\",88,box\n" + 
				"Group1,Order2,5397af8406851f02000013d0,TrailMix,\"bag, nanajoesgranola, Gluten-Free Tony's Trail Mix\",73,bag\n" + 
				"Group1,Order2,5397af8406851f02000013af,PickleOfTheMonth,\"jar, emmys, Pickle of the Month\",19,jar\n" + 
				"Group1,Order2,5397af8406851f02000013d2,Bagels,\"1/2 dozen, sourflour, Assorted Bagels\",46,1/2 dozen\n" + 
				"Group1,Order2,5397af8406851f02000013b4,Bread,\"loaf, tartinebread, Porridge Bread\",85,loaf\n" + 
				"Group1,Order3,5397af8406851f02000013ce,TortillaChips,\"bag, primavera, Tortilla Chips\",78,bag\n" + 
				"Group1,Order3,5398ad2b583349020000307a,SeaSalt,\"jar, oaktownspiceshop, Pacific Fine Sea Salt\",33,jar\n" + 
				"Group1,Order3,5398ad2b583349020000307b,FlakeSalt,\"jar, oaktownspiceshop, Cyprus White Flake Sea Salt\",63,jar\n" + 
				"Group1,Order3,5398ad2b5833490200003077,ChocolateCupcakes,\"box, nattycakes, Spring Chocolate Cupcakes\",9,box";
		importOrdersData(facility, orderCsvString);
		commitTransaction();
		
		//Import Slotting
		beginTransaction();		
		facility = facility.reload();
		String slottingCsvString = "orderId,locationId\n" + 
				"Order7,LocP8\n" + 
				"Order7,LocP11\n" + 
				"Order8,LocP15\n" + 
				"Order9,LocP16";
		importSlotting(facility, slottingCsvString);
		commitTransaction();

		//Import Batch data
		beginTransaction();		
		facility = facility.reload();
		String batchCsvStrng = "itemId,orderGroupId,containerId,description,quantity,uom\n" + 
				"TrailMix,Group1,1,\"bag, nanajoesgranola, Gluten-Free Tony's Trail Mix\",2,bag\n" + 
				"PickleOfTheMonth,Group1,2,\"jar, emmys, Pickle of the Month\",3,jar\n" + 
				"Bagels,Group1,1,\"1/2 dozen, sourflour, Assorted Bagels\",3,1/2 dozen\n" + 
				"Bread,Group1,2,\"loaf, tartinebread, Porridge Bread\",2,loaf\n" + 
				"TortillaChips,Group1,1,\"bag, primavera, Tortilla Chips\",4,bag\n" + 
				"SeaSalt,Group1,2,\"jar, oaktownspiceshop, Pacific Fine Sea Salt\",2,jar\n" + 
				"FlakeSalt,Group1,1,\"jar, oaktownspiceshop, Cyprus White Flake Sea Salt\",3,jar\n" + 
				"ChocolateCupcakes,Group1,2,\"box, nattycakes, Spring Chocolate Cupcakes\",3,box";
		importBatchData(facility, batchCsvStrng);
		commitTransaction();
		return facility;
	}
}