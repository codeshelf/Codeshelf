/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.30 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.HashMap;
import java.util.Map;

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

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
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
@Table(name = "network")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class CodeshelfNetwork extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<CodeshelfNetwork>	DAO;

	@Singleton
	public static class CodeshelfNetworkDao extends GenericDaoABC<CodeshelfNetwork> implements ITypedDao<CodeshelfNetwork> {
		@Inject
		public CodeshelfNetworkDao(PersistenceService persistenceService) {
			super(persistenceService);
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
		return DAO;
	}
	
	public final static void setDao(ITypedDao<CodeshelfNetwork> dao) {
		CodeshelfNetwork.DAO = dao;
	}

	public final String getDefaultDomainIdPrefix() {
		return "NET";
	}

	public final Facility getParent() {
		if (this.parent instanceof HibernateProxy) {
			this.parent = (Facility) PersistenceService.deproxify(this.parent);
		}
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	/*
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
	*/

	public final void addChe(Che inChe) {
		CodeshelfNetwork previousNetwork = inChe.getParent();
		if(previousNetwork == null) {
			ches.put(inChe.getDomainId(), inChe);
			inChe.setParent(this);
		} else if(!previousNetwork.equals(this)) {
			LOGGER.error("cannot add Che "+inChe.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousNetwork.getDomainId());
		}	
	}

	public final Che getChe(String inCheId) {
		return ches.get(inCheId);
	}

	public final void removeChe(String inCheId) {
		Che che = this.getChe(inCheId);
		if(che != null) {
			che.setParent(null);
			ches.remove(inCheId);
		} else {
			LOGGER.error("cannot remove Che "+inCheId+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	public final void addSiteController(SiteController inSiteController) {
		CodeshelfNetwork previousNetwork = inSiteController.getParent();
		if(previousNetwork == null) {
			siteControllers.put(inSiteController.getDomainId(), inSiteController);
			inSiteController.setParent(this);
		} else if(!previousNetwork.equals(this)) {
			LOGGER.error("cannot add SiteController "+inSiteController.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousNetwork.getDomainId());
		}	
	}

	public final SiteController getSiteController(String inSiteControllerId) {
		return siteControllers.get(inSiteControllerId);
	}

	public final void removeSiteController(String inSiteControllerId) {
		SiteController siteController = this.getSiteController(inSiteControllerId);
		if(siteController != null) {
			siteController.setParent(null);
			siteControllers.remove(inSiteControllerId);
		} else {
			LOGGER.error("cannot remove SiteController "+inSiteControllerId+" from "+this.getDomainId()+" because it isn't found in children");
		}
	}

	public final void addLedController(LedController inLedController) {
		CodeshelfNetwork previousNetwork = inLedController.getParent();
		if(previousNetwork == null) {
			ledControllers.put(inLedController.getDomainId(), inLedController);
			inLedController.setParent(this);
		} else if(!previousNetwork.equals(this)) {
			LOGGER.error("cannot add LedController "+inLedController.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousNetwork.getDomainId());
		}	
	}

	public final LedController getLedController(String inLedControllerId) {
		return ledControllers.get(inLedControllerId);
	}

	public final void removeLedController(String inLedControllerId) {
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
	public final Che createChe(String inDomainId, NetGuid inGuid) {
		// If the CHE doesn't already exist then create it.
		Che che = Che.DAO.findByDomainId(this, inGuid.getHexStringNoPrefix());
		if (che == null) {
			che = new Che();
			che.setDomainId(inDomainId);
			che.setDeviceNetGuid(inGuid);
			this.addChe(che);
			try {
				Che.DAO.store(che);
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
	public final LedController findOrCreateLedController(String inDomainId, NetGuid inGuid) {

		LedController result = LedController.DAO.findByDomainId(this, inDomainId);
		if (result == null) {
			// Get the first network in the list of networks.
			result = new LedController();
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

	public Facility getFacility() {
		return this.getParent();
	}

}
