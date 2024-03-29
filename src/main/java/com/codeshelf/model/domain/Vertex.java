/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Vertex.java,v 1.25 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@Table(name = "vertex", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Vertex extends DomainObjectTreeABC<Location> {

	public static class VertexDao extends GenericDaoABC<Vertex> implements ITypedDao<Vertex> {
		public final Class<Vertex> getDaoClass() {
			return Vertex.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(Vertex.class);

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
		return staticGetDao();
	}

	public static ITypedDao<Vertex> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Vertex.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "V";
	}

	public void updatePoint(Double x, Double y, Double z) {
		posX = x;
		posY = y;
		posZ = z;
		getDao().store(this);
	}
	
	public void setPoint(final Point inPoint) {
		posType = inPoint.getPosType();
		posX = inPoint.getX();
		posY = inPoint.getY();
		posZ = inPoint.getZ();
	}

	@Override
	public Facility getFacility() {
		return getParent().getFacility();
	}
}
