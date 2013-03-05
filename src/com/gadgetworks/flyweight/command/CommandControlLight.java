/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlLight.java,v 1.4 2013/03/05 00:05:01 jeffw Exp $
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
 *  A string command from the remote.
 *  
 *  @author jeffw
 */
public final class CommandControlLight extends CommandControlABC {

	public static final Short	POSITION_ALL		= 0;
	public static final Short	CHANNEL_ALL			= 0;
	public static final Short	CHANNEL1			= 1;
	public static final Short	CHANNEL2			= 2;
	public static final Short	CHANNEL3			= 3;
	public static final Short	CHANNEL4			= 4;

	public static final String	EFFECT_SOLID		= "SOLID";
	public static final String	EFFECT_FLASH		= "FLASH";
	public static final String	EFFECT_DIRECT		= "DIRECT";

	private static final Logger	LOGGER				= LoggerFactory.getLogger(CommandControlLight.class);

	private static final int	NUMBER_OF_SHORTS	= 2;
	private static final int	BITS_PER_BYTE		= 8;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Short				mChannel;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Short				mPosition;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private ColorEnum			mColor;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mEffect;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlLight(final NetEndpoint inEndpoint, final Short inChannel, final Short inPosition, final ColorEnum inColor, final String inEffect) {
		super(inEndpoint, new NetCommandId(CommandControlABC.LIGHT));

		mChannel = inChannel;
		mPosition = inPosition;
		mColor = inColor;
		mEffect = inEffect;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlLight() {
		super(new NetCommandId(CommandControlABC.LIGHT));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return "Light: channel:" + mChannel + " pos: " + mPosition + " color: " + mColor;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeShort(mChannel);
			inOutputStream.writeShort(mPosition);
			inOutputStream.writePString(mColor.getName());
			inOutputStream.writePString(mEffect);
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
			mChannel = inInputStream.readShort();
			mPosition = inInputStream.readShort();
			mColor = ColorEnum.valueOf(inInputStream.readPString());
			mEffect = inInputStream.readPString();
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
		return super.doComputeCommandSize() + mColor.getName().length() + mEffect.length() + (NUMBER_OF_SHORTS * (Short.SIZE / BITS_PER_BYTE));
	}

}
