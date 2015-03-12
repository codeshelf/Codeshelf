/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: DeviceLogicABC.java,v 1.1 2013/05/04 00:30:01 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.util.UUID;

import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;

/**
 * @author jeffw
 *
 */
public abstract class DeviceLogicABC implements INetworkDevice {

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short					mHardwareVersion;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short					mFirmwareVersion;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte					mRadioProtocolVersion	= null;

	// PersistentId
	@Accessors(prefix = "m")
	@Getter
	private UUID					mPersistentId;

	// MAC address.
	@Accessors(prefix = "m")
	@Getter
	private NetGuid					mGuid;

	// The CHE's net address.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetAddress				mAddress;

	// The network device state.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetworkDeviceStateEnum	mDeviceStateEnum;

	// The last known battery level.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short					mLastBatteryLevel;

	// The last time we had contact.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private long					mLastContactTime;

	// The controller for this device..
	protected IRadioController		mRadioController;

	// The device manager.
	@Accessors(prefix = "m")
	@Getter
	protected CsDeviceManager		mDeviceManager;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Transient
	private byte					mLastAckId;

	public DeviceLogicABC(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		mPersistentId = inPersistentId;
		mGuid = inGuid;
		mDeviceManager = inDeviceManager;
		mRadioController = inRadioController;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.INetworkDevice#doesMatch(com.codeshelf.flyweight.command.NetGuid)
	 */
	@Override
	public final boolean doesMatch(NetGuid inGuid) {
		return ((getGuid() != null) && (getGuid().equals(inGuid)));
	}

	// --------------------------------------------------------------------------
	/* 
	*/
	@Override
	public boolean isAckIdNew(byte inAckId) {
		int unsignedAckId = inAckId & 0xFF;
		int unsignedLastAckId = mLastAckId & 0xFF;

		if (unsignedAckId > unsignedLastAckId) {
			return true;
		} else if (unsignedLastAckId > 254 && inAckId < 3) {
			//Overflow case. If ACK last ACK is 256 and inAckID is 0, this IS a new device ID but it won't be greater than the last ACK ID.
			//We give a few ids of buffer in case packets are lost around the overflow.
			return true;
		}

		return false;
	}

	// --------------------------------------------------------------------------
	/* Is device fully associated? Only so if mDeviseStateEnum = STARTED.
	 * See RadioController.networkDeviceBecameActive()
	 */
	@Override
	public final boolean isDeviceAssociated() {
		return (mDeviceStateEnum != null && mDeviceStateEnum.equals(NetworkDeviceStateEnum.STARTED));
	}

	// --------------------------------------------------------------------------
	/**
	 * Utility function. Should be promoted, and get a cached value.
	 */
	protected String getMyGuidStr() {
		String thisGuidStr = "";
		NetGuid thisGuid = this.getGuid();
		if (thisGuid != null)
			thisGuidStr = thisGuid.getHexStringNoPrefix();
		return thisGuidStr;
	}
}
