/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvImporter.java,v 1.30 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.Reader;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.event.EventProducer;
import com.codeshelf.event.EventSeverity;
import com.codeshelf.event.EventTag;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.OrderLocation;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.InputValidationException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class OrderLocationCsvImporter extends CsvImporter<OrderLocationCsvBean> implements ICsvOrderLocationImporter {

	private static final Logger			LOGGER	= LoggerFactory.getLogger(OrderLocationCsvImporter.class);

	@Inject
	public OrderLocationCsvImporter(final EventProducer inProducer) {
		super(inProducer);
	}
/*
	private void reportBusinessEvent(Set<String> inTags, EventSeverity inSeverity, String inMessage) {
		// Replace with EventProducer call
		LOGGER.warn(inMessage);
	}
*/
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.edi.ICsvImporter#importInventoryFromCsvStream(java.io.InputStreamReader, com.codeshelf.model.domain.Facility)
	 */
	public final boolean importOrderLocationsFromCsvStream(Reader inCsvReader, Facility inFacility, Timestamp inProcessTime) {
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();

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
					lastOrderId = orderLocationCsvBeanImport(tenant,lastOrderId, orderLocationBean, inFacility, inProcessTime);
					produceRecordSuccessEvent(orderLocationBean);
				} catch (DaoException | EdiFileReadException | InputValidationException e) {
					produceRecordViolationEvent(EventSeverity.WARN, e, orderLocationBean);
					LOGGER.warn("Unable to process record: " + orderLocationBean, e);
				} catch (Exception e) {
					produceRecordViolationEvent(EventSeverity.ERROR, e, orderLocationBean);
					LOGGER.error("Unable to process record: " + orderLocationBean, e);
				}
			}

			archiveCheckOrderLocations(tenant,inFacility, inProcessTime);

			LOGGER.debug("End order location import.");
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacility
	 * @param inProcessTime
	 */
	private void archiveCheckOrderLocations(Tenant tenant,final Facility inFacility, final Timestamp inProcessTime) {
		LOGGER.debug("Archive unreferenced order location data");

		// Inactivate the locations aliases that don't match the import timestamp.
		try {
			for (OrderHeader order : inFacility.getOrderHeaders()) {
				for (OrderLocation orderLocation : order.getOrderLocations()) {
					if (!orderLocation.getUpdated().equals(inProcessTime)) {
						LOGGER.debug("Archive old orderLocation: " + orderLocation.getDomainId());
						orderLocation.setActive(false);
						OrderLocation.staticGetDao().store(tenant,orderLocation);
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
	private String orderLocationCsvBeanImport(Tenant tenant,final String lastOrderId,
		final OrderLocationCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		LOGGER.info(inCsvBean.toString());
		String errorMsg = inCsvBean.validateBean();
		if (errorMsg != null) {
			throw new InputValidationException(inCsvBean, errorMsg);
		}
		if (!inCsvBean.getOrderId().equals(lastOrderId)) {
			deleteOrderLocations(tenant,inCsvBean.getOrderId(), inFacility, inEdiProcessTime);
		}

		if ((inCsvBean.getLocationId() == null) || inCsvBean.getLocationId().length() == 0) {
			deleteOrderLocations(tenant,inCsvBean.getOrderId(), inFacility, inEdiProcessTime);
		} else if ((inCsvBean.getOrderId() == null) || inCsvBean.getOrderId().length() == 0) {
			deleteLocation(tenant,inCsvBean.getLocationId(), inFacility, inEdiProcessTime);
		} else {
			updateOrderLocation(tenant,inCsvBean, inFacility, inEdiProcessTime);
		}
		return inCsvBean.getOrderId();
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
	private OrderLocation updateOrderLocation(Tenant tenant,final OrderLocationCsvBean inCsvBean,
		final Facility inFacility,
		final Timestamp inEdiProcessTime) {

		OrderLocation result = null;

		// Get or create the item at the specified location.
		String orderId = inCsvBean.getOrderId();
		String locationId = inCsvBean.getLocationId();

		// Only create an OrderLocation mapping if the location is valid.
		Location mappedLocation = inFacility.findSubLocationById(locationId);
		if (mappedLocation == null) {
			// throw new EdiFileReadException("No location found for location: " + locationId);
			produceRecordViolationEvent(inCsvBean, "locationId", locationId, ErrorCode.FIELD_REFERENCE_NOT_FOUND);
			return null;
		} else if (!mappedLocation.isActive()) {
			produceRecordViolationEvent(inCsvBean, "locationId", locationId, ErrorCode.FIELD_REFERENCE_INACTIVE);
			return null;
		}
		// Normal case. Notice that if slotting file came before orders, we createEmptyOrderHeader to backfill later.
		else {
			OrderHeader order = inFacility.getOrderHeader(orderId);
			if (order == null) {
				order = OrderHeader.createEmptyOrderHeader(tenant,inFacility, orderId); // I have not yet figured out why FindBugs flags this -ic
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
				OrderLocation.staticGetDao().store(tenant,result);
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
	private void deleteOrderLocations(Tenant tenant,final String inOrderId, final Facility inFacility, final Timestamp inEdiProcessTime) {

		OrderHeader order = inFacility.getOrderHeader(inOrderId);
		if (order != null) {
			// For every OrderLocation on this order, set it to inactive.
			Iterator<OrderLocation> iter = order.getOrderLocations().iterator();
			while (iter.hasNext()) {
				OrderLocation orderLocation = iter.next();
				order.removeOrderLocation(orderLocation);
				OrderLocation.staticGetDao().delete(tenant,orderLocation);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inOrderId
	 * @param inFacility
	 * @param inEdiProcessTime
	 */
	private void deleteLocation(Tenant tenant,final String inLocationId, final Facility inFacility, final Timestamp inEdiProcessTime) {

		Location location = inFacility.findSubLocationById(inLocationId);

		for (OrderHeader order : inFacility.getOrderHeaders()) {
			// For every OrderLocation on this order, set it to inactive.
			Iterator<OrderLocation> iter = order.getOrderLocations().iterator();
			while (iter.hasNext()) {
				OrderLocation orderLocation = iter.next();
				if (orderLocation.getLocation().equals(location)) {
					order.removeOrderLocation(orderLocation);
					OrderLocation.staticGetDao().delete(tenant,orderLocation);
				}
			}
		}
	}

	@Override
	protected Set<EventTag> getEventTagsForImporter() {
		return EnumSet.of(EventTag.IMPORT, EventTag.ORDER_LOCATION);
	}
}
