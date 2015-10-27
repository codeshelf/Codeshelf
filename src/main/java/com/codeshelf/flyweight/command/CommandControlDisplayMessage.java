/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlMessage.java,v 1.4 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import java.io.IOException;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  A display message command from the remote.
 *  
 *  pS - line 1 message
 *  pS - line 2 message
 *  pS - line 3 message
 *  pS - line 4 message
 *
 *	}

 *  @author jeffw
 */
public final class CommandControlDisplayMessage extends CommandControlABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlDisplayMessage.class);
	
	private static final int LENGTH_BYTES = 4;
	private static final int MS_RESEND_DELAY_SCREEN_MSG = 100;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mLine1MessageStr;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mLine2MessageStr;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mLine3MessageStr;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mLine4MessageStr;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlDisplayMessage(final NetEndpoint inEndpoint,
		final String inLine1MessageStr,
		final String inLine2MessageStr,
		final String inLine3MessageStr,
		final String inLine4MessageStr) {
		super(inEndpoint, new NetCommandId(CommandControlABC.MESSAGE));

		mLine1MessageStr = inLine1MessageStr;
		mLine2MessageStr = inLine2MessageStr;
		mLine3MessageStr = inLine3MessageStr;
		mLine4MessageStr = inLine4MessageStr;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlDisplayMessage() {
		super(new NetCommandId(CommandControlABC.MESSAGE));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Message: 1: " + mLine1MessageStr + " 2: " + mLine2MessageStr + " 3: " + mLine3MessageStr + " 4: " + mLine4MessageStr;
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
			inOutputStream.writePString(mLine3MessageStr);
			inOutputStream.writePString(mLine4MessageStr);
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
			mLine3MessageStr = inInputStream.readPString();
			mLine4MessageStr = inInputStream.readPString();
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
		return super.doComputeCommandSize() + LENGTH_BYTES + mLine1MessageStr.length() + mLine2MessageStr.length() + mLine3MessageStr.length() + mLine4MessageStr.length();
	}

	public String getEntireMessageStr() {
		StringBuilder builder = new StringBuilder(getLine1MessageStr()).append(System.lineSeparator())
				.append(getLine2MessageStr()).append(System.lineSeparator())
				.append(getLine3MessageStr()).append(System.lineSeparator())
				.append(getLine4MessageStr()).append(System.lineSeparator());
		return builder.toString();
	}
	

	// --------------------------------------------------------------------------
	/**
	*  @return
	*/
	public int getResendDelay() {
		return MS_RESEND_DELAY_SCREEN_MSG;
	}

}
