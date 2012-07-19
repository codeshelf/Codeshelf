/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWidget.java,v 1.2 2012/07/19 06:11:34 jeffw Exp $
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
