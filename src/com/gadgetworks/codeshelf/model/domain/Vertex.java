/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Vertex.java,v 1.20 2013/03/04 04:47:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "VERTEX", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Vertex extends DomainObjectTreeABC<LocationABC> {

	@Inject
	public static ITypedDao<Vertex>	DAO;

	@Singleton
	public static class VertexDao extends GenericDaoABC<Vertex> implements ITypedDao<Vertex> {
		public final Class<Vertex> getDaoClass() {
			return Vertex.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Vertex.class);

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private LocationABC			parent;

	@NonNull
	@Getter
	@Setter
	private PositionTypeEnum	posTypeEnum;

	@NonNull
	@Getter
	@Setter
	private Double				posX;

	@NonNull
	@Getter
	@Setter
	private Double				posY;

	@Getter
	@Setter
	private Double				posZ;

	// The vertex order/position (zero-based).
	@Column(nullable = false)
	@Setter
	@Getter
	@JsonProperty
	private Integer				drawOrder;

	public Vertex() {

	}

	public Vertex(final LocationABC inParentLocation, final String inLocationId, final int inDrawOrder, final Point inPoint) {
		parent = inParentLocation;
		setDomainId(inLocationId);
		posTypeEnum = inPoint.getPosTypeEnum();
		posX = inPoint.getX();
		posY = inPoint.getY();
		posZ = inPoint.getZ();
		drawOrder = inDrawOrder;
	}

	public final ITypedDao<Vertex> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "V";
	}

	public final LocationABC getParent() {
		return parent;
	}

	public final void setParent(LocationABC inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final void setPoint(final Point inPoint) {
		posTypeEnum = inPoint.getPosTypeEnum();
		posX = inPoint.getX();
		posY = inPoint.getY();
		posZ = inPoint.getZ();
	}
}
