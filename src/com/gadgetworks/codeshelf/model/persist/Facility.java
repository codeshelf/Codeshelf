/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Facility.java,v 1.16 2012/04/06 20:45:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

// --------------------------------------------------------------------------
/**
 * Facility
 * 
 * The basic unit that holds all of the locations and equipment for a single facility in an organization.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "LOCATION")
@DiscriminatorValue("FACILITY")
public class Facility extends Location {

	private static final Log		LOGGER				= LogFactory.getLog(Facility.class);

	// The owning organization.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	@Getter
	private Organization			parentOrganization;

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentFacility")
	@JsonIgnore
	@Getter
	private List<Aisle>				aisles				= new ArrayList<Aisle>();

	// For a network this is a list of all of the control groups that belong in the set.
	@OneToMany(mappedBy = "parentFacility")
	@JsonIgnore
	@Getter
	private List<CodeShelfNetwork>	networks			= new ArrayList<CodeShelfNetwork>();

	public Facility() {
		// Facilities have no parent location, but we don't want to allow ANY lcoation to not have a parent.
		// So in this case we make the facility its own parent.  It's also a way to know when we've topped-out in the location tree.
		this.setParentLocation(this);
	}

	public final void setparentOrganization(final Organization inparentOrganization) {
		parentOrganization = inparentOrganization;
	}

	public final String getParentOrganizationID() {
		return getParentOrganization().getDomainId();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addAisle(Aisle inAisle) {
		aisles.add(inAisle);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeAisle(Aisle inAisle) {
		aisles.remove(inAisle);
	}
}
