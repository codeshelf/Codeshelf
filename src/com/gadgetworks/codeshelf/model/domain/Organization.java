/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Organization.java,v 1.1 2012/07/19 06:11:32 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

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

import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Organization
 * 
 * The organization is the top-level object that is the parent of all other objects.
 * To handle user-scope/security, all objects in the system must satisfy getParent()/setParent(), but since 
 * organization is the top-most object it's parent is null.
 * 
 * There is a DB constraint that parent cannot be null.  In the case of organization we break that constraint.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "ORGANIZATION")
public class Organization extends DomainObjectABC {

	@Singleton
	public static class OrganizationDao extends GenericDao<Organization> implements ITypedDao<Organization> {
		public OrganizationDao() {
			super(Organization.class);
		}
	}

	@Inject
	public static ITypedDao<Organization> DAO;

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
	public final IDomainObject getParent() {
		return null;
	}

	public final void setParent(IDomainObject inParent) {

	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return getFacilities();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addFacility(Facility inFacility) {
		facilities.add(inFacility);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeFacility(Facility inFacility) {
		facilities.remove(inFacility);
	}
}
