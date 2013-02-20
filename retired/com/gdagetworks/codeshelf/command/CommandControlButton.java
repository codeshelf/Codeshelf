/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlButton.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  The button control sub-command allows the remote to indicate that the user pressed a button.
 *  
 *  The data of the command is a single, 8-bit integer representing the button number pressed.
 *  
 *  @author jeffw
 */
public final class CommandControlButton extends CommandControlABC {

	public static final String	BEAN_ID			= "CommandControlButton";

	public static final byte	BUTTON_PRESSED	= 1;
	public static final byte	BUTTON_RELEASED	= 2;

	private static final Log	LOGGER			= LogFactory.getLog(CommandControlButton.class);

	private byte				mButtonNumber;
	private byte				mFunctionType;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlButton(final byte inButtonPressed, final byte inFunctionType) {
		super(CommandIdEnum.BUTTON);

		mButtonNumber = inButtonPressed;
		mFunctionType = inFunctionType;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlButton() {
		super(CommandIdEnum.BUTTON);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		String actionStr;
		switch (mFunctionType) {
			case BUTTON_PRESSED:
				actionStr = "pressed";
				break;
			case BUTTON_RELEASED:
				actionStr = "released";
				break;
			default:
				actionStr = "???";
		}
		return "Button: " + mButtonNumber + " " + actionStr;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandControlABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected void doToTransport(ITransport inTransport) {
		inTransport.setNextParam(mButtonNumber);
		inTransport.setNextParam(mFunctionType);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandControlABC#doFromTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected void doFromTransport(ITransport inTransport) {
		mButtonNumber = ((Byte) inTransport.getParam(1)).byteValue();
		mFunctionType = ((Byte) inTransport.getParam(2)).byteValue();
	}

	// --------------------------------------------------------------------------
	/**
	 *  This returns the value of the button pressed.
	 *  @return
	 */
	public byte getButtonPressed() {
		return mButtonNumber;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Returns the function type associated with the button.
	 *  (e.g. BUTTON_PRESSED, BUTTON_RELEASED, etc.)
	 *  @return
	 */
	public byte getFunctionType() {
		return mFunctionType;
	}
}
