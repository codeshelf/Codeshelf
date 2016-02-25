package com.codeshelf.flyweight.command;


import java.io.IOException;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class CommandControlPosconSetBroadcast extends CommandControlABC{
	
	private static final Logger			LOGGER					= LoggerFactory.getLogger(CommandControlPosconSetBroadcast.class);
	
	private static final int	RESEND_DELAY					= 60;
	public static final int		EXCLUDE_MAP_BYTES_LEN			= 32;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private PosControllerInstr	mPosconInstr					= null;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mMapBytesLen					= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private BitSet				mIncludeMap 					= null;
	
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlPosconSetBroadcast(final NetEndpoint inEndpoint,
		PosControllerInstr inPosconInstr, BitSet inIncludeMap) {
		super(inEndpoint, new NetCommandId(CommandControlABC.POSCON_SET_BROADCAST));
	
		setPosconInstr(inPosconInstr);
		setIncludeMap(inIncludeMap);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlPosconSetBroadcast() {
		super(new NetCommandId(CommandControlABC.POSCON_SET_BROADCAST));
	}

	@Override
	protected String doToString() {
		return "POSCON set broadcast: inclusive bitmap:" + mIncludeMap.toString();
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
			inOutputStream.writeByte(mPosconInstr.getReqQty());
			inOutputStream.writeByte(mPosconInstr.getMinQty());
			inOutputStream.writeByte(mPosconInstr.getMaxQty());
			inOutputStream.writeByte(mPosconInstr.getFreq());
			inOutputStream.writeByte(mPosconInstr.getDutyCycle());
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
			byte position = 0;
			byte reqQty = inInputStream.readByte();
			byte minQty = inInputStream.readByte();
			byte maxQty = inInputStream.readByte();
			byte freq = inInputStream.readByte();
			byte dutyCycle = inInputStream.readByte();
			mPosconInstr = new PosControllerInstr(position, reqQty, minQty, maxQty, freq, dutyCycle);
			
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
		return RESEND_DELAY;
	}

}