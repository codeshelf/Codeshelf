/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocABC.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.INetworkDevice;
import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.OutOfRangeException;

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
public abstract class CommandAssocABC extends CommandABC {

	private static final Log	LOGGER				= LogFactory.getLog(CommandAssocABC.class);

	private String				mGUID;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocABC(final CommandIdEnum inCommandID, final String inRemoteGUID) {
		super(inCommandID);

		if (inRemoteGUID.length() != INetworkDevice.UNIQUEID_BYTES)
			throw new OutOfRangeException("GUID is the wrong size");
		mGUID = inRemoteGUID;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create an associate request command received from the network.
	 */
	public CommandAssocABC(final CommandIdEnum inExtendedCommandID) {
		super(inExtendedCommandID);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandGroupEnum() {
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
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);
		// Write the GUID.
		inTransport.setParam(mGUID, 1);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);
		// Read the unique ID.
		mGUID = (String) inTransport.getParam(1);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final String getGUID() {
		return mGUID;
	}
}
