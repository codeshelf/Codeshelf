/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * CodeshelfNetwork
 * 
 * The CodeshelfNetwork object holds information about a Codeshelf wireless facility network 
 * including Site Controller(s), Aisle/LED Controllers and CHEs.
 * (There may be more than one running at a facility.)
 * 
 * @author jeffw
 */

@Entity
@Table(name = "codeshelf_network")
@CacheStrategy(useBeanCache = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class CodeshelfNetwork extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<CodeshelfNetwork>	DAO;

	@Singleton
	public static class CodeshelfNetworkDao extends GenericDaoABC<CodeshelfNetwork> implements ITypedDao<CodeshelfNetwork> {
		@Inject
		public CodeshelfNetworkDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<CodeshelfNetwork> getDaoClass() {
			return CodeshelfNetwork.class;
		}
	}

	public static final String			DEFAULT_NETWORK_NAME	= "DEFAULT";
	public static final Short			DEFAULT_NETWORK_NUM		= 1;
	public static final Short			DEFAULT_CHANNEL			= 10;
	
	public static final String			DEFAULT_SITECON_SERIAL	= "5000";
	public static final String			DEFAULT_SITECON_PASS	= "0.6910096026612129";

	private static final Logger			LOGGER				= LoggerFactory.getLogger(CodeshelfNetwork.class);

	// The network description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	// 802.15.4 channel number
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Short 						channel;

	// Logical network number to further subdivide channel
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Short 						networkNum;

	// Active/Inactive network
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private boolean						active;

	@Transient
	@Getter
	@Setter
	@JsonProperty
	private boolean						connected;

	// The owning facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility					parent;

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@JsonProperty
	private Map<String, Che>			ches				= new HashMap<String, Che>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@JsonProperty
	private Map<String, LedController>	ledControllers		= new HashMap<String, LedController>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	@JsonProperty
	private Map<String, SiteController>	siteControllers		= new HashMap<String, SiteController>();

	// For a network this is a list of all of the devices that belong to this network.
	@Column(nullable = false)
	@Getter
	@OneToMany(mappedBy = "parent")
	private List<WirelessDeviceABC>		devices				= new ArrayList<WirelessDeviceABC>();

	public CodeshelfNetwork() {
		this(null, null, "");
	}
	
	public CodeshelfNetwork(Facility parent, String domainId, String description) {
		super(domainId);
		this.parent = parent;
		this.description = description;
		active = true;
		connected = false;
		
		this.channel = CodeshelfNetwork.DEFAULT_CHANNEL;
		this.networkNum = CodeshelfNetwork.DEFAULT_NETWORK_NUM;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<CodeshelfNetwork> getDao() {
		return DAO;
	}
	
	public final static void setDao(ITypedDao<CodeshelfNetwork> dao) {
		CodeshelfNetwork.DAO = dao;
	}

	public final String getDefaultDomainIdPrefix() {
		return "NET";
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return getDevices();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addDevice(WirelessDeviceABC inWirelessDevice) {
		devices.add(inWirelessDevice);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeDevice(WirelessDeviceABC inWirelessDevice) {
		devices.remove(inWirelessDevice);
	}

	public final void addChe(Che inChe) {
		ches.put(inChe.getDomainId(), inChe);
	}

	public final Che getChe(String inCheId) {
		return ches.get(inCheId);
	}

	public final void removeChe(String inCheId) {
		ches.remove(inCheId);
	}

	public final void addSiteController(SiteController inSiteController) {
		siteControllers.put(inSiteController.getDomainId(), inSiteController);
	}

	public final SiteController getSiteController(String inSiteControllerId) {
		return siteControllers.get(inSiteControllerId);
	}

	public final void removeSiteController(String inSiteControllerId) {
		siteControllers.remove(inSiteControllerId);
	}

	public final void addLedController(LedController inLedController) {
		ledControllers.put(inLedController.getDomainId(), inLedController);
	}

	public final LedController getLedController(String inLedControllerId) {
		return ledControllers.get(inLedControllerId);
	}

	public final void removeLedController(String inLedControllerId) {
		ledControllers.remove(inLedControllerId);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final Che createChe(String inDomainId, NetGuid inGuid) {

		// If the CHE doesn't already exist then create it.
		Che result = Che.DAO.findByDomainId(this, inGuid.getHexStringNoPrefix());
		if (result == null) {
			result = new Che();
			result.setParent(this);
			result.setDomainId(inDomainId);
			result.setDeviceNetGuid(inGuid);

			this.addChe(result);
			try {
				Che.DAO.store(result);
			} catch (DaoException e) {
				LOGGER.error("Couldn't store new CHE "+inDomainId, e);
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Find existing (by domain ID), or create new controller. Not sure NetGuid should be passed in.
	 * @param inCodeshelfNetwork
	 * @param inGUID
	 */
	public final LedController findOrCreateLedController(String inDomainId, NetGuid inGuid) {

		LedController result = LedController.DAO.findByDomainId(this, inDomainId);
		if (result == null) {
			// Get the first network in the list of networks.
			result = new LedController();
			result.setParent(this);
			result.setDomainId(inDomainId);
			result.setDescription("LED controller for " + this.getDomainId());
			result.setDeviceNetGuid(inGuid);
			this.addLedController(result); // so that it works immediately in unit test, and not only after one rehydration cycle

			try {
				LedController.DAO.store(result);
			} catch (DaoException e) { 
				LOGGER.error("Couldn't store new LED controller "+inDomainId, e);
			}
		}
		return result;
	}

	/**
	 * all this default site controller / default site controller user stuff is just for dev/test environments
	 * 
	 * @return user
	 */
	public final User createDefaultSiteControllerUser() {
		User siteconUser = User.DAO.findByDomainId(null,CodeshelfNetwork.DEFAULT_SITECON_SERIAL);
		if(siteconUser == null) {
			// no default site controller user exists. check for default site controller.
			SiteController sitecon = SiteController.DAO.findByDomainId(null,CodeshelfNetwork.DEFAULT_SITECON_SERIAL);
			if(sitecon == null) {
				siteconUser = createSiteControllerAndUser(CodeshelfNetwork.DEFAULT_SITECON_SERIAL, "Test Area", false, CodeshelfNetwork.DEFAULT_SITECON_PASS);
			} else {
				LOGGER.error("Default site controller user doesn't exist, but default site controller does exist");
			}
		} // if default user already exists in database, we assume site controller does too; ignore and continue
		return siteconUser;
	}
	
	public final User createSiteControllerAndUser(String inDomainId, String inDescribeLocation, Boolean inMonitor, String inPassword) {
		User siteconUser = User.DAO.findByDomainId(null,inDomainId);
		if(siteconUser == null) {
			// no default site controller user exists. check for default site controller.
			SiteController sitecon = SiteController.DAO.findByDomainId(null,inDomainId);
			if(sitecon == null) {
				// ok to create site controller + user
				sitecon = new SiteController();
				sitecon.setParent(this);
				sitecon.setDomainId(inDomainId);
				sitecon.setDescription("Site Controller for " + this.getDomainId());
				sitecon.setDescribeLocation(inDescribeLocation);
				sitecon.setMonitor(inMonitor);
				this.addSiteController(sitecon);
				
				try {
					SiteController.DAO.store(sitecon); 
				} catch (DaoException e) { 
					LOGGER.error("Couldn't store new Site Controller "+CodeshelfNetwork.DEFAULT_SITECON_SERIAL, e);
					sitecon=null;
				}
				
				if(sitecon!=null) {
					siteconUser = this.getParent().getParentOrganization().createUser(inDomainId, inPassword, sitecon);
					
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
