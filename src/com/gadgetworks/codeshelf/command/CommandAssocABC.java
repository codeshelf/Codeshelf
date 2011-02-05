/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocABC.java,v 1.4 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetMacAddress;

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

	private static final Log	LOGGER	= LogFactory.getLog(CommandAssocABC.class);

	private NetMacAddress		mMacAddr;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new associate request command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandAssocABC(final CommandIdEnum inCommandID, final NetMacAddress inRemoteMacAddr) {
		super(inCommandID);

		mMacAddr = inRemoteMacAddr;
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

		if (mMacAddr != null) {
			resultStr = " id=" + mMacAddr;
		}

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {

		// Write the MacAddr.
		inTransport.setParam(mMacAddr, 1);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {

		// Read the unique ID.
		mMacAddr = new NetMacAddress((String) inTransport.getParam(1));
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final NetMacAddress getMacAddr() {
		return mMacAddr;
	}
}
