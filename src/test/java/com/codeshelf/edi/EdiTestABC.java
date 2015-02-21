/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiTestABC.java,v 1.3 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainTestABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.PropertyService;

public abstract class EdiTestABC extends DomainTestABC {
	private static final Logger	LOGGER			= LoggerFactory.getLogger(EdiTestABC.class);
	protected PropertyService mPropertyService = new PropertyService();
	
	private EventProducer		mEventProducer	= new EventProducer();

	@Override
	public void doBefore() throws Exception {
		super.doBefore();
	}
	
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
			workService,
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
			
			LOGGER.info(statusStr + " WiSort: " + wi.getGroupAndSortCode() + " cntr: " + wi.getContainerId() + " loc: "
					+ wi.getPickInstruction() + "(" + wi.getNominalLocationId() + ")" + " count: " + wi.getPlanQuantity()
					+ " SKU: " + wi.getItemId() + " order: " + wi.getOrderId() + " desc.: " + wi.getDescription());
		}
	}

	public void logOneWi(WorkInstruction inWi) {
			// If this is called from a list of WIs from the site controller, the WI may not have all its normal fields populated.
			String statusStr = padRight(inWi.getStatusString(), 8);
			
			LOGGER.info(statusStr + " " + inWi.getGroupAndSortCode() + " " + inWi.getContainerId() + " loc: "
					+ inWi.getPickInstruction() + "(" + inWi.getNominalLocationId() + ")" + " count: " + inWi.getPlanQuantity() + " actual: " + inWi.getActualQuantity()
					+ " SKU: " + inWi.getItemMasterId() + " order: " + inWi.getOrderId() + " desc.: " + inWi.getDescription());
	}
	public void logItemList(List<Item> inList) {
		for (Item item : inList)
			LOGGER.info("SKU: " + item.getItemMasterId() + " cm: " + item.getCmFromLeft() + " posAlongPath: "
					+ item.getPosAlongPathui() + " desc.: " + item.getItemDescription());
	}

	protected List<WorkInstruction> startWorkFromBeginning(Facility facility, String cheName, String containers) {
		// Now ready to run the cart
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe(cheName);
	
		workService.setUpCheContainerFromString(theChe, containers);
	
		List<WorkInstruction> wiList = workService.getWorkInstructions(theChe, ""); // This returns them in working order.
		logWiList(wiList);
		return wiList;
	
	}

}
