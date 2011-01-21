/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: MainABC.java,v 1.3 2011/01/21 04:25:54 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
abstract class MainABC {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}
	private static final Log	LOGGER	= LogFactory.getLog(MainABC.class);

	protected CodeShelfApplication	mApplication;

	// --------------------------------------------------------------------------
	/**
	 */
	public MainABC() {

	}

	public abstract void platformSetup();

	// --------------------------------------------------------------------------
	/**
	 */
	public void mainStart() {

		// Create and start the application.
		mApplication = new CodeShelfApplication();
		mApplication.startApplication();
		// This executes any platform-specific application setup.
		platformSetup();

		// Handle events until the application exits.
		mApplication.handleEvents();

		LOGGER.info("Exiting Main()");
	}
}