/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;

public abstract class EdiTestABC extends DomainTestABC {
	
	private EventProducer mEventProducer = new EventProducer();
	
	protected AislesFileCsvImporter createAisleFileImporter() {
		return new AislesFileCsvImporter(mEventProducer, mAisleDao, mBayDao, mTierDao, mSlotDao);
	}

	protected ICsvOrderImporter createOrderImporter() {
		ICsvOrderImporter orderImporter = new OutboundOrderCsvImporter(mEventProducer,
			mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		return orderImporter;
	}

	protected ICsvCrossBatchImporter createCrossBatchImporter() {
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mEventProducer,
			mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		return importer;
	}

	protected ICsvLocationAliasImporter createLocationAliasImporter() {
		ICsvLocationAliasImporter importer2 = new LocationAliasCsvImporter(mEventProducer, mLocationAliasDao);
		return importer2;
	}

	protected ICsvOrderLocationImporter createOrderLocationImporter() {
		ICsvOrderLocationImporter importer = new OrderLocationCsvImporter(mEventProducer, mOrderLocationDao);
		return importer;
	}

	protected ICsvInventoryImporter createInventoryImporter() {
		return new InventoryCsvImporter(mEventProducer, mItemMasterDao, mItemDao, mUomMasterDao);
	}
}
