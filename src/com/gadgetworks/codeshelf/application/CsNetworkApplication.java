/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsNetworkApplication.java,v 1.4 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import com.gadgetworks.codeshelf.device.ICsDeviceManager;
import com.google.inject.Inject;

public final class CsNetworkApplication extends ApplicationABC {

	private ICsDeviceManager	mDeviceManager;

	@Inject
	public CsNetworkApplication(final ICsDeviceManager inDeviceManager, final IUtil inUtil) {
		super(inUtil);
		mDeviceManager = inDeviceManager;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doStartup() {

		// Start the device manager.
		mDeviceManager.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {

		// Stop the web socket.
		mDeviceManager.stop();
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {

	}
}
