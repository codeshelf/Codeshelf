/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsIndicatorOn.java,v 1.1 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;

/**
 * @author jeffw
 *
 */
public class CommandCsIndicatorOn extends CommandCsABC {

	public CommandCsIndicatorOn() {
		super(CommandIdEnum.CS_INDICATOR_ON);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doToTransport(ITransport inTransport) {
		inTransport.setNextParam(true);
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
