/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsReportShort.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public class CommandCsReportShort extends CommandCsABC {

	private int	mQuantity;

	public CommandCsReportShort(final PickTag inPickTag) {
		super(CommandIdEnum.CS_REPORT_SHORT, inPickTag);
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
