/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Organization.java,v 1.26 2013/02/27 01:17:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
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
@Table(name = "ORGANIZATION", schema = "CODESHELF")
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
	@MapKey(name="domainId")
	@Getter
	private Map<String, User>			users		= new HashMap<String, User>();

	// For an organization this is a list of all of the facilities.
	@OneToMany(mappedBy = "parentOrganization", fetch = FetchType.EAGER)
	@MapKey(name = "domainId")
//	@Getter(lazy = false)
	private Map<String, Facility>		facilities		= new HashMap<String, Facility>();

	public Organization() {
		setParent(this);
		description = "";
	}

	public final void addUser(User inUser) {
		users.put(inUser.getDomainId(), inUser);
	}

	public final User getUser(String inUserId) {
		return users.get(inUserId);
	}

	public final void removeUser(String inUserId) {
		users.remove(inUserId);
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
		return new ArrayList<Facility>(facilities.values());
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addFacility(Facility inFacility) {
		facilities.put(inFacility.getDomainId(), inFacility);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeFacility(Facility inFacility) {
		facilities.remove(inFacility);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainId
	 * @param inDescription
	 * @param inPosTypeByStr
	 * @param inPosx
	 * @param inPosY
	 */
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
		
		// Create the default network for the facility.
		CodeshelfNetwork network = facility.createNetwork("DEFAULT");
		
		Che che1 = network.createChe("CHE1");
		Che che2 = network.createChe("CHE2");

		// Create the generic container kind (for all unspecified containers)
		facility.createDefaultContainerKind();
	}
	
	public final Facility getFacility(final String inFacilityDomainId) {
		Facility result = null;
		
		result = facilities.get(inFacilityDomainId);
		
		return result;
	}
	
	public final List<Facility> getFacilities() {
		return new ArrayList<Facility>(facilities.values());
	}
}
