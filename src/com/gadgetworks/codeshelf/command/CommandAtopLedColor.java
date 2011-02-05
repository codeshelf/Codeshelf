/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopLedColor.java,v 1.1 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

/**
 * @author jeffw
 *
 */
public class CommandAtopLedColor extends CommandAtopABC {

	public CommandAtopLedColor(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_LED_COLOR, inMsgType, inSubCommand);
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

		result.append("Color: ");
		switch (getDataBytes()[0]) {
			case 0:
				result.append("Red");
				break;
			case 1:
				result.append("Green");
				break;
			case 2:
				result.append("Orange");
				break;
			case 3:
				result.append("Blue");
				break;
			case 4:
				result.append("Pink");
				break;
			case 5:
				result.append("Cyan");
				break;
			default:
				result.append("Unknown");
		}
		return result.toString();
	}
}
