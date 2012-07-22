/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationABC.java,v 1.1 2012/07/22 20:14:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;

// --------------------------------------------------------------------------
/**
 * LocationABC
 * 
 * The anchor point and vertex collection to define the planar space of a work structure (e.g. facility, bay, shelf, etc.)
 * 
 * @author jeffw
 */

@Entity
// LocationABC is the only class we use table-per-class strategy.
// The location hierarchy will get very deep, but each parent has concrete/leaf instances of its children.
// This removes the biggest hassle/hurdle to inheritance-per-class.
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Table(name = "LOCATION")
@DiscriminatorValue("ABC")
public abstract class LocationABC extends DomainObjectABC {

	private static final Log	LOGGER	= LogFactory.getLog(LocationABC.class);

	// The position type (GPS, METERS, etc.).
	@Getter
	@Setter
	@Column(nullable = false)
	private PositionTypeEnum	posType;

	// The X anchor position.
	@Getter
	@Setter
	@Column(nullable = false)
	private Double				posX;

	// The Y anchor position.
	@Getter
	@Setter
	@Column(nullable = false)
	private Double				posY;

	// The Z anchor position.
	@Getter
	@Setter
	@Column(nullable = true)
	// Null means it's at the same nominal z coord as the parent.
	private Double				posZ;

	// The location description.
	@Getter
	@Setter
	@Column(nullable = true)
	private String				description;

	// All of the vertices that define the location's footprint.
	@OneToMany(mappedBy = "parentLocation")
	@JsonIgnore
	@Getter
	private List<Vertex>		vertices	= new ArrayList<Vertex>();

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Setter
	@Getter
	private LocationABC			parentLocation;

	// The child locations.
	@OneToMany(mappedBy = "parentLocation")
	@JsonIgnore
	@Getter
	private List<LocationABC>		locations	= new ArrayList<LocationABC>();
	
	public LocationABC() {
		
	}

	public LocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY) {
		posType = inPosType;
		posX = inPosX;
		posY = inPosY;
		// Z pos is non-null so that it doesn't need to be explicitly set.
		posZ = 0.0;
	}

	public LocationABC(final PositionTypeEnum inPosType, final Double inPosX, final double inPosY, final double inPosZ) {
		posType = inPosType;
		posX = inPosX;
		posY = inPosY;
		posZ = inPosZ;
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return getLocations();
	}
	
	public final void setPosTypeByStr(String inPosTypeStr) {
		setPosType(PositionTypeEnum.valueOf(inPosTypeStr));
	}

	public final void addVertex(Vertex inVertex) {
		vertices.add(inVertex);
	}

	public final void removeVertex(Vertex inVertex) {
		vertices.remove(inVertex);
	}
}
