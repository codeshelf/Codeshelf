/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.TravelDirectionEnum;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Aisle.AisleDao;
import com.gadgetworks.codeshelf.model.domain.Bay.BayDao;
import com.gadgetworks.codeshelf.model.domain.Che.CheDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork.CodeshelfNetworkDao;
import com.gadgetworks.codeshelf.model.domain.Container.ContainerDao;
import com.gadgetworks.codeshelf.model.domain.ContainerKind.ContainerKindDao;
import com.gadgetworks.codeshelf.model.domain.ContainerUse.ContainerUseDao;
import com.gadgetworks.codeshelf.model.domain.DropboxService.DropboxServiceDao;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.Item.ItemDao;
import com.gadgetworks.codeshelf.model.domain.ItemMaster.ItemMasterDao;
import com.gadgetworks.codeshelf.model.domain.LedController.LedControllerDao;
import com.gadgetworks.codeshelf.model.domain.LocationABC.LocationABCDao;
import com.gadgetworks.codeshelf.model.domain.LocationAlias.LocationAliasDao;
import com.gadgetworks.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.gadgetworks.codeshelf.model.domain.OrderGroup.OrderGroupDao;
import com.gadgetworks.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.gadgetworks.codeshelf.model.domain.OrderLocation.OrderLocationDao;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC.SubLocationDao;
import com.gadgetworks.codeshelf.model.domain.UomMaster.UomMasterDao;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction.WorkInstructionDao;

public abstract class DomainTestABC {

	protected OrganizationDao		mOrganizationDao;
	protected LocationABCDao		mLocationDao;
	protected SubLocationDao		mSubLocationDao;
	protected FacilityDao			mFacilityDao;
	protected PathDao				mPathDao;
	protected PathSegmentDao		mPathSegmentDao;
	protected AisleDao				mAisleDao;
	protected BayDao				mBayDao;
	protected DropboxServiceDao		mDropboxServiceDao;
	protected OrderGroupDao			mOrderGroupDao;
	protected OrderHeaderDao		mOrderHeaderDao;
	protected OrderDetailDao		mOrderDetailDao;
	protected OrderLocationDao		mOrderLocationDao;
	protected CodeshelfNetworkDao	mCodeshelfNetworkDao;
	protected CheDao				mCheDao;
	protected ContainerDao			mContainerDao;
	protected ContainerKindDao		mContainerKindDao;
	protected ContainerUseDao		mContainerUseDao;
	protected ItemMasterDao			mItemMasterDao;
	protected ItemDao				mItemDao;
	protected UomMasterDao			mUomMasterDao;
	protected LedControllerDao		mLedControllerDao;
	protected LocationAliasDao		mLocationAliasDao;
	protected WorkInstructionDao	mWorkInstructionDao;

	private IUtil					mUtil;
	protected ISchemaManager		mSchemaManager;
	private IDatabase				mDatabase;

	public DomainTestABC() {
		super();
	}

	@Before
	public final void setup() {

		try {
			mUtil = new IUtil() {

				public void setLoggingLevelsFromPrefs(Organization inOrganization,
					ITypedDao<PersistentProperty> inPersistentPropertyDao) {
				}

				public String getVersionString() {
					return "";
				}

				public String getApplicationLogDirPath() {
					return ".";
				}

				public String getApplicationDataDirPath() {
					return ".";
				}

				public void exitSystem() {
					System.exit(-1);
				}
			};

			Class.forName("org.h2.Driver");
			mSchemaManager = new H2SchemaManager(mUtil,
				"codeshelf",
				"codeshelf",
				"codeshelf",
				"codeshelf",
				"localhost",
				"",
				"false");
			mDatabase = new Database(mSchemaManager, mUtil);

			mDatabase.start();

			mOrganizationDao = new OrganizationDao(mSchemaManager);
			Organization.DAO = mOrganizationDao;

			mFacilityDao = new FacilityDao(mSchemaManager);
			Facility.DAO = mFacilityDao;

			mPathDao = new PathDao(mSchemaManager);
			Path.DAO = mPathDao;

			mPathSegmentDao = new PathSegmentDao(mSchemaManager);
			PathSegment.DAO = mPathSegmentDao;

			mAisleDao = new AisleDao(mSchemaManager);
			Aisle.DAO = mAisleDao;

			mBayDao = new BayDao(mSchemaManager);
			Bay.DAO = mBayDao;

			mDropboxServiceDao = new DropboxServiceDao(mSchemaManager);
			DropboxService.DAO = mDropboxServiceDao;

			mCodeshelfNetworkDao = new CodeshelfNetworkDao(mSchemaManager);
			CodeshelfNetwork.DAO = mCodeshelfNetworkDao;

			mCheDao = new CheDao(mSchemaManager);
			Che.DAO = mCheDao;

			mSubLocationDao = new SubLocationDao(mSchemaManager);
			SubLocationABC.DAO = mSubLocationDao;

			mLocationDao = new LocationABCDao(mSchemaManager, mDatabase);
			LocationABC.DAO = mLocationDao;

			mOrderGroupDao = new OrderGroupDao(mSchemaManager);
			OrderGroup.DAO = mOrderGroupDao;

			mOrderHeaderDao = new OrderHeaderDao(mSchemaManager);
			OrderHeader.DAO = mOrderHeaderDao;

			mOrderDetailDao = new OrderDetailDao(mSchemaManager);
			OrderDetail.DAO = mOrderDetailDao;

			mOrderLocationDao = new OrderLocationDao(mSchemaManager);
			OrderLocation.DAO = mOrderLocationDao;

			mContainerDao = new ContainerDao(mSchemaManager);
			Container.DAO = mContainerDao;

			mContainerKindDao = new ContainerKindDao(mSchemaManager);
			ContainerKind.DAO = mContainerKindDao;

			mContainerUseDao = new ContainerUseDao(mSchemaManager);
			ContainerUse.DAO = mContainerUseDao;

			mItemMasterDao = new ItemMasterDao(mSchemaManager);
			ItemMaster.DAO = mItemMasterDao;

			mItemDao = new ItemDao(mSchemaManager);
			Item.DAO = mItemDao;

			mUomMasterDao = new UomMasterDao(mSchemaManager);
			UomMaster.DAO = mUomMasterDao;

			mLedControllerDao = new LedControllerDao(mSchemaManager);
			LedController.DAO = mLedControllerDao;

			mLocationAliasDao = new LocationAliasDao(mSchemaManager);
			LocationAlias.DAO = mLocationAliasDao;

			mWorkInstructionDao = new WorkInstructionDao(mSchemaManager);
			WorkInstruction.DAO = mWorkInstructionDao;

		} catch (ClassNotFoundException e) {
		}
	}

	// --------------------------------------------------------------------------
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

		Facility resultFacility = null;

		Organization organization = new Organization();
		organization.setOrganizationId(inOrganizationName);
		mOrganizationDao.store(organization);

		organization.createFacility("F1", "test", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		resultFacility = organization.getFacility("F1");

		Aisle aisle1 = new Aisle(resultFacility, "A1", 0.0, 0.0);
		mAisleDao.store(aisle1);

		Bay baya1b1 = new Bay(aisle1, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya1b1);
		Bay baya1b2 = new Bay(aisle1, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya1b2);

		Aisle aisle2 = new Aisle(resultFacility, "A2", 0.0, 0.0);
		mAisleDao.store(aisle2);

		Bay baya2b1 = new Bay(aisle2, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya2b1);
		Bay baya2b2 = new Bay(aisle2, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya2b2);

		Path path = new Path();
		path.setDomainId(Path.DEFAULT_FACILITY_PATH_ID);
		path.setParent(resultFacility);
		path.setTravelDirEnum(TravelDirectionEnum.FORWARD);
		mPathDao.store(path);
		resultFacility.addPath(path);

		PathSegment pathSegment1 = path.createPathSegment("PS1", aisle1, path, 0, new Point(PositionTypeEnum.METERS_FROM_PARENT,
			0.0,
			0.0,
			0.0), new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0));
		mPathSegmentDao.store(pathSegment1);

		pathSegment1.addLocation(aisle2);
		aisle1.setPathSegment(pathSegment1);
		mAisleDao.store(aisle2);

		Aisle aisle3 = new Aisle(resultFacility, "A3", 0.0, 0.0);
		mAisleDao.store(aisle3);

		Bay baya3b1 = new Bay(aisle3, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya3b1);
		Bay baya3b2 = new Bay(aisle3, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya3b2);

		Aisle aisle4 = new Aisle(resultFacility, "A4", 0.0, 0.0);
		mAisleDao.store(aisle4);

		Bay baya4b1 = new Bay(aisle4, "B1", 0.0, 0.0, 0.0);
		mBayDao.store(baya4b1);
		Bay baya4b2 = new Bay(aisle4, "B2", 0.0, 0.0, 0.0);
		mBayDao.store(baya4b2);

		PathSegment pathSegment2 = path.createPathSegment("PS2", aisle3, path, 1, new Point(PositionTypeEnum.METERS_FROM_PARENT,
			0.0,
			0.0,
			0.0), new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0));
		mPathSegmentDao.store(pathSegment2);

		pathSegment2.addLocation(aisle4);
		aisle1.setPathSegment(pathSegment2);
		mAisleDao.store(aisle4);

		resultFacility.logLocationDistances();

		Container container1 = createContainer("C1", resultFacility);
		Container container2 = createContainer("C2", resultFacility);
		Container container3 = createContainer("C3", resultFacility);

		UomMaster uomMaster = new UomMaster();
		uomMaster.setUomMasterId("EA");
		uomMaster.setParent(resultFacility);
		mUomMasterDao.store(uomMaster);
		resultFacility.addUomMaster(uomMaster);

		ItemMaster itemMaster1 = createItemMaster("ITEM1", resultFacility, uomMaster);
		ItemMaster itemMaster2 = createItemMaster("ITEM2", resultFacility, uomMaster);
		ItemMaster itemMaster3 = createItemMaster("ITEM3", resultFacility, uomMaster);

		OrderHeader orderOut1 = createOrderHeader("OUT1", OrderTypeEnum.OUTBOUND, resultFacility);
		OrderDetail orderOut1Detail1 = createOrderDetail(orderOut1, itemMaster1);
		OrderDetail orderOut1Detail2 = createOrderDetail(orderOut1, itemMaster2);
		OrderDetail orderOut1Detail3 = createOrderDetail(orderOut1, itemMaster3);

		OrderHeader orderOut2 = createOrderHeader("OUT2", OrderTypeEnum.OUTBOUND, resultFacility);
		OrderDetail orderOut2Detail1 = createOrderDetail(orderOut2, itemMaster1);
		OrderDetail orderOut2Detail2 = createOrderDetail(orderOut2, itemMaster2);
		OrderDetail orderOut2Detail3 = createOrderDetail(orderOut2, itemMaster3);

		OrderHeader orderOut3 = createOrderHeader("OUT3", OrderTypeEnum.OUTBOUND, resultFacility);
		OrderDetail orderOut3Detail1 = createOrderDetail(orderOut3, itemMaster1);
		OrderDetail orderOut3Detail2 = createOrderDetail(orderOut3, itemMaster2);
		OrderDetail orderOut3Detail3 = createOrderDetail(orderOut3, itemMaster3);

		OrderHeader orderCross1 = createOrderHeader("CROSS1", OrderTypeEnum.CROSS, resultFacility);
		OrderDetail orderCross1Detail1 = createOrderDetail(orderCross1, itemMaster1);
		OrderDetail orderCross1Detail2 = createOrderDetail(orderCross1, itemMaster2);
		OrderDetail orderCross1Detail3 = createOrderDetail(orderCross1, itemMaster3);
		OrderLocation orderCross1Loc = createOrderLocation(orderCross1, baya1b1);

		OrderHeader orderCross2 = createOrderHeader("CROSS2", OrderTypeEnum.CROSS, resultFacility);
		OrderDetail orderCross2Detail1 = createOrderDetail(orderCross2, itemMaster1);
		OrderDetail orderCross2Detail2 = createOrderDetail(orderCross2, itemMaster2);
		OrderDetail orderCross2Detail3 = createOrderDetail(orderCross2, itemMaster3);

		OrderHeader orderCross3 = createOrderHeader("CROSS3", OrderTypeEnum.CROSS, resultFacility);
		OrderDetail orderCross3Detail1 = createOrderDetail(orderCross3, itemMaster1);
		OrderDetail orderCross3Detail2 = createOrderDetail(orderCross3, itemMaster2);
		OrderDetail orderCross3Detail3 = createOrderDetail(orderCross3, itemMaster3);

		return resultFacility;
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

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderId
	 * @param inOrderType
	 * @param inFacility
	 * @return
	 */
	protected final OrderHeader createOrderHeader(final String inOrderId, final OrderTypeEnum inOrderType, final Facility inFacility) {
		OrderHeader result = null;

		result = new OrderHeader();
		result.setParent(inFacility);
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
		result.setStandardUom(inUomMaster);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
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
}
