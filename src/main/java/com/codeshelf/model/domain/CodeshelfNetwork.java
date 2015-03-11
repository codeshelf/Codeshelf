/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@Table(name = "network")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class CodeshelfNetwork extends DomainObjectTreeABC<Facility> {

	public static class CodeshelfNetworkDao extends GenericDaoABC<CodeshelfNetwork> implements ITypedDao<CodeshelfNetwork> {
		public final Class<CodeshelfNetwork> getDaoClass() {
			return CodeshelfNetwork.class;
		}
	}

	public static final String			DEFAULT_NETWORK_NAME	= "DEFAULT";
	public static final Short			DEFAULT_NETWORK_NUM		= 1;
	public static final Short			DEFAULT_CHANNEL			= 10;
	
	public static final int				DEFAULT_SITECON_SERIAL	= 5000;
	public static final String			DEFAULT_SITECON_PASS	= "0.6910096026612129";
	public static final String			DEFAULT_SITECON_USERNAME= Integer.toString(CodeshelfNetwork.DEFAULT_SITECON_SERIAL);
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
	@Column(nullable = false,name="network_num")
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
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	private Facility					parent;
	
	@Getter
    @OneToMany(mappedBy = "parent",targetEntity=Che.class)
    @MapKey(name = "domainId")
	@JsonProperty
	private Map<String, Che>			ches				= new HashMap<String, Che>();

	@Getter
	@OneToMany(mappedBy = "parent",targetEntity=LedController.class)
	@MapKey(name = "domainId")
	@JsonProperty
	private Map<String, LedController>	ledControllers		= new HashMap<String, LedController>();

	@OneToMany(mappedBy = "parent",targetEntity=SiteController.class)
	@MapKey(name = "domainId")
	@Getter
	@JsonProperty
	private Map<String, SiteController>	siteControllers		= new HashMap<String, SiteController>();

	// For a network this is a list of all of the devices that belong to this network.
	// @Column(nullable = false)
	// @Getter
	// @OneToMany(mappedBy = "parent")
	// private List<WirelessDeviceABC>		devices				= new ArrayList<WirelessDeviceABC>();

	public CodeshelfNetwork() {
		super();
		
		active = true;
		connected = false;
		this.description = "";		
		this.channel = CodeshelfNetwork.DEFAULT_CHANNEL;
		this.networkNum = CodeshelfNetwork.DEFAULT_NETWORK_NUM;
	}
	
	@SuppressWarnings("unchecked")
	public final ITypedDao<CodeshelfNetwork> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<CodeshelfNetwork> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(CodeshelfNetwork.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "NET";
	}

	public void addChe(Che inChe) {
		CodeshelfNetwork previousNetwork = inChe.getParent();
		if(previousNetwork == null) {
			ches.put(inChe.getDomainId(), inChe);
			inChe.setParent(this);
		} else if(!previousNetwork.equals(this)) {
			LOGGER.error("cannot add Che "+inChe.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousNetwork.getDomainId());
		}	
	}

	public Che getChe(String inCheId) {
		return ches.get(inCheId);
	}

	public void removeChe(String inCheId) {
		Che che = this.getChe(inCheId);
		if(che != null) {
			che.setParent(null);
			ches.remove(inCheId);
		} else {
			LOGGER.error("cannot remove Che "+inCheId+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	public void addSiteController(SiteController inSiteController) {
		CodeshelfNetwork previousNetwork = inSiteController.getParent();
		if(previousNetwork == null) {
			siteControllers.put(inSiteController.getDomainId(), inSiteController);
			inSiteController.setParent(this);
		} else if(!previousNetwork.equals(this)) {
			LOGGER.error("cannot add SiteController "+inSiteController.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousNetwork.getDomainId());
		}	
	}

	public SiteController getSiteController(String inSiteControllerId) {
		return siteControllers.get(inSiteControllerId);
	}

	public void removeSiteController(String inSiteControllerId) {
		SiteController siteController = this.getSiteController(inSiteControllerId);
		if(siteController != null) {
			siteController.setParent(null);
			siteControllers.remove(inSiteControllerId);
		} else {
			LOGGER.error("cannot remove SiteController "+inSiteControllerId+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	public void addLedController(LedController inLedController) {
		CodeshelfNetwork previousNetwork = inLedController.getParent();
		if(previousNetwork == null) {
			ledControllers.put(inLedController.getDomainId(), inLedController);
			inLedController.setParent(this);
		} else if(!previousNetwork.equals(this)) {
			LOGGER.error("cannot add LedController "+inLedController.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousNetwork.getDomainId());
		}	
	}

	public LedController getLedController(String inLedControllerId) {
		return ledControllers.get(inLedControllerId);
	}

	public void removeLedController(String inLedControllerId) {
		LedController ledController = this.getLedController(inLedControllerId);
		if(ledController != null) {
			ledController.setParent(null);
			ledControllers.remove(inLedControllerId);
		} else {
			LOGGER.error("cannot remove LedController "+inLedControllerId+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public Che createChe(String inDomainId, NetGuid inGuid) {
		// If the CHE doesn't already exist then create it.
		Che che = Che.staticGetDao().findByDomainId(this, inGuid.getHexStringNoPrefix());
		if (che == null) {
			che = new Che();
			che.setDomainId(inDomainId);
			che.setDeviceNetGuid(inGuid);
			this.addChe(che);
			try {
				Che.staticGetDao().store(che);
			} catch (DaoException e) {
				LOGGER.error("Couldn't store new CHE "+inDomainId, e);
			}
		}
		return che;
	}

	// --------------------------------------------------------------------------
	/**
	 * Find existing (by domain ID), or create new controller. Not sure NetGuid should be passed in.
	 * @param inCodeshelfNetwork
	 * @param inGUID
	 */
	public LedController findOrCreateLedController(String inDomainId, NetGuid inGuid) {

		LedController result = LedController.staticGetDao().findByDomainId(this, inDomainId);
		if (result == null) {
			// Get the first network in the list of networks.
			result = new LedController();
			result.setDomainId(inDomainId);
			result.setDescription("LED controller for " + this.getDomainId());
			result.setDeviceNetGuid(inGuid);
			this.addLedController(result); // so that it works immediately in unit test, and not only after one rehydration cycle

			try {
				LedController.staticGetDao().store(result);
			} catch (DaoException e) { 
				LOGGER.error("Couldn't store new LED controller "+inDomainId, e);
			}
		}
		return result;
	}

	public Facility getFacility() {
		return this.getParent();
	}

	public void createSiteController(int serialNumber, String inDescribeLocation, Boolean inMonitor, String password) {
		String username = Integer.toString(serialNumber);

		// create site controller object (or use found)
		SiteController sitecon = SiteController.staticGetDao().findByDomainId(this,username);
		if(sitecon == null) {
			sitecon = new SiteController();
			sitecon.setDomainId(username);
			sitecon.setDescription("Site Controller for " + this.getDomainId());
			sitecon.setDescribeLocation(inDescribeLocation);
			sitecon.setMonitor(inMonitor);
			this.addSiteController(sitecon);
			
			try {
				SiteController.staticGetDao().store(sitecon); 
			} catch (DaoException e) { 
				LOGGER.error("Couldn't store new Site Controller "+username, e);
				sitecon=null;
			}
			
		} else {
			LOGGER.error("Tried to create Site Controller "+username+" but it already exists");
		}

		// create site controller user (or use found)
		
		/* not implemented anymore. 
		 * tenant manager will create "default" site controller user
		 * others must be created manually separate from domain object site controller
		User user = TenantManagerService.getInstance().getUser(username);
		if(user != null) {
			// already exists?
			if(user.tenantEquals(tenant)) {
				LOGGER.info("Site controller user already exists, not creating");
			} else {
				if(TenantManagerService.class.isAssignableFrom(TenantManagerService.getInstance().getClass()))
					// ignore for testing mock 
					LOGGER.error("Cannot create site controller user since it already exists with another tenant");
			}
		} else if(sitecon == null) {
			LOGGER.error("Will not create Site Controller user after failure to create Site Controller");
		} else {
			// try to create user
			if(null == TenantManagerService.getInstance().createUser(tenant, username, password, UserType.SITECON)) {
				LOGGER.error("Failed to create user for new site controller "+username);
			}
		}
		 */
 
	}
}
