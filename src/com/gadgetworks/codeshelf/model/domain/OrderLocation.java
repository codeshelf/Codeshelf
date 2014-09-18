/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.services.PersistencyService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

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
@Table(name = "order_location")
//@CacheStrategy(useBeanCache=false)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(of = { "location", "parent", "active" }, callSuper = true, doNotUseGetters = true)
public class OrderLocation extends DomainObjectTreeABC<OrderHeader> {

	@Inject
	public static ITypedDao<OrderLocation>	DAO;

	@Singleton
	public static class OrderLocationDao extends GenericDaoABC<OrderLocation> implements ITypedDao<OrderLocation> {
		@Inject
		public OrderLocationDao(final PersistencyService persistencyService) {
			super(persistencyService);
		}

		public final Class<OrderLocation> getDaoClass() {
			return OrderLocation.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrderLocation.class);

	@SuppressWarnings("rawtypes")
	@ManyToOne(optional = false)
	@JsonProperty
	private LocationABC			location;

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

	// The owning facility.
	@ManyToOne(optional = false)
	private OrderHeader			parent;

	// --------------------------------------------------------------------------
	/**
	 * This creates a standard domainId that keeps all of the items in different locations unique among a single ItemMaster.
	 * @param inItemId
	 * @param inLocationId
	 * @return
	 */
	public static String makeDomainId(final OrderHeader inOrder, final ILocation<?> inLocation) {
		return inOrder.getOrderId() + "-" + inLocation.getNominalLocationId();
	}

	public OrderLocation() {

	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<OrderLocation> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "MAP";
	}

	public final OrderHeader getParent() {
		return parent;
	}

	public final void setParent(OrderHeader inParent) {
		parent = inParent;
	}

	public final ISubLocation<?> getLocation() {
		return (ISubLocation<?>) location;
	}

	public final void setLocation(final ILocation<?> inLocation) {
		location = (LocationABC<?>) inLocation;
	}

}
