/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Organization.java,v 1.39 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.flyweight.command.NetGuid;
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
@Table(name = "organization")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Organization extends DomainObjectABC {

	@Inject
	public static ITypedDao<Organization>	DAO;

	@Singleton
	public static class OrganizationDao extends GenericDaoABC<Organization> implements ITypedDao<Organization> {
		@Inject
		public OrganizationDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<Organization> getDaoClass() {
			return Organization.class;
		}
	}

	private static final Logger				LOGGER					= LoggerFactory.getLogger(Organization.class);

	// The facility description.
	@NonNull
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String							description;

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, User>				users					= new HashMap<String, User>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, PersistentProperty>	persistentProperties	= new HashMap<String, PersistentProperty>();

	@OneToMany(mappedBy = "parentOrganization")
	@MapKey(name = "domainId")
	//	@Getter(lazy = false)
	private Map<String, Facility>			facilities				= new HashMap<String, Facility>();

	public Organization() {
		setParent(this);
		description = "";
	}
	
	public final static void setDao(ITypedDao<Organization> dao) {
		Organization.DAO = dao;
	}

	public Organization(String domainId) {
		super(domainId);
		setParent(this);
		description = "";
	}

	
	public final void addUser(User inUser) {
		Organization previousOrganization = inUser.getParent();
		if(previousOrganization == null) {
			users.put(inUser.getDomainId(), inUser);
			inUser.setParent(this);
		} else if(!previousOrganization.equals(this)) {
			LOGGER.error("cannot add User "+inUser.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousOrganization.getDomainId());
		}	
	}

	public final User getUser(String inUserId) {
		return users.get(inUserId);
	}

	public final void removeUser(String inUserId) {
		User user= this.getUser(inUserId);
		if(user != null) {
			user.setParent(null);
			users.remove(inUserId);
		} else {
			LOGGER.error("cannot remove UomMaster "+inUserId+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	public final void addPersistentProperty(PersistentProperty inPersistentProperty) {
		Organization previousOrganization = inPersistentProperty.getParent();
		if(previousOrganization == null) {
			persistentProperties.put(inPersistentProperty.getDomainId(), inPersistentProperty);
			inPersistentProperty.setParent(this);
		} else if(!previousOrganization.equals(this)) {
			LOGGER.error("cannot add PersistentProperty "+inPersistentProperty.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousOrganization.getDomainId());
		}	
	}

	public final PersistentProperty getPersistentProperty(String inPersistentPropertyId) {
		return persistentProperties.get(inPersistentPropertyId);
	}

	public final void removePersistentProperty(String inPersistentPropertyId) {
		PersistentProperty persistentProperty= this.getPersistentProperty(inPersistentPropertyId);
		if(persistentProperty != null) {
			persistentProperty.setParent(null);
			persistentProperties.remove(inPersistentPropertyId);
		} else {
			LOGGER.error("cannot remove PersistentProperty "+inPersistentPropertyId+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	@SuppressWarnings("unchecked")
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

	public final void addFacility(Facility inFacility) {
		Organization previousOrganization = inFacility.getOrganization();
		if(previousOrganization == null) {
			facilities.put(inFacility.getDomainId(), inFacility);
			inFacility.setOrganization(this);
		} else if(!previousOrganization.equals(this)) {
			LOGGER.error("cannot add Facility "+inFacility.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousOrganization.getDomainId());
		}	
	}

	public final void removeFacility(Facility inFacility) {
		Facility facility = this.getFacility(inFacility.getDomainId());
		if(facility != null) {
			facility.setOrganization(null);
			facilities.remove(inFacility.getDomainId());
		} else {
			LOGGER.error("cannot remove Facility "+inFacility.getDomainId()+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inFacilityDomainId
	 * @return
	 */
	public final Facility getFacility(final String inFacilityDomainId) {
		Facility result = null;

		result = facilities.get(inFacilityDomainId);

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final List<Facility> getFacilities() {
		return new ArrayList<Facility>(facilities.values());
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDomainId
	 * @param inDescription
	 * @param inPosTypeByStr
	 * @param inAnchorPosx
	 * @param inAnchorPosY
	 */
	// @Transactional
	public final Facility createFacility(final String inDomainId, final String inDescription, Double x, Double y) {
		Point point = new Point(PositionTypeEnum.GPS,x,y,0d);
		return this.createFacility(inDomainId, inDescription, point);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @param inDomainId
	 * @param inDescription
	 * @param inPosTypeByStr
	 * @param inAnchorPosx
	 * @param inAnchorPosY
	 */
	// @Transactional
	public final Facility createFacility(final String inDomainId, final String inDescription, final Point inAnchorPoint) {

		Facility facility = new Facility();
		facility.setDomainId(inDomainId);
		facility.setDescription(inDescription);
		facility.setAnchorPoint(inAnchorPoint);
		this.addFacility(facility);
		Facility.DAO.store(facility);

		// Create a first Dropbox Service entry for this facility.
		LOGGER.info("Creating dropbox service");
		@SuppressWarnings("unused")
		DropboxService dropboxService = facility.createDropboxService();

		// Create a first IronMQ Service entry for this facility.
		LOGGER.info("Creating IronMQ service");
		try {
		@SuppressWarnings("unused")
		IronMqService ironMqService = facility.createIronMqService();
		}
		catch (PSQLException e) {
			LOGGER.error("failed to create ironMQ service");			
		}

		// Create the default network for the facility.
		CodeshelfNetwork network = facility.createNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);

		// Create the generic container kind (for all unspecified containers)
		facility.createDefaultContainerKind();
		
		// facility.recomputeDdcPositions(); remove this call at v10 hibernate. DDc is not compliant with hibernate patterns.

		// Setup six dummy CHEs
		LOGGER.info("creating 6 CHEs");;
		for (int cheNum = 1; cheNum <= 6; cheNum++) {
			String cheName = "CHE" + cheNum;
			Che che = network.getChe(cheName);
			if (che == null) {
				che = network.createChe(cheName, new NetGuid("0x0000999" + cheNum));
			}
		}
		
		return facility;
	}

	// --------------------------------------------------------------------------
	/**
	 * Create a user for this organization.
	 * @return
	 */
	// @Transactional
	public final User createUser(final String inUsername, final String inPassword, UserType type) {
		User result = null;

		if(User.DAO.findByDomainId(null,inUsername) == null) {
			// Create a user for the organization.
			User user = new User();
			user.setDomainId(inUsername);
			user.setPassword(inPassword);
			user.setType(type);
			user.setActive(true);
			this.addUser(user);

			try {
				User.DAO.store(user);
				result = user;
			} catch (DaoException e) {
				LOGGER.error("error persisting new user "+inUsername,e);
			}
		} else {
			LOGGER.warn("Tried to create user but username already existed - "+inUsername);
		}

		return result;
	}

	@Override
	public Organization getOrganization() {
		return this;
	}

	@Override
	public Facility getFacility() {
		return null;
	}
}
