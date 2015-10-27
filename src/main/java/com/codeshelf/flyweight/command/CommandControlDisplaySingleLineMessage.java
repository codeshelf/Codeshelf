/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlDisplaySingleLineMessage.java
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
 *  Display a single line message with specified font at specified location
 *  
 *
 *  @author huffa
 */
public final class CommandControlDisplaySingleLineMessage extends CommandControlABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlDisplaySingleLineMessage.class);
	
	private static final int LENGTH_BYTES = 9;
	private static final int MS_RESEND_DELAY_SINGLE_LINE_MSG = 100;

	//Supported display fonts
	public static final byte	ARIAL16						= 1;
	public static final byte	ARIAL26						= 2;
	public static final byte	BAR3Of9						= 3;
	public static final byte	ARIALMONOBOLD16				= 4;
	public static final byte	ARIALMONOBOLD20				= 5;
	public static final byte	ARIALMONOBOLD24				= 6;
	public static final byte	ARIALMONOBOLD26				= 7;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mLineMessageStr;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private byte				mFontType;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short				mPosX;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short			mPosY;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  Documented at https://codeshelf.atlassian.net/wiki/display/TD/KW2+CHE+Displays
	 */
	public CommandControlDisplaySingleLineMessage(final NetEndpoint inEndpoint,
		final String inLineMessageStr,
		final byte fontType,
		final short posX,
		final short posY) {
		super(inEndpoint, new NetCommandId(CommandControlABC.SINGLE_LINE_MESSAGE));

		mLineMessageStr = inLineMessageStr;
		mFontType = fontType;
		mPosX = posX;
		mPosY = posY;

	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlDisplaySingleLineMessage() {
		super(new NetCommandId(CommandControlABC.SINGLE_LINE_MESSAGE));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Message: " + mLineMessageStr + " Font Type: " + mFontType + " posX: " + mPosX + " posY: " + mPosY;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mFontType);
			inOutputStream.writeShort(mPosX);
			inOutputStream.writeShort(mPosY);
			inOutputStream.writePString(mLineMessageStr);
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
			mLineMessageStr = inInputStream.readPString();
			mFontType = inInputStream.readByte();
			mPosX = inInputStream.readShort();
			mPosY = inInputStream.readShort();
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
		return super.doComputeCommandSize() + LENGTH_BYTES + mLineMessageStr.length();
	}

	public String getEntireMessageStr() {
		StringBuilder builder = new StringBuilder(getLineMessageStr());
		return builder.toString();
	}


	// --------------------------------------------------------------------------
	/**
	*  @return
	*/
	public int getResendDelay() {
		return MS_RESEND_DELAY_SINGLE_LINE_MSG;
	}
}
