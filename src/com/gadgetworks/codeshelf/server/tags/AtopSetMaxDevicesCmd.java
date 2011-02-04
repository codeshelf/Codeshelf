/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopSetMaxDevicesCmd.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

/**
 * @author jeffw
 *
 */
public class AtopSetMaxDevicesCmd extends AtopCommandABC {
	
	public AtopSetMaxDevicesCmd(final short inMsgType, final short inSubCommand) {
		super(inMsgType, inSubCommand);
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
	 * @see com.gadgetworks.codeshelf.server.tags.AtopCommandABC#doGetCommandName()
	 */
	protected final String doGetCommandName() {
		return "SetMaxDevices";
	}

}
