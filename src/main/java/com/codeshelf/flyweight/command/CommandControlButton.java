/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlButton.java,v 1.1 2013/09/04 20:30:05 jeffw Exp $
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
 *  A pick request.
 *  
 *  1B - Position Number
 *  1B - Value
 *
 *	}

 *  @author jeffw
 */
public final class CommandControlButton extends CommandControlABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlButton.class);
	
	private static final Integer	BUTTON_COMMAND_BYTES	= 2;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte				mPosNum;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte				mValue;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlButton(final NetEndpoint inEndpoint, final Byte inPosNum, final Byte inValue) {
		super(inEndpoint, new NetCommandId(CommandControlABC.BUTTON));

		mPosNum = inPosNum;
		mValue = inValue;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlButton() {
		super(new NetCommandId(CommandControlABC.BUTTON));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Button: pos: " + mPosNum + " qty:" + mValue;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mPosNum);
			inOutputStream.writeByte(mValue);
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
			mValue = inInputStream.readByte();
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
