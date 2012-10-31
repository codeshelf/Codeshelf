/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Organization.java,v 1.20 2012/10/31 16:55:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@CacheStrategy
@Table(name = "ORGANIZATION")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString
public class Organization extends DomainObjectABC {

	@Inject
	public static ITypedDao<Organization>	DAO;

	@Singleton
	public static class OrganizationDao extends GenericDaoABC<Organization> implements ITypedDao<Organization> {
		public final Class<Organization> getDaoClass() {
			return Organization.class;
		}
	}

	private static final Log	LOGGER		= LogFactory.getLog(Organization.class);

	// The facility description.
	@NonNull
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				description;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<User>			users		= new ArrayList<User>();

	// For an organization this is a list of all of the facilities.
	@OneToMany(mappedBy = "parentOrganization", fetch = FetchType.EAGER)
	@Getter(lazy = false)
	private List<Facility>		facilities	= new ArrayList<Facility>();

	public Organization() {
		setParent(this);
		description = "";
	}

	public final ITypedDao<Organization> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "O";
	}

	public final void setOrganizationId(String inOrganizationId) {
		setDomainId(inOrganizationId);
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

	public final void createFacility(final String inDomainId, final String inDescription, final String inPosTypeByStr, final Double inPosx, final Double inPosY) {

		Facility facility = new Facility();
		facility.setParent(this);
		facility.setDomainId(inDomainId);
		facility.setDescription(inDescription);
		facility.setPosTypeByStr(inPosTypeByStr);
		facility.setPosX(inPosx);
		facility.setPosY(inPosY);
		this.addFacility(facility);

		Facility.DAO.store(facility);

		// Create a first Dropbox Service entry for this facility.
		DropboxService dropboxService = facility.createDropboxService();

		// Create the generic container kind (for all unspecified containers)
		facility.createDefaultContainerKind();
	}
}
