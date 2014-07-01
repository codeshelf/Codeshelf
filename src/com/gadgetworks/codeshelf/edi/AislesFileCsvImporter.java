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
import java.util.Map;
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
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.flyweight.command.NetGuid;

/**
 * @author ranstrom
 *
 */
@Singleton
public class AislesFileCsvImporter implements ICsvAislesFileImporter {

	private static double		CM_PER_M		= 100D;
	private static int			maxSlotForTier	= 30;

	private static final Logger	LOGGER			= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<Aisle>	mAisleDao;
	private ITypedDao<Bay>		mBayDao;
	private ITypedDao<Tier>		mTierDao;
	private ITypedDao<Slot>		mSlotDao;

	private Facility			mFacility;
	// keep track of the file read. This instead of a state machine and other structures
	private Aisle				mLastReadAisle;
	private Bay					mLastReadBay;
	private Bay					mLastReadBayForVertices;
	private Tier				mLastReadTier;
	private int					mBayCountThisAisle;
	private int					mTierCountThisBay;

	// values from the file that will be in the bean
	private String				mControllerLed;
	private Short				mLedsPerTier;
	private Integer				mBayLengthCm;
	private Integer				mTierFloorCm;
	private boolean				mIsOrientationX;
	private Integer				mDepthCm;

	private List<Tier>			mTiersThisAisle;

	@Inject
	public AislesFileCsvImporter(final ITypedDao<Aisle> inAisleDao,
		final ITypedDao<Bay> inBayDao,
		final ITypedDao<Tier> inTierDao,
		final ITypedDao<Slot> inSlotDao) {
		// facility needed? but not facilityDao
		mAisleDao = inAisleDao;
		mBayDao = inBayDao;
		mTierDao = inTierDao;
		mSlotDao = inSlotDao;

		mLastReadAisle = null;
		mLastReadBay = null;
		mLastReadBayForVertices = null;
		mLastReadTier = null;
		mBayCountThisAisle = 0;
		mTierCountThisBay = 0;
		mBayLengthCm = 0;
		mTierFloorCm = 0;
		mIsOrientationX = true;

		mTiersThisAisle = new ArrayList<Tier>();

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importAislesFileFromCsvStream(InputStreamReader inCsvStreamReader,
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

				boolean needAisleBean = true;
				// Iterate over the location import beans.
				for (AislesFileCsvBean aislesFileBean : aislesFileBeanList) {
					String errorMsg = aislesFileBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						Aisle lastAisle = mLastReadAisle;

						// Fairly simple error handling. Throw anywhere in the read with EdiFileReadException. Causes skip to next aisle, if any
						try {
							// This creates one location: aisle, bay, tier; (tier also creates slots). 
							boolean readAisleBean = aislesFileCsvBeanImport(aislesFileBean, inProcessTime, needAisleBean);
							// debug aid
							if (readAisleBean)
								readAisleBean = true;
							// If we needed an aisle, and got one, then we don't need aisle again
							if (needAisleBean && readAisleBean)
								needAisleBean = false;
						} catch (EdiFileReadException e) {
							// Log out what the exception said
							LOGGER.error("", e);

							// Mark that that we must now skip beans until the next aisle starts
							needAisleBean = true;

							// Don't have leftover tiers in the next aisle. Normally cleared in finalize, which will not happen
							mTiersThisAisle.clear();
							mLastReadBayForVertices = null; // barely necessary. But cleanliness is good.
						}
						catch (Exception e) {
							LOGGER.error("unknown exception in file read", e);
						}
					// if we started a new aisle, then the previous aisle is done. Do those computations and set those fields
						// but not if we threw out of last aisle
						if (lastAisle != null && lastAisle != mLastReadAisle & !needAisleBean) {
							finalizeTiersInThisAisle(lastAisle);
							finalizeVerticesThisAisle(lastAisle, mLastReadBayForVertices);
							// starting an aisle copied mLastReadBay to mLastReadBayForVertices and cleared mLastReadBay
						}
					}
				}
				// finish the last aisle read, but not if we threw out of last aisle
				if (!needAisleBean) {
					finalizeTiersInThisAisle(mLastReadAisle);
					finalizeVerticesThisAisle(mLastReadAisle, mLastReadBay);
				}

				// As an aid to the configurer, create a few LED controllers.
				ensureLedControllers();

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
		// For the tierRight and tierLeft aisle types. 

		public int compare(Tier inLoc1, Tier inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				return inLoc1.getAisleTierBayForComparable().compareTo(inLoc2.getAisleTierBayForComparable());
			}
		}
	}

	private class ZigzagLeftComparable implements Comparator<Tier> {
		// We want B1T2, B1T1, B2T2, B2T1. Incrementing Bay. Decrementing Tier. Would not sort right for more than 9 tiers.
		public int compare(Tier inLoc1, Tier inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				int bayValue = inLoc1.getAisleBayForComparable().compareTo(inLoc2.getAisleBayForComparable());
				if (bayValue != 0)
					return bayValue;
				else {
					return (inLoc1.getDomainId().compareTo(inLoc2.getDomainId()) * -1);
				}
			}
		}
	}

	private class ZigzagRightComparable implements Comparator<Tier> {
		// We want B2T2, B2T1, B1T2, B1T1. Decrementing Bay. Decrementing Tier. Would not sort right for more than 9 tiers.
		public int compare(Tier inLoc1, Tier inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				int bayValue = inLoc1.getAisleBayForComparable().compareTo(inLoc2.getAisleBayForComparable());
				if (bayValue != 0)
					return (bayValue * -1);
				else
					return (inLoc1.getDomainId().compareTo(inLoc2.getDomainId()) * -1);
			}
		}
	}

	private class SlotNameComparable implements Comparator<Slot> {
		// Just order slots S1, S2, S3, etc. 
		public int compare(Slot inLoc1, Slot inLoc2) {

			if ((inLoc1 == null) && (inLoc2 == null)) {
				return 0;
			} else if (inLoc2 == null) {
				return -1;
			} else if (inLoc1 == null) {
				return 1;
			} else {
				// We need to sort S1 - S9, S10- S19, etc. Not S1, S10, S11, ... S2
				String slotOneNumerals = inLoc1.getDomainId().substring(1); // Strip off the S
				String slotTwoNumerals = inLoc2.getDomainId().substring(1); // Strip off the S
				Integer slotOneValue = Integer.valueOf(slotOneNumerals);
				Integer slotTwoValue = Integer.valueOf(slotTwoNumerals);
				return slotOneValue.compareTo(slotTwoValue);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTier
	 * @param inLastLedNumber
	 * @param inSlotLedsIncrease
	 * @param inGuardLow
	 * @param inGuardHigh
	 */
	private void setSlotLeds(Tier inTier, short inLedCountThisTier, boolean inSlotLedsIncrease, int inGuardLow, int inGuardHigh) {
		// If light tube extends the full length of the tier (rather than coming up a bit short), recommend inGuardLow = 2 and inGuardHigh = 1.
		// Any remainder slot will essentially inGuardHigh += 1 until on slots until the remainder runs out.

		// First get our list of slot. Fighting through the cast.
		List<Slot> slotList = new ArrayList<Slot>();
		List<? extends ISubLocation> locationList = inTier.getChildren();
		slotList.addAll((Collection<? extends Slot>) locationList);

		// sort the slots in the direction the led count will increase		
		Collections.sort(slotList, new SlotNameComparable());
		if (!inSlotLedsIncrease)
			Collections.reverse(slotList);

		// For this purpose, "leds" is the total span of the slot in led positions, and "lit leds" will give the ones lighted toward the center of the slot.
		short slotCount = (short) (slotList.size());
		if (slotCount == 0)
			return;
		short ledsPerSlot = (short) (inLedCountThisTier / slotCount);
		short remainderLeds = (short) (inLedCountThisTier % slotCount);

		// Guard concept might be wrong in this algorithm. Treats it slot by slot, leaving gap between slots (end of last and start of next).
		// There may also be a need to adjust leds to skip at the start and end of the tier. That is, keep the internal guards, but skip or decrease the ends.
		int guardTotal = inGuardLow + inGuardHigh;
		if (guardTotal == 0)
			guardTotal = 1;
		// The extra -1 matters only when inLedCountThisTier is divisible by (slotCount * (inGuardLow + inGuardHigh))
		short ledsToLightPerSlot = (short) ((inLedCountThisTier - 1 - (slotCount * guardTotal)) / slotCount);

		short lastSlotEndingLed = (short) (inTier.getFirstLedNumAlongPath() - 1);
		short slotIndex = 0;

		// You can see the algorithm. Work down the full width of each slot, using up the remainder making slots wider
		// until the remainder runs out. Then light a few lights in the middle of that span.

		// We will leave two dark then light some. That leaves one dark at the end, or two if we used the remainder.
		ListIterator li = null;
		li = slotList.listIterator();
		while (li.hasNext()) {
			Slot thisSlot = (Slot) li.next();
			// slotName just to follow the iteration in debugger
			// String slotName = thisSlot.getDomainId();

			slotIndex += 1;
			short thisSlotStartLed = (short) (lastSlotEndingLed + 1);
			short thisSlotEndLed = (short) (thisSlotStartLed + ledsPerSlot - 1);
			if (slotIndex < remainderLeds)
				thisSlotEndLed += 1; // distribute the unevenness among the first few slots

			short firstLitLed = (short) (thisSlotStartLed + inGuardLow);
			short lastLitLed = (short) (firstLitLed + ledsToLightPerSlot);
			if (inLedCountThisTier == (short) 0) {
				// Common case-pick: no leds installed, so just set zeros.
				firstLitLed = 0;
				lastLitLed = 0;
				thisSlotEndLed = 0;
			}

			thisSlot.setFirstLedNumAlongPath((short) (firstLitLed));
			thisSlot.setLastLedNumAlongPath((short) (lastLitLed));
			// transaction?
			mSlotDao.store(thisSlot);

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
			inTier.setFirstLedNumAlongPath((short) 0);
			inTier.setLastLedNumAlongPath((short) 0);
			mTierDao.store(inTier);
			returnValue = inLastLedNumber;
			// Odd case: setting a null tier that a cable skips to next tier.
			// Common case-pick: no leds installed, so just set zeros.
		} else {

			inTier.setFirstLedNumAlongPath(thisTierStartLed);
			inTier.setLastLedNumAlongPath(thisTierEndLed);
			// transaction?
			mTierDao.store(inTier);
			returnValue = (short) (inLastLedNumber + ledCount);
		}
		// Now the tricky bit of setting the slot leds
		boolean directionIncrease = inTier.isMTransientLedsIncrease();
		setSlotLeds(inTier, ledCount, directionIncrease, 2, 1); // Guards set at low = 2 and high = 1. Could come from file.

		return returnValue;
	}

	// --------------------------------------------------------------------------
	/**
	 * For a first aisles file read, there will need to be some controllers.
	 * Assume one controller per aisle to start. (Correct for zigzag. Maybe correct for tier aisles if separate channel per tier.)
	 */
	private void ensureLedControllers() {
		// If the LEDs are set, then assuming no multi-channel capability per controller, we simply need to count tiers with LED starting at 1.
		List<Tier> tiersList = mFacility.getChildrenAtLevel(Tier.class);
		int aisleTierCount = 0;
		ListIterator li = null;
		li = tiersList.listIterator();
		while (li.hasNext()) {
			Tier thisTier = (Tier) li.next();
			if (thisTier.getFirstLedNumAlongPath() == 1)
				aisleTierCount++;
		}

		// Count the ledControllers.
		int controllerCount = mFacility.countLedControllers();

		// Add controllers, Jeff wants them set o "0x999999" so they definitely will not work
		if (aisleTierCount > controllerCount) {
			CodeshelfNetwork network = mFacility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_ID);

			int needToMake = aisleTierCount - controllerCount;
			int made = 0;
			for (int n = 1; n <= aisleTierCount; n++) {
				int changingRadioID = 99999999 + 1 - n;
				if (made < needToMake) {
					String theDomainID = Integer.toString(changingRadioID);
					if (network.getLedController(theDomainID) == null) {
						LedController newCtlr = network.findOrCreateLedController(theDomainID, new NetGuid("0x" + theDomainID));
						if (newCtlr != null)
							made++;
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inLocation
	 */
	private Point getNewBoundaryPoint(final SubLocationABC inLocation) {
		// returns a new point with pickfaceEnd offset by depth

		// The boundary point will be the pickFaceEnd adjusted for mDepth
		Double pointX = inLocation.getPickFaceEndPosX();
		Double pointY = inLocation.getPickFaceEndPosY();
		Double depthM = mDepthCm / 100.0;

		if (mIsOrientationX) {
			pointY += depthM;
		} else {
			pointX += depthM;
		}

		Point aPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, pointX, pointY, 0.0);

		return aPoint;

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAisle
	 */
	private void finalizeVerticesThisAisle(final Aisle inAisle, Bay inLastBayThisAisle) {
		// See Facility.createOrUpdateVertices(), which is/was private
		// For this, we might editing existing vertices, or making new.
		if (mFacility == null || inLastBayThisAisle == null)
			return;

		// First we must set the pickface on the aisle from the last bay in the aisle
		Double bayX = inLastBayThisAisle.getPickFaceEndPosX();
		Double bayY = inLastBayThisAisle.getPickFaceEndPosY();

		Double anchorX = inAisle.getAnchorPosX();
		Double anchorY = inAisle.getAnchorPosY();

		Double aisleX = 0.0;
		Double aisleY = 0.0;
		// Probably not correct but so be it.
		if (bayX != 0.0)
			aisleX = anchorX + bayX; // bay points are relative to aisle
		if (bayY != 0.0)
			aisleY = anchorY + bayY;

		Point pickFacePoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, aisleX, aisleY, 0.0);
		inAisle.setPickFaceEndPoint(pickFacePoint);
		// transaction?
		mAisleDao.store(inAisle);

		Point aPoint = getNewBoundaryPoint(inAisle);

		// Create, or later adjust existing vertices, if any

		mFacility.createOrUpdateVertices(inAisle, aPoint);

		// Each bay also has vertices, by the same algorithm.
		// List<? extends ISubLocation> locationList = inAisle.getChildrenAtLevel(Bay.class);
		List<Bay> locationList = inAisle.getChildrenAtLevel(Bay.class);

		ListIterator li = null;
		li = locationList.listIterator();
		while (li.hasNext()) {
			Bay thisBay = (Bay) li.next();
			Point bayPoint = getNewBoundaryPoint(thisBay);
			mFacility.createOrUpdateVertices(thisBay, bayPoint);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAisle
	 */
	private void finalizeTiersInThisAisle(final Aisle inAisle) {
		// mTiersThisAisle has all the tiers, with their transient fields set for ledCount and ledDirection
		// We need to sort the tiers in order, then set the first and last LED for each tier. And direction of tier for zigzag tiers
		// And, once that is known, we can set the slot leds also

		// This function does not do the multi-controller aisles
		// Does not really need inAisle as mTiersThisAisle as what is needed

		boolean tierSortNeeded = true;
		if (mControllerLed.equalsIgnoreCase("zigzagLeft") || mControllerLed.equalsIgnoreCase("zigzagRight")) {
			tierSortNeeded = false;
		}
		boolean restartLedOnTierChange = tierSortNeeded;
		boolean isZigzag = false;
		boolean intialZigTierDirectionIncrease = true;

		if (tierSortNeeded) {
			Collections.sort(mTiersThisAisle, new TierBayComparable());
		} else if (mControllerLed.equalsIgnoreCase("zigzagLeft")) {
			Collections.sort(mTiersThisAisle, new ZigzagLeftComparable());
			isZigzag = true;
		} else if (mControllerLed.equalsIgnoreCase("zigzagRight")) {
			Collections.sort(mTiersThisAisle, new ZigzagRightComparable());
			isZigzag = true;
			intialZigTierDirectionIncrease = false;
		}

		// The algorithm is simple: start; increment leds as you go. 
		// For aisle types, start over if the tier name changes.
		// For zigzag types, start over on tier direction if the bay name changes

		ListIterator li = null;

		boolean forwardIterationNeeded = true;
		if (mControllerLed.equalsIgnoreCase("tierRight")) {
			forwardIterationNeeded = false;
		}

		short lastLedNumber = 0;
		short newLedNumber = 0;
		// String tierSortName = "";
		String lastTierDomainName = "";
		String lastTierBayName = "";
		Tier thisTier = null;
		boolean zigTierDirectionIncrease = intialZigTierDirectionIncrease;

		if (forwardIterationNeeded) {
			li = mTiersThisAisle.listIterator();
			while (li.hasNext()) {
				thisTier = (Tier) li.next();
				// uncomment to follow the iteration in debugger
				// tierSortName = thisTier.getTierSortName();

				// need to start over? never for zigzag. If tier changed for tier or multi-controller. Multi-controller not fully handled yet.
				if (restartLedOnTierChange) {
					String thisTierDomainName = thisTier.getDomainId();
					if (!thisTierDomainName.equalsIgnoreCase(lastTierDomainName))
						lastLedNumber = 0;
					lastTierDomainName = thisTierDomainName;
				}

				// zigzag cases only forward iterate
				if (isZigzag) {
					String thisTierBayName = thisTier.getBayName();
					if (!thisTierBayName.equalsIgnoreCase(lastTierBayName)) {
						zigTierDirectionIncrease = intialZigTierDirectionIncrease;
					}

					lastTierBayName = thisTierBayName;
					thisTier.setMTransientLedsIncrease(zigTierDirectionIncrease); // remember this direction. Used by setTierLeds()
				}

				newLedNumber = setTierLeds(thisTier, lastLedNumber);
				lastLedNumber = newLedNumber;
				zigTierDirectionIncrease = !zigTierDirectionIncrease; // only used by zigzag bays
			}
		} else {
			li = mTiersThisAisle.listIterator(mTiersThisAisle.size());
			while (li.hasPrevious()) {
				thisTier = (Tier) li.previous();
				// uncomment to follow the iteration in debugger
				// tierSortName = thisTier.getTierSortName();

				// need to start over? never for zigzag. If tier changed for tier or multi-controller. Multi-controller not fully handled yet.
				if (restartLedOnTierChange) {
					String thisTierDomainName = thisTier.getDomainId();
					if (!thisTierDomainName.equalsIgnoreCase(lastTierDomainName))
						lastLedNumber = 0;
					lastTierDomainName = thisTierDomainName;
				}

				newLedNumber = setTierLeds(thisTier, lastLedNumber);
				lastLedNumber = newLedNumber;
			}

		}

		mTiersThisAisle.clear(); // prepare to collect tiers for next aisle

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inParentTier
	 * @param inSlotNumber
	 * @param inPreviousSlot
	 * @param inSlotWidthM
	 * ** throws EdiFileReadException 
	 */
	private Slot editOrCreateOneSlot(final Tier inParentTier, Integer inSlotNumber, Slot inPreviousSlot, Double inSlotWidthM) {

		if (inParentTier == null || inSlotNumber < 1 || inSlotNumber > maxSlotForTier) {
			LOGGER.error("unreasonable value to createOneSlot");
			return null; // this should not happen. Checked upstream
		}
		// Manufacture the slotID as S1, S2, etc.
		String slotId = String.valueOf(inSlotNumber);
		slotId = "S" + slotId;

		// Slot points relative to parent tier. The Z will be zero. If X orientation, Ys will be zero.
		Double anchorX = 0.0;
		Double anchorY = 0.0;
		Double pickFaceEndX = 0.0;
		Double pickFaceEndY = 0.0;
		if (mIsOrientationX) {
			if (inPreviousSlot != null)
				anchorX = inPreviousSlot.getPickFaceEndPosX();
			pickFaceEndX = anchorX + inSlotWidthM;
		} else {
			if (inPreviousSlot != null)
				anchorY = inPreviousSlot.getPickFaceEndPosY();
			pickFaceEndY = anchorY + inSlotWidthM;
		}
		Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorX, anchorY, 0.0);
		Point pickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, pickFaceEndX, pickFaceEndY, 0.0);

		Slot slot = mSlotDao.findByDomainId(inParentTier, slotId);
		if (slot == null) {
			slot = new Slot(anchorPoint, pickFaceEndPoint);
			slot.setDomainId(slotId);
			slot.setParent(inParentTier);
			inParentTier.addLocation(slot);
		} else {
			// update existing bay. DomainId is not changing as we found it that way from the same parent.
			// So only a matter of updating the anchor and pickFace points
			slot.setAnchorPoint(anchorPoint);
			slot.setPickFaceEndPoint(pickFaceEndPoint);
		}

		try {
			// transaction?
			mSlotDao.store(slot);
		} catch (DaoException e) {
			LOGGER.error("", e);
			throw new EdiFileReadException("Could not store the slot update.");
		}

		return slot;

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTierId
	 * @param inSlotCount
	 * ** throws EdiFileReadException  on tier before bay, unreasonable slot count, invalid tier name, or after catching DaoException
	 */
	private Tier editOrCreateOneTier(final String inTierId, Integer inSlotCount, short inLedsThisTier, boolean inLedsIncrease) {
		// PickFaceEndPoint is the same as bays, so that is easy. Just need to get the Z value. Anchor point is relative to parent Bay, so 0,0.

		if (mLastReadBay == null) {
			throw new EdiFileReadException("Tier: " + inTierId + " came before it had a bay?");
		}
		if (inSlotCount < 1 || inSlotCount > maxSlotForTier) {
			throw new EdiFileReadException("unreasonable slot count during tier creation");
		}
		// We are enforcing the tier name.
		String tierCorrectName = "T" + String.valueOf(mTierCountThisBay + 1);
		if (!tierCorrectName.equals(inTierId)) {
			throw new EdiFileReadException("Incorrect tier name: " + inTierId + " Should be " + tierCorrectName);
		}

		// Get our points
		Double tierFloorM = mTierFloorCm / CM_PER_M;
		Double anchorX = 0.0;
		Double anchorY = 0.0;
		Double pickFaceEndX = mLastReadBay.getPickFaceEndPosX();
		Double pickFaceEndY = mLastReadBay.getPickFaceEndPosY();

		Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorX, anchorY, tierFloorM);
		Point pickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, pickFaceEndX, pickFaceEndY, tierFloorM);

		// create or update
		Tier tier = mTierDao.findByDomainId(mLastReadBay, inTierId);
		if (tier == null) {
			tier = new Tier(anchorPoint, pickFaceEndPoint);
			tier.setDomainId(inTierId);
			tier.setParent(mLastReadBay);
			mLastReadBay.addLocation(tier);
		} else {
			// update existing bay. DomainId is not changing as we found it that way from the same parent.
			// So only a matter of updating the anchor and pickFace points
			tier.setAnchorPoint(anchorPoint);
			tier.setPickFaceEndPoint(pickFaceEndPoint);
		}

		try {
			// transaction?
			mTierDao.store(tier);
		} catch (DaoException e) {
			LOGGER.error("", e);
			throw new EdiFileReadException("Could not store the tier update.");
		}

		// Set our transient fields
		tier.setMTransientLedsThisTier(inLedsThisTier);
		tier.setMTransientLedsIncrease(inLedsIncrease);

		// Now make or edit the slots		
		Double slotWidthMeters = (mBayLengthCm / CM_PER_M) / inSlotCount;
		Slot lastSlotMadeThisTier = null;
		for (Integer n = 1; n <= inSlotCount; n++) {
			lastSlotMadeThisTier = editOrCreateOneSlot(tier, n, lastSlotMadeThisTier, slotWidthMeters);
		}

		return tier;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inBayId
	 * @param inLengthCm
	 * ** throws EdiFileReadException 
	 */
	private Bay editOrCreateOneBay(final String inBayId, Integer inLengthCm) {
		// Normal horizontal bays have an easy algorithm for anchorPoint and pickFaceEndPoint. Ys are 0 (if mIsOrientationX). First bay starts at 0 and just goes by length.
		if (mLastReadAisle == null) {
			LOGGER.error("null last aisle when createOneBay called");
			return null;
		}

		// We are enforcing the bay name.
		String bayCorrectName = "B" + String.valueOf(mBayCountThisAisle + 1);
		if (!bayCorrectName.equals(inBayId)) {
			throw new EdiFileReadException("Incorrect bay name: " + inBayId + " Should be " + bayCorrectName);
		}

		Double lengthM = inLengthCm / CM_PER_M;

		//figure out the points
		Double anchorX = 0.0;
		Double anchorY = 0.0;
		Double pickFaceEndX = 0.0;
		Double pickFaceEndY = 0.0;

		if (mIsOrientationX) {
			if (mLastReadBay != null)
				anchorX = mLastReadBay.getPickFaceEndPosX();
			pickFaceEndX = anchorX + lengthM;
		} else {
			if (mLastReadBay != null)
				anchorY = mLastReadBay.getPickFaceEndPosY();
			pickFaceEndY = anchorY + lengthM;
		}

		Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, anchorX, anchorY, 0.0);
		Point pickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, pickFaceEndX, pickFaceEndY, 0.0);

		// Create the bay if it doesn't already exist. Easy case.
		Bay bay = mBayDao.findByDomainId(mLastReadAisle, inBayId);
		if (bay == null) {
			bay = new Bay(mLastReadAisle, inBayId, anchorPoint, pickFaceEndPoint);
			bay.setParent(mLastReadAisle);
			mLastReadAisle.addLocation(bay); // This is odd. ebeans needs it?
		} else {
			// update existing bay. DomainId is not changing as we found it that way from the same parent.
			// So only a matter of updating the anchor and pickFace points
			bay.setAnchorPoint(anchorPoint);
			bay.setPickFaceEndPoint(pickFaceEndPoint);
		}
		try {
			// transaction?
			mBayDao.store(bay);
		} catch (DaoException e) {
			LOGGER.error("", e);
			throw new EdiFileReadException("Could not store the bay update.");
		}

		return bay;

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAisleId
	 * @param inAnchorPoint
	 * ** throws EdiFileReadException 
	 */
	private Aisle editOrCreateOneAisle(final String inAisleId, Point inAnchorPoint) {
		// PickFaceEndPoint might be calculated when the final bay for the aisle is finished. Kind of hard, so for now, just pass in what we got from aisle editor.

		// We are enforcing that the aisle name has correct form, but not that the aisle numbers come in order (or even aren't duplicated).
		boolean aisleNameErrorFound = false;
		aisleNameErrorFound = aisleNameErrorFound || inAisleId.length() < 2;
		if (!aisleNameErrorFound)
			aisleNameErrorFound = !inAisleId.startsWith("A");

		if (!aisleNameErrorFound) {
			String s = inAisleId.substring(1); // strip off the A
			Integer aisleNumber = 0;
			try {
				aisleNumber = Integer.valueOf(s);
			} catch (NumberFormatException e) {
				aisleNameErrorFound = true; // not recognizable as a number
			}

			aisleNameErrorFound = aisleNameErrorFound || aisleNumber < 1; // zero or negative not allowed.
		}

		if (aisleNameErrorFound) {
			throw new EdiFileReadException("Incorrect aisle name: " + inAisleId + " Should be similar to A14");
		}

		// Create the aisle if it doesn't already exist.
		Aisle aisle = mAisleDao.findByDomainId(mFacility, inAisleId);
		if (aisle == null) {
			Point pickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0);
			aisle = new Aisle(mFacility, inAisleId, inAnchorPoint, pickFaceEndPoint);
		} else {
			// update existing aisle. DomainId is not changing as we found it that way from the facility parent.
			// So only a matter of updating the anchor point. Don't bother with pickface end as it gets reset later. Now we could only set to zero.
			aisle.setAnchorPoint(inAnchorPoint);
		}

		try {
			// transaction?
			mAisleDao.store(aisle);

		} catch (DaoException e) {
			LOGGER.error("", e);
			throw new EdiFileReadException("Could not store the aisle update.");
		}
		return aisle;

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inEdiProcessTime
	 */
	private boolean aislesFileCsvBeanImport(final AislesFileCsvBean inCsvBean,
		final Timestamp inEdiProcessTime,
		boolean inNeedAisleBean) {

		boolean returnThisIsAisleBean = false;

		LOGGER.info(inCsvBean.toString());

		String binType = inCsvBean.getBinType();
		String controllerLed = inCsvBean.getControllerLed();
		String lengthCm = inCsvBean.getLengthCm();
		String anchorX = inCsvBean.getAnchorX();
		String anchorY = inCsvBean.getAnchorY();
		String nominalDomainID = inCsvBean.getNominalDomainID();
		String slotsInTier = inCsvBean.getSlotsInTier();
		String tierFloorCm = inCsvBean.getTierFloorCm();
		String ledsPerTier = inCsvBean.getLedCountInTier();
		String orientation = inCsvBean.getOrientXorY();
		String depthCMString = inCsvBean.getDepthCm();

		// Figure out what kind of bin we have.
		if (binType.equalsIgnoreCase("aisle")) {
			returnThisIsAisleBean = true;

			Double dAnchorX = 0.0;
			Double dAnchorY = 0.0;
			// valueOf throw NumberFormatException or null exception. Catch the throw and continue since we initialized the values to 0.
			try {
				dAnchorX = Double.valueOf(anchorX);
			} catch (NumberFormatException e) {
			}

			try {
				dAnchorY = Double.valueOf(anchorY);
			} catch (NumberFormatException e) {
			}

			Integer depthCm = 0;
			try {
				depthCm = Integer.valueOf(depthCMString);
			} catch (NumberFormatException e) {
			}

			mControllerLed = controllerLed; //tierRight, tierLeft, zigzagRight, zigzagLeft, or "B1>B5;B6<B10;B11>B18;B19<B20"
			mIsOrientationX = !(orientation.equalsIgnoreCase("Y")); // make garbage in default to X			
			mDepthCm = depthCm;

			Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, dAnchorX, dAnchorY, 0.0);

			// Create the aisle with no pickface; It gets computed later when the bays are known.
			Aisle newAisle = editOrCreateOneAisle(nominalDomainID, anchorPoint);

			if (newAisle != null) {
				// We need to save this aisle as it is the master for the next bay line. 

				mLastReadAisle = newAisle;
				mLastReadBayForVertices = mLastReadBay; // remember the last bay of the previous aisle
				// null out bay/tier
				mLastReadBay = null;
				mLastReadTier = null;
				mBayCountThisAisle = 0;
				mTierCountThisBay = 0;
			}
		}

		else if (binType.equalsIgnoreCase("bay")) {
			// create a bay
			if (inNeedAisleBean) // skip this bean if we are waiting for next aisle
				return false;

			Integer intValueLengthCm = 122; // Giving default length of 4 foot bay. Not that this is common; I want people to notice.

			try {
				intValueLengthCm = Integer.valueOf(lengthCm);
			} catch (NumberFormatException e) {
			}

			Bay newBay = editOrCreateOneBay(nominalDomainID, intValueLengthCm);

			if (newBay != null) {
				mLastReadBay = newBay;
				mBayLengthCm = intValueLengthCm;
				mBayCountThisAisle++;

				// null out tier
				mLastReadTier = null;
				mTierCountThisBay = 0;
			} else {
				throw new EdiFileReadException("Bay not created. Unknown error");
			}

		} else if (binType.equalsIgnoreCase("tier")) {
			if (inNeedAisleBean) // skip this bean if we are waiting for next aisle
				return false;

			// create a tier
			Integer intValueSlotsDesired = 5; // Giving default

			// Pay attention to the tier fields
			try {
				intValueSlotsDesired = Integer.valueOf(slotsInTier);
			} catch (NumberFormatException e) {
			}

			try {
				mLedsPerTier = Short.valueOf(ledsPerTier);
			} catch (NumberFormatException e) {
				mLedsPerTier = 0; // 0 is valid. Means no LEDs in this tier. Should report out on the parse error
				LOGGER.info("Leds per tier not resolved", e);
			}
			if (mLedsPerTier < 0) { // zero is valid. No leds installed for this tier.
				mLedsPerTier = 0;
			}
			if (mLedsPerTier > 400) {
				mLedsPerTier = 400;
			}

			try {
				mTierFloorCm = Integer.valueOf(tierFloorCm);
			} catch (NumberFormatException e) {
			}
			
			// just a stop sign for zero leds per tier.
			if (mLedsPerTier == 0) {
				LOGGER.info(" zero leds this tier");
			}

			// We know and can set led count on this tier.
			// Can we know the led increase direction yet? Not necessarily for zigzag bay, but can for the other aisle types
			boolean ledsIncrease = true;
			if (mControllerLed.equalsIgnoreCase("tierRight"))
				ledsIncrease = false;
			// Knowable, but a bit tricky for the multi-controller aisle case. If this tier is in B3, within B1>B5;, ledsIncrease would be false.

			Tier newTier = editOrCreateOneTier(nominalDomainID, intValueSlotsDesired, mLedsPerTier, ledsIncrease);

			if (newTier != null) {
				mLastReadTier = newTier;
				// Add this tier to our aisle tier list for later led calculations
				mTiersThisAisle.add(newTier);
				mTierCountThisBay++;
			} else {
				throw new EdiFileReadException("Tier not created. Unknown error");
			}

		} else {
			throw new EdiFileReadException("Unknown bin type");
		}

		return returnThisIsAisleBean;
	}

}
