/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlRequestQty.java,v 1.1 2013/09/05 03:26:03 jeffw Exp $
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
 *  Clear position controller.
 *  
 *  1B - Position Number
 *
 *	}

 *  @author jeffw
 */
public final class CommandControlClearPosController extends CommandControlABC {

	private static final Logger		LOGGER					= LoggerFactory.getLogger(CommandControlClearPosController.class);

	private static final Integer	REQUEST_COMMAND_BYTES	= 1;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte					mPosNum;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlClearPosController(final NetEndpoint inEndpoint,
		final Byte inPosNum) {
		super(inEndpoint, new NetCommandId(CommandControlABC.CLR_POSCONTROLLER));

		mPosNum = inPosNum;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlClearPosController() {
		super(new NetCommandId(CommandControlABC.SET_POSCONTROLLER));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Clear pos controller: pos: " + mPosNum;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mPosNum);
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
			mPosNum = inInputStream.readByte();
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
		return super.doComputeCommandSize() + REQUEST_COMMAND_BYTES;
	}

}
