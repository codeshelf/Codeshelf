/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlMessageInterpreterABC.java,v 1.1 2011/01/21 02:22:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server;

import java.util.List;

/**
 * @author jeffw
 *
 */
public abstract class ControlMessageInterpreterABC implements IControlMessageInterpreter {
	
	protected ControlMessageHandler mMessageHandler;
	protected List<IWidget> mWidgets;
	
	public ControlMessageInterpreterABC(final ControlMessageHandler inMessageHandler) {
		mMessageHandler = inMessageHandler;
	}
	
	

}
