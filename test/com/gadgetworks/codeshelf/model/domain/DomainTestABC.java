/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;

import com.gadgetworks.codeshelf.edi.AislesFileCsvImporter;
import com.gadgetworks.codeshelf.edi.ICsvLocationAliasImporter;
import com.gadgetworks.codeshelf.edi.LocationAliasCsvImporter;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.flyweight.command.NetGuid;

public abstract class DomainTestABC extends DAOTestABC {

	public DomainTestABC() {
		super();
	}
	
	// --------------------------------------------------------------------------

	protected Organization getOrganization(final String inOrganizationName) {
		Organization organization = mOrganizationDao.findByDomainId(null, inOrganizationName);
		if (organization == null) {
			organization = new Organization();
			organization.setOrganizationId(inOrganizationName);
			mOrganizationDao.store(organization);
		}
		return organization;
	}
	
	/**
	 * Create a basic organization of the specified name with enough domain data to make it easy to setup various
	 * business case unit tests.
	 * 
	 * @param inOrganizationName
	 */ 
	protected Facility getFacility(Organization inOrganization) {
		String defaultDomainId = "F1";
		
		Facility resultFacility = mFacilityDao.findByDomainId(inOrganization, defaultDomainId);
		if(resultFacility == null) {
			inOrganization.createFacility(defaultDomainId, "test", new Point(PositionTypeEnum.GPS, -120.0, 30.0, 0.0));
			resultFacility = inOrganization.getFacility(defaultDomainId);
		}
		return resultFacility;
	}	
	
	protected CodeshelfNetwork getNetwork(Facility inFacility) {
		String defaultDomainId = "0xFEDCBA";
		
		CodeshelfNetwork codeshelfNetwork = mCodeshelfNetworkDao.findByDomainId(inFacility, defaultDomainId);
		if (codeshelfNetwork == null) {
			codeshelfNetwork = new CodeshelfNetwork(inFacility, defaultDomainId, "Description");
			codeshelfNetwork.getDao().store(codeshelfNetwork);
		}
		return codeshelfNetwork;
	}

	protected Aisle getAisle(Facility facility, String inDomainId) {
		return getAisle(facility, inDomainId, Point.getZeroPoint(), Point.getZeroPoint().add(5.0, 0.0)); 
	}

	protected Aisle getAisle(Facility facility, String inDomainId, Point anchorPoint, Point pickFaceEndPoint) {
		Aisle aisle = mAisleDao.findByDomainId(facility, inDomainId);
		if (aisle == null) {
			aisle = new Aisle(facility, inDomainId, anchorPoint, pickFaceEndPoint);
			mAisleDao.store(aisle);
		}
		return aisle;
	}

	
	protected Bay getBay(Aisle aisle, String inDomainId) {
		Bay bay  = mBayDao.findByDomainId(aisle, inDomainId);
		if (bay == null) {
			bay = new Bay(aisle, inDomainId, Point.getZeroPoint(), Point.getZeroPoint());
			mBayDao.store(bay);
		}
		return bay;
	}

	protected Tier getTier(Bay bay, String inDomainId) {
		Tier tier  = mTierDao.findByDomainId(bay, inDomainId);
		if (tier == null) {
			tier = new Tier(bay, inDomainId, Point.getZeroPoint(), Point.getZeroPoint());
			mTierDao.store(tier);
		}
		return tier;
	}

	protected PathSegment getPathSegment(Path path, Integer inOrder) {
		PathSegment segment = path.getPathSegment(inOrder); 
		if (segment == null) {
			segment = path.createPathSegment(inOrder.toString(), inOrder,  anyPoint(), anyPoint());
			// createPathSegment() does the store
			// mPathSegmentDao.store(segment);
		}
		return segment;
	}

	protected Path getPath(Facility facility, String inPathId) {
		Path path = facility.getPath(inPathId);
		if (path == null) {
			path = new Path(facility, inPathId, "Description");
			mPathDao.store(path);
			// looks wrong. Does not add to facility
		}
		return path;
	}
	
	protected LedController getController(CodeshelfNetwork network, final String inControllerDomainId) {
		LedController controller = network.findOrCreateLedController(inControllerDomainId, new NetGuid(inControllerDomainId));
		controller.setDomainId(inControllerDomainId);
		mLedControllerDao.store(controller);
		return controller;
	}
	
	
	protected Facility createFacility(String orgId) {
		Organization organization = getOrganization(orgId);
		Facility facility = getFacility(organization);
		return facility;
	}
	
	/**
	 * Create a basic organization of the specified name with enough domain data to make it easy to setup various
	 * business case unit tests.
	 * 
	 * @param inOrganizationName

	 * Organization "passed in name"
	 * 	Facility F1
	 * 		Aisle A1
	 * 			Bay B1, Bay B2
	 * 		Aisle 2
	 * 			Bay B1, Bay B2
	 * 
	 * 		Path: Aisle 1 & 2
	 * 		Path Segments: PS1  (<--- need more of these!)
	 * 
	 * 		Container: C1, C2, & C3
	 * 		UomMaster: EA
	 * 		ItemMaster: item1, item2, item3
	 * 		Order: OUT1, OUT2, OUT3, CROSS1, CROSS2, CROSS3
	 * 		OrderDetail: 2 of item1, 4 of item 2, 8 of item 3
	 * 
	 */
	@SuppressWarnings("unused")
	protected Facility createFacilityWithOutboundOrders(final String inOrganizationName) {

		Organization organization = getOrganization(inOrganizationName);
		
		Facility resultFacility = getFacility(organization);
		
		CodeshelfNetwork network = resultFacility.createNetwork("WITEST");
		Che che = network.createChe("WITEST", new NetGuid("0x00000001"));

		LedController controller = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000002"));

		Aisle aisle1 = getAisle(resultFacility, "A1");

		Bay baya1b1 = new Bay(aisle1, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya1b1.setFirstLedNumAlongPath((short) 0);
		baya1b1.setLastLedNumAlongPath((short) 0);
		baya1b1.setLedController(controller);
		mBayDao.store(baya1b1);

		Bay baya1b2 = new Bay(aisle1, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya1b2.setFirstLedNumAlongPath((short) 0);
		baya1b2.setLastLedNumAlongPath((short) 0);
		baya1b2.setLedController(controller);
		mBayDao.store(baya1b2);

		Aisle aisle2 = getAisle(resultFacility, "A2");

		Bay baya2b1 = new Bay(aisle2, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya2b1.setFirstLedNumAlongPath((short) 0);
		baya2b1.setLastLedNumAlongPath((short) 0);
		baya2b1.setLedController(controller);
		mBayDao.store(baya2b1);

		Bay baya2b2 = new Bay(aisle2, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya2b2.setFirstLedNumAlongPath((short) 0);
		baya2b2.setLastLedNumAlongPath((short) 0);
		baya2b2.setLedController(controller);
		mBayDao.store(baya2b2);

		Path path = new Path();
		path.setDomainId(Path.DEFAULT_FACILITY_PATH_ID);
		path.setParent(resultFacility);
		path.setTravelDirEnum(TravelDirectionEnum.FORWARD);
		mPathDao.store(path);
		resultFacility.addPath(path);

		Point startPoint1 = Point.getZeroPoint().add(5.0,0.0);
		PathSegment pathSegment1 = path.createPathSegment("PS1", 0, startPoint1, Point.getZeroPoint());
		mPathSegmentDao.store(pathSegment1);

		aisle1.setPathSegment(pathSegment1);
		mAisleDao.store(aisle1);

		aisle2.setPathSegment(pathSegment1);
		mAisleDao.store(aisle2);

		Aisle aisle3 = getAisle(resultFacility, "A3");

		Bay baya3b1 = new Bay(aisle3, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya3b1.setFirstLedNumAlongPath((short) 0);
		baya3b1.setLastLedNumAlongPath((short) 0);
		baya3b1.setLedController(controller);
		mBayDao.store(baya3b1);

		Bay baya3b2 = new Bay(aisle3, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya3b2.setFirstLedNumAlongPath((short) 0);
		baya3b2.setLastLedNumAlongPath((short) 0);
		baya3b2.setLedController(controller);
		mBayDao.store(baya3b2);

		Aisle aisle4 = getAisle(resultFacility, "A4");

		Bay baya4b1 = new Bay(aisle4, "B1", Point.getZeroPoint(), Point.getZeroPoint());
		baya4b1.setFirstLedNumAlongPath((short) 0);
		baya4b1.setLastLedNumAlongPath((short) 0);
		baya4b1.setLedController(controller);
		mBayDao.store(baya4b1);

		Bay baya4b2 = new Bay(aisle4, "B2", Point.getZeroPoint(), Point.getZeroPoint());
		baya4b2.setFirstLedNumAlongPath((short) 0);
		baya4b2.setLastLedNumAlongPath((short) 0);
		baya4b2.setLedController(controller);
		mBayDao.store(baya4b2);

		PathSegment pathSegment2 = path.createPathSegment("PS2", 1, Point.getZeroPoint(), Point.getZeroPoint());
		mPathSegmentDao.store(pathSegment2);

		aisle3.setPathSegment(pathSegment2);
		mAisleDao.store(aisle3);

		aisle4.setPathSegment(pathSegment2);
		mAisleDao.store(aisle4);

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
//		Container container6 = createContainer("C6", resultFacility);
//		Container container7 = createContainer("C7", resultFacility);

		ItemMaster itemMaster4 = createItemMaster("ITEM4", resultFacility, uomMaster);
		ItemMaster itemMaster5 = createItemMaster("ITEM5", resultFacility, uomMaster);
//		ItemMaster itemMaster6 = createItemMaster("ITEM6", resultFacility, uomMaster);
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

//		OrderHeader orderCross6 = createOrderHeader("CROSS6", OrderTypeEnum.CROSS, resultFacility, orderGroup2);
//		OrderDetail orderCross6Detail1 = createOrderDetail(orderCross6, itemMaster6);
//
//		OrderHeader orderCross7 = createOrderHeader("CROSS7", OrderTypeEnum.CROSS, resultFacility, null);
//		OrderDetail orderCross7Detail1 = createOrderDetail(orderCross7, itemMaster7);

		ContainerUse containerUse4 = createContainerUse(container4, orderCross4, resultFacility);
		ContainerUse containerUse5 = createContainerUse(container5, orderCross5, resultFacility);
//		ContainerUse containerUse6 = createContainerUse(container6, orderCross6, resultFacility);
//		ContainerUse containerUse7 = createContainerUse(container7, orderCross7, resultFacility);

		return resultFacility;
	}
	
	@SuppressWarnings("rawtypes")
	protected Facility setUpSimpleNoSlotFacility(String inOrganizationName) {
		// This returns a facility with aisle A1, with two bays with one tier each. No slots. With a path, associated to the aisle.
		//   With location alias for first baytier only, not second.
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used
		// Valid tier names: A1.B1.T1 = D101, and A1.B2.T1
		// Also, A1.B1 has alias D100
		// Just for variance, bay3 has 4 slots
		// Aisle 2 associated to same path segment. But with aisle controller on the other side
		// Aisle 3 will be on a separate path.
		// All tiers have controllers associated.
		// There is a single CHE called CHE1

		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,0,,\r\n" //
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Aisle,A3,,,,,tierNotB1S1Side,12.85,65.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);

		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = new AislesFileCsvImporter(mAisleDao, mBayDao, mTierDao, mSlotDao);
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		// Get the aisle
		Aisle aisle1 = Aisle.DAO.findByDomainId(facility, "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest("F5X.1", facility);
		PathSegment segment0 = addPathSegmentForTest("F5X.1.0", aPath, 0, 22.0, 48.45, 10.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.DAO.findByDomainId(facility, "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest("F5X.3", facility);
		PathSegment segment02 = addPathSegmentForTest("F5X.3.0", path2, 0, 22.0, 58.45, 10.85, 58.45);

		Aisle aisle3 = Aisle.DAO.findByDomainId(facility, "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);

		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D100\r\n" //
				+ "A1.B1.T1, D101\r\n" //
				+ "A1.B1.T1.S1, D301\r\n" //
				+ "A1.B1.T1.S2, D302\r\n" //
				+ "A1.B1.T1.S3, D303\r\n" //
				+ "A1.B1.T1.S4, D304\r\n" //
				+ "A2.B1.T1, D402\r\n" //
				+ "A2.B2.T1, D403\r\n"//
				+ "A3.B1.T1, D502\r\n" //
				+ "A3.B2.T1, D503\r\n";//

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mLocationAliasDao);
		importer2.importLocationAliasesFromCsvStream(reader2, facility, ediProcessTime2);

		String nName = "N-" + inOrganizationName;
		CodeshelfNetwork network = facility.createNetwork(nName);
		//Che che = 
		network.createChe("CHE1", new NetGuid("0x00000001"));

		LedController controller1 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController(inOrganizationName, new NetGuid("0x00000013"));
		String uuid1 = controller1.getPersistentId().toString();
		String uuid2 = controller2.getPersistentId().toString();
		String uuid3 = controller3.getPersistentId().toString();

		SubLocationABC tier = (SubLocationABC) facility.findSubLocationById("A1.B1.T1");

		((Tier) tier).setControllerChannel(uuid1, "1", "aisle"); // all A1 T1

		tier = (SubLocationABC) facility.findSubLocationById("A2.B1.T1");
		((Tier) tier).setControllerChannel(uuid2, "1", "aisle"); // all A2 T1

		tier = (SubLocationABC) facility.findSubLocationById("A3.B1.T1");
		((Tier) tier).setControllerChannel(uuid3, "1", "aisle"); // all A3 T1

		return facility;

	}

	protected UomMaster createUomMaster(String inUom, Facility inFacility) {
		UomMaster uomMaster = new UomMaster();
		uomMaster.setUomMasterId(inUom);
		uomMaster.setParent(inFacility);
		mUomMasterDao.store(uomMaster);
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
		result.setParent(inFacility);
		result.setKind(inFacility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mContainerDao.store(result);
		
		inFacility.addContainer(result);

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
		result.setParentContainer(inContainer);
		result.setDomainId(inContainer.getContainerId());
		result.setOrderHeader(inOrderHeader);
		result.setUsedOn(new Timestamp(System.currentTimeMillis()));
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mContainerUseDao.store(result);
		
		inContainer.addContainerUse(result);
		
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
		result.setParent(inFacility);
		result.setOrderGroupId(inOrderGroupId);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderGroupDao.store(result);
		
		inFacility.addOrderGroup(result);
		
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
		result.setOrderTypeEnum(inOrderType);
		result.setOrderDate(new Timestamp(System.currentTimeMillis()));
		result.setDueDate(new Timestamp(System.currentTimeMillis()));
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderHeaderDao.store(result);
		inFacility.addOrderHeader(result);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderHeader
	 * @param inItemMaster
	 * @return
	 */
	protected final OrderDetail createOrderDetail(final OrderHeader inOrderHeader, final ItemMaster inItemMaster) {

		OrderDetail result = new OrderDetail();
		result.setDomainId(inItemMaster.getItemId());
		result.setParent(inOrderHeader);
		result.setItemMaster(inItemMaster);
		result.setQuantities(5);
		result.setUomMaster(inItemMaster.getStandardUom());
		result.setStatusEnum(OrderStatusEnum.CREATED);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderDetailDao.store(result);
		inOrderHeader.addOrderDetail(result);

		return result;
	}

	protected final ItemMaster createItemMaster(final String inItemId, final Facility inFacility, final UomMaster inUomMaster) {
		ItemMaster result = null;

		result = new ItemMaster();
		result.setItemId(inItemId);
		result.setParent(inFacility);
		result.setDescription(inItemId);
		result.setStandardUom(inUomMaster);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		inFacility.addItemMaster(result);
		mItemMasterDao.store(result);

		return result;
	}

	protected final OrderLocation createOrderLocation(final OrderHeader inOrderHeader, final ILocation<?> inLocation) {
		OrderLocation result = null;

		result = new OrderLocation();
		result.setDomainId(OrderLocation.makeDomainId(inOrderHeader, inLocation));
		result.setParent(inOrderHeader);
		result.setLocation(inLocation);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderLocationDao.store(result);
		inOrderHeader.addOrderLocation(result);

		return result;
	}
	
	protected Point anyPoint() {
		return new Point(PositionTypeEnum.METERS_FROM_PARENT, Math.random()*10, Math.random()*10, Math.random()*10);
	}
	
	protected PathSegment addPathSegmentForTest(final String inSegmentId,
		final Path inPath,
		final Integer inSegmentOrder,
		Double inStartX,
		Double inStartY,
		Double inEndX,
		Double inEndY) {

		Point head = new Point(PositionTypeEnum.METERS_FROM_PARENT, inStartX, inStartY, 0.0);
		Point tail = new Point(PositionTypeEnum.METERS_FROM_PARENT, inEndX, inEndY, 0.0);
		PathSegment returnSeg = inPath.createPathSegment(inSegmentId, inSegmentOrder, head, tail);
		return returnSeg;
	}

	protected Path createPathForTest(String inDomainId, Facility inFacility) {
		return inFacility.createPath(inDomainId);
	}

}
