/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.flyweight.command.CommandControlMessage;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetEndpoint;


public final class CommandControlTest extends CommandABCTest {

	private static final String			TEST_MSG1			= "TEST1";
	private static final String			TEST_MSG2			= "TEST2";

	public CommandControlTest(final String inName) {
		super(inName);
	}

	@Override
	protected ICommand createCommandABC() throws Exception {
		return new CommandControlMessage(NetEndpoint.PRIMARY_ENDPOINT, TEST_MSG1, TEST_MSG2);
	}
	
	@Test
	public void testEmpty() {
		Assert.assertTrue(true);
	}

}
