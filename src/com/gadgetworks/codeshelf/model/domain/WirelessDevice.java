/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDevice.java,v 1.15 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetMacAddress;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 * WirelessDevice
 * 
 * A WirelessDevice is a base class that holds information and behavior that's common to all devices
 * attached to the network.
 * 
 * @author jeffw
 */

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
@Table(name = "WIRELESSDEVICE", schema = "CODESHELF")
//@DiscriminatorValue("ABC")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class WirelessDevice extends DomainObjectTreeABC<CodeshelfNetwork> implements INetworkDevice {

	@Inject
	private static IWirelessDeviceDao	DAO;

	public interface IWirelessDeviceDao extends ITypedDao<WirelessDevice> {

		WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr);

		INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr);

		INetworkDevice getNetworkDevice(NetAddress inAddress);

	}

	public static final int			MAC_ADDR_BYTES		= 8;
	public static final int			PUBLIC_KEY_BYTES	= 8;

	private static final Log		LOGGER				= LogFactory.getLog(WirelessDevice.class);

	// The owning network.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private CodeshelfNetwork		parent;

	@Column(nullable = false)
	private byte[]					macAddress;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					publicKey;

	// The description.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String					description;

	// The last seen battery level.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private short					lastBatteryLevel;

	//@Transient
	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private NetworkDeviceStateEnum	networkDeviceStatus;

	//@Transient
	@Column(nullable = false)
	@Setter
	@JsonProperty
	private Long					lastContactTime;

	// The network address last assigned to this wireless device.
	@Transient
	@Column(nullable = false)
	@JsonProperty
	private byte					networkAddress;

	public WirelessDevice() {
		macAddress = new byte[NetMacAddress.NET_MACADDR_BYTES];
		publicKey = "";
		description = "";
		lastBatteryLevel = 0;
	}

	public final ITypedDao<WirelessDevice> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "W";
	}

	public final CodeshelfNetwork getParent() {
		return parent;
	}

	public final void setParent(CodeshelfNetwork inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}

	public final NetMacAddress getMacAddress() {
		return new NetMacAddress(macAddress);
	}

	public final void setMacAddress(NetMacAddress inMacAddress) {
		macAddress = inMacAddress.getParamValueAsByteArray();
	}

	public final void setNetAddress(NetAddress inNetworkAddress) {
		networkAddress = (byte) inNetworkAddress.getValue();
	}

	@JsonProperty
	public final NetAddress getNetAddress() {
		return new NetAddress(networkAddress);
	}

	public final boolean doesMatch(String inMacAddr) {
		return macAddress.equals(inMacAddr);
	}

	public final long getLastContactTime() {
		long result = 0;
		Long longVal = lastContactTime;
		if (longVal != null) {
			result = longVal.longValue();
		}
		return result;
	}

	public final NetworkDeviceStateEnum getNetworkDeviceState() {
		NetworkDeviceStateEnum result = networkDeviceStatus;
		if (result == null) {
			result = NetworkDeviceStateEnum.getNetworkDeviceStateEnum(0); //INVALID;
		}
		return result;
	}

	public final void setNetworkDeviceState(NetworkDeviceStateEnum inState) {
		LOGGER.debug(macAddress + " state changed: " + networkDeviceStatus + "->" + inState);
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

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#setDesc(java.lang.String)
	 */
	public final void setDesc(String inDeviceDescription) {
		//		mDeviceDesc = inDeviceDescription;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#setDeviceType(short)
	 */
	public final void setDeviceType(short inDeviceType) {
		//		mDeviceType = inDeviceType;
	}

}
