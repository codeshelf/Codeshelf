package com.codeshelf.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

//--------------------------------------------------------------------------
/**
*  A pick request.
*  
*  1B - Acknowledgement number
*  @author huffa
*/

public class CommandControlAck extends CommandControlABC {
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlAck.class);
	
	private static final Integer	ACK_COMMAND_BYTES	= 2;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte	mAckNum = 0;
	
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Byte	mLQI = 0;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlAck(final NetEndpoint inEndpoint, final Byte inAckNum) {
		super(inEndpoint, new NetCommandId(CommandControlABC.ACK));
		mAckNum = inAckNum;
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlAck() {
		super(new NetCommandId(CommandControlABC.ACK));
	}


	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	@Override
	protected String doToString() {
		return "ack num: " + mAckNum + " LQI: " + getLQI();
	}
	
	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mAckNum);
			inOutputStream.writeByte(mLQI);
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
			mAckNum = inInputStream.readByte();
			mLQI = inInputStream.readByte();
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
		return super.doComputeCommandSize() + ACK_COMMAND_BYTES;
	}
}
