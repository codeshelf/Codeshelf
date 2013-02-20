/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtABC.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

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
public abstract class CommandNetMgmtABC extends ExtendedCommandABC {

	public static final byte	NETSETUP_COMMAND		= 1;
	public static final byte	NETCHECK_COMMAND		= 2;
	public static final byte	NETINTFTEST_COMMAND		= 3;

//	private static final Log	LOGGER					= LogFactory.getLog(CommandNetMgmtABC.class);

	private static final byte	NETMGMT_HEADER_BYTES	= 0;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtABC(final NetCommandId inExtendedCommandID) {
		super(NetEndpoint.MGMT_ENDPOINT, inExtendedCommandID);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a wake command received from the network.
	 */
	//	public CommandNetMgmtABC() {
	//		super();
	//	}
	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandTypeEnum() {
		return CommandGroupEnum.NETMGMT;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		//		try {
		//			// Write the sub-command type.
		//		} catch (IOException e) {
		//			LOGGER.error("", e);
		//		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		//		try {
		//			// Read the channel selected.
		//			mSubCommand = inInputStream.readByte();
		//
		//		} catch (IOException e) {
		//			LOGGER.error("", e);
		//		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return NETMGMT_HEADER_BYTES;
	}

}
