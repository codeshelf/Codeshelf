/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocReq.java,v 1.1 2011/01/21 01:08:20 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  A command associate request is broadcast by a remote to all controllers on a channel.
 *  Only the controller that manages that remote device (known by its GUID) will send an associate response.
 *  
 *  @author jeffw
 */
public final class CommandAssocReq extends CommandAssocABC {

	public static final String	BEAN_ID	= "CommandAssocReq";

	private static final Log	LOGGER	= LogFactory.getLog(CommandAssocReq.class);
	private byte				mDeviceVersion;
	private byte				mSystemStatus;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocReq(final byte inDeviceVersion, final byte inSystemStatus, final String inRemoteGUID) {
		super(CommandIdEnum.ASSOC_REQ, inRemoteGUID);

		mDeviceVersion = inDeviceVersion;
		mSystemStatus = inSystemStatus;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor useed to create an associate request command received from the network.
	 */
	public CommandAssocReq() {
		super(CommandIdEnum.ASSOC_REQ);
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
		return CommandIdEnum.ASSOC_REQ + super.doToString() + " ver=" + mDeviceVersion + " sys stat=" + statusStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);

		// Write the device version.
		inTransport.setParam(Byte.toString(mDeviceVersion), 1);
		inTransport.setParam(Byte.toString(mSystemStatus), 2);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		// Read the device version.
		mDeviceVersion = ((Byte) inTransport.getParam(1)).byteValue();
		mSystemStatus = ((Byte) inTransport.getParam(2)).byteValue();
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
