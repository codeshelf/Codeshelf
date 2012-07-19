/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Vertex.java,v 1.1 2012/07/19 06:11:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDao;
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
public class Vertex extends DomainObjectABC {

	private static final Log	LOGGER	= LogFactory.getLog(Vertex.class);

	@Singleton
	public static class VertexDao extends GenericDao<Vertex> implements ITypedDao<Vertex> {
		public VertexDao() {
			super(Vertex.class);
		}
	}

	@Inject
	public static ITypedDao<Vertex> DAO;

	// The position type (GPS, METERS, etc.).
	@Column(nullable = false)
	@Getter
	@Setter
	private PositionTypeEnum	posType;

	// The X position.
	@Column(nullable = false)
	@Getter
	@Setter
	private Double				posX;

	// The Y position.
	@Column(nullable = false)
	@Getter
	@Setter
	private Double				posY;

	// The vertex order/position (zero-based).
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Setter
	@Getter
	private Integer				drawOrder;

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private Location			parentLocation;

	public Vertex() {
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final IDomainObject getParent() {
		return getParentLocation();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Location) {
			setParentLocation((Location) inParent);
		}
	}
	
	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final void setParentLocation(final Location inParentLocation) {
		parentLocation = inParentLocation;
	}

	public final void setPosTypeByStr(String inPosTypeStr) {
		setPosType(PositionTypeEnum.valueOf(inPosTypeStr));
	}
}
