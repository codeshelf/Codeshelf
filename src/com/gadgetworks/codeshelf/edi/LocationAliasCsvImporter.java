/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.event.EventSeverity;
import com.gadgetworks.codeshelf.event.EventTag;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class LocationAliasCsvImporter extends CsvImporter<LocationAliasCsvBean> implements ICsvLocationAliasImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(LocationAliasCsvImporter.class);

	private final ITypedDao<LocationAlias>	mLocationAliasDao;

	@Inject
	public LocationAliasCsvImporter(final EventProducer inProducer, final ITypedDao<LocationAlias> inLocationAliasDao) {
		super(inProducer);
		mLocationAliasDao = inLocationAliasDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importLocationAliasesFromCsvStream(Reader inCsvReader, Facility inFacility, Timestamp inProcessTime) {

		boolean result = true;
		List<LocationAliasCsvBean> locationAliasBeanList = toCsvBean(inCsvReader, LocationAliasCsvBean.class);

		if (locationAliasBeanList.size() > 0) {

			LOGGER.debug("Begin location alias map import.");

			// Iterate over the location alias import beans.
			for (LocationAliasCsvBean locationAliasBean : locationAliasBeanList) {
				try {
					LocationAlias locationAlias = locationAliasCsvBeanImport(locationAliasBean, inFacility, inProcessTime);
					if (locationAlias != null) {
						produceRecordSuccessEvent(locationAliasBean);
					}
				}
				catch(InputValidationException e) {
					produceRecordViolationEvent(EventSeverity.WARN, e, locationAliasBean);
					LOGGER.warn("Unable to process record: " + locationAliasBean, e);
				}
				catch(Exception e) {
					produceRecordViolationEvent(EventSeverity.ERROR, e, locationAliasBean);
					LOGGER.error("Unable to process record: " + locationAliasBean, e);
				}
			}

			archiveCheckLocationAliases(inFacility, inProcessTime);

			LOGGER.debug("End location alias import.");
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckLocationAliases(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced location alias data");

		// Inactivate the locations aliases that don't match the import timestamp.
		try {
			//mLocationAliasDao.beginTransaction();
			for (LocationAlias locationAlias : inFacility.getLocationAliases()) {
				if (!locationAlias.getUpdated().equals(inProcessTime)) {
					LOGGER.debug("Archive old locationAlias: " + locationAlias.getAlias());
					locationAlias.setActive(false);
					mLocationAliasDao.store(locationAlias);
				}
			}
			//mLocationAliasDao.commitTransaction();
		} finally {
			//mLocationAliasDao.endTransaction();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @throws ViolationException 
	 */
	private LocationAlias locationAliasCsvBeanImport(final LocationAliasCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) throws InputValidationException, DaoException {

		try {
			//mLocationAliasDao.beginTransaction();

			LOGGER.info(inCsvBean.toString());
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				produceRecordViolationEvent(inCsvBean, errorMsg);
				return null;
			}

			// Get or create the item at the specified location.
			String locationAliasId = inCsvBean.getLocationAlias();
			LocationAlias result = inFacility.getLocationAlias(locationAliasId);
			String mappedLocationId = inCsvBean.getMappedLocationId();
			ISubLocation<?> mappedLocation = inFacility.findSubLocationById(mappedLocationId);
			
			// Check for deleted location
			if (mappedLocation == null || mappedLocation instanceof Facility) {
				produceRecordViolationEvent(inCsvBean, "mappedLocationId", mappedLocationId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
				return null;
			}
			if (!mappedLocation.isActive() ){
				produceRecordViolationEvent(inCsvBean, "mappedLocationId", mappedLocationId, ErrorCode.FIELD_REFERENCE_INACTIVE);
				return null;
			}

			if ((result == null) && (inCsvBean.getMappedLocationId() != null) && (mappedLocation != null)) {
				result = new LocationAlias();
				result.setDomainId(locationAliasId);
				inFacility.addLocationAlias(result);
			}

			// If we were able to get/create an item then update it.
			if (result != null) {
				result.setLocationAlias(locationAliasId);
				result.setMappedLocation(mappedLocation);
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mLocationAliasDao.store(result);
			}
			//mLocationAliasDao.commitTransaction();
			return result;
		} finally {
			//mLocationAliasDao.endTransaction();
		}
	}
	
	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS);
	}

}
