/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Organization.java,v 1.39 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

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

	public Organization() {
		description = "";
	}
	
	public final static void setDao(ITypedDao<Organization> dao) {
		Organization.DAO = dao;
	}

	public Organization(String domainId) {
		super(domainId);
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
	 * @param inFacilityDomainId
	 * @return
	 */
	public final Facility getFacility(final String inFacilityDomainId) {
		return Facility.DAO.findByDomainId(null, inFacilityDomainId);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final List<Facility> getFacilities() {
		return Facility.DAO.getAll();
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
		CodeshelfNetwork network = facility.createNetwork(this,CodeshelfNetwork.DEFAULT_NETWORK_NAME);

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
	public Facility getFacility() {
		return null;
	}

	public static void CreateDemo() {
		// Create a demo organization
		createOrganizationUser("DEMO1", "a@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "view@example.com", "testme"); //view
		createOrganizationUser("DEMO1", "configure@example.com", "testme"); //all
		createOrganizationUser("DEMO1", "simulate@example.com", "testme"); //simulate + configure
		createOrganizationUser("DEMO1", "che@example.com", "testme"); //view + simulate
		createOrganizationUser("DEMO1", "work@example.com", "testme"); //view + simulate

		createOrganizationUser("DEMO1", "view@goodeggs.com", "goodeggs"); //view
		createOrganizationUser("DEMO1", "view@accu-logistics.com", "accu-logistics"); //view

		// Recompute path positions,
		//   and ensure IronMq configuration
		//   and create a default site controller user if doesn't already exist
		List<Organization> orgs = Organization.DAO.getAll();
		Organization organization = orgs.get(0);
		for (Facility facility : Facility.DAO.getAll()) {
			for (Path path : facility.getPaths()) {
				// TODO: Remove once we have a tool for linking path segments to locations (aisles usually).
				facility.recomputeLocationPathDistances(path);
			}
		}

	}


	// --------------------------------------------------------------------------
	/**
	 * @param inOrganizationId
	 * @param inPassword
	 */
	private static User createOrganizationUser(String inOrganizationId, String inDefaultUserId, String inDefaultUserPw) {
		Organization organization = Organization.DAO.findByDomainId(null, inOrganizationId);
		if (organization == null) {
			organization = new Organization();
			organization.setDomainId(inOrganizationId);
			try {
				Organization.DAO.store(organization);

			} catch (DaoException e) {
				e.printStackTrace();
			}

		}
		User user = organization.getUser(inDefaultUserId);
		if (user == null) {
			user = organization.createUser(inDefaultUserId, inDefaultUserPw, UserType.APPUSER);
		}
		return user;
	}
	/**
	 * all this default site controller / default site controller user stuff is just for dev/test environments
	 * 
	 * @return user
	 */
	public static User createDefaultSiteControllerUser(Organization org,CodeshelfNetwork network) {
		User siteconUser = User.DAO.findByDomainId(null,CodeshelfNetwork.DEFAULT_SITECON_SERIAL);
		if(siteconUser == null) {
			// no default site controller user exists. check for default site controller.
			SiteController sitecon = SiteController.DAO.findByDomainId(null,CodeshelfNetwork.DEFAULT_SITECON_SERIAL);
			if(sitecon == null) {
				siteconUser = createSiteControllerAndUser(network,org,CodeshelfNetwork.DEFAULT_SITECON_SERIAL, "Test Area", false, CodeshelfNetwork.DEFAULT_SITECON_PASS);
			} else {
				LOGGER.error("Default site controller user doesn't exist, but default site controller does exist");
			}
		} // if default user already exists in database, we assume site controller does too; ignore and continue
		return siteconUser;
	}
	
	public static User createSiteControllerAndUser(CodeshelfNetwork network,Organization org, String inDomainId, String inDescribeLocation, Boolean inMonitor, String inPassword) {
		User siteconUser = User.DAO.findByDomainId(null,inDomainId);
		if(siteconUser == null) {
			// no default site controller user exists. check for default site controller.
			SiteController sitecon = SiteController.DAO.findByDomainId(null,inDomainId);
			if(sitecon == null) {
				// ok to create site controller + user
				sitecon = new SiteController();
				sitecon.setDomainId(inDomainId);
				sitecon.setDescription("Site Controller for " + network.getDomainId());
				sitecon.setDescribeLocation(inDescribeLocation);
				sitecon.setMonitor(inMonitor);
				network.addSiteController(sitecon);
				
				try {
					SiteController.DAO.store(sitecon); 
				} catch (DaoException e) { 
					LOGGER.error("Couldn't store new Site Controller "+CodeshelfNetwork.DEFAULT_SITECON_SERIAL, e);
					sitecon=null;
				}
				
				if(sitecon!=null && org!=null) {
					siteconUser = org.createUser(inDomainId, inPassword, UserType.SITECON);
					
					if (siteconUser == null) {
						LOGGER.error("Failed to create user for new site controller "+inDomainId);
					}
				}
			} else {
				LOGGER.error("Tried to create Site Controller User "+inDomainId+" but it already exists (Site Controller does not exist)");
			}
		} else {
			LOGGER.info("Tried to create Site Controller "+inDomainId+" but it already exists");
		}
		return siteconUser;
	}
	

}
