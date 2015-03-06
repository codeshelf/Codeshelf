/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: NetCmdIDTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.controller;

import org.junit.Test;

import com.codeshelf.flyweight.command.CommandGroupEnum;
import com.codeshelf.flyweight.command.NetCommandGroup;
import com.codeshelf.testframework.MinimalTest;

/** --------------------------------------------------------------------------
 *  Test the net command ID.
 *  @author jeffw
 */
public final class NetCmdIDTest extends MinimalTest {

	/**
	 * Test method for {@link com.codeshelf.flyweightcontroller.command.NetEndpoint#NetEndpoint(int)}.
	 */
	@Test
	public void testNetEndpoint() {
		new NetCommandGroup(CommandGroupEnum.CONTROL);
		new NetCommandGroup(CommandGroupEnum.CONTROL);

	}

}
