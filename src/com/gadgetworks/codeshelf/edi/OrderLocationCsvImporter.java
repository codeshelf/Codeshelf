/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
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
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.OrderLocation;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class OrderLocationCsvImporter extends CsvImporter<OrderLocationCsvBean> implements ICsvOrderLocationImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(OrderLocationCsvImporter.class);

	private ITypedDao<OrderLocation>	mOrderLocationDao;

	@Inject
	public OrderLocationCsvImporter(final EventProducer inProducer, final ITypedDao<OrderLocation> inOrderLocationDao) {
		super(inProducer);

		mOrderLocationDao = inOrderLocationDao;
	}
	
	private void reportBusinessEvent(Set<String> inTags, EventSeverity inSeverity, String inMessage){
		// Replace with EventProducer call
		LOGGER.warn(inMessage);
	}


	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.gadgetworks.codeshelf.model.domain.Facility)
	 */
	public final boolean importOrderLocationsFromCsvStream(Reader inCsvReader,
		Facility inFacility,
		Timestamp inProcessTime) {
		boolean result = true;
		List<OrderLocationCsvBean> orderLocationBeanList = toCsvBean(inCsvReader, OrderLocationCsvBean.class);
		//Sort to put orders with same id together
		Collections.sort(orderLocationBeanList);

		if (orderLocationBeanList.size() > 0) {

			LOGGER.debug("Begin order location import.");

			String lastOrderId = null; //when changes, locations should be cleared
			// Iterate over the order location map import beans.
			for (OrderLocationCsvBean orderLocationBean : orderLocationBeanList) {
				try {	
					lastOrderId = orderLocationCsvBeanImport(lastOrderId, orderLocationBean, inFacility, inProcessTime);
					produceRecordSuccessEvent(orderLocationBean);
				} catch(DaoException | EdiFileReadException | InputValidationException e) {
					produceRecordViolationEvent(EventSeverity.WARN, e, orderLocationBean);
					LOGGER.warn("Unable to process record: " + orderLocationBean, e);
				} catch (Exception e) {
					produceRecordViolationEvent(EventSeverity.ERROR, e, orderLocationBean);
					LOGGER.error("Unable to process record: " + orderLocationBean, e);
				} 
			}

			archiveCheckOrderLocations(inFacility, inProcessTime);

			LOGGER.debug("End order location import.");
		}
		return result;
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
			for (OrderHeader order : inFacility.getOrderHeaders()) {
				for (OrderLocation orderLocation : order.getOrderLocations()) {
					if (!orderLocation.getUpdated().equals(inProcessTime)) {
						LOGGER.debug("Archive old orderLocation: " + orderLocation.getDomainId());
						orderLocation.setActive(false);
						mOrderLocationDao.store(orderLocation);
					}
				}
			}
		} finally {
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Caller should wrap in a transaction and handle exceptions
	 * 
	 * @param inCsvBean
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private String orderLocationCsvBeanImport(final String lastOrderId, final OrderLocationCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		LOGGER.info(inCsvBean.toString());
		try {	
			//mOrderLocationDao.beginTransaction();
			String errorMsg = inCsvBean.validateBean();
			if (errorMsg != null) {
				throw new InputValidationException(inCsvBean, errorMsg);
			} 
			if (!inCsvBean.getOrderId().equals(lastOrderId)) {
				deleteOrderLocations(inCsvBean.getOrderId(), inFacility, inEdiProcessTime);
			}
		
			if ((inCsvBean.getLocationId() == null) || inCsvBean.getLocationId().length() == 0) {
				deleteOrderLocations(inCsvBean.getOrderId(), inFacility, inEdiProcessTime);
			} else if ((inCsvBean.getOrderId() == null) || inCsvBean.getOrderId().length() == 0) {
				deleteLocation(inCsvBean.getLocationId(), inFacility, inEdiProcessTime);
			} else {
				updateOrderLocation(inCsvBean, inFacility, inEdiProcessTime);
			}
			//mOrderLocationDao.commitTransaction();
			return inCsvBean.getOrderId();
		}
		finally {
			//mOrderLocationDao.endTransaction();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * 
	 * Caller should wrap in a transaction and handle exceptions
	 * 
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
		LocationABC mappedLocation = inFacility.findSubLocationById(locationId);
		if (mappedLocation == null) {
			// throw new EdiFileReadException("No location found for location: " + locationId);
			produceRecordViolationEvent(inCsvBean, "locationId", locationId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
			return null;
		}
		else if (!mappedLocation.isActive()){
			produceRecordViolationEvent(inCsvBean, "locationId", locationId, ErrorCode.FIELD_REFERENCE_INACTIVE);
			return null;
		}
		// Normal case. Notice that if slotting file came before orders, we createEmptyOrderHeader to backfill later.
		else {
			OrderHeader order = inFacility.getOrderHeader(orderId);
			if (order == null) {
				order = OrderHeader.createEmptyOrderHeader(inFacility, orderId); // I have not yet figured out why FindBugs flags this -ic
			}

			String orderLocationId = OrderLocation.makeDomainId(order, mappedLocation);

			result = order.getOrderLocation(orderLocationId);

			if ((result == null) && (locationId != null)) {
				result = new OrderLocation();
				result.setDomainId(orderLocationId);
				order.addOrderLocation(result);
			}

			// If we were able to get/create an item then update it.
			if (result != null) {
				result.setLocation(mappedLocation);
				result.setActive(true);
				result.setUpdated(inEdiProcessTime);
				mOrderLocationDao.store(result);
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
	private void deleteOrderLocations(final String inOrderId, final Facility inFacility, final Timestamp inEdiProcessTime) {

		OrderHeader order = inFacility.getOrderHeader(inOrderId);
		if (order != null) {
			// For every OrderLocation on this order, set it to inactive.
			Iterator<OrderLocation> iter = order.getOrderLocations().iterator();
			while (iter.hasNext()) {
				OrderLocation orderLocation = iter.next();
				order.removeOrderLocation(orderLocation);
				mOrderLocationDao.delete(orderLocation);
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

		LocationABC location = inFacility.findSubLocationById(inLocationId);

		for (OrderHeader order : inFacility.getOrderHeaders()) {
			// For every OrderLocation on this order, set it to inactive.
			Iterator<OrderLocation> iter = order.getOrderLocations().iterator();
			while (iter.hasNext()) {
				OrderLocation orderLocation = iter.next();
				if (orderLocation.getLocation().equals(location)) {
					order.removeOrderLocation(orderLocation);
					mOrderLocationDao.delete(orderLocation);
				}
			}
		}
	}
	

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_LOCATION);
	}
}
