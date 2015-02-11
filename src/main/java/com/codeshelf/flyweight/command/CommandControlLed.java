/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlLed.java,v 1.8 2013/07/12 21:44:38 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  An LED command from the remote.
 *  
 *  1B - effect
 * 	1B - Channel
 *  1B - sample count
 *  nB - samples {
 *		2B - position
 *		1B - red
 *		1B - green 
 *		1B - blue
 *	}
 *  
 *  @author jeffw
 */
public final class CommandControlLed extends CommandControlABC {

	public static final Short	POSITION_NONE		= -1;
	public static final Short	POSITION_ALL		= 0;
	public static final Short	CHANNEL_ALL			= 0;
	public static final Short	CHANNEL1			= 1;
	public static final Short	CHANNEL2			= 2;
	public static final Short	CHANNEL3			= 3;
	public static final Short	CHANNEL4			= 4;

	private static final Logger	LOGGER				= LoggerFactory.getLogger(CommandControlLed.class);

	private static final int	CHANNEL_BYTES		= 1;
	private static final int	EFFECT_BYTES		= 1;
	private static final int	SAMPLE_COUNT_BYTES	= 1;
	private static final int	ONE_SAMPLE_BYTES	= 4;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Short				mChannel;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private EffectEnum			mEffect;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private List<LedSample>		mSamples;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlLed(final NetEndpoint inEndpoint,
		final Short inChannel,
		final EffectEnum inEffect,
		final List<LedSample> inSampleList) {
		super(inEndpoint, new NetCommandId(CommandControlABC.LIGHT));

		mChannel = inChannel;
		mEffect = inEffect;
		mSamples = inSampleList;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlLed() {
		super(new NetCommandId(CommandControlABC.LIGHT));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		String result = "Light: channel:" + mChannel + " effect:" + mEffect;

		for (LedSample sample : mSamples) {
			result += " pos:" + sample.getPosition() + " color: " + sample.getColor();
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mChannel.byteValue());
			inOutputStream.writeByte(mEffect.getValue());
			inOutputStream.writeByte((byte) mSamples.size());
			for (LedSample sample : mSamples) {
				inOutputStream.writeShort(sample.getPosition());
				inOutputStream.writeBytes(LedSample.convertColorToBytes(sample.getColor()));
			}
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
			mChannel = (short) inInputStream.readByte();
			mEffect = EffectEnum.getEffectEnum(inInputStream.readByte());
			short sampleCount = inInputStream.readByte();
			mSamples = new ArrayList<LedSample>();
			for (int sampleNum = 0; sampleNum < sampleCount; sampleNum++) {
				short position = inInputStream.readShort();
				byte[] colorBytes = new byte[3];
				inInputStream.readBytes(colorBytes, 3);
				LedSample sample = new LedSample(position, LedSample.convertBytesToColor(colorBytes));
				mSamples.add(sample);
			}
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
		return super.doComputeCommandSize() + CHANNEL_BYTES + EFFECT_BYTES + SAMPLE_COUNT_BYTES
				+ (mSamples.size() * ONE_SAMPLE_BYTES);
	}
}
