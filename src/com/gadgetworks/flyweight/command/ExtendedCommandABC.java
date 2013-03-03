/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: ExtendedCommandABC.java,v 1.2 2013/03/03 23:27:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;
import com.gadgetworks.flyweight.bitfields.NBitInteger;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public abstract class ExtendedCommandABC extends CommandABC {

	private static final Logger	LOGGER						= LoggerFactory.getLogger(ExtendedCommandABC.class);

	private static final int	EXTENDED_COMMAND_HDR_BYTES	= 1;

	private NetCommandId		mExtendedCommandID;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new extended command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public ExtendedCommandABC(final NetEndpoint inEndpoint, final NetCommandId inExtendedCommandID) {
		super(inEndpoint);

		mExtendedCommandID = inExtendedCommandID;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create an extended  command received from the network.
	 */
	public ExtendedCommandABC(final NetCommandId inExtendedCommandID) {
		super();

		mExtendedCommandID = inExtendedCommandID;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create an extended  command received from the network.
	 */
	public ExtendedCommandABC() {
		super();

		mExtendedCommandID = new NetCommandId(NBitInteger.INIT_VALUE);
	}

	// --------------------------------------------------------------------------
	/**
	 *  The parent method that controls the reading of a command from the input stream.
	 *  @param inEndpoint	The endpoint of the command (read from the input stream in the factory).
	 *  @param inBitFieldInputStream	The input stream to read.
	 *  @param inCommandByteCount	The number of bytes to read from the input stream.
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {

		// This byte will have already been read in the command factory.
		//		try {
		//			mExtendedCommandID = new NetCommandId(0);
		//			inBitFieldInputStream.readNBitInteger(mExtendedCommandID);
		//		} catch (IOException e) {
		//			LOGGER.error("", e);
		//		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  The parent method that controls the writing of a command to the output stream.
	 *  @param inBitFieldOutputStream	The output stream to write.
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {

		try {
			inOutputStream.writeNBitInteger(mExtendedCommandID);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public final NetCommandId getExtendedCommandID() {
		return mExtendedCommandID;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return EXTENDED_COMMAND_HDR_BYTES;
	}

}
