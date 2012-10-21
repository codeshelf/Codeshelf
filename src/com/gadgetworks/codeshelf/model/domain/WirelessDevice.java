/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDevice.java,v 1.10 2012/10/21 02:02:17 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.gadgetworks.codeshelf.command.CommandControlABC;
import com.gadgetworks.codeshelf.controller.IDeviceMaintainer;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
//@Table(name = "WIRELESSDEVICE")
//@DiscriminatorValue("ABC")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class WirelessDevice extends DomainObjectABC implements INetworkDevice {

	@Inject
	private static IWirelessDeviceDao	DAO;

	public interface IWirelessDeviceDao extends ITypedDao<WirelessDevice>, IDeviceMaintainer {

		WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr);

		INetworkDevice findNetworkDeviceByMacAddr(NetMacAddress inMacAddr);

		INetworkDevice getNetworkDevice(NetAddress inAddress);

	}

	public static final int			MAC_ADDR_BYTES		= 8;
	public static final int			PUBLIC_KEY_BYTES	= 8;

	private static final Log		LOGGER				= LogFactory.getLog(WirelessDevice.class);

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

	// The network address last assigned to this wireless device.
	@Column(nullable = false)
	@JsonProperty
	private byte[]					networkAddress;

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

	// The owning network.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private ControlGroup			parent;

	@Transient
	@Getter
	@Setter
	@JsonProperty
	private short					expectedEndpointCount;

	@Transient
	@Getter
	@Setter
	@JsonProperty
	private Map<String, String>		kvpMap;

	@Transient
	@Getter
	@Setter
	@JsonProperty
	private short					expectedKvpCount;

	//	@Transient
	//	private String						mDeviceDesc;
	//	@Transient
	//	private short						mDeviceType;

	public WirelessDevice() {
		macAddress = new byte[NetMacAddress.NET_MACADDR_BYTES];
		publicKey = "";
		description = "";
		lastBatteryLevel = 0;
		kvpMap = new HashMap<String, String>();
	}

	public final ITypedDao<WirelessDevice> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "W";
	}

	public final ControlGroup getParentControlGroup() {
		return parent;
	}

	public final void setParentControlGroup(final ControlGroup inControlGroup) {
		parent = inControlGroup;
	}

	public final IDomainObject getParent() {
		return getParentControlGroup();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof ControlGroup) {
			setParentControlGroup((ControlGroup) inParent);
		}
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
		networkAddress = inNetworkAddress.getParamValueAsByteArray();
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
		kvpMap.put(inKey, inValue);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getStoredKVPCount()
	 */
	public final short getStoredKvpCount() {
		return (short) kvpMap.size();
	}
 
	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getHWDesc()
	 */
	public final String getHwDesc() {
		String result;
		result = kvpMap.get(INetworkDevice.HW_VERSION_KEY);
		if (result == null) {
			result = "unknown";
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getSWRevision()
	 */
	public final String getSwRevision() {
		String result;
		result = kvpMap.get(INetworkDevice.SW_VERSION_KEY);
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
