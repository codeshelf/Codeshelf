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
 * CodeShelfNetwork
 * 
 * The CodeShelfNetwork object holds information about how to create a standalone CodeShelf network.
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

	private static final Logger			LOGGER				= LoggerFactory.getLogger(CodeshelfNetwork.class);

	// The network description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						description;

	// Attachment credential.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String						credential;

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
	private Map<String, Che>			ches				= new HashMap<String, Che>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, LedController>	ledControllers		= new HashMap<String, LedController>();

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, SiteController>	siteControllers		= new HashMap<String, SiteController>();

	// For a network this is a list of all of the devices that belong to this network.
	@Column(nullable = false)
	@Getter
	@OneToMany(mappedBy = "parent")
	private List<WirelessDeviceABC>		devices				= new ArrayList<WirelessDeviceABC>();

	public CodeshelfNetwork() {
		this(null, null, "", null);
	}
	
	public CodeshelfNetwork(Facility parent, String domainId, String description, String credential) {
		super(domainId);
		this.parent = parent;
		this.description = description;
		this.credential = credential;
		active = true;
		connected = false;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<CodeshelfNetwork> getDao() {
		return DAO;
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

	public final boolean isCredentialValid(final String inCredential) {
		boolean result = false;

		if (inCredential != null) {
			result = credential.equals(inCredential);
		}
		return result;
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
				LOGGER.error("", e);
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
			result.setDesc("LED controller for " + this.getDomainId());
			result.setDeviceNetGuid(inGuid);
			this.addLedController(result); // so that it works immediately in unit test, and not only after one rehydration cycle

			try {
				LedController.DAO.store(result);
			} catch (DaoException e) { 
				LOGGER.error("", e);
			}
		}
		return result;
	}
}
