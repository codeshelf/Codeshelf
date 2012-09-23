/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Vertex.java,v 1.10 2012/09/23 03:05:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

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
public class Vertex extends DomainObjectABC {

	@Inject
	public static ITypedDao<Vertex>	DAO;

	@Singleton
	public static class VertexDao extends GenericDaoABC<Vertex> implements ITypedDao<Vertex> {
		public final Class<Vertex> getDaoClass() {
			return Vertex.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Vertex.class);

	// The position type (GPS, METERS, etc.).
	@Column(nullable = false)
	@Getter
	@Setter
	private PositionTypeEnum		posType;

	// The X position.
	@Column(nullable = false)
	@Getter
	@Setter
	private Double					posX;

	// The Y position.
	@Column(nullable = false)
	@Getter
	@Setter
	private Double					posY;

	// The vertex order/position (zero-based).
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Setter
	@Getter
	private Integer					drawOrder;

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JsonIgnore
	private LocationABC				parent;
	
	public Vertex() {
		
	}

	public Vertex(final LocationABC inParentLocation, final PositionTypeEnum inPosType, final int inDrawOrder, final Double inPosX, final Double inPosY) {
		setParentLocation(inParentLocation);
		setPosType(inPosType);
		setDrawOrder(inDrawOrder);
		setPosX(inPosX);
		setPosY(inPosY);
		setDomainId(computeDefaultDomainId());
	}

	@JsonIgnore
	public final ITypedDao<Vertex> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "V";
	}

	@JsonIgnore
	public final LocationABC getParentLocation() {
		return parent;
	}
	
	public final void setParentLocation(final LocationABC inParentLocation) {
		parent = inParentLocation;
	}

	@JsonIgnore
	public final IDomainObject getParent() {
		return getParentLocation();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof LocationABC) {
			setParentLocation((LocationABC) inParent);
		}
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final void setPosTypeByStr(String inPosTypeStr) {
		setPosType(PositionTypeEnum.valueOf(inPosTypeStr));
	}
}
