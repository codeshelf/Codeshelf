package com.codeshelf.integration;

import java.io.IOException;

import org.junit.Assert;

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
import com.codeshelf.testframework.ServerTest;

public class CheProcessScanPickMultiPath extends ServerTest {
	protected Facility setUpMultiPathFacilityWithOrders() throws IOException{
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" + 
				"Aisle,A1,,,,,zigzagB1S1Side,3,4,X,20\n" + 
				"Bay,B1,100,,,,,,,,\n" + 
				"Tier,T1,100,0,32,0,,,,,\n" + 
				"Bay,B2,100,,,,,,,,\n" + 
				"Tier,T1,100,0,32,0,,,,,\n" + 
				"Aisle,A2,,,,,zigzagB1S1Side,3,6,X,20\n" + 
				"Bay,B1,100,,,,,,,,\n" + 
				"Tier,T1,100,0,32,0,,,,,\n" + 
				"Bay,B2,100,,,,,,,,\n" + 
				"Tier,T1,100,0,32,0,,,,,\n";
		importAislesData(getFacility(), aislesCsvString);

		// Get the aisles
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle1);
		Assert.assertNotNull(aisle2);

		Path path1 = createPathForTest(getFacility());
		PathSegment segment1_1 = addPathSegmentForTest(path1, 0, 3d, 4.5, 5d, 4.5);

		Path path2 = createPathForTest(getFacility());
		PathSegment segment2_1 = addPathSegmentForTest(path2, 0, 3d, 6.5, 5d, 6.5);

		String persistStr1 = segment1_1.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr1);

		String persistStr2 = segment2_1.getPersistentId().toString();
		aisle2.associatePathSegment(persistStr2);

		String csvLocationAliases = "mappedLocationId,locationAlias\r\n" +
				"A1.B1.T1,LocX24\r\n" + 
				"A1.B2.T1,LocX25\r\n" + 
				"A2.B1.T1,LocX26\r\n" + 
				"A2.B2.T1,LocX27\r\n";
		importLocationAliasesData(getFacility(), csvLocationAliases);

		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("LED2", new NetGuid("0x00000012"));

		Short channel1 = 1;
		Location tier1 = getFacility().findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier1);
		tier1.setLedChannel(channel1);
		tier1.getDao().store(tier1);
		
		Location tier2 = getFacility().findSubLocationById("A2.B1.T1");
		controller2.addLocation(tier2);
		tier2.setLedChannel(channel1);
		tier2.getDao().store(tier1);

		
		propertyService.changePropertyValue(getFacility(), DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		
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
				"1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1,,\n" + 
				"1,1,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1,,\n" + 
				"1,1,347,12/03/14 12:00,12/31/14 12:00,Item14,,120,a,Group1,,\n" + 
				"1,1,348,12/03/14 12:00,12/31/14 12:00,Item1,,11,a,Group1,,\n" + 
				"1,1,349,12/03/14 12:00,12/31/14 12:00,Item10,,22,a,Group1,,\n" + 
				"1,1,350,12/03/14 12:00,12/31/14 12:00,Item5,,33,a,Group1,,\n" + 
				"1,1,351,12/03/14 12:00,12/31/14 12:00,Item3,,22,a,Group1,,\n" + 
				"1,1,352,12/03/14 12:00,12/31/14 12:00,Item6,,33,a,Group1,,\n";
		importOrdersData(getFacility(), orders);
		return getFacility();
	}

}
