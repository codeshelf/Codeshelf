/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroup.java,v 1.24 2012/07/12 08:18:06 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

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

import com.gadgetworks.codeshelf.controller.NetGroup;
import com.gadgetworks.codeshelf.model.TagProtocolEnum;
import com.gadgetworks.codeshelf.model.dao.GenericDao;
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
public class ControlGroup extends PersistABC {

	@Singleton
	public static class ControlGroupDao extends GenericDao<ControlGroup> implements ITypedDao<ControlGroup> {
		public ControlGroupDao() {
			super(ControlGroup.class);
		}
	}

	@Inject
	public static ITypedDao<ControlGroup> DAO;
//	public static ITypedDao<ControlGroup> DAO = new GenericDao<ControlGroup>(ControlGroup.class);

	private static final long		serialVersionUID	= -4923129546531851147L;

	// The owning CodeShelf network.
	@Getter
	@Setter
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private CodeShelfNetwork		parentCodeShelfNetwork;
	// The control group ID
	@Column(nullable = false)
	//	@Getter
	//	@Setter
	private byte[]					controlGroupId;
	// The control group description.
	@Getter
	@Setter
	@Column(nullable = false)
	private String					description;
	// Interface port number
	@Getter
	@Setter
	@Column(nullable = false)
	private short					interfacePortNum;
	// Active/Inactive rule
	@Getter
	@Setter
	@Column(nullable = false)
	private boolean					active;
	// Active/Inactive rule
	@Getter
	@Setter
	@Column(nullable = false)
	private TagProtocolEnum			tagProtocolEnum;
	// For a control group this is a list of all of the pick tags that belong in the set.
	@OneToMany(mappedBy = "parentControlGroup")
	@Getter
	private List<WirelessDevice>	wirelessDevices		= new ArrayList<WirelessDevice>();

	@Transient
	private IControllerConnection	controllerConnection;

	public ControlGroup() {
		parentCodeShelfNetwork = null;
		controlGroupId = new byte[NetGroup.NET_GROUP_BYTES];
		description = "";
		active = true;
		tagProtocolEnum = TagProtocolEnum.ATOP;
	}

	public final PersistABC getParent() {
		return getParentCodeShelfNetwork();
	}

	public final void setParent(PersistABC inParent) {
		if (inParent instanceof CodeShelfNetwork) {
			setParentCodeShelfNetwork((CodeShelfNetwork) inParent);
		}
	}
	
	public final NetGroup getControlGroupId() {
		return new NetGroup(controlGroupId);
	}

	public final void setControlGroupId(NetGroup inId) {
		controlGroupId = inId.getParamValueAsByteArray();
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
