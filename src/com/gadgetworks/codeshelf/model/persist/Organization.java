/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Organization.java,v 1.8 2012/04/10 08:01:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

// --------------------------------------------------------------------------
/**
 * CodeShelfNetwork
 * 
 * The CodeShelfNetwork object holds information about how to create a standalone CodeShelf network.
 * (There may be more than one running at a facility.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "ORGANIZATION")
public class Organization extends PersistABC {

	//	public interface IOrganizationDao extends IGenericDao<Organization> {		
	//	}

	private static final Log	LOGGER		= LogFactory.getLog(Organization.class);

	// The facility description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String				description;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parentOrganization")
	@JsonIgnore
	@Getter
	private List<User>			users		= new ArrayList<User>();

	// For a network this is a list of all of the facilities that belong in the set.
	@OneToMany(mappedBy = "parentOrganization")
	@JsonIgnore
	@Getter
	private List<Facility>		facilities	= new ArrayList<Facility>();

	public Organization() {
		description = "";
	}

	// --------------------------------------------------------------------------
	/**
	 * Someday, organizations may have other organizations.
	 * @return
	 */
	public final PersistABC getParent() {
		return null;
	}

	public final void setParent(PersistABC inParent) {

	}
	
	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(Facility inFacility) {
		facilities.add(inFacility);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(Facility inFacility) {
		facilities.remove(inFacility);
	}
}
