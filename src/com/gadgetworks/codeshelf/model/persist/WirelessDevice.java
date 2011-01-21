/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: WirelessDevice.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.CommandControlABC;
import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkDeviceStateEnum;

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
@Table(name = "WIRELESSDEVICE")
@DiscriminatorValue("ABC")
public class WirelessDevice extends PersistABC implements INetworkDevice {

	public static final int				GUID_BYTES			= 8;
	public static final int				PUBLIC_KEY_BYTES	= 8;

	private static final long			serialVersionUID	= 2371198193026330676L;

	private static final Log			LOGGER				= LogFactory.getLog(WirelessDevice.class);

	//	The owning account.
	//	@Column(nullable = false)
	//	@ManyToOne(optional = false)
	//	@Transient
	//	private Account					mParentAccount;
	@Column(nullable = false)
	private String						mGUID;
	@Column(nullable = false)
	private String						mPublicKey;
	// The description.
	private String						mDescription;
	// The network address last assigned to this wireless device.
	private byte[]						mNetworkAddress;
	// The last seen battery level.
	private short						mLastBatteryLevel;
	//@Transient
	@Enumerated(value = EnumType.STRING)
	private NetworkDeviceStateEnum		mNetworkDeviceStatus;
	//@Transient
	private Long						mLastContactTime;

	@Transient
	private short						mExpectedEndpointCount;
	@Transient
	private Map<String, String>			mKVPMap;
	@Transient
	private short						mExpectedKVPCount;

	//	@Transient
	//	private String						mDeviceDesc;
	//	@Transient
	//	private short						mDeviceType;

	public WirelessDevice() {
		mGUID = "";
		mPublicKey = "";
		mDescription = "";
		mLastBatteryLevel = 0;
		mKVPMap = new HashMap<String, String>();
	}

	public final String getGUID() {
		return mGUID;
	}

	public final void setGUID(String inGUID) {
		mGUID = inGUID;
	}

	public final String getPublicKey() {
		return mPublicKey;
	}

	public final void setPublicKey(String inPublicKey) {
		mPublicKey = inPublicKey;
	}

	public final String getDescription() {
		return mDescription;
	}

	public final void setDescription(String inDescription) {
		mDescription = inDescription;
	}

	public final void setNetAddress(NetAddress inNetworkAddress) {
		mNetworkAddress = inNetworkAddress.getParamValue();
	}

	public final NetAddress getNetAddress() {
		return new NetAddress(mNetworkAddress);
	}

	public final boolean doesMatch(String inGUID) {
		return mGUID.equals(inGUID);
	}

	public final long getLastContactTime() {
		long result = 0;
		Long longVal = mLastContactTime;
		if (longVal != null) {
			result = longVal.longValue();
		}
		return result;
	}

	public final void setLastContactTime(long inContactTime) {
		mLastContactTime = inContactTime;
	}

	public final short getLastBatteryLevel() {
		return mLastBatteryLevel;
	}

	public final void setLastBatteryLevel(short inLastBatteryLevel) {
		mLastBatteryLevel = inLastBatteryLevel;
	}

	public final NetworkDeviceStateEnum getNetworkDeviceState() {
		NetworkDeviceStateEnum result = mNetworkDeviceStatus;
		if (result == null) {
			result = NetworkDeviceStateEnum.getNetworkDeviceStateEnum(0); //INVALID;
		}
		return result;
	}

	public final void setNetworkDeviceState(NetworkDeviceStateEnum inState) {
		LOGGER.debug(mGUID + " state changed: " + mNetworkDeviceStatus + "->" + inState);
		mNetworkDeviceStatus = inState;
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
	public void addKeyValuePair(String inKey, String inValue) {
		mKVPMap.put(inKey, inValue);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getExpectedKVPCount()
	 */
	public final short getExpectedKVPCount() {
		return mExpectedKVPCount;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getStoredKVPCount()
	 */
	public final short getStoredKVPCount() {
		return (short) mKVPMap.size();
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#setKVPCount(short)
	 */
	public final void setKVPCount(short inKVPCount) {
		mExpectedKVPCount = inKVPCount;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.INetworkDevice#getHWDesc()
	 */
	public final String getHWDesc() {
		String result;
		result = mKVPMap.get(INetworkDevice.HW_VERSION_KEY);
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
		result = mKVPMap.get(INetworkDevice.SW_VERSION_KEY);
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
