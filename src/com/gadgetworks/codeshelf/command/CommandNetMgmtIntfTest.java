/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtIntfTest.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  This command is sent from the controller to the gateway (dongle), and then
 *  the gateway (dongle) sends back a corresponding nettest.
 *  
 *  @author jeffw
 */
public final class CommandNetMgmtIntfTest extends CommandNetMgmtABC {

	public static final String	BEAN_ID	= "CommandNetMgmtIntfTest";

	private static final Log	LOGGER	= LogFactory.getLog(CommandNetMgmtIntfTest.class);

	private byte				mTestNumber;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtIntfTest(final byte inTestNumber) {
		super(CommandIdEnum.NET_TEST);

		mTestNumber = inTestNumber;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a wake command received from the network.
	 */
	public CommandNetMgmtIntfTest() {
		super(CommandIdEnum.NET_TEST);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {

		String resultStr = CommandIdEnum.NET_TEST + " number=" + mTestNumber;

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);

		inTransport.setParam(mTestNumber, 1);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		// Read the channel selected.
		mTestNumber = ((Byte) inTransport.getParam(1)).byteValue();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getTestNumber() {
		return mTestNumber;
	}
}
