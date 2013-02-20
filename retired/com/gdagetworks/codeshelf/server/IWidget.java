/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWidget.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;


/**
 * @author jeffw
 *
 */
public interface IWidget {
	
	void receiveWidgetCommand(IWidgetCommand inWidgetCommand);
	
	void sendWidgetCommand(IWidgetCommand inWidgetCommand);

}
