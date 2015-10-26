/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlMessage.java,v 1.4 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 * Clear the display
 *
 *  @author huffa
 */
public final class CommandControlClearDisplay extends CommandControlABC {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlClearDisplay.class);
	
	@SuppressWarnings("unused")
	private static final int LENGTH_BYTES = 0;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlClearDisplay(final NetEndpoint inEndpoint) {
		super(inEndpoint, new NetCommandId(CommandControlABC.CLEAR_DISPLAY));
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlClearDisplay() {
		super(new NetCommandId(CommandControlABC.CLEAR_DISPLAY));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return null;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localEncode(com.gadgetworks.bitfields.BitFieldInputStream)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doComputeCommandSize()
	 */
	@Override
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize();
	}

	public String getEntireMessageStr() {
		return null;
	}

	

	// --------------------------------------------------------------------------
	/**
	*  @return
	*/
	public int getResendDelay() {
		return DEFAULT_RESEND_DELAY;
	}
}
