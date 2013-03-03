/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocReq.java,v 1.2 2013/03/03 23:27:20 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  A command associate request is broadcast by a remote to all controllers on a channel.
 *  Only the controller that manages that remote device (known by its GUID) will send an associate response.
 *  
 *  @author jeffw
 */
public final class CommandAssocReq extends CommandAssocABC {

	public static final int		ASSOC_REQ_BYTES	= 2;

	private static final Logger	LOGGER			= LoggerFactory.getLogger(CommandAssocReq.class);
	private byte				mDeviceVersion;
	private byte				mSystemStatus;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocReq(final byte inDeviceVersion, final byte inSystemStatus, final String inRemoteGUID) {
		super(new NetCommandId(ASSOC_REQ_COMMAND), inRemoteGUID);

		mDeviceVersion = inDeviceVersion;
		mSystemStatus = inSystemStatus;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor useed to create an associate request command received from the network.
	 */
	public CommandAssocReq() {
		super(new NetCommandId(ASSOC_REQ_COMMAND));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		String statusStr = Integer.toBinaryString(mSystemStatus);
		if (statusStr.length() > 8) {
			statusStr = String.copyValueOf(statusStr.toCharArray(), 24, 8);
		}
		return Integer.toHexString(ASSOC_REQ_COMMAND) + " REQ" + super.doToString() + " ver=" + mDeviceVersion + " sys stat=" + statusStr;
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
			inOutputStream.writeByte(mSystemStatus);
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
			mSystemStatus = inInputStream.readByte();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + ASSOC_REQ_BYTES;
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
	public byte getSystemStatus() {
		return mSystemStatus;
	}

}
