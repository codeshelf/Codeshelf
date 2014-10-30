package com.gadgetworks.codeshelf.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.EdiTestABC;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.InventoryGenerator;
import com.gadgetworks.codeshelf.edi.VirtualSlottedFacilityGenerator;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

public class LightServiceTest extends EdiTestABC {
	
	@Test
	public final void checkLedChaserVirtualSlottedItems() throws IOException, InterruptedException, ExecutionException {

		VirtualSlottedFacilityGenerator facilityGenerator = new VirtualSlottedFacilityGenerator(createAisleFileImporter(),
			createLocationAliasImporter(),
			createOrderImporter());
		Facility facility = facilityGenerator.generateFacilityForVirtualSlotting(testName.getMethodName());
		Aisle aisle = (Aisle) facility.getChildren().get(0);
		List<Tier> tiers = aisle.getActiveChildrenAtLevel(Tier.class);
		int itemsPerTier = 5;

		
		InventoryGenerator inventoryGenerator = new InventoryGenerator((InventoryCsvImporter) createInventoryImporter());
		inventoryGenerator.setupVirtuallySlottedInventory(aisle, itemsPerTier);

		aisle = aisle.getDao().findByPersistentId(aisle.getPersistentId());

		List<Item> items = aisle.getInventoryInWorkingOrder();
		Assert.assertNotEquals(0,  items.size());
		
		SessionManager sessionManager = mock(SessionManager.class);
		LightService lightService = new LightService(sessionManager, Executors.newSingleThreadScheduledExecutor());
		Future<?> complete = lightService.lightInventory(ColorEnum.RED, facility.getPersistentId(), aisle);
		complete.get();
		
		ArgumentCaptor<MessageABC> messagesCaptor = ArgumentCaptor.forClass(MessageABC.class);
		verify(sessionManager, times(tiers.size() * itemsPerTier)).sendMessage(any(Set.class), messagesCaptor.capture());
		
		List<MessageABC> messages = messagesCaptor.getAllValues();
		Iterator<Item> itemIterator = items.iterator();
		for (MessageABC messageABC : messages) {
			LightLedsMessage message = (LightLedsMessage) messageABC;
			assertWillLightItem(itemIterator.next(), message);
		}
	}


	@Test
	public final void checkLedChaserLocationSequence() throws IOException, InterruptedException, ExecutionException {
		Facility facility = setUpSimpleSlottedFacility("XB06");
		ISubLocation<?> locationS1 = facility.findSubLocationById("A1.B1.T2.S1");
		ISubLocation<?> locationS2 = facility.findSubLocationById("A1.B1.T2.S2");
		ISubLocation<?> locationS3 = facility.findSubLocationById("A1.B1.T2.S3");
		ISubLocation<?> locationS4 = facility.findSubLocationById("A1.B1.T2.S4");
		ISubLocation<?> locationS5 = facility.findSubLocationById("A1.B1.T2.S5");
		List<ISubLocation<?>> sublocations = ImmutableList.<ISubLocation<?>>of(
			locationS1,
			locationS2,
			locationS3,
			locationS4,
			locationS5);
		SessionManager sessionManager = mock(SessionManager.class);
		LightService lightService = new LightService(sessionManager, Executors.newSingleThreadScheduledExecutor());
		Future<?> complete = lightService.lightLocations(ColorEnum.RED, facility.getPersistentId(), sublocations);
		complete.get();
		
		ArgumentCaptor<MessageABC> messagesCaptor = ArgumentCaptor.forClass(MessageABC.class);
		verify(sessionManager, times(5)).sendMessage(any(Set.class), messagesCaptor.capture());
		
		List<MessageABC> messages = messagesCaptor.getAllValues();
		Iterator<ISubLocation<?>> locations = sublocations.iterator();
		for (MessageABC messageABC : messages) {
			LightLedsMessage message = (LightLedsMessage) messageABC;
			assertWillLightsLocation(locations.next(), message);
		}
	}
	

	private void assertWillLightsLocation(ISubLocation<?> location, LightLedsMessage ledMessage) {
		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledMessage.getLedCommands());
		for (LedCmdGroup ledCmdGroup : ledCmdGroups) {
			for(LedSample ledSample : ledCmdGroup.getLedSampleList()) {
				short pos = ledSample.getPosition();
				short first = location.getFirstLedNumAlongPath();
				short last = location.getLastLedNumAlongPath();
				String message = Objects.toStringHelper("Failed")
					.add("first", first)
					.add("pos", pos)
					.add("last", last)
					.add("location", location).toString();

				Assert.assertTrue(message ,  first <= pos && pos <= last);
			}
		}
		
	}

	

	private void assertWillLightItem(Item item, LightLedsMessage ledMessage) {
		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledMessage.getLedCommands());
		for (LedCmdGroup ledCmdGroup : ledCmdGroups) {
			Assert.assertEquals(item.getStoredLocation().getEffectiveLedController().getDeviceGuidStr(),
								ledCmdGroup.getControllerId());

			Assert.assertEquals(item.getStoredLocation().getEffectiveLedChannel(),
								ledCmdGroup.getChannelNum());

			for(LedSample ledSample : ledCmdGroup.getLedSampleList()) {
				item.getFirstLastLedsForItem().isWithin(ledSample.getPosition());
			}			
		}	
	}
	
	// Important: BayChangeExceptSamePathDistance is not tested here. Need positive and negative tests
	@SuppressWarnings({ "unused" })
	private Facility setUpSimpleSlottedFacility(String inOrganizationName) {
		// Besides basic crossbatch functionality, with this facility we want to test housekeeping WIs for
		// 1) same position on cart
		// 2) Bay done/change bay
		// 3) aisle done/change aisle

		// This returns a facility with aisle A1 and A2, with two bays with two tier each. 5 slots per tier, like GoodEggs. With a path, associated to both aisles.
		// Zigzag bays like GoodEggs. 10 valid locations per aisle, named as GoodEggs
		// One controllers associated per aisle
		// Two CHE called CHE1 and CHE2. CHE1 colored green and CHE2 magenta

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,zigzagB1S1Side,12.85,43.45,X,40,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Aisle,A2,,,,,zigzagB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n" //
				+ "Bay,B2,112,,,,,\r\n" //
				+ "Tier,T1,,5,32,0,,\r\n" //
				+ "Tier,T2,,5,32,120,,\r\n"; //

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(new StringReader(csvString), facility, ediProcessTime);

		// Get the aisles
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 10.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		String csvLocationAlias = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1.T2.S5,D-1\r\n" //
				+ "A1.B1.T2.S4,D-2\r\n" //
				+ "A1.B1.T2.S3, D-3\r\n" //
				+ "A1.B1.T2.S2, D-4\r\n" //
				+ "A1.B1.T2.S1, D-5\r\n" //
				+ "A1.B1.T1.S5, D-6\r\n" //
				+ "A1.B1.T1.S4, D-7\r\n" //
				+ "A1.B1.T1.S3, D-8\r\n" //
				+ "A1.B1.T1.S2, D-9\r\n" //
				+ "A1.B2.T1.S1, D-10\r\n" //
				+ "A1.B2.T2.S5, D-21\r\n" //
				+ "A1.B2.T2.S4, D-22\r\n" //
				+ "A1.B2.T2.S3, D-23\r\n" //
				+ "A1.B2.T2.S2, D-24\r\n" //
				+ "A1.B2.T2.S1, D-25\r\n" //
				+ "A1.B2.T1.S5, D-26\r\n" //
				+ "A1.B2.T1.S4, D-27\r\n" //
				+ "A1.B2.T1.S3, D-28\r\n" //
				+ "A1.B2.T1.S2, D-29\r\n" //
				+ "A1.B2.T1.S1, D-30\r\n" //
				+ "A2.B1.T2.S5, D-11\r\n" //
				+ "A2.B1.T2.S4, D-12\r\n" //
				+ "A2.B1.T2.S3, D-13\r\n" //
				+ "A2.B1.T2.S2, D-14\r\n" //
				+ "A2.B1.T2.S1, D-15\r\n" //
				+ "A2.B1.T1.S5, D-16\r\n" //
				+ "A2.B1.T1.S4, D-17\r\n" //
				+ "A2.B1.T1.S3, D-18\r\n" //
				+ "A2.B1.T1.S2, D-19\r\n" //
				+ "A2.B2.T1.S1, D-20\r\n" //
				+ "A2.B2.T2.S5, D-31\r\n" //
				+ "A2.B2.T2.S4, D-32\r\n" //
				+ "A2.B2.T2.S3, D-33\r\n" //
				+ "A2.B2.T2.S2, D-34\r\n" //
				+ "A2.B2.T2.S1, D-35\r\n" //
				+ "A2.B2.T1.S5, D-36\r\n" //
				+ "A2.B2.T1.S4, D-37\r\n" //
				+ "A2.B2.T1.S3, D-38\r\n" //
				+ "A2.B2.T1.S2, D-39\r\n" //
				+ "A2.B2.T1.S1, D-40\r\n"; //

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = createLocationAliasImporter();
		importer2.importLocationAliasesFromCsvStream(new StringReader(csvLocationAlias), facility, ediProcessTime2);

		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = facility.createNetwork(nName);
		//Che che = 
		network.createChe("CHE1", new NetGuid("0x00000001"));
		network.createChe("CHE2", new NetGuid("0x00000002"));

		Che che1 = network.getChe("CHE1");
		che1.setColor(ColorEnum.GREEN);
		Che che2 = network.getChe("CHE2");
		che1.setColor(ColorEnum.MAGENTA);

		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		Short channel1 = 1;
		aisle1.setLedController(controller1);
		aisle1.setLedChannel(channel1);
		aisle1.getDao().store(aisle1);
		aisle2.setLedController(controller2);
		aisle2.setLedChannel(channel1);
		aisle2.getDao().store(aisle2);

		return facility;

	}


}
