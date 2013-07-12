/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlMessage.java,v 1.4 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  A display message command from the remote.
 *  
 *  pS - line 1 message
 *  pS - line 2 message
 *
 *	}

 *  @author jeffw
 */
public final class CommandControlMessage extends CommandControlABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlMessage.class);

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mLine1MessageStr;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mLine2MessageStr;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlMessage(final NetEndpoint inEndpoint, final String inLine1MessageStr, final String inLine2MessageStr) {
		super(inEndpoint, new NetCommandId(CommandControlABC.MESSAGE));

		mLine1MessageStr = inLine1MessageStr;
		mLine2MessageStr = inLine2MessageStr;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlMessage() {
		super(new NetCommandId(CommandControlABC.MESSAGE));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Message: " + mLine1MessageStr + " (line1) " + mLine2MessageStr + " (line2)";
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writePString(mLine1MessageStr);
			inOutputStream.writePString(mLine2MessageStr);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localEncode(com.gadgetworks.bitfields.BitFieldInputStream)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			mLine1MessageStr = inInputStream.readPString();
			mLine2MessageStr = inInputStream.readPString();
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doComputeCommandSize()
	 */
	@Override
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + mLine1MessageStr.length();
	}

}
