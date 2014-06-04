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
	private String mTubeLightKind;


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
	 * @param inBayId
	 * @param inAnchorPoint
	 * @param inPickFaceEndPoint
	 */
	private Tier createOneTier(final String inTierId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		// PickFaceEndPoint might be calculated when the final bay for the aisle is finished. Kind of hard, so for now, just pass in what we got from aisle editor.
		if (mLastReadBay == null){
			LOGGER.error("null last bay when createOneTier called");
			return null;
		}
		// Create the bay if it doesn't already exist. Easy case.
		Tier newTier = null;
		Tier tier = Tier.DAO.findByDomainId(mLastReadBay, inTierId);
		if (tier == null) {
			newTier = new Tier(inAnchorPoint, inPickFaceEndPoint);
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
		}
		return newTier;	

	}
	// --------------------------------------------------------------------------
	/**
	 * @param inBayId
	 * @param inAnchorPoint
	 * @param inPickFaceEndPoint
	 */
	private Bay createOneBay(final String inBayId, Point inAnchorPoint, Point inPickFaceEndPoint) {
		// PickFaceEndPoint might be calculated when the final bay for the aisle is finished. Kind of hard, so for now, just pass in what we got from aisle editor.
		if (mLastReadAisle == null) {
			LOGGER.error("null last aisle when createOneBay called");
			return null;
		}
		// Create the bay if it doesn't already exist. Easy case.
		Bay newBay = null;
		Bay bay = Bay.DAO.findByDomainId(mFacility, inBayId);
		if (bay == null) {
			newBay = new Bay(mLastReadAisle, inBayId, inAnchorPoint, inPickFaceEndPoint);
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
		String tubeLightKind = inCsvBean.getTubeLightKind();
		
		String a = "";
		// Figure out what kind of bin we have.
		if (binType.equalsIgnoreCase("aisle")) {
			
			Double dAnchorX = 0.0;
			Double dAnchorY = 0.0;
			Double dPickFaceEndX = 0.0;
			Double dPickFaceEndY = 0.0;
			// parseDouble throw NumberFormatException or null exception. Catch the throw and continue since we initialized the values to 0.
			try { dAnchorX = Double.parseDouble(anchorX); }
			catch(NumberFormatException e) { }
			
			try { dAnchorY = Double.parseDouble(anchorY); }
			catch(NumberFormatException e) { }
			
			try { dPickFaceEndX = Double.parseDouble(pickFaceEndX);}
			catch(NumberFormatException e) { }
			
			try { dPickFaceEndY = Double.parseDouble(pickFaceEndY);}
			catch(NumberFormatException e) { }
			
			Point anchorPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, dAnchorX, dAnchorY, 0.0);
			
			Point pickFaceEndPoint = new Point(PositionTypeEnum.METERS_FROM_PARENT, dPickFaceEndX, dPickFaceEndY, 0.0);
			
			Aisle newAisle = createOneAisle(nominalDomainID, anchorPoint, pickFaceEndPoint);
			
			mControllerLed = controllerLed; //tierRight, tierLeft, zigzagRight, zigzagLeft, or "B1>B5;B6<B10;B11>B18;B19<B20"
			mTubeLightKind = tubeLightKind;  //convert to enum? We basically want to get number of LEDs per tier.

			if (newAisle != null) {
				// We need to save this aisle as it is the master for the next bay line. And save other fields needed for final calculations.
				
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
		}
		else if (binType.equalsIgnoreCase("tier")) {
			// create a tier
		}
		else if (binType.equalsIgnoreCase("slot")) {
			// create a slot
		}
		
		
/*
		try {
			mLocationAliasDao.beginTransaction();


			try {
				LocationAlias locationAlias = updateLocationAlias(inCsvBean, inFacility, inEdiProcessTime);
			} catch (Exception e) {
				LOGGER.error("", e);
			}

			mLocationAliasDao.commitTransaction();

		} finally {
			mLocationAliasDao.endTransaction();
		}
*/
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @return
	 *
	private LocationAlias updateLocationAlias(final LocationAliasCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		LocationAlias result = null;

		// Get or create the item at the specified location.
		String locationAliasId = inCsvBean.getLocationAlias();
		result = inFacility.getLocationAlias(locationAliasId);
		ISubLocation<?> mappedLocation = inFacility.findSubLocationById(inCsvBean.getMappedLocationId());

		if ((result == null) && (inCsvBean.getMappedLocationId() != null) && (mappedLocation != null)) {
			result = new LocationAlias();
			result.setDomainId(locationAliasId);
			result.setParent(inFacility);
			inFacility.addLocationAlias(result);
		}

		// If we were able to get/create an item then update it.
		if (result != null) {
			result.setLocationAlias(locationAliasId);
			result.setMappedLocation(mappedLocation);
			try {
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mLocationAliasDao.store(result);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}

		return result;
	}
	
*/
}
