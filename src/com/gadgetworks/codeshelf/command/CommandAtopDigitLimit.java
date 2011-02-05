/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopDigitLimit.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

/**
 * @author jeffw
 *
 */
public class CommandAtopDigitLimit extends CommandAtopABC {

	public CommandAtopDigitLimit(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_DIGIT_LIMIT, inMsgType, inSubCommand);
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
	 * @see com.gadgetworks.codeshelf.server.tags.CommandAtopABC#toString()
	 */
	public final String doToString() {
		String result = super.doToString() + "\nLimit: " + getDataBytes()[0];
		return result;
	}
}
