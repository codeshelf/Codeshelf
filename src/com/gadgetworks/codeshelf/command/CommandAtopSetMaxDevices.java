/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopSetMaxDevices.java,v 1.2 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jeffw
 *
 */
public class CommandAtopSetMaxDevices extends CommandAtopABC {
	
	public CommandAtopSetMaxDevices(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_SET_MAX_DEVICES, inMsgType, inSubCommand);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected final boolean doHasSubNode() {
		return true;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doSetupOutboundCsCommands()
	 */
	public final List<ICsCommand> doSetupOutboundCsCommands() {
		
		List<ICsCommand> result = new ArrayList<ICsCommand>();;
		return result;
	}
}
