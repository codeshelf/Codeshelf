/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WidgetCommandListener.java,v 1.2 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

/**
 * @author jeffw
 *
 */
public interface WidgetCommandListener {

	void processWidgetCommand(IWidgetCommand inWidgetCommand);
}
