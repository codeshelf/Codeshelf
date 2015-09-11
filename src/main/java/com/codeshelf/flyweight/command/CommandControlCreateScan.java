package com.codeshelf.flyweight.command;

//--------------------------------------------------------------------------
/**
*  A automated scan creation request
*  
*  pS - Scan String to return
*
*	}

*  @author huffa
*/

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class CommandControlCreateScan extends CommandControlABC {
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandControlDisplayMessage.class);
	
	private int LENGTH_BYTES = 4;
	
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String				mScanString;
	
	
	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlCreateScan(final NetEndpoint inEndpoint,
		final String inScanString) {
		super(inEndpoint, new NetCommandId(CommandControlABC.CREATE_SCAN));
		
		mScanString = inScanString;
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlCreateScan() {
		super(new NetCommandId(CommandControlABC.CREATE_SCAN));
	}


	@Override
	protected String doToString() {
		return "Command String: " + mScanString;
	}
	

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writePString(mScanString);
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
			mScanString= inInputStream.readPString();
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
		return super.doComputeCommandSize() + LENGTH_BYTES + mScanString.length();
	}

}
