/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtSetup.java,v 1.3 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetworkId;

// --------------------------------------------------------------------------
/**
 *  Once the controller has decided what channel it wants to use for the new network it sends the net-setup
 *  command.  The gateway (dongle) acts on it, and does not send this to the air.  The gateway (dongle) switches 
 *  to the specified channel and stores that channel in its NVRAM.  Since the gateway (dongle) just passes 
 *  traffic between the radio and serial links, it can be pretty dumb, and hence more reliable.  The only state 
 *  information that dongle needs is the network's channel.  This way if the gateway (dongle) crashes 
 *  and restarts it simply changes to the channel stored in NVRAM and starts exchanging packets again.  
 *  This reduced complexity ensures that all CPU power goes to exchanging packets: servicing the radio 
 *  and serial links.
 *  
 *  @author jeffw
 */
public final class CommandNetMgmtSetup extends CommandNetMgmtABC {

	public static final String	BEAN_ID	= "CommandNetSetup";

	private static final Log	LOGGER	= LogFactory.getLog(CommandNetMgmtSetup.class);

	private byte				mChannelNumber;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtSetup(final NetworkId inNetworkId, final byte inChannelNumber) {
		super(CommandIdEnum.NET_SETUP);

		mChannelNumber = inChannelNumber;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a wake command received from the network.
	 */
	public CommandNetMgmtSetup() {
		super(CommandIdEnum.NET_SETUP);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {

		String resultStr = CommandIdEnum.NET_SETUP + " channel=" + mChannelNumber;

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);

		// Write the channel requested.
		inTransport.setNextParam(mChannelNumber);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		// Read the channel selected.
		mChannelNumber = ((Byte) inTransport.getParam(1)).byteValue();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getChannelNumber() {
		return mChannelNumber;
	}
}
