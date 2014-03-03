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
import java.util.Iterator;
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
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderLocation;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class CsvPutBatchImporter implements ICsvPutBatchImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<OrderLocation>	mOrderLocationDao;

	@Inject
	public CsvPutBatchImporter(final ITypedDao<OrderLocation> inOrderLocationDao) {

		mOrderLocationDao = inOrderLocationDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final void importPutBatchesFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<OrderLocationCsvImportBean> strategy = new HeaderColumnNameMappingStrategy<OrderLocationCsvImportBean>();
			strategy.setType(OrderLocationCsvImportBean.class);

			CsvToBean<OrderLocationCsvImportBean> csv = new CsvToBean<OrderLocationCsvImportBean>();
			List<OrderLocationCsvImportBean> orderLocationImportBeanList = csv.parse(strategy, csvReader);

			if (orderLocationImportBeanList.size() > 0) {

				Timestamp processTime = new Timestamp(System.currentTimeMillis());

				LOGGER.debug("Begin order location import.");

				// Iterate over the inventory import beans.
				for (OrderLocationCsvImportBean importBean : orderLocationImportBeanList) {
					String errorMsg = importBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						putBatchCsvBeanImport(importBean, inFacility, processTime);
					}
				}

				archiveOrderLocations(inFacility, processTime);

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
	private void archiveOrderLocations(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced item data");

		// Inactivate the locations aliases that don't match the import timestamp.
		try {
			mOrderLocationDao.beginTransaction();
			for (OrderHeader order : inFacility.getOrderHeaders()) {
				for (OrderLocation orderLocation : order.getOrderLocations()) {
					if (!orderLocation.getUpdated().equals(inProcessTime)) {
						LOGGER.debug("Archive old orderLocation: " + orderLocation.getDomainId());
						orderLocation.setActive(false);
						mOrderLocationDao.store(orderLocation);
					}
				}
			}
			mOrderLocationDao.commitTransaction();
		} finally {
			mOrderLocationDao.endTransaction();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private void putBatchCsvBeanImport(final OrderLocationCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mOrderLocationDao.beginTransaction();

			LOGGER.info(inCsvImportBean.toString());

			if ((inCsvImportBean.getLocationId() == null) || inCsvImportBean.getLocationId().length() == 0) {
				deleteOrder(inCsvImportBean.getOrderId(), inFacility, inEdiProcessTime);
			} else if ((inCsvImportBean.getOrderId() == null) || inCsvImportBean.getOrderId().length() == 0) {
				deleteLocation(inCsvImportBean.getLocationId(), inFacility, inEdiProcessTime);
			} else {
				OrderLocation orderLocation = updateOrderLocation(inCsvImportBean, inFacility, inEdiProcessTime);
			}

			mOrderLocationDao.commitTransaction();

		} finally {
			mOrderLocationDao.endTransaction();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvImportBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @return
	 */
	private OrderLocation updateOrderLocation(final OrderLocationCsvImportBean inCsvImportBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		OrderLocation result = null;

		// Get or create the item at the specified location.
		String orderId = inCsvImportBean.getOrderId().trim();
		String locationId = inCsvImportBean.getLocationId().trim();

		OrderHeader order = inFacility.findOrder(orderId);
		if (order != null) {
			for (OrderLocation orderLocation : order.getOrderLocations()) {
				if (orderLocation.getLocation().getLocationId().equals(locationId)) {
					result = orderLocation;
				}
			}

			if ((result == null) && (locationId != null)) {
				result = new OrderLocation();
				result.setDomainId(inCsvImportBean.getOrderId() + "-" + inCsvImportBean.locationId);
				result.setParent(order);
				order.addOrderLocation(result);
			}

			// If we were able to get/create an item then update it.
			if (result != null) {
				ILocation mappedLocation = inFacility.findSubLocationById(locationId);
				result.setLocation(mappedLocation);
				try {
					result.setActive(true);
					result.setUpdated(inEdiProcessTime);
					mOrderLocationDao.store(result);
				} catch (DaoException e) {
					LOGGER.error("", e);
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderId
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private void deleteOrder(final String inOrderId, final Facility inFacility, final Timestamp inEdiProcessTime) {

		OrderHeader order = inFacility.findOrder(inOrderId);
		if (order != null) {
			// For every OrderLocation on this order, set it to inactive.
			Iterator<OrderLocation> iter = order.getOrderLocations().iterator();
			while (iter.hasNext()) {
				OrderLocation orderLocation = iter.next();
				orderLocation.setParent(null);
				mOrderLocationDao.delete(orderLocation);
				iter.remove();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderId
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private void deleteLocation(final String inLocationId, final Facility inFacility, final Timestamp inEdiProcessTime) {

		ILocation location = inFacility.findSubLocationById(inLocationId);
		
		for (OrderHeader order : inFacility.getOrderHeaders()) {
			// For every OrderLocation on this order, set it to inactive.
			Iterator<OrderLocation> iter = order.getOrderLocations().iterator();
			while (iter.hasNext()) {
				OrderLocation orderLocation = iter.next();
				if (orderLocation.getLocation().equals(location)) {
					orderLocation.setParent(null);
					mOrderLocationDao.delete(orderLocation);
					iter.remove();
				}
			}
		}
	}
}
