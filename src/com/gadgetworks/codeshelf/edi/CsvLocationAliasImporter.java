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
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class CsvLocationAliasImporter implements ICsvLocationAliasImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<LocationAlias>	mLocationAliasDao;

	@Inject
	public CsvLocationAliasImporter(final ITypedDao<LocationAlias> inLocationAliasDao) {

		mLocationAliasDao = inLocationAliasDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final void importLocationAliasesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<LocationAliasCsvImportBean> strategy = new HeaderColumnNameMappingStrategy<LocationAliasCsvImportBean>();
			strategy.setType(LocationAliasCsvImportBean.class);

			CsvToBean<LocationAliasCsvImportBean> csv = new CsvToBean<LocationAliasCsvImportBean>();
			List<LocationAliasCsvImportBean> locationAliasImportBeanList = csv.parse(strategy, csvReader);

			if (locationAliasImportBeanList.size() > 0) {

				Timestamp processTime = new Timestamp(System.currentTimeMillis());

				LOGGER.debug("Begin location alias map import.");

				// Iterate over the inventory import beans.
				for (LocationAliasCsvImportBean importBean : locationAliasImportBeanList) {
					String errorMsg = importBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						locationAliasCsvBeanImport(importBean, inFacility, processTime);
					}
				}

				archiveLocationAliases(inFacility, processTime);

				LOGGER.debug("End slotted inventory import.");
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
	private void archiveLocationAliases(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced item data");

		// Inactivate the locations aliases that don't match the import timestamp.
		try {
			mLocationAliasDao.beginTransaction();
			for (LocationAlias locationAlias : inFacility.getLocationAliases()) {
				if (!locationAlias.getUpdated().equals(inProcessTime)) {
					LOGGER.debug("Archive old locationAlias: " + locationAlias.getLocationAlias());
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
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private void locationAliasCsvBeanImport(final LocationAliasCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mLocationAliasDao.beginTransaction();

			LOGGER.info(inCsvImportBean.toString());

			LocationAlias locationAlias = updateLocationAlias(inCsvImportBean, inFacility, inEdiProcessTime);

			mLocationAliasDao.commitTransaction();

		} finally {
			mLocationAliasDao.endTransaction();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @return
	 */
	private LocationAlias updateLocationAlias(final LocationAliasCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		LocationAlias result = null;

		// Get or create the item at the specified location.
		String locationAliasId = inCsvImportBean.getLocationAlias().trim();
		result = inFacility.getLocationAlias(locationAliasId);
		ILocation mappedLocation = inFacility.findSubLocationById(inCsvImportBean.getMappedLocationId());

		if ((result == null) && (inCsvImportBean.getMappedLocationId() != null) && (mappedLocation != null)) {
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
