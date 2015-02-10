/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Aisle.AisleDao;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Bay.BayDao;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Che.CheDao;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork.CodeshelfNetworkDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Container.ContainerDao;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
import com.gadgetworks.codeshelf.model.domain.ContainerKind.ContainerKindDao;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.ContainerUse.ContainerUseDao;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.DropboxService.DropboxServiceDao;
import com.gadgetworks.codeshelf.model.domain.EdiDocumentLocator;
import com.gadgetworks.codeshelf.model.domain.EdiDocumentLocator.EdiDocumentLocatorDao;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC.EdiServiceABCDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.IronMqService;
import com.gadgetworks.codeshelf.model.domain.IronMqService.IronMqServiceDao;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.Item.ItemDao;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.ItemMaster.ItemMasterDao;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LedController.LedControllerDao;
//import com.gadgetworks.codeshelf.model.domain.LocationABC.LocationABCDao;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.model.domain.LocationAlias.LocationAliasDao;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderGroup.OrderGroupDao;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.gadgetworks.codeshelf.model.domain.OrderLocation;
import com.gadgetworks.codeshelf.model.domain.OrderLocation.OrderLocationDao;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.SiteController.SiteControllerDao;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Slot.SlotDao;
//import com.gadgetworks.codeshelf.model.domain.SubLocationABC.SubLocationDao;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Tier.TierDao;
import com.gadgetworks.codeshelf.model.domain.UnspecifiedLocation;
import com.gadgetworks.codeshelf.model.domain.UnspecifiedLocation.UnspecifiedLocationDao;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.UomMaster.UomMasterDao;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;
import com.gadgetworks.codeshelf.model.domain.WorkArea;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction.WorkInstructionDao;
import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

public abstract class DAOTestABC {
	@Rule
	public TestName testName = new TestName();
	
	static {
		Configuration.loadConfig("test");
		try {
			org.h2.tools.Server.createWebServer("-webPort", "8082").start();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected PersistenceService persistenceService;
	Facility defaultFacility = null;
	
	protected FacilityDao			mFacilityDao;
	protected PathDao				mPathDao;
	protected PathSegmentDao		mPathSegmentDao;
	protected AisleDao				mAisleDao;
	protected BayDao				mBayDao;
	protected TierDao				mTierDao;
	protected SlotDao				mSlotDao;
	protected DropboxServiceDao		mDropboxServiceDao;
	protected EdiServiceABCDao 		mEdiServiceABCDao;
	protected EdiDocumentLocatorDao			mEdiDocumentLocatorDao;
	protected OrderGroupDao			mOrderGroupDao;
	protected OrderHeaderDao		mOrderHeaderDao;
	protected OrderDetailDao		mOrderDetailDao;
	protected OrderLocationDao		mOrderLocationDao;
	protected CodeshelfNetworkDao	mCodeshelfNetworkDao;
	protected ITypedDao<Che>				mCheDao;
	protected SiteControllerDao		mSiteControllerDao; 
	protected ContainerDao			mContainerDao;
	protected ContainerKindDao		mContainerKindDao;
	protected ContainerUseDao		mContainerUseDao;
	protected ItemMasterDao			mItemMasterDao;
	protected ItemDao				mItemDao;
	protected UnspecifiedLocationDao mUnspecifiedLocationDao;
	protected UomMasterDao			mUomMasterDao;
	protected IronMqServiceDao		mIronMqServiceDao;
	protected LedControllerDao		mLedControllerDao;
	protected LocationAliasDao		mLocationAliasDao;
	protected VertexDao				mVertexDao;
	protected WorkInstructionDao	mWorkInstructionDao;
	protected WorkAreaDao			mWorkAreaDao;

	public DAOTestABC() {
		super();
	}
	
	public Tenant getDefaultTenant() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

	public PersistenceService getPersistenceService() {
		return PersistenceService.getInstance();
	}
	
	public Facility createFacility() {
		return Facility.createFacility(getDefaultTenant(), this.getTestName(), "Test Facility", Point.getZeroPoint());
	}
	
	public Facility getDefaultFacility() {
		if(defaultFacility == null) {
			defaultFacility = createFacility();
		}
		return defaultFacility;
	}
	
	@Before
	public final void setup() throws Exception {
		TenantManagerService.getInstance().connect();
		
		persistenceService = PersistenceService.getInstance();
		assertTrue(persistenceService.isRunning());

		mFacilityDao = new FacilityDao(persistenceService);
		Facility.DAO = mFacilityDao;

		mAisleDao = new AisleDao(persistenceService);
		Aisle.DAO = mAisleDao;

		mBayDao = new BayDao(persistenceService);
		Bay.DAO = mBayDao;

		mTierDao = new TierDao(persistenceService);
		Tier.DAO = mTierDao;

		mSlotDao = new SlotDao(persistenceService);
		Slot.DAO = mSlotDao;

		mPathDao = new PathDao(persistenceService);
		Path.DAO = mPathDao;

		mPathSegmentDao = new PathSegmentDao(persistenceService);
		PathSegment.DAO = mPathSegmentDao;

		mDropboxServiceDao = new DropboxServiceDao(persistenceService);
		DropboxService.DAO = mDropboxServiceDao;

		mIronMqServiceDao = new IronMqServiceDao(persistenceService);
		IronMqService.DAO = mIronMqServiceDao;

		mEdiServiceABCDao = new EdiServiceABCDao(persistenceService);
		EdiServiceABC.DAO = mEdiServiceABCDao;
		
		mEdiDocumentLocatorDao = new EdiDocumentLocatorDao(persistenceService);
		EdiDocumentLocator.DAO = mEdiDocumentLocatorDao;
		
		mCodeshelfNetworkDao = new CodeshelfNetworkDao(persistenceService);
		CodeshelfNetwork.DAO = mCodeshelfNetworkDao;

		mCheDao = new CheDao(persistenceService);
		Che.DAO = mCheDao;

		mSiteControllerDao = new SiteControllerDao(persistenceService);
		SiteController.DAO = mSiteControllerDao;

		mOrderGroupDao = new OrderGroupDao(persistenceService);
		OrderGroup.DAO = mOrderGroupDao;

		mOrderHeaderDao = new OrderHeaderDao(persistenceService);
		OrderHeader.DAO = mOrderHeaderDao;

		mOrderDetailDao = new OrderDetailDao(persistenceService);
		OrderDetail.DAO = mOrderDetailDao;

		mOrderLocationDao = new OrderLocationDao(persistenceService);
		OrderLocation.DAO = mOrderLocationDao;

		mContainerDao = new ContainerDao(persistenceService);
		Container.DAO = mContainerDao;

		mContainerKindDao = new ContainerKindDao(persistenceService);
		ContainerKind.DAO = mContainerKindDao;

		mContainerUseDao = new ContainerUseDao(persistenceService);
		ContainerUse.DAO = mContainerUseDao;

		mItemMasterDao = new ItemMasterDao(persistenceService);
		ItemMaster.DAO = mItemMasterDao;

		mItemDao = new ItemDao(persistenceService);
		Item.DAO = mItemDao;
		
		mIronMqServiceDao = new IronMqServiceDao(persistenceService);
		IronMqService.DAO = mIronMqServiceDao;
		
		mUomMasterDao = new UomMasterDao(persistenceService);
		UomMaster.DAO = mUomMasterDao;

		mUnspecifiedLocationDao = new UnspecifiedLocationDao(persistenceService);
		UnspecifiedLocation.DAO = mUnspecifiedLocationDao;
		
		mLedControllerDao = new LedControllerDao(persistenceService);
		LedController.DAO = mLedControllerDao;

		mLocationAliasDao = new LocationAliasDao(persistenceService);
		LocationAlias.DAO = mLocationAliasDao;
		
		mVertexDao = new VertexDao(persistenceService);
		Vertex.DAO = mVertexDao;

		mWorkAreaDao = new WorkAreaDao(persistenceService);
		WorkArea.DAO = mWorkAreaDao;

		mWorkInstructionDao = new WorkInstructionDao(persistenceService);
		WorkInstruction.DAO = mWorkInstructionDao;

		mWorkAreaDao = new WorkAreaDao(persistenceService);
		WorkArea.DAO = mWorkAreaDao;
		
		// make sure default properties are in the database
		persistenceService.beginTenantTransaction();
        PropertyDao.getInstance().syncPropertyDefaults();
        persistenceService.commitTenantTransaction();
			
		doBefore();
	}
	
	public void doBefore() throws Exception {
	}
	
	@After
	public final void tearDown() {
		doAfter();
	}
	
	public void doAfter() {
		persistenceService.stop();
		TenantManagerService.getInstance().resetTenant(getDefaultTenant());
	}

	protected String getTestName() {
		return testName.getMethodName();
	}
	
}
