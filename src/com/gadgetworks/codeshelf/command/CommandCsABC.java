/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsABC.java,v 1.1 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;

/**
 * @author jeffw
 *
 */
public abstract class CommandCsABC extends CommandABC implements ICsCommand {
	
	public CommandCsABC(final CommandIdEnum inCommandIdEnum) {
		super(inCommandIdEnum);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandGroupEnum() {
		return CommandGroupEnum.CS;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doToString()
	 */
	protected String doToString() {
		CommandIdEnum cmdId = getCommandIdEnum();
		String result = "CmdId:" + cmdId.toString();
		return result;
	}
}
