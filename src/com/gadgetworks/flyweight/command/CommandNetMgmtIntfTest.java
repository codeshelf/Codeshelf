/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtIntfTest.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  This command is sent from the controller to the gateway (dongle), and then
 *  the gateway (dongle) sends back a corresponding nettest.
 *  
 *  @author jeffw
 */
public final class CommandNetMgmtIntfTest extends CommandNetMgmtABC {

	public static final String	BEAN_ID				= "CommandNetMgmtIntfTest";
	private static final Log	LOGGER				= LogFactory.getLog(CommandNetMgmtIntfTest.class);

	private static final byte	TEST_COMMAND_BYTES	= 1;

	private byte				mTestNumber;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtIntfTest(final byte inTestNumber) {
		super(new NetCommandId(NETINTFTEST_COMMAND));

		mTestNumber = inTestNumber;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a wake command received from the network.
	 */
	public CommandNetMgmtIntfTest() {
		super(new NetCommandId(NETINTFTEST_COMMAND));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {

		String resultStr = "NetTest number=" + mTestNumber;

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			// Write the channel requested.
			inOutputStream.writeByte(mTestNumber);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			// Read the channel selected.
			mTestNumber = inInputStream.readByte();

		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + TEST_COMMAND_BYTES;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getTestNumber() {
		return mTestNumber;
	}
}
