/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsIndicatorOff.java,v 1.1 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;

/**
 * @author jeffw
 *
 */
public class CommandCsIndicatorOff extends CommandCsABC {

	public CommandCsIndicatorOff() {
		super(CommandIdEnum.CS_INDICATOR_OFF);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doToTransport(ITransport inTransport) {
		inTransport.setNextParam(false);
		inTransport.setNextParam(false);
		inTransport.setNextParam(false);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doFromTransport(ITransport inTransport) {

	}
}
