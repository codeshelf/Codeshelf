/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlScan.java,v 1.2 2013/03/03 23:27:21 jeffw Exp $
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
 *  A string command from the remote.
 *  
 *  @author jeffw
 */
public final class CommandControlScan extends CommandControlABC {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlScan.class);

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mCommandString;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlScan(final NetEndpoint inEndpoint, final String inCommandString) {
		super(inEndpoint, new NetCommandId(CommandControlABC.SCAN));

		mCommandString = inCommandString;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlScan() {
		super(new NetCommandId(CommandControlABC.SCAN));
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
			LOGGER.error("CommandControlScan.doToStream() could not write", e);
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
			LOGGER.warn("CommandControlScan.doFromStream() could not read stream. (Stack trace shown, but this exception is swallowed here.)", e);
			// This can be reproduced by slow unplug/replug of wired scanner.
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doComputeCommandSize()
	 */
	@Override
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + mCommandString.length();
	}


	// --------------------------------------------------------------------------
	/**
	*  @return
	*/
	public int getResendDelay() {
		return DEFAULT_RESEND_DELAY;
	}
}
