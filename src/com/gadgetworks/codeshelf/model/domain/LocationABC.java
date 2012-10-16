/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationABC.java,v 1.7 2012/10/16 06:23:21 jeffw Exp $
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
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

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
@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "LOCATION")
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ABC")
@JsonAutoDetect(getterVisibility=Visibility.NONE)
public abstract class LocationABC extends DomainObjectABC implements ILocation {

	private static final Log	LOGGER		= LogFactory.getLog(LocationABC.class);

	// The position type (GPS, METERS, etc.).
	@Column(nullable = false)
	@JsonProperty
	@Getter
	@Setter
	private PositionTypeEnum	posType;

	// The X anchor position.
	@Column(nullable = false)
	@JsonProperty
	@Getter
	@Setter
	private Double				posX;

	// The Y anchor position.
	@Column(nullable = false)
	@JsonProperty
	@Getter
	@Setter
	private Double				posY;

	// The Z anchor position.
	@Column(nullable = true)
	@JsonProperty
	@Getter
	@Setter
	// Null means it's at the same nominal z coord as the parent.
	private Double				posZ;

	// The location description.
	@Column(nullable = true)
	@JsonProperty
	@Getter
	@Setter
	private String				description;

	// The owning location.
	@Column(nullable = false)
	@ManyToOne(optional = true)
	protected LocationABC		parent;

	// All of the vertices that define the location's footprint.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<Vertex>		vertices	= new ArrayList<Vertex>();

	// The child locations.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<LocationABC>	locations	= new ArrayList<LocationABC>();

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
