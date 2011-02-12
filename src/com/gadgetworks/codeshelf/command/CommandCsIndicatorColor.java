/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandCsIndicatorColor.java,v 1.2 2011/02/12 02:00:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;

/**
 * @author jeffw
 *
 */
public class CommandCsIndicatorColor extends CommandCsABC {

	public static final boolean	RED_ON		= true;
	public static final boolean	RED_OFF		= false;
	public static final boolean	GREEN_ON	= true;
	public static final boolean	GREEN_OFF	= false;
	public static final boolean	BLUE_ON		= true;
	public static final boolean	BLUE_OFF	= false;

	private boolean				mRed;
	private boolean				mGreen;
	private boolean				mBlue;

	public CommandCsIndicatorColor(final boolean inRed, final boolean inGreen, final boolean inBlue) {
		super(CommandIdEnum.CS_INDICATOR_ON);
		mRed = inRed;
		mGreen = inGreen;
		mBlue = inBlue;
	}

	protected final void doToTransport(ITransport inTransport) {
		inTransport.setNextParam(mRed);
		inTransport.setNextParam(mGreen);
		inTransport.setNextParam(mBlue);
	}
	
	protected final void doFromTransport(ITransport inTransport) {

	}
	
	public final String doToString() {
		StringBuffer result = new StringBuffer(super.doToString());

		result.append(" Colors red: " + mRed + " grn: " + mGreen + " blue: " + mBlue);
		
		return result.toString();
	}
}
