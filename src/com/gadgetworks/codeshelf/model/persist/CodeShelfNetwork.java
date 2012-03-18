/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetwork.java,v 1.14 2012/03/18 04:12:26 jeffw Exp $
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
import com.gadgetworks.codeshelf.model.dao.IDaoRegistry;
import com.gadgetworks.codeshelf.model.dao.IGenericDao;
import com.google.inject.Inject;

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

	public interface ICodeShelfNetworkDao extends IGenericDao<CodeShelfNetwork> {		
	}
	
	private static final Log							LOGGER				= LogFactory.getLog(CodeShelfNetwork.class);

	private static final long							serialVersionUID	= 3001609308065821464L;

	// The network ID.
	@Column(nullable = false)
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
	private boolean										active;
	// The network ID.
	@Column(nullable = false)
	private byte[]										gatewayAddr;
	// The gateway URL.
	@Column(nullable = false)
	@Getter
	@Setter
	private String										gatewayUrl;
	// For a network this is a list of all of the control groups that belong in the set.
	@Column(nullable = false)
	@Getter
	@OneToMany(mappedBy = "parentCodeShelfNetwork")
	private List<ControlGroup>							controlGroups		= new ArrayList<ControlGroup>();

	@Transient
	@Getter
	@Setter
	private boolean										connected;
	@Transient
	@Getter
	@Setter
	private IWirelessInterface							wirelessInterface;

	public CodeShelfNetwork() {
		networkId = new byte[NetworkId.NETWORK_ID_BYTES];
		description = "";
		gatewayAddr = new byte[NetAddress.NET_ADDRESS_BYTES];
		gatewayUrl = "";
		active = true;
		connected = false;
	}

	public final NetworkId getNetworkId() {
		return new NetworkId(networkId);
	}

	public final void setNetworkId(NetworkId inNetworkId) {
		networkId = inNetworkId.getParamValueAsByteArray();
	}

	public final NetAddress getGatewayAddr() {
		return new NetAddress(gatewayAddr);
	}

	public final void setGatewayAddr(NetAddress inNetAddress) {
		gatewayAddr = inNetAddress.getParamValueAsByteArray();
	}

	// We always need to return the object cached in the DAO.
//	public final List<ControlGroup> getControlGroups() {
//		if (IGenericDao.USE_DAO_CACHE) {
//			List<ControlGroup> result = new ArrayList<ControlGroup>();
//			CodeShelfNetworkDao codeShelfNetworkDao = new CodeShelfNetworkDao();
//			if (!codeShelfNetworkDao.isObjectPersisted(this)) {
//				result = controlGroups;
//			} else {
//				ControlGroupDao controlGroupDao = new ControlGroupDao();
//				for (ControlGroup controlGroup : controlGroupDao.getAll()) {
//					if (controlGroup.getParentCodeShelfNetwork().equals(this)) {
//						result.add(controlGroup);
//					}
//				}
//			}
//			return result;
//		} else {
//			return controlGroups;
//		}
//	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addControlGroup(ControlGroup inControlGroup) {
		controlGroups.add(inControlGroup);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeControlGroup(ControlGroup inControlGroup) {
		controlGroups.remove(inControlGroup);
	}
}
