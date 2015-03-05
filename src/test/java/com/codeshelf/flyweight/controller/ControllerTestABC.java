/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: ControllerABCTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.controller;

import org.junit.Assert;

/** --------------------------------------------------------------------------
 *  Test the controller.
 *  @author jeffw
 */

public abstract class ControllerTestABC {

	//private static final byte			TEST_ENDPOINT_NUM	= 0x01;
	@SuppressWarnings("unused")
	private static final byte	SRS_BYTE	= 0x00;
	//private static final NetEndpoint	TEST_ENDPOINT		= new NetEndpoint(TEST_ENDPOINT_NUM);
	@SuppressWarnings("unused")
	private static final String	TEST_ID		= "12345678";

	private static final String	TEST_MSG1	= "TEST1";
	private static final String	TEST_MSG2	= "TEST2";
	private static final String	TEST_MSG3	= "TEST3";
	private static final String	TEST_MSG4	= "TEST4";

	private IRadioController	mControllerABC;

	public final void setUp() throws Exception {
		mControllerABC = createControllerABC();
		Assert.assertNotNull("Problem creating ControllerABC instance.", mControllerABC);
	}

	/**
	 * Every test of a concrete implementation must override this to
	 * return an instance of the actual implementation.
	 */
	protected abstract IRadioController createControllerABC() throws Exception;

}
