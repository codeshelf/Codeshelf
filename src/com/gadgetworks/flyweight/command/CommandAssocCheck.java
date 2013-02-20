/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocCheck.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  A command associate check is broadcast by a remote to all controllers on a channel.
 *  Only the controller that manages that remote device (known by its GUID) will send an associate ack.
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
	public CommandAssocCheck(final byte inDeviceVersion, final String inRemoteGUID, final byte inBatteryLevel) {
		super(new NetCommandId(ASSOC_CHECK_COMMAND), inRemoteGUID);

		mDeviceVersion = inDeviceVersion;
		mBatteryLevel = inBatteryLevel;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor useed to create an associate request command received from the network.
	 */
	public CommandAssocCheck() {
		super(new NetCommandId(ASSOC_CHECK_COMMAND));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return Integer.toHexString(ASSOC_CHECK_COMMAND) + " CHECK" + super.doToString() + " batt= " + mBatteryLevel + " ver=" + mDeviceVersion;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			// Write the device version.
			inOutputStream.writeByte(mDeviceVersion);
			inOutputStream.writeByte(mBatteryLevel);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			// Read the device version.
			mDeviceVersion = inInputStream.readByte();
			mBatteryLevel = inInputStream.readByte();
			if (mBatteryLevel < 0) {
				mBatteryLevel = 0;
			} else if (mBatteryLevel > 100) {
				mBatteryLevel = 100;
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + DEVICE_VERSION_BYTES;
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
