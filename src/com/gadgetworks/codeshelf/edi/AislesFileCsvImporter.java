/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameMappingStrategy;

import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author ranstrom
 *
 */
@Singleton
public class AislesFileCsvImporter {
	
	private static double CM_PER_M = 100D;
	private static int maxSlotForTier = 30;
	
	private static final Logger			LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<Aisle>	mAisleDao;
	private ITypedDao<Bay>		mBayDao;
	private ITypedDao<Tier>		mTierDao;
	private ITypedDao<Slot>		mSlotDao;
	
	private Facility mFacility;
	private Aisle mLastReadAisle;
	private Bay mLastReadBay;
	private Tier mLastReadTier;
	
	private String mControllerLed;
	private Integer mLedsPerTier;
	private Integer mBayLengthCm;
	private Integer mTierFloorCm;
	private boolean mIsOrientationX;
	private Integer mDepthCm;


	@Inject
	public AislesFileCsvImporter(final ITypedDao<Aisle> inAisleDao, 
		final ITypedDao<Bay>		inBayDao,
		final ITypedDao<Tier>		inTierDao,
		final ITypedDao<Slot>		inSlotDao) {
		// facility needed? but not facilityDao
		mAisleDao = inAisleDao;
		mBayDao = inBayDao;
		mTierDao = inTierDao;
		mSlotDao = inSlotDao;
		
		mLastReadAisle = null;
		mLastReadBay = null;
		mLastReadTier = null;
		mBayLengthCm = 0;
		mTierFloorCm = 0;
		mIsOrientationX = true;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importAislesFromCsvStream(InputStreamReader inCsvStreamReader,
		Facility inFacility,
		Timestamp inProcessTime) {
		boolean result = true;
		
		mFacility = inFacility;
		
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<AislesFileCsvBean> strategy = new HeaderColumnNameMappingStrategy<AislesFileCsvBean>();
			strategy.setType(AislesFileCsvBean.class);

			CsvToBean<AislesFileCsvBean> csv = new CsvToBean<AislesFileCsvBean>();
			List<AislesFileCsvBean> aislesFileBeanList = csv.parse(strategy, csvReader);



			if (aislesFileBeanList.size() > 0) {

				LOGGER.debug("Begin location alias map import.");

				// Iterate over the location alias import beans.
				for (AislesFileCsvBean aislesFileBean : aislesFileBeanList) {
					String errorMsg = aislesFileBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						aislesFileCsvBeanImport(aislesFileBean, inProcessTime);
					}
				}

				// archiveCheckLocationAliases(inFacility, inProcessTime);

				LOGGER.debug("End aisles file import.");
			}

			csvReader.close();
		} catch (FileNotFoundException e) {
			result = false;
			LOGGER.error("", e);
		} catch (IOException e) {
			result = false;
			LOGGER.error("", e);
		}
	
		
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTierId
	 * @param inSlotCount
	 */
	private Slot createOneSlot(final Tier inParentTier, Integer inSlotNumber, Slot inPreviousSlot, Double inSlotWidthM) {

		if (inParentTier == null || inSlotNumber < 1 || inSlotNumber > maxSlotForTier) {
			LOGGER.error("unreasonable value to createOneSlot");
			return null;
		}
		// Manufacture the slotID as S1, S2, et.
		String slotId = String.valueOf(inSlotNumber);
		slotId = "S" + slotId;
		
		Slot newSlot = null;
		// Next line is incorrect. If it succeeds in finding something, it will throw.  COD-81
		// It will find second of two S1 slots belonging to different tiers
		Slot slot = Slot.DAO.findByDomainId(inParentTier, slotId);
		if (slot == null) {
			// Slot points relative to parent tier. The Z will be zero. If X orientation, Ys will be zero.
			
			Double anchorX = 0.0;
			Double anchorY = 0.0;
			Double pickFaceEndX = 0.0;
			Double pickFaceEndY = 0.0;
			if (mIsOrientationX) {
				if (inPreviousSlot != null)
					anchorX = inPreviousSlot.getPickFaceEndPosX();
				pickFaceEndX = anchorX + inSlotWidthM;
			}
			else {
				if (inPreviousSlot != null)
					anchorY = inPreviousSlot.getPickFaceEndPosY();
				pickFaceEndY = anchorY + inSlotWidthM;
	
			}
				
			Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorX, anchorY, 0.0);
			Point inPickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, pickFaceEndX, pickFaceEndY, 0.0);
			
			newSlot = new Slot(anchorPoint, inPickFaceEndPoint);
			newSlot.setDomainId(slotId);
			newSlot.setParent(inParentTier);
			inParentTier.addLocation(newSlot);

			try {
				// transaction?
				Slot.DAO.store(newSlot);
				
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
		else {
			LOGGER.error("Slot not made");
			// update existing?
		}
		
		return newSlot;	

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTierId
	 * @param inSlotCount
	 */
	private Tier createOneTier(final String inTierId, Integer inSlotCount) {
		// PickFaceEndPoint is the same as bays, so that is easy. Just need to get the Z value. Anchor point is relative to parent Bay, so 0,0.
		if (mLastReadBay == null){
			LOGGER.error("null last bay when createOneTier called");
			return null;
		}
		if (inSlotCount < 1 || inSlotCount > 30){
			LOGGER.error("unreasonable slot count during tier creation");
			return null;
		}

		// Create the bay if it doesn't already exist. Easy case.
		Double tierFloorM = mTierFloorCm / CM_PER_M;

		Tier newTier = null;
		Tier tier = null;
		// Next line is incorrect. If it succeeds in finding something, it will throw.  COD-81
		// It will find second of two T1 bays belonging to different bays
		tier = Tier.DAO.findByDomainId(mLastReadBay, inTierId);
		if (tier == null) {
			
			Double anchorX = 0.0;
			Double anchorY = 0.0;
			Double pickFaceEndX = mLastReadBay.getPickFaceEndPosX();
			Double pickFaceEndY = mLastReadBay.getPickFaceEndPosY();
			
			Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorX, anchorY, tierFloorM);
			Point inPickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, pickFaceEndX, pickFaceEndY, tierFloorM);
			
			newTier = new Tier(anchorPoint, inPickFaceEndPoint);
			newTier.setDomainId(inTierId);
			newTier.setParent(mLastReadBay);
			mLastReadBay.addLocation(newTier);

			try {
				// transaction?
				Tier.DAO.store(newTier);
				
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
		else {
			LOGGER.error("Tier not made");
			// update existing?
			return null;
		}
		// Now make the slots
		
		Double slotWidthMeters = (mBayLengthCm / CM_PER_M) / inSlotCount;
		Slot lastSlotMadeThisTier = null;
		for (Integer n = 1; n <= inSlotCount; n++) {
			lastSlotMadeThisTier = createOneSlot(newTier, n, lastSlotMadeThisTier, slotWidthMeters);
		}
		
		return newTier;	

	}
	// --------------------------------------------------------------------------
	/**
	 * @param inBayId
	 * @param inLengthCm
	 */
	private Bay createOneBay(final String inBayId, Integer inLengthCm) {
		// Normal horizontal bays have an easy algorithm for anchorPoint and pickFaceEndPoint. Ys are 0 (if mIsOrientationX). First bay starts at 0 and just goes by length.
		if (mLastReadAisle == null) {
			LOGGER.error("null last aisle when createOneBay called");
			return null;
		}
		Double lengthM = inLengthCm / CM_PER_M;
		// Create the bay if it doesn't already exist. Easy case.
		Bay newBay = null;
		// Next line is incorrect. If it succeeds in finding something, it will throw.  COD-81
		// It will find second of two B1 bays belonging to different aisle
		Bay bay = Bay.DAO.findByDomainId(mLastReadAisle, inBayId);
		if (bay == null) {
			
			Double anchorX = 0.0;
			Double anchorY = 0.0;
			Double pickFaceEndX = 0.0;
			Double pickFaceEndY = 0.0;
			
			if (mIsOrientationX) {
				if (mLastReadBay != null)
					anchorX = mLastReadBay.getPickFaceEndPosX();
				pickFaceEndX = anchorX + lengthM;
			}
			else {
				if (mLastReadBay != null)
					anchorY = mLastReadBay.getPickFaceEndPosY();
				pickFaceEndY = anchorY + lengthM;
			}

			Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorX, anchorY, 0.0);
			Point pickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, pickFaceEndX, pickFaceEndY, 0.0);
			newBay = new Bay(mLastReadAisle, inBayId, anchorPoint, pickFaceEndPoint);
			newBay.setParent(mLastReadAisle);
			mLastReadAisle.addLocation(newBay);

			try {
				// transaction?
				Bay.DAO.store(newBay);
				
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
		else {
			LOGGER.error("Bay not made");
			// update existing?
		}
		return newBay;	

	}
	// --------------------------------------------------------------------------
	/**
	 * @param inAisleId
	 * @param inAnchorPoint
	 * @param inPickFaceEndPoint
	 */
	private Aisle createOneAisle(final String inAisleId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		// PickFaceEndPoint might be calculated when the final bay for the aisle is finished. Kind of hard, so for now, just pass in what we got from aisle editor.

		// Create the aisle if it doesn't already exist. Easy case.
		Aisle newAisle = null;
		// Next line is incorrect. If it succeeds in finding something, it will throw.  COD-81
		Aisle aisle = Aisle.DAO.findByDomainId(mFacility, inAisleId);
		if (aisle == null) {
			newAisle = new Aisle(mFacility, inAisleId, inAnchorPoint, inPickFaceEndPoint);
			try {
				// transaction?
				Aisle.DAO.store(newAisle);
				
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
		else {
			LOGGER.error("Aisle not made");
			// update existing?
		}
		return newAisle;	

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inEdiProcessTime
	 */
	private void aislesFileCsvBeanImport(final AislesFileCsvBean inCsvBean,
		final Timestamp inEdiProcessTime) {
		
		LOGGER.info(inCsvBean.toString());
		
		String binType = inCsvBean.getBinType();
		String controllerLed = inCsvBean.getControllerLed();
		String lengthCm = inCsvBean.getLengthCm();
		String anchorX = inCsvBean.getAnchorX();
		String anchorY = inCsvBean.getAnchorY();
		String pickFaceEndX = inCsvBean.getPickFaceEndX();
		String pickFaceEndY = inCsvBean.getPickFaceEndY();
		String nominalDomainID = inCsvBean.getNominalDomainID();
		String slotsInTier = inCsvBean.getSlotsInTier();
		String tierFloorCm = inCsvBean.getTierFloorCm();
		String ledsPerTier = inCsvBean.getLedCountInTier();
		String orientation = inCsvBean.getOrientXorY();
		String depthCMString = inCsvBean.getDepthCm();
		
		// Figure out what kind of bin we have.
		if (binType.equalsIgnoreCase("aisle")) {
			
			Double dAnchorX = 0.0;
			Double dAnchorY = 0.0;
			Double dPickFaceEndX = 0.0;
			Double dPickFaceEndY = 0.0;
			// valueOf throw NumberFormatException or null exception. Catch the throw and continue since we initialized the values to 0.
			try { dAnchorX = Double.valueOf(anchorX); }
			catch (NumberFormatException e) { }
			
			try { dAnchorY = Double.valueOf(anchorY); }
			catch (NumberFormatException e) { }
			
			try { dPickFaceEndX = Double.valueOf(pickFaceEndX); }
			catch (NumberFormatException e) { }
			
			try { dPickFaceEndY = Double.valueOf(pickFaceEndY); }
			catch (NumberFormatException e) { }
			
			Integer depthCm = 0;
			try { depthCm = Integer.valueOf(depthCMString); }
			catch (NumberFormatException e) { }

			
			Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, dAnchorX, dAnchorY, 0.0);
			
			Point pickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, dPickFaceEndX, dPickFaceEndY, 0.0);
			
			Aisle newAisle = createOneAisle(nominalDomainID, anchorPoint, pickFaceEndPoint);
			
			mControllerLed = controllerLed; //tierRight, tierLeft, zigzagRight, zigzagLeft, or "B1>B5;B6<B10;B11>B18;B19<B20"
			

			if (newAisle != null) {
				// We need to save this aisle as it is the master for the next bay line. And save other fields needed for final calculations.
				mIsOrientationX = !(orientation.equalsIgnoreCase("Y")); // make garbage in default to X
				
				mDepthCm = depthCm;
				
				mLastReadAisle = newAisle;
				// null out bay/tier
				mLastReadBay = null;
				mLastReadTier = null;
			}
			else {
				
			}			
		}
		
		else if (binType.equalsIgnoreCase("bay")) {
			// create a bay

			Integer intValueLengthCm = 122; // Giving default length of 4 foot bay. Not that this is common; I want people to notice.

			try { intValueLengthCm = Integer.valueOf(lengthCm);}
			catch (NumberFormatException e) { }
			
			Bay newBay = createOneBay(nominalDomainID, intValueLengthCm);
			
			if (newBay != null) {
				mLastReadBay = newBay;
				mBayLengthCm = intValueLengthCm;
				// null out tier
				mLastReadTier = null;
			}

		}
		else if (binType.equalsIgnoreCase("tier")) {
			// create a tier
			Integer intValueSlotsDesired = 5; // Giving default

			// Pay attention to the tier fields
			try { intValueSlotsDesired = Integer.valueOf(slotsInTier); }
			catch (NumberFormatException e) { }

			try { mLedsPerTier = Integer.valueOf(ledsPerTier); }
			catch (NumberFormatException e) { }
			
			try { mTierFloorCm = Integer.valueOf(tierFloorCm); }
			catch (NumberFormatException e) { }
			


			Tier newTier = createOneTier(nominalDomainID, intValueSlotsDesired);

			if (newTier != null) {
				mLastReadTier = newTier;
			}

		}
		
	}

}
