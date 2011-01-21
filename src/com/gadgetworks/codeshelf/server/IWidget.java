/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWidget.java,v 1.1 2011/01/21 02:22:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public interface IWidget {
	
	void receiveWidgetCommand(IWidgetCommand inWidgetCommand);
	
	void sendWidgetCommand(IWidgetCommand inWidgetCommand);

}
