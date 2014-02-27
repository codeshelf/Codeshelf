/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import org.junit.Before;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC.SubLocationDao;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.UomMaster.UomMasterDao;

public abstract class EdiTestABC {

	protected OrganizationDao		mOrganizationDao;
	protected LocationABCDao		mLocationDao;
	protected SubLocationDao		mSubLocationDao;
	protected FacilityDao			mFacilityDao;
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

	private IUtil					mUtil;
	protected ISchemaManager		mSchemaManager;
	private IDatabase				mDatabase;

	public EdiTestABC() {
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

			mLocationDao = new LocationABCDao(mSchemaManager);
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

		} catch (ClassNotFoundException e) {
		}
	}
}
