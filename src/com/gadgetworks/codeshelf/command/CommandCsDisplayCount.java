/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsDisplayCount.java,v 1.1 2011/02/15 22:16:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public class CommandCsDisplayCount extends CommandCsABC {

	private int	mQuantity;

	public CommandCsDisplayCount(final PickTag inPickTag, final int inQuantity) {
		super(CommandIdEnum.CS_DISPLAY_COUNT, inPickTag);
		mQuantity = inQuantity;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doToTransport(ITransport inTransport) {
		inTransport.setNextParam(mQuantity);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doFromTransport(ITransport inTransport) {

	}
}
