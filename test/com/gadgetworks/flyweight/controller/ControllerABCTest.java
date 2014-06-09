/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: ControllerABCTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.flyweight.command.CommandAssocReq;
import com.gadgetworks.flyweight.command.CommandControlDisplayMessage;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetEndpoint;

/** --------------------------------------------------------------------------
 *  Test the controller.
 *  @author jeffw
 */
public abstract class ControllerABCTest {

	private static final byte			TEST_ENDPOINT_NUM	= 0x01;
	private static final byte			SRS_BYTE			= 0x00;
	private static final NetEndpoint	TEST_ENDPOINT		= new NetEndpoint(TEST_ENDPOINT_NUM);
	private static final String			TEST_ID				= "12345678";

	private static final String			TEST_MSG1			= "TEST1";
	private static final String			TEST_MSG2			= "TEST2";
	private static final String			TEST_MSG3			= "TEST3";
	private static final String			TEST_MSG4			= "TEST4";

	private IRadioController			mControllerABC;

	public final void setUp() throws Exception {
		mControllerABC = createControllerABC();
		Assert.assertNotNull("Problem creating ControllerABC instance.", mControllerABC);
	}

	/**
	 * Every test of a concrete implementation must override this to
	 * return an instance of the actual implementation.
	 */
	protected abstract IRadioController createControllerABC() throws Exception;

	/**
	 * Test method for {@link com.gadgetworks.flyweightcontroller.controller.ControllerABC#receiveCommand(com.gadgetworks.controller.CommandABC, com.gadgetworks.flyweightcontroller.command.NetAddress)}.
	 */
	@Test
	public final void testReceiveCommand() {

		//		try {
		NetAddress srcAddress = new NetAddress((byte) 0x04);

		byte[] cmdBytes = { 0x00, 0x01, 0x02, 0x03, 0x04 };
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT, TEST_MSG1, TEST_MSG2, TEST_MSG3, TEST_MSG4);
		mControllerABC.receiveCommand(command, srcAddress);

		CommandAssocReq netCmd = new CommandAssocReq(INetworkDevice.PROTOCOL_VERSION_1, SRS_BYTE, TEST_ID);
		if (netCmd == null) {

		}
		//		} catch (OutOfRangeException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
	}

	//	/**
	//	 * Test method for {@link com.gadgetworks.controller.ControllerABC#sendCommand(com.gadgetworks.controller.CommandABC, 
	//   * com.gadgetworks.controller.NetAddress, com.gadgetworks.controller.NetAddress)}.
	//	 */
	//	@Test
	//	public void testSendCommand() {
	//		fail("Not yet implemented");
	//	}

}
