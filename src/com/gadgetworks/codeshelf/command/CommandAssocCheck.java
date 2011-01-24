/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocCheck.java,v 1.3 2011/01/24 07:22:42 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetMacAddress;

// --------------------------------------------------------------------------
/**
 *  A command associate check is broadcast by a remote to all controllers on a channel.
 *  Only the controller that manages that remote device (known by its MacAddr) will send an associate ack.
 *  
 *  @author jeffw
 */
public final class CommandAssocCheck extends CommandAssocABC {

	public static final String	BEAN_ID					= "CommandAssocCheck";

	public static final int		DEVICE_VERSION_BYTES	= 1;

	private static final Log	LOGGER					= LogFactory.getLog(CommandAssocCheck.class);

	private byte				mDeviceVersion;
	private byte				mBatteryLevel;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocCheck(final byte inDeviceVersion, final NetMacAddress inRemoteMacAddr, final byte inBatteryLevel) {
		super(CommandIdEnum.ASSOC_CHECK, inRemoteMacAddr);

		mDeviceVersion = inDeviceVersion;
		mBatteryLevel = inBatteryLevel;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor useed to create an associate request command received from the network.
	 */
	public CommandAssocCheck() {
		super(CommandIdEnum.ASSOC_CHECK);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return CommandIdEnum.ASSOC_CHECK + super.doToString() + " batt= " + mBatteryLevel + " ver=" + mDeviceVersion;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);

		// Write the device version.
		inTransport.setParam(mDeviceVersion, 1);
		inTransport.setParam(mBatteryLevel, 2);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		// Read the device version.
		mDeviceVersion = ((Byte) inTransport.getParam(1)).byteValue();
		mBatteryLevel = ((Byte) inTransport.getParam(1)).byteValue();
		if (mBatteryLevel < 0) {
			mBatteryLevel = 0;
		} else if (mBatteryLevel > 100) {
			mBatteryLevel = 100;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getDeviceVersion() {
		return mDeviceVersion;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getBatteryLevel() {
		return mBatteryLevel;
	}

}
