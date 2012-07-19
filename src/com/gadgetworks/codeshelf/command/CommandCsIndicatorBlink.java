/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsIndicatorBlink.java,v 1.2 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public class CommandCsIndicatorBlink extends CommandCsABC {

	private int	mBlinkMillis;

	public CommandCsIndicatorBlink(final PickTag inPickTag, final int inBlinkMillis) {
		super(CommandIdEnum.CS_INDICATOR_BLINK, inPickTag);
		mBlinkMillis = inBlinkMillis;
	}

	protected final void doToTransport(ITransport inTransport) {
		inTransport.setNextParam(mBlinkMillis);
	}

	protected final void doFromTransport(ITransport inTransport) {

	}

	public final String doToString() {
		StringBuffer result = new StringBuffer(super.doToString());

		result.append(" Blink millis: " + mBlinkMillis);

		return result.toString();
	}
}
