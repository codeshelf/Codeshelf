/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsAisleApplication.java,v 1.1 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import com.gadgetworks.codeshelf.device.IEmbeddedDevice;
import com.google.inject.Inject;

public final class CsAisleApplication extends ApplicationABC {

	private IEmbeddedDevice		mCheDevice;

	@Inject
	public CsAisleApplication(final IEmbeddedDevice inCheDevice, final IUtil inUtil) {
		super(inUtil);
		mCheDevice = inCheDevice;
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
