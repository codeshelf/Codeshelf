/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopReadAllStatus.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

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
}
