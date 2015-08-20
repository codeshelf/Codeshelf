/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocCheck.java,v 1.2 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  A command associate check is broadcast by a remote to all controllers on a channel.
 *  Only the controller that manages that remote device (known by its GUID) will send an associate ack.
 *  
 *  @author jeffw
 */
public final class CommandAssocCheck extends CommandAssocABC {

	public static final int		DEVICE_VERSION_BYTES	= 3;

	private static final Logger	LOGGER					= LoggerFactory.getLogger(CommandAssocCheck.class);

	private byte				mBatteryLevel;
	private byte				mRestartCause;
	private byte				mRestartData;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocCheck(final byte inDeviceVersion, final String inRemoteGUID, final byte inBatteryLevel) {
		super(new NetCommandId(ASSOC_CHECK_COMMAND), inRemoteGUID);
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
	@Override
	public String doToString() {
		return Integer.toHexString(ASSOC_CHECK_COMMAND) + " CHECK" + super.doToString() + " batt= " + mBatteryLevel + 
				"reboot= " + mRestartCause;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	@Override
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mBatteryLevel);
			inOutputStream.writeByte(mRestartCause);
			inOutputStream.writeByte(mRestartData);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	@Override
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			mBatteryLevel = inInputStream.readByte();
			mRestartCause = inInputStream.readByte();
			mRestartData = inInputStream.readByte();
			
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
	@Override
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + DEVICE_VERSION_BYTES;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getBatteryLevel() {
		return mBatteryLevel;
	}

	public byte getRestartCause() {
		return mRestartCause;
	}
	
	public byte getRestartData() {
		return mRestartData;
	}
}
