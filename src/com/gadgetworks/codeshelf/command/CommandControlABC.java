/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlABC.java,v 1.3 2011/02/05 01:41:56 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import com.gadgetworks.codeshelf.controller.ITransport;

// --------------------------------------------------------------------------
/**
 *  The control command is the primary means by which the remotes are controlled by the controller.
 *  There are sub-commands for sending audio, motor control, etc.  There are also sub-commands for receiving inputs from the remote.
 *  
 *  Format of the control command is:
 *  
 *  1B - the command ID.
 *  1B - the command ACK ID.  (If non-zero, then the command requires ACK.)
 *  nB - the control command data.
 *  
 *  @author jeffw
 *  
 */
public abstract class CommandControlABC extends CommandABC {

	public static final byte	MOTOR_CONTROL_COMMAND				= 1;
	public static final byte	BUTTON_COMMAND						= 2;

	public static final String	BEAN_ID								= "CommandControl";

//	private static final Log	LOGGER								= LogFactory.getLog(CommandControlABC.class);

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a control command to send to the network.
	 *  @param inControlBytes	The data to send in the command.
	 */
	public CommandControlABC(final CommandIdEnum inCommandID) {
		super(inCommandID);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.ICommand#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandGroupEnum() {
		return CommandGroupEnum.CONTROL;
	}
}
