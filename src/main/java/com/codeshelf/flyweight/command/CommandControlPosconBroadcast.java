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

public class CommandControlPosconBroadcast extends CommandControlABC{
	
	private static final Logger			LOGGER					= LoggerFactory.getLogger(CommandControlPosconBroadcast.class);
	
	public static final byte	POSCON_DSP_ADDRESS 		= CommandControlABC.POSCON_DSP_ADDRESS;
	public static final byte	CLR_POSCONTROLLER		= CommandControlABC.CLR_POSCONTROLLER;
	public static final byte	POSCON_DSP_FWVER		= CommandControlABC.POSCON_DSP_FWVER;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mCommandId 						= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mMapBytesLen					= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private BitSet 				mIncludeMap 					= null;
	
	/** 
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlPosconBroadcast(final NetEndpoint inEndpoint, byte commandId, BitSet inBitSet) {
		super(inEndpoint, new NetCommandId(CommandControlABC.POSCON_BROADCAST));
		setCommandId(commandId);
		setIncludeMap(inBitSet);
	}
	
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlPosconBroadcast(final NetEndpoint inEndpoint, byte commandId) {
		super(inEndpoint, new NetCommandId(CommandControlABC.POSCON_BROADCAST));
		setCommandId(commandId);
	}
	
	/**
	 * Sets the inclusive bitmap from a byte array
	 * @param inBytes	The byte array to use for inclusive bitmap
	 */
	public void setIncludeMapBytes(byte[] inBytes) {
		mIncludeMap = BitSet.valueOf(inBytes);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlPosconBroadcast() {
		super(new NetCommandId(CommandControlABC.POSCON_BROADCAST));
	}

	@Override
	protected String doToString() {
		String cmd = "";
		
		switch(mCommandId) {
			case POSCON_DSP_ADDRESS:
				cmd = "POSCON display address cmd";
				break;
			case CLR_POSCONTROLLER:
				cmd = "Clear POSCON cmd";
				break;
			case POSCON_DSP_FWVER:
				cmd = "POSCON display firmware cmd";
				break;
		}
		
		return cmd + " Inclusive bitmap: " + mIncludeMap.toString();
	}
	
	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);
		
		byte[] bytes = null;
		
		if (mIncludeMap!= null) {
			bytes = mIncludeMap.toByteArray();
		}
		
		try {
			inOutputStream.writeByte(mCommandId);
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
			mCommandId = inInputStream.readByte();
			mMapBytesLen = inInputStream.readByte();
			
			if (mMapBytesLen > 0) {
				byte[] bytes = new byte[mMapBytesLen];
				inInputStream.readBytes(bytes, (int) mMapBytesLen);
				mIncludeMap = BitSet.valueOf(bytes);
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
