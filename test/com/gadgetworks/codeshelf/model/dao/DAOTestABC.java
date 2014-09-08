/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import org.junit.Before;

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
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.LocationABC.LocationABCDao;
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
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Slot.SlotDao;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC.SubLocationDao;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Tier.TierDao;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.UomMaster.UomMasterDao;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.Vertex.VertexDao;
import com.gadgetworks.codeshelf.model.domain.WorkArea;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction.WorkInstructionDao;

public abstract class DAOTestABC {
	
	static {
		Configuration.loadConfig("server");
	}
	
	protected OrganizationDao		mOrganizationDao;
	protected LocationABCDao		mLocationDao;
	protected SubLocationDao		mSubLocationDao;
	protected FacilityDao			mFacilityDao;
	protected PathDao				mPathDao;
	protected PathSegmentDao		mPathSegmentDao;
	protected AisleDao				mAisleDao;
	protected BayDao				mBayDao;
	protected TierDao				mTierDao;
	protected SlotDao				mSlotDao;
	protected DropboxServiceDao		mDropboxServiceDao;
	protected IronMqServiceDao		mIronMqServiceDao;
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
	protected VertexDao				mVertexDao;
	protected WorkInstructionDao	mWorkInstructionDao;
	protected WorkAreaDao			mWorkAreaDao;

	protected ISchemaManager		mSchemaManager;
	private IDatabase				mDatabase;

	public DAOTestABC() {
		super();
	}

	@Before
	public final void setup() {

		try {

			Class.forName("org.h2.Driver");
			mSchemaManager = new H2SchemaManager(
				"codeshelf",
				"codeshelf",
				"codeshelf",
				"codeshelf",
				"localhost",
				"");
			mDatabase = new Database(mSchemaManager);

			mDatabase.start();

			mOrganizationDao = new OrganizationDao(mSchemaManager);
			Organization.DAO = mOrganizationDao;

			mFacilityDao = new FacilityDao(mSchemaManager);
			Facility.DAO = mFacilityDao;

			mAisleDao = new AisleDao(mSchemaManager);
			Aisle.DAO = mAisleDao;

			mBayDao = new BayDao(mSchemaManager);
			Bay.DAO = mBayDao;

			mTierDao = new TierDao(mSchemaManager);
			Tier.DAO = mTierDao;

			mSlotDao = new SlotDao(mSchemaManager);
			Slot.DAO = mSlotDao;

			mPathDao = new PathDao(mSchemaManager);
			Path.DAO = mPathDao;

			mPathSegmentDao = new PathSegmentDao(mSchemaManager);
			PathSegment.DAO = mPathSegmentDao;

			mDropboxServiceDao = new DropboxServiceDao(mSchemaManager);
			DropboxService.DAO = mDropboxServiceDao;

			mIronMqServiceDao = new IronMqServiceDao(mSchemaManager);
			IronMqService.DAO = mIronMqServiceDao;

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
			
			mVertexDao = new VertexDao(mSchemaManager);
			Vertex.DAO = mVertexDao;

			mWorkAreaDao = new WorkAreaDao(mSchemaManager);
			WorkArea.DAO = mWorkAreaDao;

			mWorkInstructionDao = new WorkInstructionDao(mSchemaManager);
			WorkInstruction.DAO = mWorkInstructionDao;

			mWorkAreaDao = new WorkAreaDao(mSchemaManager);
			WorkArea.DAO = mWorkAreaDao;
			
			doBefore();
		} catch (ClassNotFoundException e) {
		}
	}
	
	protected void doBefore() {
		
	}
}
