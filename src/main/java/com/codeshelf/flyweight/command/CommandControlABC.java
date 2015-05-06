/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlABC.java,v 1.6 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import lombok.EqualsAndHashCode;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

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
@EqualsAndHashCode(callSuper=true, doNotUseGetters=true)
public abstract class CommandControlABC extends ExtendedCommandABC {

	public static final int		COMMAND_CONTROL_HDR_BYTES	= 1;
	public static final int		MAX_CONTROL_BYTES			= ICommand.MAX_COMMAND_BYTES - COMMAND_CONTROL_HDR_BYTES;

	public static final byte	SCAN						= 0;
	public static final byte	MESSAGE						= 1;
	public static final byte	LIGHT						= 2;
	public static final byte	SET_POSCONTROLLER			= 3;
	public static final byte	CLR_POSCONTROLLER			= 4;
	public static final byte	BUTTON						= 5;
	public static final byte	SINGLE_LINE_MESSAGE			= 6;
	public static final byte	CLEAR_DISPLAY				= 7;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a control command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 *  @param inControlBytes	The data to send in the command.
	 */
	public CommandControlABC(final NetEndpoint inEndpoint, final NetCommandId inExtendedCommandID) {
		super(inEndpoint, inExtendedCommandID);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlABC(final NetCommandId inExtendedCommandID) {
		super(inExtendedCommandID);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.ICommand#getCommandTypeEnum()
	 */
	public final CommandGroupEnum getCommandTypeEnum() {
		return CommandGroupEnum.CONTROL;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localEncode(com.gadgetworks.bitfields.BitFieldInputStream)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + COMMAND_CONTROL_HDR_BYTES;
	}

}
