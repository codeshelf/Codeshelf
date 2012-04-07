/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Location.java,v 1.3 2012/04/07 19:42:16 jeffw Exp $
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

// --------------------------------------------------------------------------
/**
 * Location
 * 
 * The anchor point and vertex collection to define the planar space of a work structure (e.g. facility, bay, shelf, etc.)
 * 
 * @author jeffw
 */

@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Entity
@Table(name = "LOCATION")
@DiscriminatorValue("ABC")
public class Location extends PersistABC {

	private static final Log	LOGGER		= LogFactory.getLog(Location.class);

	// The X anchor position.
	@Getter
	@Setter
	@Column(nullable = false)
	private long				posX;

	// The Y anchor position.
	@Getter
	@Setter
	@Column(nullable = false)
	private long				posY;

	// The Z anchor position.
	@Getter
	@Setter
	@Column(nullable = false)
	private long				posZ;

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

	}

	public final PersistABC getParent() {
		// Every location must have a parent location except we top-out at facility.
		// But we don't want to allow ANY location to have a null parent.  For this reason
		// the facility has itself as its own parent.  We detect that here, so if we parse the location
		// "tree" we can detect a null return value (fpr a facility) and stop.
		PersistABC parent = getParentLocation();
		if (parent == this) {
			return null;
		} else {
			return parent;
		}
	}

	public final void addVertex(Vertex inVertex) {
		vertices.add(inVertex);
	}

	public final void removeVertex(Vertex inVertex) {
		vertices.remove(inVertex);
	}
}
