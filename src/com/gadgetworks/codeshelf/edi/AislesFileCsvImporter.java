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
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

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
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
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
	private Short mLedsPerTier;
	private Integer mBayLengthCm;
	private Integer mTierFloorCm;
	private boolean mIsOrientationX;
	private Integer mDepthCm;
	
	List<Tier> mTiersThisAisle;



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
		
		mTiersThisAisle = new ArrayList<Tier>();

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

				LOGGER.debug("Begin aisles file import.");

				// Iterate over the location import beans.
				for (AislesFileCsvBean aislesFileBean : aislesFileBeanList) {
					String errorMsg = aislesFileBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						Aisle lastAisle = mLastReadAisle;
						// This creates one location: aisle, bay, tier; (tier also creates slots). 
						aislesFileCsvBeanImport(aislesFileBean, inProcessTime);
						// if we started a new aisle, then the previous aisle is done. Do those computations and set those fields
						if (lastAisle != mLastReadAisle) {
							finalizeTiersInThisAisle(lastAisle);
						}
					}
				}
				finalizeTiersInThisAisle(mLastReadAisle); 

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

	private class TierBayComparable implements Comparator<Tier> {
		// For the tierRight and tierLeft aisle types
		public int compare(Tier inLoc1, Tier inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				return inLoc1.getTierSortName().compareTo(inLoc2.getTierSortName());
			}
		}
	}
	
	private class BayTierComparable implements Comparator<Tier> {
		// For the zigzagRight and zigzagLeft aisle types.
		// Fix later. This is not right as we want B1T2, B1T1, B2T2, B2T1. But getBaySortName gives us B1T1, B1T2, B2T1, B2T2
		public int compare(Tier inLoc1, Tier inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				return inLoc1.getBaySortName().compareTo(inLoc2.getBaySortName());
			}
		}
	}

	private class SlotNameComparable implements Comparator<Slot> {
		// For the zigzagRight and zigzagLeft aisle types.
		// Fix later. This is not right as we want B1T2, B1T1, B2T2, B2T1. But getBaySortName gives us B1T1, B1T2, B2T1, B2T2
		public int compare(Slot inLoc1, Slot inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				return inLoc1.getDomainId().compareTo(inLoc2.getDomainId());
			}
		}
	}

	 // --------------------------------------------------------------------------
	/**
	 * @param inTier
	 * @param inLastLedNumber
	 * @param slotLedsIncrease
	 */
	private void setSlotLeds(Tier inTier, short inLedCountThisTier, boolean slotLedsIncrease) {
		// First get our list of slot. Fighting through the cast.
		List<Slot> slotList = new ArrayList<Slot>();	
		List<ISubLocation> locationList = inTier.getChildren();	
		slotList.addAll((Collection<? extends Slot>) locationList);
		
		// sort the slots in the direction the led count will increase		
		Collections.sort(slotList, new SlotNameComparable());
		if (!slotLedsIncrease)
			Collections.reverse(slotList);
		
		// For this purpose, "leds" is the total span of the slot in led positions, and "lit leds" will give the ones lighted toward the center of the slot.
		short slotCount = (short) (slotList.size());
		short ledsPerSlot = (short)  (inLedCountThisTier / slotCount);
		short remainderLeds  = (short) (inLedCountThisTier % slotCount);
		
		// The extra -1 matters only when inLedCountThisTier is divisible by (slotCount * 3)
		short ledsToLightPerSlot = (short) ((inLedCountThisTier - 1 - (slotCount * 3)) / slotCount);
	
		short lastSlotEndingLed = (short) (inTier.getFirstLedNumAlongPath() - 1);
		short slotIndex = 0;
		
		// You can see the algorithm. Work down the full width of each slot, using up the remainder making slots wider
		// until the remainder runs out. Then light a few lights in the middle of that span.
		
		// We will leave two dark then light some. That leaves one dark at the end, or two if we used the remainder.
		ListIterator li = null;
		li = slotList.listIterator();
		while (li.hasNext()) {
			Slot thisSlot = (Slot) li.next();
			// tierSortName just to follow the iteration in debugger
			String slotName = thisSlot.getDomainId();
			  
			slotIndex += 1;
			short thisSlotStartLed = (short) (lastSlotEndingLed +  1);
			short thisSlotEndLed =  (short) (thisSlotStartLed +  ledsPerSlot - 1);
			if (slotIndex < remainderLeds)
				thisSlotEndLed += 1; // distribute the unevenness among the first few slots
  
			short firstLitLed = (short) (thisSlotStartLed + 2);
			short lastLitLed = (short) (firstLitLed + ledsToLightPerSlot);
		  
			thisSlot.setFirstLedNumAlongPath((short) (firstLitLed));
			thisSlot.setLastLedNumAlongPath((short) (lastLitLed));
			// transaction?
			Slot.DAO.store(thisSlot);	  
		  
			lastSlotEndingLed = thisSlotEndLed;
		}

	}
	 
	 // --------------------------------------------------------------------------
	/**
	 * @param inTier
	 * @param inLastLedNumber
	 */
	private short setTierLeds(Tier inTier, short inLastLedNumber) {
		// would be more obvious with an inOut parameter
		// returns the value of last led position in this tier, so that the next tier knows where to start.
		short returnValue = 0;
		short ledCount = inTier.getMTransientLedsThisTier();
		short thisTierStartLed = (short) (inLastLedNumber + 1);
		short thisTierEndLed = (short) (inLastLedNumber + ledCount);
		// We should still have hold of the tier reference that has the transient field set. If not, ledCount == 0
		
		if (ledCount == 0) {
			return inLastLedNumber;
			// Not sure we will need this case ever. Would be something like setting a null tier that a cable skips to next tier.
		}
		else {
			
			inTier.setFirstLedNumAlongPath(thisTierStartLed);
			inTier.setLastLedNumAlongPath(thisTierEndLed);
			// transaction?
			Tier.DAO.store(inTier);
			returnValue = (short) (inLastLedNumber + ledCount); 
		}
		// Now the tricky bit of setting the slot leds
		boolean directionIncrease = inTier.isMTransientLedsIncrease();
		setSlotLeds(inTier, ledCount, directionIncrease);
		
		return returnValue;
	}
	 
	 // --------------------------------------------------------------------------
	/**
	 * @param inAisle
	 */
	private void finalizeTiersInThisAisle(final Aisle inAisle) {
		// mTiersThisAisle has all the tiers, with their transient fields set for ledCount and ledDirection
		// We need to sort the tiers in order, then set the first and last LED for each tier.
		// And, once that is known, we can set the slot leds also
		
		boolean tierSortNeeded = true;
		if (mControllerLed.equalsIgnoreCase("zigzagLeft") || mControllerLed.equalsIgnoreCase("zigzagRight")) {
			tierSortNeeded = false;
		}
		boolean restartLedOnTierChange = tierSortNeeded;
		
		if (tierSortNeeded)
			Collections.sort(mTiersThisAisle, new TierBayComparable());
		else {
			Collections.sort(mTiersThisAisle, new BayTierComparable());
		}
		
		// The algorithm is simple: start; increment leds as you go. Start over if the tier name changes.
		
		ListIterator li = null;

		boolean forwardIterationNeeded = true;
		if (mControllerLed.equalsIgnoreCase("tierRight") || mControllerLed.equalsIgnoreCase("zigzagRight")) {
			forwardIterationNeeded = false;
		}
		
		short lastLedNumber = 0;
		short newLedNumber = 0;
		String tierSortName = "";
		String lastTierDomainName = "";
		Tier thisTier = null;
		if (forwardIterationNeeded) {
			li = mTiersThisAisle.listIterator();
			while (li.hasNext()) {
			  thisTier = (Tier) li.next();
			  // tierSortName just to follow the iteration in debugger
			  tierSortName = thisTier.getTierSortName();

			  // need to start over? never for zigzag. If tier changed for tier or multi-controller. And extra restarts on the same tier for multi-controller.
			  if (restartLedOnTierChange) {
				  String thisTierDomainName = thisTier.getDomainId();
				  if (! thisTierDomainName.equalsIgnoreCase(lastTierDomainName))
					  lastLedNumber = 0;
				  lastTierDomainName = thisTierDomainName;
			  }
				  
			  newLedNumber = setTierLeds(thisTier, lastLedNumber);
			  lastLedNumber = newLedNumber;
			}
		} else {
			li = mTiersThisAisle.listIterator(mTiersThisAisle.size());
			while (li.hasPrevious()) {
				  thisTier = (Tier) li.previous();
				  // tierSortName just to follow the iteration in debugger
				  tierSortName = thisTier.getTierSortName();

				  // need to start over? never for zigzag. If tier changed for tier or multi-controller. And extra times for multi-controller.
				  if (restartLedOnTierChange) {
					  String thisTierDomainName = thisTier.getDomainId();
					  if (! thisTierDomainName.equalsIgnoreCase(lastTierDomainName))
						  lastLedNumber = 0;
					  lastTierDomainName = thisTierDomainName;
				  }

				  newLedNumber = setTierLeds(thisTier, lastLedNumber);
				  lastLedNumber = newLedNumber;
			}
			
		}
		

		
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @param inParentTier
	 * @param inSlotNumber
	 * @param inPreviousSlot
	 * @param inSlotWidthM
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
	private Tier createOneTier(final String inTierId, Integer inSlotCount, short inLedsThisTier, boolean inLedsIncrease) {
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
		
		// Set our transient fields
		newTier.setMTransientLedsThisTier(inLedsThisTier);
		newTier.setMTransientLedsIncrease(inLedsIncrease);
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
			
			mTiersThisAisle.clear(); // prepare to collect tiers for this aisle
			
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

			try { mLedsPerTier = Short.valueOf(ledsPerTier); }
			catch (NumberFormatException e) { }
			if (mLedsPerTier < 2) {
				mLedsPerTier = 2;
			}
			if (mLedsPerTier > 400) {
				mLedsPerTier = 400;
			}
			
			try { mTierFloorCm = Integer.valueOf(tierFloorCm); }
			catch (NumberFormatException e) { }
			
			if (mTierFloorCm < 0) {
				mLedsPerTier = 0;
			}
			// We know and can set leds count on this tier.
			// Can we know the led increase direction yet? Not necessarily for zigzag bay, but can for the other aisle types
			boolean ledsIncrease = true;
			if (mControllerLed.equalsIgnoreCase("tierRight")) 
				ledsIncrease = false;
			//Knowable, but a bit tricky for the multi-controller aisle case. If this tier is in B3, within B1>B5;, ledsIncrease would be false.

			Tier newTier = createOneTier(nominalDomainID, intValueSlotsDesired, mLedsPerTier, ledsIncrease);

			if (newTier != null) {
				mLastReadTier = newTier;
				// Add this tier to our aisle tier list for later led calculations
				mTiersThisAisle.add(newTier);
				
			}

		}
		
	}

}
