/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.15 2013/02/17 04:22:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@Table(name = "CODESHELFNETWORK", schema = "CODESHELF")
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

	private static final Log		LOGGER			= LogFactory.getLog(CodeshelfNetwork.class);

	// The network ID.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private byte[]					serializedId;

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

	// The network ID.
	@Column(nullable = false)
	@JsonProperty
	private byte[]					gatewayAddr;

	// The gateway URL.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					gatewayUrl;

	@Transient
	@Getter
	@Setter
	@JsonProperty
	private boolean					connected;

	@Transient
	@Getter
	@Setter
	private IWirelessInterface		wirelessInterface;

	// The owning facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility				parent;

	// For a network this is a list of all of the devices that belong to this network.
	@Column(nullable = false)
	@Getter
	@OneToMany(mappedBy = "parent")
	private List<WirelessDevice>	controlGroups	= new ArrayList<WirelessDevice>();

	public CodeshelfNetwork() {
		serializedId = new byte[NetworkId.NETWORK_ID_BYTES];
		description = "";
		gatewayAddr = new byte[NetAddress.NET_ADDRESS_BYTES];
		gatewayUrl = "";
		active = true;
		connected = false;
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
		return getControlGroups();
	}

	@JsonProperty
	public final NetworkId getNetworkId() {
		return new NetworkId(serializedId);
	}

	public final void setNetworkId(NetworkId inNetworkId) {
		serializedId = inNetworkId.getParamValueAsByteArray();
	}

	@JsonProperty
	public final NetAddress getGatewayAddr() {
		return new NetAddress(gatewayAddr);
	}

	public final void setGatewayAddr(NetAddress inNetAddress) {
		gatewayAddr = inNetAddress.getParamValueAsByteArray();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(WirelessDevice inWirelessDevice) {
		controlGroups.add(inWirelessDevice);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(WirelessDevice inWirelessDevice) {
		controlGroups.remove(inWirelessDevice);
	}

	public final boolean isCredentialValid(final String inCredential) {
		boolean result = false;

		if (inCredential != null) {
			result = credential.equals(inCredential);
		}
		return result;
	}
}
