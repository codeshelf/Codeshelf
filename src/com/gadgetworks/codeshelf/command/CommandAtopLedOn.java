/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopLedOn.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

/**
 * @author jeffw
 *
 */
public class CommandAtopLedOn extends CommandAtopABC {

	public CommandAtopLedOn(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_LED_ON, inMsgType, inSubCommand);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected final boolean doHasSubNode() {
		return true;
	}
}
