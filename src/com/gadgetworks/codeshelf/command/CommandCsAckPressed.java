/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsAckPressed.java,v 1.3 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public class CommandCsAckPressed extends CommandCsABC {

	private String	mMenuText;

	public CommandCsAckPressed(final PickTag inPickTag) {
		super(CommandIdEnum.CS_ACK_PRESSED, inPickTag);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doToTransport(ITransport inTransport) {
		inTransport.setNextParam(true);
		inTransport.setNextParam(false);
		inTransport.setNextParam(false);
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doFromTransport(ITransport inTransport) {
		Object object = inTransport.getParam(0);
		if (object instanceof String) {
			mMenuText = (String) object;
		} else if (object instanceof byte[]) {
			mMenuText = new String((byte[]) object);
		}
	}

	public final String getMenuText() {
		return mMenuText;
	}
}
