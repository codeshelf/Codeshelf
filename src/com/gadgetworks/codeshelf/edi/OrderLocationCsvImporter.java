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
public class OrderLocationCsvImporter implements ICsvOrderLocationImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(EdiProcessor.class);

	private ITypedDao<OrderLocation>	mOrderLocationDao;

	@Inject
	public OrderLocationCsvImporter(final ITypedDao<OrderLocation> inOrderLocationDao) {

		mOrderLocationDao = inOrderLocationDao;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final void importOrderLocationsFromCsvStream(InputStreamReader inCsvStreamReader, Facility inFacility) {
		try {

			CSVReader csvReader = new CSVReader(inCsvStreamReader);

			HeaderColumnNameMappingStrategy<OrderLocationCsvBean> strategy = new HeaderColumnNameMappingStrategy<OrderLocationCsvBean>();
			strategy.setType(OrderLocationCsvBean.class);

			CsvToBean<OrderLocationCsvBean> csv = new CsvToBean<OrderLocationCsvBean>();
			List<OrderLocationCsvBean> orderLocationBeanList = csv.parse(strategy, csvReader);

			if (orderLocationBeanList.size() > 0) {

				Timestamp processTime = new Timestamp(System.currentTimeMillis());

				LOGGER.debug("Begin order location import.");

				// Iterate over the order location map import beans.
				for (OrderLocationCsvBean orderLocationBean : orderLocationBeanList) {
					String errorMsg = orderLocationBean.validateBean();
					if (errorMsg != null) {
						LOGGER.error("Import errors: " + errorMsg);
					} else {
						orderLocationCsvBeanImport(orderLocationBean, inFacility, processTime);
					}
				}

				archiveCheckOrderLocations(inFacility, processTime);

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
	private void archiveCheckOrderLocations(final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced order location data");

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
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private void orderLocationCsvBeanImport(final OrderLocationCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		try {
			mOrderLocationDao.beginTransaction();

			LOGGER.info(inCsvBean.toString());

			try {
				if ((inCsvBean.getLocationId() == null) || inCsvBean.getLocationId().length() == 0) {
					deleteOrder(inCsvBean.getOrderId(), inFacility, inEdiProcessTime);
				} else if ((inCsvBean.getOrderId() == null) || inCsvBean.getOrderId().length() == 0) {
					deleteLocation(inCsvBean.getLocationId(), inFacility, inEdiProcessTime);
				} else {
					OrderLocation orderLocation = updateOrderLocation(inCsvBean, inFacility, inEdiProcessTime);
				}
			} catch (Exception e) {
				LOGGER.error("", e);
			}

			mOrderLocationDao.commitTransaction();

		} finally {
			mOrderLocationDao.endTransaction();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 * @return
	 */
	private OrderLocation updateOrderLocation(final OrderLocationCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		OrderLocation result = null;

		// Get or create the item at the specified location.
		String orderId = inCsvBean.getOrderId();
		String locationId = inCsvBean.getLocationId();

		// Only create an OrderLocation mapping if the location is valid.
		ILocation mappedLocation = inFacility.findSubLocationById(locationId);
		if (mappedLocation != null) {

			OrderHeader order = inFacility.getOrderHeader(orderId);
			if (order != null) {
				String orderLocationId = OrderLocation.makeDomainId(order, mappedLocation);

				result = order.getOrderLocation(orderLocationId);

				if ((result == null) && (locationId != null)) {
					result = new OrderLocation();
					result.setDomainId(orderLocationId);
					result.setParent(order);
					order.addOrderLocation(result);
				}

				// If we were able to get/create an item then update it.
				if (result != null) {
					result.setLocation(mappedLocation);
					try {
						result.setActive(true);
						result.setUpdated(inEdiProcessTime);
						mOrderLocationDao.store(result);
					} catch (DaoException e) {
						LOGGER.error("", e);
					}
				} else {
					LOGGER.error("OrderLocation incorrectly setup");
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

		OrderHeader order = inFacility.getOrderHeader(inOrderId);
		if (order != null) {
			// For every OrderLocation on this order, set it to inactive.
			Iterator<OrderLocation> iter = order.getOrderLocations().iterator();
			while (iter.hasNext()) {
				OrderLocation orderLocation = iter.next();
				orderLocation.setParent(null);
				mOrderLocationDao.delete(orderLocation);
				order.removeOrderLocation(orderLocation.getDomainId());
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
					order.removeOrderLocation(orderLocation.getDomainId());
				}
			}
		}
	}
}
