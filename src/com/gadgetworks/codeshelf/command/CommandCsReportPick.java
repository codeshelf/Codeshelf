/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsReportPick.java,v 1.2 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public class CommandCsReportPick extends CommandCsABC {

	private int	mQuantity;

	public CommandCsReportPick(final PickTag inPickTag) {
		super(CommandIdEnum.CS_REPORT_PICK, inPickTag);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doToTransport(ITransport inTransport) {

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected final void doFromTransport(ITransport inTransport) {
		Object object = inTransport.getParam(0);
		if (object instanceof Integer) {
			mQuantity = (Integer) object;
		}
	}

	public final int getQuantity() {
		return mQuantity;
	}
}
