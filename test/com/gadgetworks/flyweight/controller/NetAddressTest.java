/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAddressTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import org.junit.Test;

import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;

/** --------------------------------------------------------------------------
 *  Test the NetAddress calss.
 *  @author jeffw
 */
public final class NetAddressTest {

	/**
	 * Test method for {@link com.gadgetworks.flyweightcontroller.command.NetAddress#NetAddress(int)}.
	 */
	@Test
	public void testNetAddress() {

		@SuppressWarnings("unused")
		NetAddress address;

//		try {
			address = new NetAddress(IPacket.GATEWAY_ADDRESS);
			address = new NetAddress(IPacket.BROADCAST_ADDRESS);
			
			// Expected result.
		//		} catch (OutOfRangeException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//			fail();
		//		}
	}

}
