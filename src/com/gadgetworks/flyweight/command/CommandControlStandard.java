/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlStandard.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  The button control sub-command allows the remote to indicate that the user pressed a button.
 *  
 *  The data of the command is a single, 8-bit integer representing the button number pressed.
 *  
 *  @author jeffw
 */
public final class CommandControlStandard extends CommandControlABC {

	public static final int		BUTTON_COMMAND_BYTES	= 2;

	private static final Log	LOGGER					= LogFactory.getLog(CommandControlStandard.class);

	private String				mCommandString;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlStandard(final NetEndpoint inEndpoint, final String inCommandString) {
		super(inEndpoint, new NetCommandId(CommandControlABC.STANDARD));

		mCommandString = inCommandString;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlStandard() {
		super(new NetCommandId(CommandControlABC.STANDARD));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Contents: " + mCommandString;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writePString(mCommandString);
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
			mCommandString = inInputStream.readPString();
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
		return super.doComputeCommandSize() + BUTTON_COMMAND_BYTES;
	}

}
