/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDevice.java,v 1.9 2012/01/02 11:43:18 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.persist;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.CommandControlABC;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.model.dao.WirelessDeviceDao;

// --------------------------------------------------------------------------
/**
 * WirelessDevice
 * 
 * A WirelessDevice is a base class that holds information and behavior that's common to all devices
 * attached to the network.
 * 
 * @author jeffw
 */

@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "DTYPE", discriminatorType = DiscriminatorType.STRING)
//@MappedSuperclass
@Entity
//@Data
@Table(name = "WIRELESSDEVICE")
@DiscriminatorValue("ABC")
public class WirelessDevice extends PersistABC implements INetworkDevice {

	public static final int									MAC_ADDR_BYTES		= 8;
	public static final int									PUBLIC_KEY_BYTES	= 8;

	public static final WirelessDeviceDao<WirelessDevice>	DAO					= new WirelessDeviceDao<WirelessDevice>(WirelessDevice.class);

	private static final long								serialVersionUID	= 2371198193026330676L;

	private static final Log								LOGGER				= LogFactory.getLog(WirelessDevice.class);

	@Column(nullable = false)
	private byte[]											macAddress;
	@Column(nullable = false)
	private String											publicKey;
	// The description.
	@Column
	private String											description;
	// The network address last assigned to this wireless device.
	@Column
	@Getter
	@Setter
	private byte[]											networkAddress;
	// The last seen battery level.
	@Column
	private short											lastBatteryLevel;
	//@Transient
	@Enumerated(value = EnumType.STRING)
	@Column
	private NetworkDeviceStateEnum							networkDeviceStatus;
	//@Transient
	@Column
	private Long											lastContactTime;

	@Transient
	private short											expectedEndpointCount;
	@Transient
	private Map<String, String>								KVPMap;
	@Transient
	private short											expectedKVPCount;

	//	@Transient
	//	private String						mDeviceDesc;
	//	@Transient
	//	private short						mDeviceType;

	public WirelessDevice() {
		macAddress = new byte[NetMacAddress.NET_MACADDR_BYTES];
		publicKey = "";
		description = "";
		lastBatteryLevel = 0;
		KVPMap = new HashMap<String, String>();
	}

	public final NetMacAddress getMacAddress() {
		return new NetMacAddress(macAddress);
	}

	public final void setMacAddress(NetMacAddress inMacAddress) {
		macAddress = inMacAddress.getParamValueAsByteArray();
	}

	public final String getPublicKey() {
		return publicKey;
	}

	public final void setPublicKey(String inPublicKey) {
		publicKey = inPublicKey;
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(String inDescription) {
		description = inDescription;
	}

	public final void setNetAddress(NetAddress inNetworkAddress) {
		networkAddress = inNetworkAddress.getParamValueAsByteArray();
	}

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

	public final void setLastContactTime(long inContactTime) {
		lastContactTime = inContactTime;
	}

	public final short getLastBatteryLevel() {
		return lastBatteryLevel;
	}

	public final void setLastBatteryLevel(short inLastBatteryLevel) {
		lastBatteryLevel = inLastBatteryLevel;
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

	public void buttonCommandReceived(byte inButtonNumberPressed, byte inButtonFunction) {
		// See above note.
	}

	public void controlCommandReceived(CommandControlABC inCommandControl) {
		// See above note.
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#addKeyValuePair(java.lang.String, java.lang.String)
	 */
	public final void addKeyValuePair(String inKey, String inValue) {
		KVPMap.put(inKey, inValue);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getExpectedKVPCount()
	 */
	public final short getExpectedKVPCount() {
		return expectedKVPCount;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getStoredKVPCount()
	 */
	public final short getStoredKVPCount() {
		return (short) KVPMap.size();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#setKVPCount(short)
	 */
	public final void setKVPCount(short inKVPCount) {
		expectedKVPCount = inKVPCount;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getHWDesc()
	 */
	public final String getHWDesc() {
		String result;
		result = KVPMap.get(INetworkDevice.HW_VERSION_KEY);
		if (result == null) {
			result = "unknown";
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getSWRevision()
	 */
	public final String getSWRevision() {
		String result;
		result = KVPMap.get(INetworkDevice.SW_VERSION_KEY);
		if (result == null) {
			result = "unknown";
		}
		return result;
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
