/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeshelfNetwork.java,v 1.8 2012/10/02 03:17:58 jeffw Exp $
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
import org.codehaus.jackson.annotate.JsonIgnore;

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
@Table(name = "CODESHELFNETWORK")
@CacheStrategy
public class CodeShelfNetwork extends DomainObjectABC {

	@Inject
	public static ITypedDao<CodeShelfNetwork>	DAO;

	@Singleton
	public static class CodeShelfNetworkDao extends GenericDaoABC<CodeShelfNetwork> implements ITypedDao<CodeShelfNetwork> {
		public final Class<CodeShelfNetwork> getDaoClass() {
			return CodeShelfNetwork.class;
		}
	}

	private static final Log	LOGGER			= LogFactory.getLog(CodeShelfNetwork.class);

	// The network ID.
	@Column(nullable = false)
	private byte[]				serializedId;

	// The network description.
	@Column(nullable = false)
	@Getter
	@Setter
	private String				description;

	// Active/Inactive network
	@Column(nullable = false)
	@Getter
	@Setter
	private boolean				active;

	// The network ID.
	@Column(nullable = false)
	private byte[]				gatewayAddr;

	// The gateway URL.
	@Column(nullable = false)
	@Getter
	@Setter
	private String				gatewayUrl;

	@Transient
	@Getter
	@Setter
	private boolean				connected;

	@Transient
	@Getter
	@Setter
	private IWirelessInterface	wirelessInterface;

	// The owning facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility			parent;

	// For a network this is a list of all of the control groups that belong in the set.
	@Column(nullable = false)
	@Getter
	@OneToMany(mappedBy = "parent")
	private List<ControlGroup>	controlGroups	= new ArrayList<ControlGroup>();

	public CodeShelfNetwork() {
		serializedId = new byte[NetworkId.NETWORK_ID_BYTES];
		description = "";
		gatewayAddr = new byte[NetAddress.NET_ADDRESS_BYTES];
		gatewayUrl = "";
		active = true;
		connected = false;
	}

	public final ITypedDao<CodeShelfNetwork> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "NET";
	}

	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(Facility inParentFacility) {
		parent = inParentFacility;
	}

	public final IDomainObject getParent() {
		return getParentFacility();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	@JsonIgnore
	public final List<? extends IDomainObject> getChildren() {
		return getControlGroups();
	}

	public final NetworkId getNetworkId() {
		return new NetworkId(serializedId);
	}

	public final void setNetworkId(NetworkId inNetworkId) {
		serializedId = inNetworkId.getParamValueAsByteArray();
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
