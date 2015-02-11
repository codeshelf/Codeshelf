package com.codeshelf.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.LedSample;
import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.AislesFileCsvImporter.ControllerLayout;
import com.codeshelf.edi.EdiTestABC;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.InventoryGenerator;
import com.codeshelf.edi.VirtualSlottedFacilityGenerator;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.codeshelf.ws.jetty.server.SessionManager;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;

public class LightServiceTest extends EdiTestABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(LightServiceTest.class);
	
	@SuppressWarnings("unchecked")
	@Test
	public final void checkLedChaserVirtualSlottedItems() throws IOException, InterruptedException, ExecutionException {
		
		LOGGER.info("0: Starting test:  getting facility");
		this.getTenantPersistenceService().beginTransaction();

		VirtualSlottedFacilityGenerator facilityGenerator = new VirtualSlottedFacilityGenerator(
			getDefaultTenant(),
			createAisleFileImporter(),
			createLocationAliasImporter(),
			createOrderImporter());
		Facility facility = facilityGenerator.generateFacilityForVirtualSlotting(testName.getMethodName());
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("1: reload facility. Get childre aisle, tiers.");
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Aisle aisle = (Aisle) facility.getChildren().get(0);
		List<Tier> tiers = aisle.getActiveChildrenAtLevel(Tier.class);
		int itemsPerTier = 5;

		
		LOGGER.info("2: setup inventory.");
		InventoryGenerator inventoryGenerator = new InventoryGenerator((InventoryCsvImporter) createInventoryImporter());
		inventoryGenerator.setupVirtuallySlottedInventory(aisle, itemsPerTier);
		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("3: get inventory in working order for one aisle.");
		this.getTenantPersistenceService().beginTransaction();
		aisle = Aisle.DAO.reload(aisle);
		List<Item> items = aisle.getInventoryInWorkingOrder();
		Assert.assertNotEquals(0,  items.size());
		this.getTenantPersistenceService().commitTransaction();
		
		LOGGER.info("4: mockProp.getPropertyAsColor");
		SessionManager sessionManager = mock(SessionManager.class);
		PropertyService mockProp = mock(PropertyService.class);
		
		when(mockProp.getPropertyAsColor(any(IDomainObject.class), anyString(), any(ColorEnum.class))).then(new Answer<ColorEnum>() {
		    @Override
		    public ColorEnum answer(InvocationOnMock invocation) throws Throwable {
		    	return ColorEnum.RED;
		    }
		});
		
		LOGGER.info("5: new LightService");
		LightService lightService = new LightService(mockProp, sessionManager, Executors.newSingleThreadScheduledExecutor());

		LOGGER.info("6: lightService.lightInventory. This is the slow step: 23 seconds");
		// To speed up: fewer inventory items? 2250 ms per item. Or lightService could pass in or get config value to set that lower.
		// and pass through to Future<Void> chaserLight()
		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		aisle = Aisle.DAO.reload(aisle);
		items = aisle.getInventoryInWorkingOrder();
		Future<Void> complete = lightService.lightInventory(facility.getPersistentId().toString(), aisle.getLocationId());
		complete.get();
		
		LOGGER.info("7: ArgumentCaptor");

		ArgumentCaptor<MessageABC> messagesCaptor = ArgumentCaptor.forClass(MessageABC.class);
		verify(sessionManager, times(tiers.size() * itemsPerTier)).sendMessage(any(Set.class), messagesCaptor.capture());
		
		LOGGER.info("8: assertWillLightItem() from messagesCaptor.getAllValues");
		List<MessageABC> messages = messagesCaptor.getAllValues();
		Iterator<Item> itemIterator = items.iterator();
		for (MessageABC messageABC : messages) {
			LightLedsMessage message = (LightLedsMessage) messageABC;
			assertWillLightItem(itemIterator.next(), message);
		}

		this.getTenantPersistenceService().commitTransaction();
	}


	@Test
	public final void checkTierChildLocationSequence() throws IOException, InterruptedException, ExecutionException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setupPhysicalSlottedFacility("XB06", ControllerLayout.zigzagB1S1Side);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Location parent = facility.findSubLocationById("A1.B1.T2");
		List<Location> sublocations = parent.getChildrenInWorkingOrder();
		List<MessageABC> messages = captureLightMessages(facility, parent, sublocations.size());

		//Messages came in same working order
		Iterator<Location> subLocationsIter = sublocations.iterator();
		for (MessageABC messageABC : messages) {
			LightLedsMessage message = (LightLedsMessage) messageABC;
			assertASampleWillLightLocation(subLocationsIter.next(), message);
		}

		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void checkZigZagBayChildLocationSequence() throws IOException, InterruptedException, ExecutionException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setupPhysicalSlottedFacility("XB06", ControllerLayout.zigzagB1S1Side);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Location parent = facility.findSubLocationById("A1.B1");
		List<Location> sublocations = parent.getChildrenInWorkingOrder();
		List<MessageABC> messages = captureLightMessages(facility, parent, 1 /*whole bay*/);

		//Messages came in same working order
		Iterator<Location> subLocationsIter = sublocations.iterator();
		for (MessageABC messageABC : messages) {
			LightLedsMessage message = (LightLedsMessage) messageABC;
			assertASampleWillLightLocation(subLocationsIter.next(), message);
		}

		this.getTenantPersistenceService().commitTransaction();
	}
	
	
	/**
	 * Special cased for now
	 */
	@Test
	public final void lightAisleWhenSomeDisassociated() throws IOException, InterruptedException, ExecutionException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setupPhysicalSlottedFacility("XB06", ControllerLayout.tierLeft);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Tier b1t1 = (Tier)facility.findSubLocationById("A1.B1.T1");
		b1t1.clearControllerChannel();
		Tier b2t1 = (Tier)facility.findSubLocationById("A1.B2.T1");
		b2t1.clearControllerChannel();
		
		
		Location parent = facility.findSubLocationById("A1");
		List<Location> bays = parent.getChildrenInWorkingOrder();
		List<MessageABC> messages = captureLightMessages(facility, parent, 2 /*2 bays x 1 tiers*/);
		
		Iterator<MessageABC> messageIter = messages.iterator();
		for (Location bay : bays) {
			Tier tier = (Tier) bay.findSubLocationById("T2");
			assertASampleWillLightLocation(tier, (LightLedsMessage) messageIter.next());
		}
		
		this.getTenantPersistenceService().commitTransaction();
	}
		

	@Test
	public final void lightBayWhenSomeDisassociated() throws IOException, InterruptedException, ExecutionException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setupPhysicalSlottedFacility("XB06", ControllerLayout.tierLeft);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Tier b1t1 = (Tier)facility.findSubLocationById("A1.B1.T1");
		b1t1.clearControllerChannel();
		
		
		Location parent = facility.findSubLocationById("A1.B1");
		List<Location> tiers = parent.getChildrenInWorkingOrder();
		List<MessageABC> messages = captureLightMessages(facility, parent, 1 /*1 bays x 1 tiers*/);
		@SuppressWarnings("unused")
		Iterator<MessageABC> messageIter = messages.iterator();
		for (Location tier : tiers) {
			if (tier.getDomainId().equals("T2")) {
			}
		}
	
		this.getTenantPersistenceService().commitTransaction();
	}

	
	/**
	 * Special cased for now
	 */
	@Test
	public final void checkChildLocationSequenceForZigZagLayoutAisle() throws IOException, InterruptedException, ExecutionException {
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = setupPhysicalSlottedFacility("XB06", ControllerLayout.zigzagB1S1Side);
		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();
		facility = Facility.DAO.reload(facility);
		Location parent = facility.findSubLocationById("A1");
		List<Location> bays = parent.getChildrenInWorkingOrder();
		List<MessageABC> messages = captureLightMessages(facility, parent, 2/*2 bays all tiers on the one controller*/);
		Iterator<MessageABC> messageIter = messages.iterator();
		for (Location bay : bays) {
			Tier tier = (Tier) bay.findSubLocationById("T2");
			assertASampleWillLightLocation(tier, (LightLedsMessage) messageIter.next());
		}
		
		this.getTenantPersistenceService().commitTransaction();
	}

	
	@SuppressWarnings("unchecked")
	private List<MessageABC> captureLightMessages(Facility facility, Location parent, int expectedTotal) throws InterruptedException, ExecutionException {
		Assert.assertTrue(expectedTotal > 0);// test a reasonable amount
		SessionManager sessionManager = mock(SessionManager.class);
		ColorEnum color = ColorEnum.RED;
		
		LightService lightService = new LightService(mock(PropertyService.class),  sessionManager, Executors.newSingleThreadScheduledExecutor());
		Future<Void> complete = lightService.lightChildLocations(facility, parent, color);
		complete.get(); //wait for completion
		
		ArgumentCaptor<MessageABC> messagesCaptor = ArgumentCaptor.forClass(MessageABC.class);
		verify(sessionManager, times(expectedTotal)).sendMessage(any(Set.class), messagesCaptor.capture());
		
		List<MessageABC> messages = messagesCaptor.getAllValues();
		return messages;

	}
		
	private void assertASampleWillLightLocation(Location location, LightLedsMessage ledMessage) {
		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledMessage.getLedCommands());
		boolean found = false;
		ToStringHelper message = Objects.toStringHelper("Failed, probably lit out of order ");
		for (LedCmdGroup ledCmdGroup : ledCmdGroups) {
			for(LedSample ledSample : ledCmdGroup.getLedSampleList()) {
				short pos = ledSample.getPosition();
				short first = location.getFirstLedNumAlongPath();
				short last = location.getLastLedNumAlongPath();
					message.add("first", first)
					.add("pos", pos)
					.add("last", last)
					.add("location", location);
				found |= (first <= pos && pos <= last);
			}

		}
		Assert.assertTrue(message.toString(),  found);
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
	private Facility setupPhysicalSlottedFacility(String inOrganizationName, ControllerLayout controllerLayout) {
		// Besides basic crossbatch functionality, with this facility we want to test housekeeping WIs for
		// 1) same position on cart
		// 2) Bay done/change bay
		// 3) aisle done/change aisle

		// This returns a facility with aisle A1 and A2, with two bays with two tier each. 5 slots per tier, like GoodEggs. With a path, associated to both aisles.
		// Zigzag bays like GoodEggs. 10 valid locations per aisle, named as GoodEggs
		// One controllers associated per aisle
		// Two CHE called CHE1 and CHE2. CHE1 colored green and CHE2 magenta
		String controllerLayoutStr = controllerLayout.name();
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

		String fName = "F-" + inOrganizationName;
		Facility facility= Facility.createFacility(getDefaultTenant(),fName, "TEST", Point.getZeroPoint());

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(new StringReader(csvString), facility, ediProcessTime);

		// Get the aisles
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(facility);
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 10.85, 48.45);

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

		CodeshelfNetwork network = facility.getNetworks().get(0);
		Che che1 = network.getChe("CHE1");
		che1.setColor(ColorEnum.GREEN);
		Che che2 = network.getChe("CHE2");
		che2.setColor(ColorEnum.MAGENTA);

		LedController controller1 = network.findOrCreateLedController("0x00000011", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("0x00000012", new NetGuid("0x00000012"));
		Short channel1 = 1;
		if (controllerLayout.equals(ControllerLayout.zigzagB1S1Side)) {
			controller1.addLocation(aisle1);
			aisle1.setLedChannel(channel1);
			aisle1.getDao().store(aisle1);
			controller2.addLocation(aisle2);
			aisle2.setLedChannel(channel1);
			aisle2.getDao().store(aisle2);
		} else if (controllerLayout.equals(ControllerLayout.tierLeft)) {
			Tier tier1 = (Tier) facility.findSubLocationById("A1.B1.T1");
			tier1.setControllerChannel(controller1.getPersistentId().toString(), String.valueOf(channel1), Tier.ALL_TIERS_IN_AISLE);
			tier1.getDao().store(tier1);
			Tier tier2 = (Tier) facility.findSubLocationById("A1.B1.T2");
			tier2.setControllerChannel(controller2.getPersistentId().toString(), String.valueOf(channel1), Tier.ALL_TIERS_IN_AISLE);
			tier1.getDao().store(tier2);
		}
		
		return facility;

	}


}