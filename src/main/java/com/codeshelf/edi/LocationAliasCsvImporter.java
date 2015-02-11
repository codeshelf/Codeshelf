/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventSeverity;
import com.codeshelf.event.EventTag;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class LocationAliasCsvImporter extends CsvImporter<LocationAliasCsvBean> implements ICsvLocationAliasImporter {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(LocationAliasCsvImporter.class);

	private final ITypedDao<LocationAlias>	mLocationAliasDao;

	@Inject
	public LocationAliasCsvImporter(final EventProducer inProducer, final ITypedDao<LocationAlias> inLocationAliasDao) {
		super(inProducer);
		mLocationAliasDao = inLocationAliasDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
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
				} catch (InputValidationException e) {
					produceRecordViolationEvent(EventSeverity.WARN, e, locationAliasBean);
					LOGGER.warn("Unable to process record: " + locationAliasBean, e);
				} catch (Exception e) {
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
		for (LocationAlias locationAlias : inFacility.getLocationAliases()) {
			if (!locationAlias.getUpdated().equals(inProcessTime)) {
				LOGGER.debug("Archive old locationAlias: " + locationAlias.getAlias());
				locationAlias.setActive(false);
				mLocationAliasDao.store(locationAlias);
			}
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
		Location mappedLocation = inFacility.findSubLocationById(mappedLocationId);

		// Check for deleted location
		if (mappedLocation == null || mappedLocation.isFacility()) {
			produceRecordViolationEvent(inCsvBean, "mappedLocationId", mappedLocationId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
			return null;
		}
		if (!mappedLocation.isActive()) {
			produceRecordViolationEvent(inCsvBean, "mappedLocationId", mappedLocationId, ErrorCode.FIELD_REFERENCE_INACTIVE);
			return null;
		}

		boolean isNewAlias = false;
		if ((result == null) && (inCsvBean.getMappedLocationId() != null) && (mappedLocation != null)) {
			// create a new alias
			result = new LocationAlias();
			result.setDomainId(locationAliasId);
			inFacility.addLocationAlias(result);
			isNewAlias = true;
		}

		if (result != null) {
			// if brand new alias, just add to location.
			// Found old alias? If to the same location, do not add again. But make active, etc.
			// If to a different location, then we must first remove from the different location, then add
			// Or if old location not resolved, still must add to new location.
			if (isNewAlias) {
				mappedLocation.addAlias(result);
			} else {
				Location oldLocation = result.getMappedLocation();
				if (oldLocation != null && !oldLocation.equals(mappedLocation)) {
					oldLocation.removeAlias(result);
					mappedLocation.addAlias(result);
				} else if (oldLocation == null) {
					mappedLocation.addAlias(result);
				}
			}

			result.setActive(true);
			result.setUpdated(inEdiProcessTime);
			mLocationAliasDao.store(result);
		}
		return result;
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS);
	}

}
