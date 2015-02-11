/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtSetup.java,v 1.2 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

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

	private static final Logger	LOGGER				= LoggerFactory.getLogger(CommandNetMgmtSetup.class);

	private static final byte	SETUP_COMMAND_BYTES	= 1;

	private byte				mChannelNumber;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtSetup(final NetworkId inNetworkId, final byte inChannelNumber) {
		super(new NetCommandId(NETSETUP_COMMAND));

		mChannelNumber = inChannelNumber;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a wake command received from the network.
	 */
	public CommandNetMgmtSetup() {
		super(new NetCommandId(NETSETUP_COMMAND));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {

		String resultStr = "NetSetup channel=" + mChannelNumber;

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			// Write the channel requested.
			inOutputStream.writeByte(mChannelNumber);
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
			// Read the channel selected.
			mChannelNumber = inInputStream.readByte();

		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + SETUP_COMMAND_BYTES;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getChannelNumber() {
		return mChannelNumber;
	}
}
