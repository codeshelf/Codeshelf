/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Location.java,v 1.4 2012/04/10 08:01:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

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
 * Location
 * 
 * The anchor point and vertex collection to define the planar space of a work structure (e.g. facility, bay, shelf, etc.)
 * 
 * @author jeffw
 */

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Table(name = "LOCATION")
@DiscriminatorValue("ABC")
public abstract class Location extends PersistABC {

	private static final Log	LOGGER		= LogFactory.getLog(Location.class);

	// The X anchor position.
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
	@Column(nullable = false)
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
	private Location			parentLocation;

	// The child locations.
	@OneToMany(mappedBy = "parentLocation")
	@JsonIgnore
	@Getter
	private List<Location>		locations	= new ArrayList<Location>();

	public Location() {
		// Z pos is non-null so that it doesn't need to be explicitly set.
		posZ = 0.0;
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
