/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsDisplayClear.java,v 1.1 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;


/**
 * @author jeffw
 *
 */
public class CommandCsDisplayClear extends CommandCsABC {
	
	public CommandCsDisplayClear() {
		super(CommandIdEnum.CS_DISPLAY_CLEAR);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doToTransport(ITransport inTransport) {

	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doFromTransport(ITransport inTransport) {

	}
}
