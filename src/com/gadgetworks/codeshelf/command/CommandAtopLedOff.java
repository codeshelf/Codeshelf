/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopLedOff.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

/**
 * @author jeffw
 *
 */
public class CommandAtopLedOff extends CommandAtopABC {

	public CommandAtopLedOff(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_LED_OFF, inMsgType, inSubCommand);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected final boolean doHasSubNode() {
		return true;
	}
}
