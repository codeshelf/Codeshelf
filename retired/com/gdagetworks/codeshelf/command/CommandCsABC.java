/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsABC.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public abstract class CommandCsABC extends CommandABC implements ICsCommand {

	private PickTag	mPickTag;

	public CommandCsABC(final CommandIdEnum inCommandIdEnum, final PickTag inPickTag) {
		super(inCommandIdEnum);
		mPickTag = inPickTag;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandGroupEnum() {
		return CommandGroupEnum.CODESHELF;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.ICsCommand#getPickTag()
	 */
	public final PickTag getPickTag() {
		return mPickTag;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doToString()
	 */
	protected String doToString() {
		CommandIdEnum cmdId = getCommandIdEnum();
		String result = "tag:" + String.valueOf(mPickTag) + "subcmd: " + cmdId.getName();
		return result;
	}
}
