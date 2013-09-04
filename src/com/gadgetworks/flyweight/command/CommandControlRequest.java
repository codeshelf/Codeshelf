/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlRequest.java,v 1.1 2013/09/04 20:30:05 jeffw Exp $
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
 *  A pick request.
 *  
 *  1B - Position Number
 *  1B - Requested Value
 *  1B - Min Value
 *  1B - Max Value
 *
 *	}

 *  @author jeffw
 */
public final class CommandControlRequest extends CommandControlABC {

	public static final Byte		POSITION_ALL			= 0;

	private static final Logger		LOGGER					= LoggerFactory.getLogger(CommandControlRequest.class);

	private static final Integer	REQUEST_COMMAND_BYTES	= 4;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte					mPosNum;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte					mReqValue;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte					mMinValue;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte					mMaxValue;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlRequest(final NetEndpoint inEndpoint,
		final Byte inPosNum,
		final Byte inReqValue,
		final Byte inMinValue,
		final Byte inMaxValue) {
		super(inEndpoint, new NetCommandId(CommandControlABC.REQUEST));

		mPosNum = inPosNum;
		mReqValue = inReqValue;
		mMinValue = inMinValue;
		mMaxValue = inMaxValue;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlRequest() {
		super(new NetCommandId(CommandControlABC.REQUEST));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Pick Req: pos: " + mPosNum + " req qty:" + mReqValue + " min: " + mMinValue + " max: " + mMaxValue;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mPosNum);
			inOutputStream.writeByte(mReqValue);
			inOutputStream.writeByte(mMinValue);
			inOutputStream.writeByte(mMaxValue);
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
			mReqValue = inInputStream.readByte();
			mMinValue = inInputStream.readByte();
			mMaxValue = inInputStream.readByte();
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
