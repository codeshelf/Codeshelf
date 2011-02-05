/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopPickMode.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

/**
 * @author jeffw
 *
 */
public class CommandAtopPickMode extends CommandAtopABC {

	public CommandAtopPickMode(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_PICK_MODE, inMsgType, inSubCommand);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected final boolean doHasSubNode() {
		return true;
	}
}
