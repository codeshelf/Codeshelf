/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public abstract class EdiTestABC extends DomainTestABC {
	private static final Logger	LOGGER			= LoggerFactory.getLogger(EdiTestABC.class);

	private EventProducer		mEventProducer	= new EventProducer();

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

	private  String padRight(String s, int n) {
	    return String.format("%1$-" + n + "s", s);
	  }
	public void logWiList(List<WorkInstruction> inList) {
		for (WorkInstruction wi : inList) {
			// If this is called from a list of WIs from the site controller, the WI may not have all its normal fields populated.
			String statusStr = padRight(wi.getStatusString(), 8);
			
			LOGGER.debug(statusStr + " WiSort: " + wi.getGroupAndSortCode() + " cntr: " + wi.getContainerId() + " loc: "
					+ wi.getPickInstruction() + "(" + wi.getNominalLocationId() + ")" + " count: " + wi.getPlanQuantity()
					+ " SKU: " + wi.getItemMasterId() + " order: " + wi.getOrderId() + " desc.: " + wi.getDescription());
		}
	}

	public void logOneWi(WorkInstruction inWi) {
			// If this is called from a list of WIs from the site controller, the WI may not have all its normal fields populated.
			String statusStr = padRight(inWi.getStatusString(), 8);
			
			LOGGER.debug(statusStr + " " + inWi.getGroupAndSortCode() + " " + inWi.getContainerId() + " loc: "
					+ inWi.getPickInstruction() + "(" + inWi.getNominalLocationId() + ")" + " count: " + inWi.getPlanQuantity() + " actual: " + inWi.getActualQuantity()
					+ " SKU: " + inWi.getItemMasterId() + " order: " + inWi.getOrderId() + " desc.: " + inWi.getDescription());
	}
	public void logItemList(List<Item> inList) {
		for (Item item : inList)
			LOGGER.debug("SKU: " + item.getItemMasterId() + " cm: " + item.getCmFromLeft() + " posAlongPath: "
					+ item.getPosAlongPathui() + " desc.: " + item.getItemDescription());
	}

}
