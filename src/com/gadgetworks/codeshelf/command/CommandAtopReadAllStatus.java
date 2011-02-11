/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopReadAllStatus.java,v 1.2 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jeffw
 *
 */
public class CommandAtopReadAllStatus extends CommandAtopABC {
	
	public CommandAtopReadAllStatus(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_READ_ALL_STATUS, inMsgType, inSubCommand);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.CommandAtopABC#doHasSubNode()
	 */
	protected final boolean doHasSubNode() {
		return false;
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
