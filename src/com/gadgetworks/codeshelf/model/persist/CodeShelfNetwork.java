/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetwork.java,v 1.10 2011/12/29 09:15:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;
import com.gadgetworks.codeshelf.model.dao.GenericDao;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;

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
@Table(name = "CODESHELFNETWORK")
public class CodeShelfNetwork extends PersistABC {

	public static final GenericDao<CodeShelfNetwork>	DAO					= new GenericDao<CodeShelfNetwork>(CodeShelfNetwork.class);

	private static final Log							LOGGER				= LogFactory.getLog(CodeShelfNetwork.class);

	private static final long							serialVersionUID	= 3001609308065821464L;

	// The network ID.
	@Column(nullable = false)
	//	@Getter
	//	@Setter
	//@Embedded
	private byte[]										networkId;
	// The network description.
	@Column(nullable = false)
	@Getter
	@Setter
	private String										description;
	// Active/Inactive network
	@Column(nullable = false)
	@Getter
	@Setter
	private boolean										isActive;
	// The network ID.
	@Column(nullable = false)
	//	@Getter
	//	@Setter
	private byte[]										gatewayAddr;
	// The gateway URL.
	@Column(nullable = false)
	@Getter
	@Setter
	private String										gatewayUrl;
	// For a network this is a list of all of the control groups that belong in the set.
	@Column(nullable = false)
	//	@Getter
	//	@Setter
	@OneToMany(mappedBy = "parentCodeShelfNetwork")
	private List<ControlGroup>							controlGroups		= new ArrayList<ControlGroup>();

	@Transient
	@Getter
	@Setter
	private boolean										isConnected;
	@Transient
	@Getter
	@Setter
	private IWirelessInterface							wirelessInterface;

	public CodeShelfNetwork() {
		networkId = new byte[NetworkId.NETWORK_ID_BYTES];
		description = "";
		gatewayAddr = new byte[NetAddress.NET_ADDRESS_BYTES];
		gatewayUrl = "";
		isActive = true;
		isConnected = false;
	}

	//	public final String toString() {
	//		return getId().toString() + " " + mDescription;
	//	}

	public NetworkId getNetworkId() {
		return new NetworkId(networkId);
	}

	public void setNetworkId(NetworkId inNetworkId) {
		networkId = inNetworkId.getParamValueAsByteArray();
	}

	//	public  String getDescription() {
	//		return mDescription;
	//	}
	//
	//	public  void setDescription(String inDescription) {
	//		mDescription = inDescription;
	//	}
	//
	//	public  boolean getIsActive() {
	//		return mIsActive;
	//	}
	//
	//	public  void setIsActive(boolean inIsActive) {
	//		mIsActive = inIsActive;
	//	}
	//
	//	public  boolean getIsConnected() {
	//		return mIsConnected;
	//	}
	//
	//	public  void setIsConnected(boolean inIsConnected) {
	//		mIsConnected = inIsConnected;
	//	}
	//
	//	public  IWirelessInterface getWirelessInterface() {
	//		return mWirelessInterface;
	//	}
	//
	//	public  void setWirelessInterface(IWirelessInterface inGatewayInterface) {
	//		mWirelessInterface = inGatewayInterface;
	//	}

	public NetAddress getGatewayAddr() {
		return new NetAddress(gatewayAddr);
	}

	public void setGatewayAddr(NetAddress inNetAddress) {
		gatewayAddr = inNetAddress.getParamValueAsByteArray();
	}

	//	public  String getGatewayUrl() {
	//		return mGatewayUrl;
	//	}
	//
	//	public  void setGatewayUrl(String inUrlString) {
	//		mGatewayUrl = inUrlString;
	//	}

	// We always need to return the object cached in the DAO.
	public List<ControlGroup> getControlGroups() {
		if (IGenericDao.USE_DAO_CACHE) {
			List<ControlGroup> result = new ArrayList<ControlGroup>();
			if (!CodeShelfNetwork.DAO.isObjectPersisted(this)) {
				result = controlGroups;
			} else {
				for (ControlGroup controlGroup : ControlGroup.DAO.getAll()) {
					if (controlGroup.getParentCodeShelfNetwork().equals(this)) {
						result.add(controlGroup);
					}
				}
			}
			return result;
		} else {
			return controlGroups;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public void addControlGroup(ControlGroup inControlGroup) {
		controlGroups.add(inControlGroup);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public void removeControlGroup(ControlGroup inControlGroup) {
		controlGroups.remove(inControlGroup);
	}
}
