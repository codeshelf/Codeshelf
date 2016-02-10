package com.codeshelf.flyweight.command;


import java.io.IOException;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class CommandControlPosconLedBroadcast extends CommandControlABC{
	
	private static final Logger			LOGGER					= LoggerFactory.getLogger(CommandControlSetPosController.class);
	
	public static final int		EXCLUDE_MAP_BYTES_LEN			= 32;
	public static final byte	BLINK							= 0;
	public static final byte	STEADY							= 1;

	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mRedValue						= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mGreenValue						= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mBlueValue						= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mLightStyle						= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mMapBytesLen					= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private BitSet				mExcludeMap 					= null;
	
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlPosconLedBroadcast(final NetEndpoint inEndpoint,
		byte inRed, byte inGreen, byte inBlue, byte inLightStyle, BitSet inExcludeMap) {
		super(inEndpoint, new NetCommandId(CommandControlABC.POSCON_LED_BROADCAST));
		setRedValue(inRed);
		setGreenValue(inGreen);
		setBlueValue(inBlue);
		setLightStyle(inLightStyle);
		setExcludeMap(inExcludeMap);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlPosconLedBroadcast() {
		super(new NetCommandId(CommandControlABC.POSCON_LED_BROADCAST));
	}

	@Override
	protected String doToString() {
		return null;
	}
	
	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);
		
		byte[] bytes = null;
		
		if (mExcludeMap!= null) {
			bytes = mExcludeMap.toByteArray();
		}
		
		try {
			inOutputStream.writeByte(mRedValue);
			inOutputStream.writeByte(mGreenValue);
			inOutputStream.writeByte(mBlueValue);
			inOutputStream.writeByte(mLightStyle);
			if (bytes != null) {
				inOutputStream.writeByte((byte) bytes.length);
				inOutputStream.writeBytes(bytes, bytes.length);
			} else {
				inOutputStream.writeByte((byte) 0);
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
			mRedValue = inInputStream.readByte();
			mGreenValue = inInputStream.readByte();
			mBlueValue = inInputStream.readByte();
			mMapBytesLen = inInputStream.readByte();
			mLightStyle = inInputStream.readByte();
			
			if (mMapBytesLen > 0) {
				byte[] bytes = new byte[mMapBytesLen];
				inInputStream.readBytes(bytes, (int) mMapBytesLen);
				mExcludeMap = BitSet.valueOf(bytes);
			}
			
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}
	

	// --------------------------------------------------------------------------
	/**
	*  @return
	*/
	public int getResendDelay() {
		return DEFAULT_RESEND_DELAY;
	}

}