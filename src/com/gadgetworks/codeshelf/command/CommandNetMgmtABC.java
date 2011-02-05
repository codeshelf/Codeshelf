/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtABC.java,v 1.3 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  The network management command is used to build and maintain networks.
 *  
 *  There are two sub-command types:
 *  
 *  Network Setup
 *  Network Check
 *  
 *  The main network management command is an abstract class.  There is a concrete class for each sub-command type.
 *  
 *  
 *  The network mgmt command is a bi-directional command sent directly between the controller and the
 *  gateway (dongle).  The command is never broadcast directly to the air.
 *  
 *  
 *  @author jeffw
 */
public abstract class CommandNetMgmtABC extends CommandABC {

	//	private static final Log	LOGGER					= LogFactory.getLog(CommandNetMgmtABC.class);

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtABC(final CommandIdEnum inCommandID) {
		super(inCommandID);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandGroupEnum() {
		return CommandGroupEnum.NETMGMT;
	}
	
	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToTransport(ITransport inTransport) {

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localEncode(com.gadgetworks.bitfields.BitFieldInputStream)
	 */
	protected void doFromTransport(ITransport inTransport) {

	}
}
