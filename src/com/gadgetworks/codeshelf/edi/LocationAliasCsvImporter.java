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
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class LocationAliasCsvImporter implements ICsvLocationAliasImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<LocationAlias>	mLocationAliasDao;

	@Inject
	public LocationAliasCsvImporter(final ITypedDao<LocationAlias> inLocationAliasDao) {

		mLocationAliasDao = inLocationAliasDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final void importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility, Timestamp inProcessTime) {
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
					String errorMsg = locationAliasBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						locationAliasCsvBeanImport(locationAliasBean, inFacility, inProcessTime);
					}
				}

				archiveCheckLocationAliases(inFacility, inProcessTime);

				LOGGER.debug("End location alias import.");
			}

			csvReader.close();
		} catch (FileNotFoundException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
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
	 */
	private void locationAliasCsvBeanImport(final LocationAliasCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mLocationAliasDao.beginTransaction();

			LOGGER.info(inCsvBean.toString());

			try {
				LocationAlias locationAlias = updateLocationAlias(inCsvBean, inFacility, inEdiProcessTime);
			} catch (Exception e) {
				LOGGER.error("", e);
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
	 * @return
	 */
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

}
