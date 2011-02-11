/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopLedOn.java,v 1.2 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.util.ArrayList;
import java.util.List;

import com.gadgetworks.codeshelf.controller.ITransport;

/**
 * @author jeffw
 *
 */
public class CommandAtopLedOn extends CommandAtopABC {

	public CommandAtopLedOn(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_LED_ON, inMsgType, inSubCommand);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);
		inTransport.setNextParam(true);
		inTransport.setNextParam(false);
		inTransport.setNextParam(false);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doSetupOutboundCsCommands()
	 */
	public final List<ICsCommand> doSetupOutboundCsCommands() {
		
		List<ICsCommand> result = new ArrayList<ICsCommand>();;
		
		ICsCommand command = new CommandCsIndicatorOn();
		result.add(command);
		
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected final boolean doHasSubNode() {
		return true;
	}
}
