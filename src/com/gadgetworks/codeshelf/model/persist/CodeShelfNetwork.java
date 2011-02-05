/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetwork.java,v 1.7 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;
import com.gadgetworks.codeshelf.model.dao.ISystemDAO;

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

	//	private static final Log		LOGGER			= LogFactory.getLog(CodeShelfNetwork.class);

	private static final long	serialVersionUID	= 3001609308065821464L;

	// The network ID.
	@Column(nullable = false)
	private byte[]				mId;
	// The network description.
	@Column(nullable = false)
	private String				mDescription;
	// Active/Inactive network
	@Column(nullable = false)
	private boolean				mIsActive;
	// The network ID.
	@Column(nullable = false)
	private byte[]				mGatewayAddr;
	// The gateway URL.
	@Column(nullable = false)
	private String				mGatewayUrl;
	// For a network this is a list of all of the control groups that belong in the set.
	@Column(nullable = false)
	@OneToMany(mappedBy = "mParentCodeShelfNetwork")
	private List<ControlGroup>	mControlGroups		= new ArrayList<ControlGroup>();

	@Transient()
	private boolean				mIsConnected;
	@Transient()
	private IWirelessInterface	mWirelessInterface;

	public CodeShelfNetwork() {
		mId = new byte[NetworkId.NETWORK_ID_BYTES];
		mDescription = "";
		mGatewayAddr = new byte[NetAddress.NET_ADDRESS_BYTES];
		mGatewayUrl = "";
		mIsActive = true;
		mIsConnected = false;
	}

	public final String toString() {
		return getId().toString() + " " + mDescription;
	}

	public final NetworkId getId() {
		return new NetworkId(mId);
	}

	public final void setId(NetworkId inNetworkId) {
		mId = inNetworkId.getParamValueAsByteArray();
	}

	public final String getDescription() {
		return mDescription;
	}

	public final void setDescription(String inDescription) {
		mDescription = inDescription;
	}

	public final boolean getIsActive() {
		return mIsActive;
	}

	public final void setIsActive(boolean inIsActive) {
		mIsActive = inIsActive;
	}

	public final boolean getIsConnected() {
		return mIsConnected;
	}

	public final void setIsConnected(boolean inIsConnected) {
		mIsConnected = inIsConnected;
	}

	public final IWirelessInterface getWirelessInterface() {
		return mWirelessInterface;
	}

	public final void setWirelessInterface(IWirelessInterface inGatewayInterface) {
		mWirelessInterface = inGatewayInterface;
	}

	public final NetAddress getGatewayAddr() {
		return new NetAddress(mGatewayAddr);
	}

	public final void setGatewayAddr(NetAddress inNetAddress) {
		mGatewayAddr = inNetAddress.getParamValueAsByteArray();
	}

	public final String getGatewayUrl() {
		return mGatewayUrl;
	}

	public final void setGatewayUrl(String inUrlString) {
		mGatewayUrl = inUrlString;
	}

	// We always need to return the object cached in the DAO.
	public final List<ControlGroup> getControlGroups() {
		if (ISystemDAO.USE_CACHE) {
			List<ControlGroup> result = new ArrayList<ControlGroup>();
			if (!Util.getSystemDAO().isObjectPersisted(this)) {
				result = mControlGroups;
			} else {
				for (ControlGroup controlGroup : Util.getSystemDAO().getControlGroups()) {
					if (controlGroup.getParentCodeShelfNetwork().equals(this)) {
						result.add(controlGroup);
					}
				}
			}
			return result;
		} else {
			return mControlGroups;
		}
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(ControlGroup inControlGroup) {
		mControlGroups.add(inControlGroup);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(ControlGroup inControlGroup) {
		mControlGroups.remove(inControlGroup);
	}
}
