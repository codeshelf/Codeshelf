/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: NetEndpointTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import junit.framework.TestCase;

import org.junit.Test;

import com.gadgetworks.flyweight.command.NetEndpoint;

/** --------------------------------------------------------------------------
 *  Test the net endpoint.
 *  @author jeffw
 */
public final class NetEndpointTest extends TestCase {

	private static final byte	DEVICE_TEST_ENDPOPINT	= 0x01;

	/**
	 * Test method for {@link com.gadgetworks.flyweightcontroller.command.NetEndpoint#NetEndpoint(int)}.
	 */
	@Test
	public void testNetEndpoint() {

		@SuppressWarnings("unused")
		NetEndpoint endpoint;

		//		try {
		endpoint = new NetEndpoint(NetEndpoint.MGMT_ENDPOINT_NUM);
		endpoint = new NetEndpoint(NetEndpoint.BROADCAST_ENDPOINT_NUM);
		endpoint = new NetEndpoint(DEVICE_TEST_ENDPOPINT);

		// Expected result.
		//		} catch (OutOfRangeException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//			fail();
		//		}
	}

}
