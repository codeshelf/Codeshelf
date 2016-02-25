/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlRequestQty.java,v 1.1 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import java.io.IOException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  Clear position controller.
 *  
 *  1B - Position Number
 *
 *	}

 *  @author jeffw
 */
@EqualsAndHashCode(callSuper=true, doNotUseGetters=true)
public final class CommandControlPosconDisplayFWVer extends CommandControlABC {

	private static final Logger		LOGGER					= LoggerFactory.getLogger(CommandControlClearPosController.class);

	private static final Integer	REQUEST_COMMAND_BYTES	= 1;
	private static final Integer	RESEND_DELAY			= 60;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte					mPosNum;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlPosconDisplayFWVer(final NetEndpoint inEndpoint,
		final Byte inPosNum) {
		super(inEndpoint, new NetCommandId(CommandControlABC.POSCON_DSP_FWVER));

		mPosNum = inPosNum;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlPosconDisplayFWVer() {
		super(new NetCommandId(CommandControlABC.POSCON_DSP_FWVER));
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
	

	// --------------------------------------------------------------------------
	/**
	*  @return
	*/
	public int getResendDelay() {
		return RESEND_DELAY;
	}
}
