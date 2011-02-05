/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopAlphaPush.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

/**
 * @author jeffw
 *
 */
public class CommandAtopAlphaPush extends CommandAtopABC {

	public CommandAtopAlphaPush(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_ALPHA_NUM_PUSH, inMsgType, inSubCommand);
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
		StringBuffer result = new StringBuffer(super.doToString() + "\n");

		for (byte value : this.getDataBytes()) {
			result.append((char) value + " ");
		}
		return result.toString();
	}

}
