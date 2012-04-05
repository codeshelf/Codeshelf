/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Location.java,v 1.1 2012/04/05 00:02:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
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

@Entity
@Table(name = "LOCATION")
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

	// The owning organization.
	@Column(nullable = false)
	//@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private PersistABC			parentStructure;

	// All of the vertices that define the location's footprint.
	@OneToMany(mappedBy = "parentLocation")
	@JsonIgnore
	@Getter
	private List<Vertex>		vertices	= new ArrayList<Vertex>();

	public Location() {

	}
	
	public final PersistABC getParent() {
		return getParentStructure();
	}

	public final PersistABC getParentStrucgture() {
		return getParentStructure();
	}

	public final void setparentStructure(final PersistABC inParentStructure) {
		parentStructure = inParentStructure;
	}

	public final void addVertex(Vertex inVertex) {
		vertices.add(inVertex);
	}

	public final void removeVertex(Vertex inVertex) {
		vertices.remove(inVertex);
	}
}
