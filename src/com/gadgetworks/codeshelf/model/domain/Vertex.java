/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Vertex.java,v 1.25 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Vertex
 * 
 * A bounds point used to define a location's footprint.
 * N.B. A location's vertices lie in the same Z plane as the location's anchor point.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "vertex")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Vertex extends DomainObjectTreeABC<Location> {

	@Inject
	public static ITypedDao<Vertex>	DAO;

	@Singleton
	public static class VertexDao extends GenericDaoABC<Vertex> implements ITypedDao<Vertex> {
		@Inject
		public VertexDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}
		
		public final Class<Vertex> getDaoClass() {
			return Vertex.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Vertex.class);

	// The owning location.
	@ManyToOne(optional = false,fetch=FetchType.LAZY)
	private Location			parent;

	@NonNull
	@Getter
	@Column(nullable=true,name="pos_type")
	private PositionTypeEnum	posType;

	@NonNull
	@Getter
	@Column(nullable=true,name="pos_x")
	private Double				posX;

	@NonNull
	@Getter
	@Column(nullable=true,name="pos_y")
	private Double				posY;

	@Getter
	@Column(nullable=true,name="pos_z")
	private Double				posZ;

	// The vertex order/position (zero-based).
	@Column(nullable = false,name="draw_order")
	@Setter
	@Getter
	@JsonProperty
	private Integer				drawOrder;

	public Vertex() {

	}

	/*
	public Vertex(final LocationABC inParentLocation, final String inLocationId, final int inDrawOrder, final Point inPoint) {
		parent = (LocationABC<?>) inParentLocation;
		setDomainId(inLocationId);
		posTypeEnum = inPoint.getPosTypeEnum();
		posX = inPoint.getX();
		posY = inPoint.getY();
		posZ = inPoint.getZ();
		drawOrder = inDrawOrder;
	}
	*/

	@SuppressWarnings("unchecked")
	public final ITypedDao<Vertex> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "V";
	}

	public final Location getParent() {
		if (this.parent instanceof HibernateProxy) {
			this.parent = Location.deproxify(this.parent);
		}
		return parent;
	}

	public final void setParent(Location inParent) {
		parent = inParent;
	}


	public final void updatePoint(Double x, Double y, Double z) {
		posX = x;
		posY = y;
		posZ = z;
		getDao().store(this);
	}
	
	public final void setPoint(final Point inPoint) {
		posType = inPoint.getPosType();
		posX = inPoint.getX();
		posY = inPoint.getY();
		posZ = inPoint.getZ();
	}

	public static void setDao(ITypedDao<Vertex> inVertexDao) {
		Vertex.DAO = inVertexDao;
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}
}
