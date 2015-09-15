package com.codeshelf.testframework;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvOrderLocationImporter;
import com.codeshelf.edi.ICsvWorkerImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.LocationAliasCsvImporter;
import com.codeshelf.edi.OrderLocationCsvImporter;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.edi.WorkerCsvImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.TravelDirectionEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.validation.BatchResult;

public abstract class MockDaoTest extends MinimalTest {

	@Override
	Type getFrameworkType() {
		return Type.MOCK_DAO;
	}

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return false;
	}
	protected static Path createPathForTest(Facility facility) {
		return facility.createPath("");
	}
	
	// these two belong in ServerTest?
	protected ICsvInventoryImporter createInventoryImporter() {
		return new InventoryCsvImporter(eventProducer);
	}
	protected ICsvOrderLocationImporter createOrderLocationImporter() {
		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(eventProducer);
		return importer;
	}

	protected static PathSegment addPathSegmentForTest(final Path inPath,
		final Integer inSegmentOrder,
		Double inStartX,
		Double inStartY,
		Double inEndX,
		Double inEndY) {
	
		Point head = new Point(PositionTypeEnum.METERS_FROM_PARENT, inStartX, inStartY, 0.0);
		Point tail = new Point(PositionTypeEnum.METERS_FROM_PARENT, inEndX, inEndY, 0.0);
		PathSegment returnSeg = inPath.createPathSegment(inSegmentOrder, head, tail);
		return returnSeg;
	}
	
	protected static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}

	
	protected Aisle getDefaultAisle(Facility facility, String inDomainId) {
		return getDefaultAisle(facility, inDomainId, Point.getZeroPoint(), Point.getZeroPoint().add(5.0, 0.0)); 
	}

	protected Aisle getDefaultAisle(Facility facility, String inDomainId, Point anchorPoint, Point pickFaceEndPoint) {
		Aisle aisle = Aisle.staticGetDao().findByDomainId(facility, inDomainId);
		if (aisle == null) {
			aisle = facility.createAisle(inDomainId, anchorPoint, pickFaceEndPoint);
			Aisle.staticGetDao().store(aisle);
		}
		return aisle;
	}

	
	protected Bay getDefaultBay(Aisle aisle, String inDomainId) {
		Bay bay  = Bay.staticGetDao().findByDomainId(aisle, inDomainId);
		if (bay == null) {
			bay = aisle.createBay(inDomainId, Point.getZeroPoint(), Point.getZeroPoint());
			Bay.staticGetDao().store(bay);
		}
		return bay;
	}

	protected Tier getDefaultTier(Bay bay, String inDomainId) {
		Tier tier  = Tier.staticGetDao().findByDomainId(bay, inDomainId);
		if (tier == null) {
			tier = bay.createTier(inDomainId, Point.getZeroPoint(), Point.getZeroPoint());
			Tier.staticGetDao().store(tier);
		}
		return tier;
	}

	protected PathSegment getDefaultPathSegment(Path path, Integer inOrder) {
		PathSegment segment = path.getPathSegment(inOrder); 
		if (segment == null) {
			segment = path.createPathSegment(inOrder,  anyPoint(), anyPoint());
			// createPathSegment() does the store
			// PathSegment.staticGetDao().store(segment);
		}
		return segment;
	}

	protected Path getDefaultPath(Facility facility, String inPathId) {
		Path path = facility.getPath(inPathId);
		if (path == null) {
			path = facility.createPath(inPathId);
			Path.staticGetDao().store(path);
			// looks wrong. Does not add to facility
		}
		return path;
	}
	
	protected LedController getDefaultController(CodeshelfNetwork network, final String inControllerDomainId) {
		LedController controller = network.findOrCreateLedController(inControllerDomainId, new NetGuid(inControllerDomainId));
		controller.setDomainId(inControllerDomainId);
		LedController.staticGetDao().store(controller);
		return controller;
	}
	
	protected Point anyPoint() {
		return new Point(PositionTypeEnum.METERS_FROM_PARENT, Math.random()*10, Math.random()*10, Math.random()*10);
	}
	
	@SuppressWarnings("unused")
	protected Facility createFacilityWithOutboundOrders() {

		Facility resultFacility = getFacility();
		CodeshelfNetwork network = resultFacility.getNetworks().get(0);
		
		Che che = network.createChe("WITEST", new NetGuid("0x00000001"));

		LedController controller = network.findOrCreateLedController("LEDCON", new NetGuid("0x00000002"));

		Aisle aisle1 = getDefaultAisle(resultFacility, "A1");

		Short channel1 = 1;
		
		// Notice: we are setting the controller on the bay in this test. But the application UI only does tiers or aisles.
		Bay baya1b1 = aisle1.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya1b1.setFirstLedNumAlongPath((short) 0);
		baya1b1.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya1b1);
		baya1b1.setLedChannel(channel1);
		Bay.staticGetDao().store(baya1b1);

		Bay baya1b2 = aisle1.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya1b2.setFirstLedNumAlongPath((short) 0);
		baya1b2.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya1b2);
		baya1b2.setLedChannel(channel1);
		Bay.staticGetDao().store(baya1b2);

		Aisle aisle2 = getDefaultAisle(resultFacility, "A2");

		Bay baya2b1 = aisle2.createBay("B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya2b1.setFirstLedNumAlongPath((short) 0);
		baya2b1.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya2b1);
		baya2b1.setLedChannel(channel1);
		Bay.staticGetDao().store(baya2b1);

		Bay baya2b2 = aisle2.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya2b2.setFirstLedNumAlongPath((short) 0);
		baya2b2.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya2b2);
		baya2b2.setLedChannel(channel1);
		Bay.staticGetDao().store(baya2b2);

		Path path = new Path();
		path.setDomainId(Path.DEFAULT_FACILITY_PATH_ID);
		path.setParent(resultFacility);
		path.setTravelDir(TravelDirectionEnum.FORWARD);
		Path.staticGetDao().store(path);
		resultFacility.addPath(path);

		Point startPoint1 = Point.getZeroPoint().add(5.0,0.0);
		PathSegment pathSegment1 = path.createPathSegment(0, startPoint1, Point.getZeroPoint());
		PathSegment.staticGetDao().store(pathSegment1);

		aisle1.setPathSegment(pathSegment1);
		Aisle.staticGetDao().store(aisle1);

		aisle2.setPathSegment(pathSegment1);
		Aisle.staticGetDao().store(aisle2);

		Aisle aisle3 = getDefaultAisle(resultFacility, "A3");

		Bay baya3b1 = aisle3.createBay( "B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya3b1.setFirstLedNumAlongPath((short) 0);
		baya3b1.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya3b1);
		baya3b1.setLedChannel(channel1);
		Bay.staticGetDao().store(baya3b1);

		Bay baya3b2 = aisle3.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya3b2.setFirstLedNumAlongPath((short) 0);
		baya3b2.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya3b2);
		baya3b2.setLedChannel(channel1);
		Bay.staticGetDao().store(baya3b2);

		Aisle aisle4 = getDefaultAisle(resultFacility, "A4");

		Bay baya4b1 = aisle4.createBay( "B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya4b1.setFirstLedNumAlongPath((short) 0);
		baya4b1.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya4b1);
		baya4b1.setLedChannel(channel1);
		Bay.staticGetDao().store(baya4b1);

		Bay baya4b2 = aisle4.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya4b2.setFirstLedNumAlongPath((short) 0);
		baya4b2.setLastLedNumAlongPath((short) 0);
		controller.addLocation(baya4b2);
		baya4b2.setLedChannel(channel1);
		Bay.staticGetDao().store(baya4b2);

		PathSegment pathSegment2 = path.createPathSegment(1, Point.getZeroPoint(), Point.getZeroPoint());
		PathSegment.staticGetDao().store(pathSegment2);

		aisle3.setPathSegment(pathSegment2);
		Aisle.staticGetDao().store(aisle3);

		aisle4.setPathSegment(pathSegment2);
		Aisle.staticGetDao().store(aisle4);

		resultFacility.recomputeLocationPathDistances(path);

		Container container1 = createContainer("C1", resultFacility);
		Container container2 = createContainer("C2", resultFacility);
		Container container3 = createContainer("C3", resultFacility);

		UomMaster uomMaster = createUomMaster("EA", resultFacility);

		ItemMaster itemMaster1 = createItemMaster("ITEM1", resultFacility, uomMaster);
		ItemMaster itemMaster2 = createItemMaster("ITEM2", resultFacility, uomMaster);
		ItemMaster itemMaster3 = createItemMaster("ITEM3", resultFacility, uomMaster);
		
		// Create order headers that are not in a group.

		OrderHeader orderOut1 = createOrderHeader("OUT1", OrderTypeEnum.OUTBOUND, resultFacility, null);
		OrderDetail orderOut1Detail1 = createOrderDetail(orderOut1, itemMaster1);
		OrderDetail orderOut1Detail2 = createOrderDetail(orderOut1, itemMaster2);
		OrderDetail orderOut1Detail3 = createOrderDetail(orderOut1, itemMaster3);

		OrderHeader orderOut2 = createOrderHeader("OUT2", OrderTypeEnum.OUTBOUND, resultFacility, null);
		OrderDetail orderOut2Detail1 = createOrderDetail(orderOut2, itemMaster1);
		OrderDetail orderOut2Detail2 = createOrderDetail(orderOut2, itemMaster2);
		OrderDetail orderOut2Detail3 = createOrderDetail(orderOut2, itemMaster3);

		OrderHeader orderOut3 = createOrderHeader("OUT3", OrderTypeEnum.OUTBOUND, resultFacility, null);
		OrderDetail orderOut3Detail1 = createOrderDetail(orderOut3, itemMaster1);
		OrderDetail orderOut3Detail2 = createOrderDetail(orderOut3, itemMaster2);
		OrderDetail orderOut3Detail3 = createOrderDetail(orderOut3, itemMaster3);

		OrderHeader orderCross1 = createOrderHeader("CROSS1", OrderTypeEnum.CROSS, resultFacility, null);
		OrderDetail orderCross1Detail1 = createOrderDetail(orderCross1, itemMaster1);
		OrderDetail orderCross1Detail2 = createOrderDetail(orderCross1, itemMaster2);
		OrderDetail orderCross1Detail3 = createOrderDetail(orderCross1, itemMaster3);
		OrderLocation orderCross1Loc = createOrderLocation(orderCross1, baya1b1);

		OrderHeader orderCross2 = createOrderHeader("CROSS2", OrderTypeEnum.CROSS, resultFacility, null);
		OrderDetail orderCross2Detail1 = createOrderDetail(orderCross2, itemMaster1);
		OrderDetail orderCross2Detail2 = createOrderDetail(orderCross2, itemMaster2);
		OrderDetail orderCross2Detail3 = createOrderDetail(orderCross2, itemMaster3);

		OrderHeader orderCross3 = createOrderHeader("CROSS3", OrderTypeEnum.CROSS, resultFacility, null);
		OrderDetail orderCross3Detail1 = createOrderDetail(orderCross3, itemMaster1);
		OrderDetail orderCross3Detail2 = createOrderDetail(orderCross3, itemMaster2);
		OrderDetail orderCross3Detail3 = createOrderDetail(orderCross3, itemMaster3);
		
		// Create order headers that are in groups with items separate between the groups (to test that we process groups separately).
		
		Container container4 = createContainer("C4", resultFacility);
		Container container5 = createContainer("C5", resultFacility);
		Container container6 = createContainer("C6", resultFacility);
//		Container container7 = createContainer("C7", resultFacility);

		ItemMaster itemMaster4 = createItemMaster("ITEM4", resultFacility, uomMaster);
		ItemMaster itemMaster5 = createItemMaster("ITEM5", resultFacility, uomMaster);
		ItemMaster itemMaster6 = createItemMaster("ITEM6", resultFacility, uomMaster);
//		ItemMaster itemMaster7 = createItemMaster("ITEM7", resultFacility, uomMaster);

		OrderGroup orderGroup1 = createOrderGroup("GROUP1", resultFacility);
		OrderHeader orderOut1Group1 = createOrderHeader("OUT1GROUP1", OrderTypeEnum.OUTBOUND, resultFacility, orderGroup1);
		OrderDetail orderOut1Group1Detail1 = createOrderDetail(orderOut1Group1, itemMaster4);
		OrderLocation orderOut1Group1Loc = createOrderLocation(orderOut1Group1, baya1b1);

		OrderHeader orderOut2Group1 = createOrderHeader("OUT2GROUP1", OrderTypeEnum.OUTBOUND, resultFacility, orderGroup1);
		OrderDetail orderOut2Group1Detail1 = createOrderDetail(orderOut2Group1, itemMaster4);
		OrderLocation orderOut2Group1Loc = createOrderLocation(orderOut2Group1, baya1b1);

		OrderGroup orderGroup2 = createOrderGroup("GROUP2", resultFacility);
		OrderHeader orderOut3Group2 = createOrderHeader("OUT3GROUP2", OrderTypeEnum.OUTBOUND, resultFacility, orderGroup2);
		OrderDetail orderOut3Group2Detail1 = createOrderDetail(orderOut3Group2, itemMaster4);
		OrderLocation orderOut3Group2Loc = createOrderLocation(orderOut3Group2, baya1b1);
		
		OrderHeader orderOut4NoGroup = createOrderHeader("OUT4NOGROUP", OrderTypeEnum.OUTBOUND, resultFacility, null);
		OrderDetail orderOut4NoGroupDetail1 = createOrderDetail(orderOut4NoGroup, itemMaster5);
		OrderLocation orderOut4NoGroupLoc = createOrderLocation(orderOut4NoGroup, baya1b1);
		
		OrderHeader orderCross4 = createOrderHeader("CROSS4", OrderTypeEnum.CROSS, resultFacility, orderGroup1);
		OrderDetail orderCross4Detail1 = createOrderDetail(orderCross4, itemMaster4);

		OrderHeader orderCross5 = createOrderHeader("CROSS5", OrderTypeEnum.CROSS, resultFacility, null);
		OrderDetail orderCross5Detail1 = createOrderDetail(orderCross5, itemMaster5);

		OrderHeader orderCross6 = createOrderHeader("CROSS6", OrderTypeEnum.CROSS, resultFacility, orderGroup2);
//		OrderDetail orderCross6Detail1 = createOrderDetail(orderCross6, itemMaster6);
//
//		OrderHeader orderCross7 = createOrderHeader("CROSS7", OrderTypeEnum.CROSS, resultFacility, null);
//		OrderDetail orderCross7Detail1 = createOrderDetail(orderCross7, itemMaster7);

		ContainerUse containerUse4 = createContainerUse(container4, orderCross4, resultFacility);
		ContainerUse containerUse5 = createContainerUse(container5, orderCross5, resultFacility);
		ContainerUse containerUse6 = createContainerUse(container6, orderCross6, resultFacility);
//		ContainerUse containerUse7 = createContainerUse(container7, orderCross7, resultFacility);

		return resultFacility;
	}


	protected UomMaster createUomMaster(String inUom, Facility inFacility) {
		UomMaster uomMaster = new UomMaster();
		uomMaster.setUomMasterId(inUom);
		uomMaster.setParent(inFacility);
		UomMaster.staticGetDao().store(uomMaster);
		inFacility.addUomMaster(uomMaster);
		return uomMaster;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @param inContainerId
	 * @param inFacility
	 * @return
	 */
	protected final Container createContainer(final String inContainerId, final Facility inFacility) {
		Container result = null;

		result = new Container();
		result.setDomainId(inContainerId);
		// result.setParent(inFacility);
		result.setKind(inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		result.setParent(inFacility);
		
		Container.staticGetDao().store(result);

		return result;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @param inContainer
	 * @param inOrderHeader
	 * @param inFacility
	 * @return
	 */
	protected final ContainerUse createContainerUse(final Container inContainer, final OrderHeader inOrderHeader, final Facility inFacility) {
		ContainerUse result = null;
		
		result = new ContainerUse();
		result.setDomainId(inContainer.getContainerId());
		
		// This works because new of DomainObjectABC() descendant calls super on constructor, giving a new random persistentId.
		// Not dependant on the DAO save. HOWEVER, if the store() fails, then the parents are spoiled
		inOrderHeader.addHeadersContainerUse(result);
		
		result.setUsedOn(new Timestamp(System.currentTimeMillis()));
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		
		inContainer.addContainerUse(result);
		
		// This one-to-one relationship needs both persisted
		ContainerUse.staticGetDao().store(result);
		OrderHeader.staticGetDao().store(inOrderHeader);
		
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderGroupId
	 * @param inFacility
	 * @return
	 */
	protected final OrderGroup createOrderGroup(final String inOrderGroupId, final Facility inFacility) {
		OrderGroup result = null;
		
		result = new OrderGroup();
		result.setOrderGroupId(inOrderGroupId);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		inFacility.addOrderGroup(result);
		OrderGroup.staticGetDao().store(result);
		
		
		return result;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @param inOrderId
	 * @param inOrderType
	 * @param inFacility
	 * @return
	 */
	protected final OrderHeader createOrderHeader(final String inOrderId, final OrderTypeEnum inOrderType, final Facility inFacility, final OrderGroup inOrderGroup) {
		OrderHeader result = null;

		result = new OrderHeader();
		result.setParent(inFacility);
		result.setOrderGroup(inOrderGroup);
		result.setOrderId(inOrderId);
		result.setOrderType(inOrderType);
		result.setOrderDate(new Timestamp(System.currentTimeMillis()));
		result.setDueDate(new Timestamp(System.currentTimeMillis()));
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		result.setParent(inFacility);
		OrderHeader.staticGetDao().store(result);
		if (inOrderGroup != null) {
			inOrderGroup.addOrderHeader(result);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Creates an orderdetail using the itemMaster and its standard UOM
	 * 
	 * @param inOrderHeader
	 * @param inItemMaster
	 * @return
	 */
	protected final OrderDetail createOrderDetail(final OrderHeader inOrderHeader, final ItemMaster inItemMaster) {

		OrderDetail result = new OrderDetail();
		result.setDomainId(inItemMaster.getItemId());
		result.setItemMaster(inItemMaster);
		result.setQuantities(5);
		result.setUomMaster(inItemMaster.getStandardUom());
		result.setStatus(OrderStatusEnum.RELEASED);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		
		inOrderHeader.addOrderDetail(result);
		OrderDetail.staticGetDao().store(result);

		return result;
	}

	protected final ItemMaster createItemMaster(final String inItemId, final Facility inFacility, final UomMaster inUomMaster) {
		ItemMaster result = null;

		result = new ItemMaster(inFacility, inItemId, inUomMaster);
		result.setDescription(inItemId);
		ItemMaster.staticGetDao().store(result);

		return result;
	}

	protected final OrderLocation createOrderLocation(final OrderHeader inOrderHeader, final Location inLocation) {
		OrderLocation result = null;

		result = new OrderLocation();
		result.setDomainId(OrderLocation.makeDomainId(inOrderHeader, inLocation));
		result.setParent(inOrderHeader);
		result.setLocation(inLocation);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		OrderLocation.staticGetDao().store(result);
		inOrderHeader.addOrderLocation(result);

		return result;
	}
	
	protected AislesFileCsvImporter createAisleFileImporter() {
		return new AislesFileCsvImporter(this.eventProducer);
	}
	protected ICsvLocationAliasImporter createLocationAliasImporter() {
		return new LocationAliasCsvImporter(this.eventProducer);
	}
	protected ICsvWorkerImporter createWorkerImporter() {
		return new WorkerCsvImporter(this.eventProducer);
	}
	protected ICsvOrderImporter createOrderImporter() {
		//return new OutboundOrderCsvImporter(this.eventProducer);
		return new OutboundOrderPrefetchCsvImporter(this.eventProducer);
	}
	
	protected boolean importAislesData(Facility facility, String aislesCsvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		return importer.importAislesFileFromCsvStream(new StringReader(aislesCsvString), facility, ediProcessTime);
	}
	
	protected boolean importLocationAliasesData(Facility facility, String locationsCsvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter locationAliasImporter = createLocationAliasImporter();
		return locationAliasImporter.importLocationAliasesFromCsvStream(new StringReader(locationsCsvString), facility, ediProcessTime);
	}

	protected boolean importWorkersData(Facility facility, String workersCsvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvWorkerImporter workerImporter = createWorkerImporter();
		return workerImporter.importWorkersFromCsvStream(new StringReader(workersCsvString), facility, ediProcessTime);
	}

	protected void importInventoryData(Facility facility, String csvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvInventoryImporter importer = createInventoryImporter();
		importer.importSlottedInventoryFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}

	protected BatchResult<Object> importOrdersData(Facility facility, String csvString) throws IOException {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = createOrderImporter();
		return importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}

	protected BatchResult<Object> importOrdersDataHandlingTruncatedGtins(Facility facility, String csvString) throws IOException {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer = createOrderImporter();
		importer.setTruncatedGtins(true); // kludge to test accus strange case
		return importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}

	protected boolean importSlotting(Facility facility, String csvString) {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		ICsvOrderLocationImporter importer = createOrderLocationImporter();
		return importer.importOrderLocationsFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}
	
	protected void compareInstructionsList(List<WorkInstruction> instructions, String[] expectations) {
		Assert.assertEquals(expectations.length, instructions.size());
		for (int i = 0; i < expectations.length; i++) {
			WorkInstruction instruction = instructions.get(i);
			if (!expectations[i].equals(instruction.getItemId())){
				Assert.fail(String.format("Mismatch in item %d. Expected list %s, got [%s]", i, Arrays.toString(expectations), instructionsListToString(instructions)));
			}
		}
	}
	
	private String instructionsListToString(List<WorkInstruction> instructions) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < instructions.size(); i++) {
			result.append(instructions.get(i).getItemId());
			if (i < instructions.size() - 1) {
				result.append(",");
			}
		}
		return result.toString();
	}

	protected void beginTransaction() {
		tenantPersistenceService.beginTransaction();
	}
	protected void commitTransaction() {
		tenantPersistenceService.commitTransaction();
	}
}
