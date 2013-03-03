/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: LighterDeviceABC.java,v 1.1 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;

/**
 * @author jeffw
 *
 */
public abstract class LighterDeviceABC implements INetworkDevice {

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
	protected ICsDeviceManager		mDeviceManager;

	public LighterDeviceABC(final NetGuid inGuid, final ICsDeviceManager inDeviceManager, final IRadioController inRadioController) {
		mGuid = inGuid;
		mDeviceManager = inDeviceManager;
		mRadioController = inRadioController;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.INetworkDevice#doesMatch(com.gadgetworks.flyweight.command.NetGuid)
	 */
	@Override
	public final boolean doesMatch(NetGuid inGuid) {
		return ((getGuid() != null) && (getGuid().equals(inGuid)));
	}
}
