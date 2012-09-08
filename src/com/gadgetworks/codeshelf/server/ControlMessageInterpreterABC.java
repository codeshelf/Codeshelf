/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlMessageInterpreterABC.java,v 1.2 2012/09/08 03:03:22 jeffw Exp $
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
