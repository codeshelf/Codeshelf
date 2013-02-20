/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WidgetABC.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import com.gadgetworks.flyweightcontroller.command.ICommand;
import com.gadgetworks.flyweightcontroller.controller.IController;

/**
 * @author jeffw
 *
 */
public abstract class WidgetABC implements IWidget {
	
	private IController mController;
	private PickTag mPickTag;
	
	public WidgetABC(final IController inController, final PickTag inPickTag) {
		mController = inController;
		mPickTag = inPickTag;
	}
	
	public abstract ICommand doReceiveWidgetCommand(IWidgetCommand inWidgetCommand);

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IWidget#receiveWidgetCommand(com.gadgetworks.codeshelf.server.IWidgetCommand)
	 */
	public final void receiveWidgetCommand(IWidgetCommand inWidgetCommand) {
		ICommand command = doReceiveWidgetCommand(inWidgetCommand);
		mController.sendCommandNow(command, mPickTag.getNetAddress(), false);
		
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.IWidget#sendWidgetCommand(com.gadgetworks.codeshelf.server.IWidgetCommand)
	 */
	public void sendWidgetCommand(IWidgetCommand inWidgetCommand) {
		// TODO Auto-generated method stub

	}

}
