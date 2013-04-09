/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.27 2013/04/09 07:58:20 jeffw Exp $
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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "codeshelf_network", schema = "codeshelf")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class CodeshelfNetwork extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<CodeshelfNetwork>	DAO;

	@Singleton
	public static class CodeshelfNetworkDao extends GenericDaoABC<CodeshelfNetwork> implements ITypedDao<CodeshelfNetwork> {
		public final Class<CodeshelfNetwork> getDaoClass() {
			return CodeshelfNetwork.class;
		}
	}

	public static final String		DEFAULT_NETWORK_ID	= "DEFAULT";

	private static final Logger		LOGGER				= LoggerFactory.getLogger(CodeshelfNetwork.class);

	// The network description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					description;

	// Attachment credential.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					credential;

	// Active/Inactive network
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private boolean					active;

	@Transient
	@Getter
	@Setter
	@JsonProperty
	private boolean					connected;

	// The owning facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility				parent;

	@OneToMany(mappedBy = "parent")
	@MapKey(name = "domainId")
	@Getter
	private Map<String, Che>		ches				= new HashMap<String, Che>();

	// For a network this is a list of all of the devices that belong to this network.
	@Column(nullable = false)
	@Getter
	@OneToMany(mappedBy = "parent")
	private List<WirelessDeviceABC>	devices				= new ArrayList<WirelessDeviceABC>();

	public CodeshelfNetwork() {
		description = "";
		active = true;
		connected = false;
		ches = new HashMap<String, Che>();
	}

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
	 * Create the aisle's controller.
	 * @param inCodeshelfNetwork
	 * @param inGUID
	 */
	public final void createLedController(final String inGUID) {

		LedController controller = LedController.DAO.findByDomainId(this, inGUID);
		if (controller == null) {
			// Get the first network in the list of networks.
			controller = new LedController();
			controller.setParent(this);
			controller.setDomainId(inGUID);
			controller.setDesc("Default controller for " + this.getDomainId());
			controller.setDeviceNetGuid(new NetGuid(inGUID));
			try {
				LedController.DAO.store(controller);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}
}
