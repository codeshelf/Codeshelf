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
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class LocationAliasCsvImporter extends CsvImporter<LocationAliasCsvBean> implements ICsvLocationAliasImporter {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAliasCsvImporter.class);

	@Inject
	public LocationAliasCsvImporter(final EventProducer inProducer) {
		super(inProducer);
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
	 *  deactivate previous location aliases if they now resolve to no or inactive location
	 */
	private void archiveCheckLocationAliases(final Facility inFacility, final Timestamp inProcessTime) {
		// Finally, in v21, do something intelligent. DEV-475. Used to be if you uploaded a new aisle and aliases only for that aisle, you lost all prior aliases
		// Dangerous before!
		LOGGER.debug("Archive unreferenced location alias data");

		// Inactivate the locations aliases that don't match the import timestamp.
		int archiveCount = 0;
		List<LocationAlias> aliasList = LocationAlias.staticGetDao().findByParent(inFacility);
		for (LocationAlias locationAlias : aliasList) {
			if (!locationAlias.getUpdated().equals(inProcessTime)) {
				Location theLoc = locationAlias.getMappedLocation();
				boolean shouldDelete = theLoc == null || !theLoc.getActive();
				if (!shouldDelete && theLoc != null) {
					// We should do:
					//    if the location we are pointing at has another active alias that matches this timestamp, then delete this in favor of that
					// We are doing
					//    if the location we are pointing at has another active alias, then delete this in favor of that
					//    would need to iterate all the alias to find the best one otherwise
					LocationAlias otherAlias = theLoc.getActiveAliasOtherThan(locationAlias);
					if (otherAlias != null){
						shouldDelete = true;
						LOGGER.info("found other {}.",otherAlias);
					}
				}
				if (shouldDelete) {
					LOGGER.debug("Archive old locationAlias: " + locationAlias.getAlias());
					if (theLoc != null) {
						LOGGER.info("removing {}.",locationAlias);
						theLoc.removeAlias(locationAlias);
					}
					locationAlias.setActive(false);

					try {
						locationAlias.getDao().store(locationAlias);
						archiveCount++;
					} catch (DaoException e) {
						LOGGER.info("archiveCheckLocationAliases", e);
					}
				}
			}
		}
		if (archiveCount > 0) {
			LOGGER.info("Archived {} location aliases that are no longer relevant", archiveCount);
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
		LocationAlias result = LocationAlias.staticGetDao().findByDomainId(inFacility, locationAliasId);
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
			result.setParent(inFacility);
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
			try {
				LocationAlias.staticGetDao().store(result);
			} catch (DaoException e) {
				LOGGER.info("LocationAliases bean import", e);
			}
		}
		return result;
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.LOCATION_ALIAS);
	}

}
