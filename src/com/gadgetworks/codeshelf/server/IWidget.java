/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWidget.java,v 1.3 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public interface IWidget {
	
	void receiveWidgetCommand(IWidgetCommand inWidgetCommand);
	
	void sendWidgetCommand(IWidgetCommand inWidgetCommand);

}
