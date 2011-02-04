/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopReadAllTagStatusesCmd.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

/**
 * @author jeffw
 *
 */
public class AtopReadAllTagStatusesCmd extends AtopCommandABC {
	
	public AtopReadAllTagStatusesCmd(final short inMsgType, final short inSubCommand) {
		super(inMsgType, inSubCommand);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.AtopCommandABC#doHasSubNode()
	 */
	protected final boolean doHasSubNode() {
		return false;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.AtopCommandABC#doGetCommandName()
	 */
	protected final String doGetCommandName() {
		return "ReadAllTagStatuses";
	}

}
