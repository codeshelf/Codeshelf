/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: NetCmdIDTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import org.junit.Test;

import com.gadgetworks.flyweight.command.CommandGroupEnum;
import com.gadgetworks.flyweight.command.NetCommandGroup;

/** --------------------------------------------------------------------------
 *  Test the net command ID.
 *  @author jeffw
 */
public final class NetCmdIDTest {

	/**
	 * Test method for {@link com.gadgetworks.flyweightcontroller.command.NetEndpoint#NetEndpoint(int)}.
	 */
	@Test
	public void testNetEndpoint() {

		@SuppressWarnings("unused")
		NetCommandGroup cmd;

//		try {
			cmd = new NetCommandGroup(CommandGroupEnum.CONTROL);
			cmd = new NetCommandGroup(CommandGroupEnum.CONTROL);
			
			// Expected result.
//		} catch (OutOfRangeException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			fail();
//		}
	}

}
