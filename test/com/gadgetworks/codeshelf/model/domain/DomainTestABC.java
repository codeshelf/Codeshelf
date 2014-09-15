/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

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
			codeshelfNetwork = new CodeshelfNetwork(inFacility, defaultDomainId, "Description", "Credential");
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
		result.setQuantity(5);
		result.setMinQuantity(5);
		result.setMaxQuantity(5);
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
		Point tail = new Point(PositionTypeEnum.METERS_FROM_PARENT, inEndX, inEndX, 0.0);
		PathSegment returnSeg = inPath.createPathSegment(inSegmentId, inSegmentOrder, head, tail);
		return returnSeg;
	}

	protected Path createPathForTest(String inDomainId, Facility inFacility) {
		return inFacility.createPath(inDomainId);
	}

}
