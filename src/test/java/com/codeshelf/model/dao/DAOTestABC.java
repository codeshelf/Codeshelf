/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.codeshelf.application.JvmProperties;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Aisle.AisleDao;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Bay.BayDao;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Che.CheDao;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.CodeshelfNetwork.CodeshelfNetworkDao;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.Container.ContainerDao;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.ContainerKind.ContainerKindDao;
import com.codeshelf.model.domain.ContainerUse;
import com.codeshelf.model.domain.ContainerUse.ContainerUseDao;
import com.codeshelf.model.domain.DropboxService;
import com.codeshelf.model.domain.DropboxService.DropboxServiceDao;
import com.codeshelf.model.domain.EdiDocumentLocator;
import com.codeshelf.model.domain.EdiDocumentLocator.EdiDocumentLocatorDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Facility.FacilityDao;
import com.codeshelf.model.domain.IronMqService;
import com.codeshelf.model.domain.IronMqService.IronMqServiceDao;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.Item.ItemDao;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.ItemMaster.ItemMasterDao;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.LedController.LedControllerDao;
//import com.codeshelf.model.domain.LocationABC.LocationABCDao;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.LocationAlias.LocationAliasDao;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderDetail.OrderDetailDao;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderGroup.OrderGroupDao;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderHeader.OrderHeaderDao;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.model.domain.OrderLocation.OrderLocationDao;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.Path.PathDao;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.SiteController.SiteControllerDao;
import com.codeshelf.model.domain.Slot;
import com.codeshelf.model.domain.Slot.SlotDao;
//import com.codeshelf.model.domain.SubLocationABC.SubLocationDao;
import com.codeshelf.model.domain.Tier;
import com.codeshelf.model.domain.Tier.TierDao;
import com.codeshelf.model.domain.UnspecifiedLocation;
import com.codeshelf.model.domain.UnspecifiedLocation.UnspecifiedLocationDao;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.UomMaster.UomMasterDao;
import com.codeshelf.model.domain.Vertex;
import com.codeshelf.model.domain.Vertex.VertexDao;
import com.codeshelf.model.domain.WorkArea;
import com.codeshelf.model.domain.WorkArea.WorkAreaDao;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkInstruction.WorkInstructionDao;
import com.codeshelf.platform.multitenancy.ITenantManager;
import com.codeshelf.platform.multitenancy.ManagerPersistenceService;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.TenantManagerService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.WorkService;
import com.google.common.collect.ImmutableCollection;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.common.util.concurrent.ServiceManager;

public abstract class DAOTestABC {
	@Rule
	public TestName testName = new TestName();

	static ServiceManager jvmServiceManager;
	static {
		JvmProperties.load("test");
		
		// start singleton services here (i.e. per jvm, not per test)
		// see below for ephemeral services
		List<Service> services = new ArrayList<Service>();
		services.add(TenantManagerService.getNonRunningInstance());
		services.add(ManagerPersistenceService.getNonRunningInstance());
		services.add(TenantPersistenceService.getNonRunningInstance());
		jvmServiceManager = new ServiceManager(services);
		try {
			jvmServiceManager.startAsync().awaitHealthy(60, TimeUnit.SECONDS);
		} catch (TimeoutException e1) {
			throw new RuntimeException("Could not start unit test services",e1);
		}
		
		// start h2 web service
		try {
			org.h2.tools.Server.createWebServer("-webPort", "8082").start();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	protected ServiceManager ephemeralServiceManager;
	ITenantManager tenantManager;

	public TenantPersistenceService tenantPersistenceService; // convenience
	
	// ephemeral services:
	protected WorkService workService;
	
	Facility defaultFacility = null;
	
	protected FacilityDao			mFacilityDao;
	protected PathDao				mPathDao;
	protected PathSegmentDao		mPathSegmentDao;
	protected AisleDao				mAisleDao;
	protected BayDao				mBayDao;
	protected TierDao				mTierDao;
	protected SlotDao				mSlotDao;
	protected DropboxServiceDao		mDropboxServiceDao;
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

	//@Inject
	//public DAOTestABC(ITenantManager tenantManager) {
	public DAOTestABC() {
		super();
		//this.tenantManager = tenantManager;
		this.tenantManager = TenantManagerService.getMaybeRunningInstance();
	}
	
	public Tenant getDefaultTenant() {
		return TenantManagerService.getInstance().getDefaultTenant();
	}

	public TenantPersistenceService getTenantPersistenceService() {
		return TenantPersistenceService.getInstance();
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
		this.tenantPersistenceService = TenantPersistenceService.getInstance();
		
		mFacilityDao = new FacilityDao();
		Facility.DAO = mFacilityDao;

		mAisleDao = new AisleDao();
		Aisle.DAO = mAisleDao;

		mBayDao = new BayDao();
		Bay.DAO = mBayDao;

		mTierDao = new TierDao();
		Tier.DAO = mTierDao;

		mSlotDao = new SlotDao();
		Slot.DAO = mSlotDao;

		mPathDao = new PathDao();
		Path.DAO = mPathDao;

		mPathSegmentDao = new PathSegmentDao();
		PathSegment.DAO = mPathSegmentDao;

		mDropboxServiceDao = new DropboxServiceDao();
		DropboxService.DAO = mDropboxServiceDao;

		mIronMqServiceDao = new IronMqServiceDao();
		IronMqService.DAO = mIronMqServiceDao;

		mEdiDocumentLocatorDao = new EdiDocumentLocatorDao();
		EdiDocumentLocator.DAO = mEdiDocumentLocatorDao;
		
		mCodeshelfNetworkDao = new CodeshelfNetworkDao();
		CodeshelfNetwork.DAO = mCodeshelfNetworkDao;

		mCheDao = new CheDao();
		Che.DAO = mCheDao;

		mSiteControllerDao = new SiteControllerDao();
		SiteController.DAO = mSiteControllerDao;

		mOrderGroupDao = new OrderGroupDao();
		OrderGroup.DAO = mOrderGroupDao;

		mOrderHeaderDao = new OrderHeaderDao();
		OrderHeader.DAO = mOrderHeaderDao;

		mOrderDetailDao = new OrderDetailDao();
		OrderDetail.DAO = mOrderDetailDao;

		mOrderLocationDao = new OrderLocationDao();
		OrderLocation.DAO = mOrderLocationDao;

		mContainerDao = new ContainerDao();
		Container.DAO = mContainerDao;

		mContainerKindDao = new ContainerKindDao();
		ContainerKind.DAO = mContainerKindDao;

		mContainerUseDao = new ContainerUseDao();
		ContainerUse.DAO = mContainerUseDao;

		mItemMasterDao = new ItemMasterDao();
		ItemMaster.DAO = mItemMasterDao;

		mItemDao = new ItemDao();
		Item.DAO = mItemDao;
		
		mIronMqServiceDao = new IronMqServiceDao();
		IronMqService.DAO = mIronMqServiceDao;
		
		mUomMasterDao = new UomMasterDao();
		UomMaster.DAO = mUomMasterDao;

		mUnspecifiedLocationDao = new UnspecifiedLocationDao();
		UnspecifiedLocation.DAO = mUnspecifiedLocationDao;
		
		mLedControllerDao = new LedControllerDao();
		LedController.DAO = mLedControllerDao;

		mLocationAliasDao = new LocationAliasDao();
		LocationAlias.DAO = mLocationAliasDao;
		
		mVertexDao = new VertexDao();
		Vertex.DAO = mVertexDao;

		mWorkAreaDao = new WorkAreaDao();
		WorkArea.DAO = mWorkAreaDao;

		mWorkInstructionDao = new WorkInstructionDao();
		WorkInstruction.DAO = mWorkInstructionDao;

		mWorkAreaDao = new WorkAreaDao();
		WorkArea.DAO = mWorkAreaDao;
		
		// make sure default properties are in the database
		TenantPersistenceService.getInstance().beginTransaction();
        PropertyDao.getInstance().syncPropertyDefaults();
        TenantPersistenceService.getInstance().commitTransaction();
			
		doBefore();
	}
	
	protected void initializeEphemeralServiceManager() {
		if(ephemeralServiceManager != null) {
			//throw new RuntimeException("could not initialize ephemeralServiceManager (already started)");
		} else {
			// start ephemeral services. these will be stopped in @After
			// must use new service objects (services cannot be restarted)
			List<Service> services = new ArrayList<Service>();
			// services.add(new Service()); e.g.
			this.workService = this.generateWorkService();
			if(this.workService != null)
				services.add(this.workService);
			this.ephemeralServiceManager = new ServiceManager(services);
			try {
				this.ephemeralServiceManager.startAsync().awaitHealthy(10, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout starting ephemeralServiceManager",e);
			}
		}
	}

	protected WorkService generateWorkService() {
		return new WorkService();
	}

	public void doBefore() throws Exception {
		initializeEphemeralServiceManager();
	}
	
	@After
	public final void tearDown() {
		doAfter();
	}
	
	public void doAfter() {
		boolean hadActiveTransactions = this.tenantPersistenceService.rollbackAnyActiveTransactions();
		
		TenantManagerService.getInstance().resetTenant(getDefaultTenant());
		this.tenantPersistenceService.forgetInitialActions(getDefaultTenant());

		if(this.ephemeralServiceManager != null) {
			try {
				this.ephemeralServiceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("timeout stopping ephemeralServiceManager",e);
			}
			ImmutableCollection<Service> failedServices = ephemeralServiceManager.servicesByState().get(State.FAILED);
			Assert.assertTrue(failedServices == null || failedServices.isEmpty());
		}

		Assert.assertFalse(hadActiveTransactions);

	}

	protected String getTestName() {
		return testName.getMethodName();
	}
	
}
