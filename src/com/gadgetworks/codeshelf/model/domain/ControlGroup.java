/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroup.java,v 1.9 2012/10/21 02:02:17 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.controller.NetGroup;
import com.gadgetworks.codeshelf.model.TagProtocolEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.server.tags.IControllerConnection;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * This is the persistence object that contains information about the control group associated with a CodeShelf network.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "CONTROLGROUP")
@CacheStrategy
@JsonAutoDetect(getterVisibility=Visibility.NONE)
public class ControlGroup extends DomainObjectABC {

	@Inject
	public static ITypedDao<ControlGroup>	DAO;

	@Singleton
	public static class ControlGroupDao extends GenericDaoABC<ControlGroup> implements ITypedDao<ControlGroup> {
		public final Class<ControlGroup> getDaoClass() {
			return ControlGroup.class;
		}
	}

	// The owning CodeShelf network.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private CodeShelfNetwork				parent;
	
	// The control group ID
	@Column(nullable = false)
	@Getter
	@JsonProperty
	private byte[]							serializedId;
	
	// The control group description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String							description;
	
	// Interface port number
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private short							interfacePortNum;
	
	// Active/Inactive rule
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private boolean							active;
	
	// Active/Inactive rule
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private TagProtocolEnum					tagProtocolEnum;
	
	// For a control group this is a list of all of the pick tags that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<WirelessDevice>			wirelessDevices		= new ArrayList<WirelessDevice>();

	@Transient
	private IControllerConnection			controllerConnection;

	public ControlGroup() {
		serializedId = new byte[NetGroup.NET_GROUP_BYTES];
		description = "";
		active = true;
		tagProtocolEnum = TagProtocolEnum.ATOP;
	}

	public final ITypedDao<ControlGroup> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "CG";
	}

	public final CodeShelfNetwork getParentCodeShelfNetwork() {
		return parent;
	}
	
	public final void setParentCodeShelfNetwork(CodeShelfNetwork inParentCodeShelfNetwork) {
		parent = inParentCodeShelfNetwork;
	}

	public final IDomainObject getParent() {
		return getParentCodeShelfNetwork();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof CodeShelfNetwork) {
			setParentCodeShelfNetwork((CodeShelfNetwork) inParent);
		}
	}

	public final List<? extends IDomainObject> getChildren() {
		return getWirelessDevices();
	}

	public final NetGroup getControlGroupId() {
		return new NetGroup(serializedId);
	}

	public final void setControlGroupId(NetGroup inId) {
		serializedId = inId.getParamValueAsByteArray();
	}

	public final TagProtocolEnum getTagProtocol() {
		TagProtocolEnum result = tagProtocolEnum;
		if (result == null) {
			result = TagProtocolEnum.getTagProtocolEnum(0); //INVALID;
		}
		return result;
	}

	public final void setTagProtocol(TagProtocolEnum inTagProtocolEnum) {
		tagProtocolEnum = inTagProtocolEnum;
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addWirelessDevice(WirelessDevice inWirelessDevice) {
		wirelessDevices.add(inWirelessDevice);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeWirelessDevice(WirelessDevice inWirelessDevice) {
		wirelessDevices.remove(inWirelessDevice);
	}

	// --------------------------------------------------------------------------
	/**
	 * Atop tags do not have unique IDs.  Instead they are "numbered" on a serial bus from 1-to-200.
	 * The host s/w must remember the bus number for each device on the controller.
	 * We, on the other hand, have a MAC for each device.  We address commands to the MAC address
	 * of the device (anywhere on the network).
	 * @param inSerialBusNumber
	 * @return
	 */
	public final WirelessDevice getWirelessDeviceBySerialBusNumber(short inSerialBusNumber) {

		WirelessDevice result = null;

		// To deal with the mismatch here, we maintain a mapping from the serial bus order to the MAC address.
		for (WirelessDevice device : getWirelessDevices()) {
			if (device instanceof PickTag) {
				PickTag tag = (PickTag) device;
				if (tag.getSerialBusPosition() == inSerialBusNumber) {
					result = tag;
				}
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public final IControllerConnection getControllerConnection() {
		return controllerConnection;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inControllerConnection
	 */
	public final void setControllerConnection(IControllerConnection inControllerConnection) {
		controllerConnection = inControllerConnection;
	}
}
