/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopSetMaxDevices.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

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
}
