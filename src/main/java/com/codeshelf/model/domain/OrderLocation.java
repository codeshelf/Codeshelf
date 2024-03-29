/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

// --------------------------------------------------------------------------
/**
 * OrderLocation
 * 
 * Map an order to a location (an order may have several of these if it span multiple locations).
 * This makes it possible to learn the locations of WonderWall orders.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "order_location", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "location", "active" }, callSuper = true, doNotUseGetters = true)
public class OrderLocation extends DomainObjectTreeABC<OrderHeader> {

	public static class OrderLocationDao extends GenericDaoABC<OrderLocation> implements ITypedDao<OrderLocation> {
		public final Class<OrderLocation> getDaoClass() {
			return OrderLocation.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrderLocation.class);

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JsonProperty
	@Getter
	@Setter
	private Location			location;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			updated;

	// --------------------------------------------------------------------------
	/**
	 * This creates a standard domainId that keeps all of the items in different locations unique among a single ItemMaster.
	 * @param inItemId
	 * @param inLocationId
	 * @return
	 */
	public static String makeDomainId(final OrderHeader inOrder, final Location inLocation) {
		return inOrder.getOrderId() + "-" + inLocation.getNominalLocationId();
	}

	public OrderLocation() {

	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderLocation> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<OrderLocation> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(OrderLocation.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "MAP";
	}

	public final Facility getFacility() {
		return getParent().getFacility();
	}

	// --------------------------------------------------------------------------
	/* 
	 * This is just order ID
	 */
	@JsonIgnore
	public String getParentId() {
		OrderHeader theParent = getParent();
		if (theParent != null)
			return theParent.getDomainId();
		else
			return "";
	}

	// --------------------------------------------------------------------------
	/* 
	 * Give the logable name of the location for this OrderLocation
	 */
	public String getLocationName() {
		Location loc = getLocation();
		return loc.getBestUsableLocationName();
	}

	// --------------------------------------------------------------------------
	/* 
	 * Helper method. This has a bad query. This assumes it is in a tenant transaction.
	 * Somewaht similar to OrderLocationCsvImporter.deleteLocation.
	 */
	public static List<OrderLocation> findOrderLocationsAtLocation(final Location inLocation, final Facility inFacility) {
		Map<String, Object> filterArgs = ImmutableMap.<String, Object> of(
			"facilityId", inFacility.getPersistentId(),
			"locationId", inLocation.getPersistentId());
		List<OrderLocation> orderLocations = OrderLocation.staticGetDao().findByFilter("orderLocationByFacilityAndLocationAll", filterArgs);
		return orderLocations;
	}
	
	public static List<OrderLocation> findOrderLocationsAtLocationAndChildren(Location inLocation, Facility inFacility) {
		List<OrderLocation> orderLocations = Lists.newArrayList();
		findOrderLocationsAtLocationAndChildrenHelper(orderLocations, inLocation, inFacility);
		return orderLocations;
	}
	
	private static void findOrderLocationsAtLocationAndChildrenHelper(List<OrderLocation> accumulator, Location inLocation, Facility inFacility) {
		List<OrderLocation> orderLocations = findOrderLocationsAtLocation(inLocation, inFacility);
		accumulator.addAll(orderLocations);
		List<Location> children = inLocation.getChildren();
		for (Location child : children) {
			findOrderLocationsAtLocationAndChildrenHelper(accumulator, child, inFacility);
		}
	}
}
