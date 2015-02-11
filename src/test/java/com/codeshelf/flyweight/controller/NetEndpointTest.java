/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: NetEndpointTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import org.junit.Test;

import com.codeshelf.flyweight.command.NetEndpoint;

/** --------------------------------------------------------------------------
 *  Test the net endpoint.
 *  @author jeffw
 */
public final class NetEndpointTest {

	private static final byte	DEVICE_TEST_ENDPOPINT	= 0x01;

	/**
	 * Test method for {@link com.codeshelf.flyweightcontroller.command.NetEndpoint#NetEndpoint(int)}.
	 */
	@Test
	public void testNetEndpoint() {

		//		try {
		new NetEndpoint(NetEndpoint.MGMT_ENDPOINT_NUM);
		new NetEndpoint(NetEndpoint.BROADCAST_ENDPOINT_NUM);
		new NetEndpoint(DEVICE_TEST_ENDPOPINT);

		// Expected result.
		//		} catch (OutOfRangeException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//			fail();
		//		}
	}

}
