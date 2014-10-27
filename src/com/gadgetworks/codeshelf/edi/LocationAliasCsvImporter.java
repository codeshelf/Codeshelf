/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import static com.gadgetworks.codeshelf.event.EventProducer.tags;

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

import com.gadgetworks.codeshelf.event.EventSeverity;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.validation.ViolationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class LocationAliasCsvImporter extends CsvImporter implements ICsvLocationAliasImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(LocationAliasCsvImporter.class);

	private final ITypedDao<LocationAlias>	mLocationAliasDao;

	@Inject
	public LocationAliasCsvImporter(final ITypedDao<LocationAlias> inLocationAliasDao) {
		super();
		mLocationAliasDao = inLocationAliasDao;
	}

	private void reportAsResolution(Object inRelatedObject){
		// Replace with EventProducer call
		getEventProducer().reportAsResolution(tags("import", "location alias"), inRelatedObject);
	}
	
	private void reportBusinessEvent(EventSeverity inSeverity, Exception e, Object inRelatedObject){
		// Replace with EventProducer call
		getEventProducer().produceEvent(tags("import", "location alias"), inSeverity, e, inRelatedObject);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader,
		Facility inFacility,
		Timestamp inProcessTime) {
		boolean result = true;
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<LocationAliasCsvBean> strategy = new HeaderColumnNameMappingStrategy<LocationAliasCsvBean>();
			strategy.setType(LocationAliasCsvBean.class);

			CsvToBean<LocationAliasCsvBean> csv = new CsvToBean<LocationAliasCsvBean>();
			List<LocationAliasCsvBean> locationAliasBeanList = csv.parse(strategy, csvReader);

			if (locationAliasBeanList.size() > 0) {

				LOGGER.debug("Begin location alias map import.");

				// Iterate over the location alias import beans.
				for (LocationAliasCsvBean locationAliasBean : locationAliasBeanList) {
					try {
						locationAliasCsvBeanImport(locationAliasBean, inFacility, inProcessTime);
						reportAsResolution(locationAliasBean);
					}
					catch(Exception e) {
						reportBusinessEvent(EventSeverity.WARN, e, locationAliasBean);
					}
				}

				archiveCheckLocationAliases(inFacility, inProcessTime);

				LOGGER.debug("End location alias import.");
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
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckLocationAliases(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced location alias data");

		// Inactivate the locations aliases that don't match the import timestamp.
		try {
			mLocationAliasDao.beginTransaction();
			for (LocationAlias locationAlias : inFacility.getLocationAliases()) {
				if (!locationAlias.getUpdated().equals(inProcessTime)) {
					LOGGER.debug("Archive old locationAlias: " + locationAlias.getAlias());
					locationAlias.setActive(false);
					mLocationAliasDao.store(locationAlias);
				}
			}
			mLocationAliasDao.commitTransaction();
		} finally {
			mLocationAliasDao.endTransaction();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @throws ViolationException 
	 */
	private void locationAliasCsvBeanImport(final LocationAliasCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) throws ViolationException {

		try {
			mLocationAliasDao.beginTransaction();

			LOGGER.info(inCsvBean.toString());
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				throw new ViolationException(inCsvBean, errorMsg);
			}

			// Get or create the item at the specified location.
			String locationAliasId = inCsvBean.getLocationAlias();
			LocationAlias result = inFacility.getLocationAlias(locationAliasId);
			String mappedLocationID = inCsvBean.getMappedLocationId();
			ISubLocation<?> mappedLocation = inFacility.findSubLocationById(mappedLocationID);
			
			// Check for deleted location
			if (mappedLocation == null || mappedLocation instanceof Facility) {
				throw new ViolationException(inCsvBean, "Could not resolve location: " + mappedLocationID);
			}
			if (!mappedLocation.isActive() ){
				throw new ViolationException(inCsvBean, "Location was deleted and is now inactive: " + mappedLocationID);
			}

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
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mLocationAliasDao.store(result);
			}
			mLocationAliasDao.commitTransaction();
		} finally {
			mLocationAliasDao.endTransaction();
		}
	}
}
