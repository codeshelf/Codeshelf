/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Vertex.java,v 1.16 2012/11/02 03:00:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

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
@Table(name = "VERTEX")
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

	private static final Log	LOGGER	= LogFactory.getLog(Vertex.class);

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private LocationABC			parent;

	// The Y position.
	@Embedded
	@Getter
	@Setter
	@JsonProperty
	private Point				point;

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
		point = inPoint;
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

	public final PositionTypeEnum getPosType() {
		return point.getPosTypeEnum();
	}

	public final void setPosType(final PositionTypeEnum inPosType) {
		point.setPosTypeEnum(inPosType);
	}

	public final Double getPosX() {
		return point.getX();
	}

	public final void setPosX(final Double inPosX) {
		point.setX(inPosX);
	}

	public final Double getPosY() {
		return point.getY();
	}

	public final void setPosY(final Double inPosY) {
		point.setY(inPosY);
	}
}
