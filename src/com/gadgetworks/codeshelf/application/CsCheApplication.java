/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsCheApplication.java,v 1.7 2013/04/14 23:33:18 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.IEmbeddedDevice;
import com.google.inject.Inject;

public final class CsCheApplication extends ApplicationABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsCheApplication.class);

	private IEmbeddedDevice		mCheDevice;

	@Inject
	public CsCheApplication(final IEmbeddedDevice inCheDevice, final IUtil inUtil) {
		super(inUtil);
		mCheDevice = inCheDevice;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.application.ApplicationABC#doLoadLibraries()
	 */
	@Override
	protected void doLoadLibraries() {
		//System.loadLibrary("jd2xx");
		//System.loadLibrary("libjSSC-0.9_x86_64");
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doStartup() {
		// Start the device.
		mCheDevice.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {
		mCheDevice.stop();
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {

	}
}
