/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlMessageInterpreterABC.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
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
