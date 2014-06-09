/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandABCTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.flyweight.command.ICommand;

/** --------------------------------------------------------------------------
 *  
 *  Test the abstract command.
 *  @author jeffw
 */

public abstract class CommandABCTest {

	private ICommand	mCommandABC;

	@Test
	public final void setUp() throws Exception {
		mCommandABC = createCommandABC();
		Assert.assertNotNull("Problem creating CommandABC instance.", mCommandABC);
	}

	/**
	 * Every test of a concrete implementation must override this to
	 * return an instance of the actual implementation.
	 */
	protected abstract ICommand createCommandABC() throws Exception;

}
