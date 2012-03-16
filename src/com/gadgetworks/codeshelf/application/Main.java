/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: Main.java,v 1.1 2012/03/16 15:59:09 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class Main {

	// See the top of Util to understand why we do the following:
	static {
		Util.initLogging();
	}
	private static final Log	LOGGER	= LogFactory.getLog(Main.class);

	//protected CodeShelfApplication	mApplication;

	// --------------------------------------------------------------------------
	/**
	 */
	public Main() {

	}

	//public abstract void platformSetup();

	// --------------------------------------------------------------------------
	/**
	 */
	public static void main(String[] inArgs) {

		// Create and start the application.
		CodeShelfApplication mApplication = new CodeShelfApplication();
		mApplication.startApplication();
		
		// This executes any platform-specific application setup.
		//platformSetup();

		// Handle events until the application exits.
		mApplication.handleEvents();

		LOGGER.info("Exiting Main()");
	}
}