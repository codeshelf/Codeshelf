/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDeviceABC.java,v 1.9 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.codeshelf.application.ContextLogging;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;

// --------------------------------------------------------------------------
/**
 * WirelessDevice
 * 
 * A WirelessDevice is a base class that holds information and behavior that's common to all devices
 * attached to the network.
 * 
 * @author jeffw
 */

@MappedSuperclass
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class WirelessDeviceABC extends DomainObjectTreeABC<CodeshelfNetwork> {

	public static final int						MAC_ADDR_BYTES		= 8;
	public static final int						PUBLIC_KEY_BYTES	= 8;

	private static final Logger		LOGGER	= LoggerFactory.getLogger(WirelessDeviceABC.class);

	// The owning network. 
	@ManyToOne(optional = false, fetch=FetchType.EAGER)
	@Getter
	@Setter
	protected CodeshelfNetwork		parent;

	@Column(nullable = false,name="device_guid")
	@Getter
	@Setter
	@JsonProperty
	private byte[]					deviceGuid;

	// The description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					description;

	// The last seen battery level.
	@Column(nullable = false,name="last_battery_level")
	@Getter
	@Setter
	@JsonProperty
	private short					lastBatteryLevel;

	@Transient
	@Column(nullable = true)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private NetworkDeviceStateEnum	networkDeviceStatus;

	@Transient
	@Column(nullable = true)
	@Setter
	@JsonProperty
	private Long					lastContactTime;

	// The network address last assigned to this wireless device.
	@Transient
	@Column(nullable = true)
	@JsonProperty
	private byte					networkAddress;

	public WirelessDeviceABC() {
		deviceGuid = new byte[NetGuid.NET_GUID_BYTES];
		description = "";
		lastBatteryLevel = 0;
	}

	public NetGuid getDeviceNetGuid() {
		return new NetGuid(deviceGuid);
	}

	public void setDeviceNetGuid(NetGuid inGuid) {
		deviceGuid = inGuid.getParamValueAsByteArray();
	}

	@JsonIgnore
	public String getDeviceGuidStr() {
		return new NetGuid(deviceGuid).toString();
	}

	public void setDeviceGuidStr(String inGuidStr) {
		deviceGuid = new NetGuid(inGuidStr).getParamValueAsByteArray();
	}

	public void setNetAddress(NetAddress inNetworkAddress) {
		networkAddress = (byte) inNetworkAddress.getValue();
	}

	public NetAddress getNetAddress() {
		return new NetAddress(networkAddress);
	}

	public boolean doesMatch(NetGuid inGuid) {
		return Arrays.equals(deviceGuid, inGuid.getParamValueAsByteArray());
	}

	public long getLastContactTime() {
		long result = 0;
		Long longVal = lastContactTime;
		if (longVal != null) {
			result = longVal.longValue();
		}
		return result;
	}

	public NetworkDeviceStateEnum getNetworkDeviceState() {
		NetworkDeviceStateEnum result = networkDeviceStatus;
		if (result == null) {
			result = NetworkDeviceStateEnum.getNetworkDeviceStateEnum(0); //INVALID;
		}
		return result;
	}

	public void setNetworkDeviceState(NetworkDeviceStateEnum inState) {
		ContextLogging.setNetGuid(this.getDeviceNetGuid());
		LOGGER.debug(Arrays.toString(deviceGuid) + " state changed: " + networkDeviceStatus + "->" + inState);
		ContextLogging.clearNetGuid();

		networkDeviceStatus = inState;
	}

	/*
	 * These methods are needed, because it's not currentyl possible (as of v1.1.0) to have
	 * abstract super classes in an EBean hierarchy that are also entity classes since EBean tries to create
	 * an instance of every entity class.  If you make this super class a non-entity class then EBean
	 * doesn't create a functional inheritance info tree (one of the parent refs in the tree is null), thus
	 * causing the EBean server to not start.
	 * 
	 */

	public void commandReceived(final String inCommandStr) {
		// See above note.
	}

	public Facility getFacility() {
		return this.getParent().getParent();
	}
		
	public String toString() {
		// used to set domainId from changeControllerId(), so don't reference domainId unless that is changed
		return getDefaultDomainIdPrefix()+"-"+this.getDeviceNetGuid().getHexStringNoPrefix();
	}


}
