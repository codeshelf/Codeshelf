/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocABC.java,v 1.3 2013/05/26 21:50:39 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;
import com.gadgetworks.flyweight.bitfields.OutOfRangeException;

//--------------------------------------------------------------------------
/**
 *  The associate  command is used to attach devices to a controller running a network.
 *  
 *  There are four sub-command types:
 *  
 *  Associate request - when the device first comes up it tries to associate with a controller/gateway.
 *  Associate response - when the controller/gateway receives a valid associate request command we respond when associate succeeds.
 *  Associate check - the remote checks with the controller/gateway to make sure there is still a network and it is assoicated to it.
 *  Associate ack command - the controller/gateway responds to valid associate check commands.
 *  
 *  The main associate command is an abstract class.  There is a concrete class for each sub-command type.
 *  
 *  @author jeffw
 */
public abstract class CommandAssocABC extends ExtendedCommandABC {

	public static final byte	ASSOC_REQ_COMMAND	= 0;
	public static final byte	ASSOC_RESP_COMMAND	= 1;
	public static final byte	ASSOC_CHECK_COMMAND	= 2;
	public static final byte	ASSOC_ACK_COMMAND	= 3;

	private static final Logger	LOGGER				= LoggerFactory.getLogger(CommandAssocABC.class);

	private String				mGUID;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocABC(final NetCommandId inExtendedCommandID, final String inRemoteGUID) {
		super(NetEndpoint.MGMT_ENDPOINT, inExtendedCommandID);

		try {
			if (inRemoteGUID.replace("0x", "").length() != NetGuid.NET_GUID_HEX_CHARS)
				throw new OutOfRangeException("Unique ID is the wrong size");
		} catch (OutOfRangeException e) {
			LOGGER.error("", e);
		}
		mGUID = inRemoteGUID;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create an associate request command received from the network.
	 */
	public CommandAssocABC(final NetCommandId inExtendedCommandID) {
		super(inExtendedCommandID);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandTypeEnum() {
		return CommandGroupEnum.ASSOC;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {

		String resultStr = "";

		if (mGUID != null) {
			resultStr = " id=" + mGUID;
		}

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);
		try {
			// Write the GUID.
			inOutputStream.writeBytes(mGUID.getBytes());
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
			// Read the unique ID.
			byte[] temp = new byte[NetGuid.NET_GUID_HEX_CHARS];
			inInputStream.readBytes(temp, NetGuid.NET_GUID_HEX_CHARS);
			mGUID = new String(temp);

		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return NetGuid.NET_GUID_BYTES + 1;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final String getGUID() {
		return mGUID;
	}
}
