/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: Monitor.java,v 1.1 2013/04/04 19:05:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */
public class Monitor implements IMonitor {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Monitor.class);

	public Monitor() {

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.monitor.IMonitor#logToCentralAdmin(java.lang.String)
	 */
	@Override
	public final void logToCentralAdmin(String inLogString) {
		LOGGER.warn(inLogString);
	}
}
