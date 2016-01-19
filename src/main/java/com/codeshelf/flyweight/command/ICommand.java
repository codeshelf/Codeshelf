/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: ICommand.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.command;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface ICommand {

	// See CommandABC for header description.
	byte	COMMAND_HEADER_BYTES	= 1;

	byte	MAX_COMMAND_BYTES		= (byte) (IPacket.MAX_PACKET_BYTES - IPacket.PACKET_HEADER_BYTES);

	// --------------------------------------------------------------------------
	/**
	 *  Get the command type enum of this command.
	 *  @return	The command type.
	 */
	CommandGroupEnum getCommandTypeEnum();

	// --------------------------------------------------------------------------
	/**
	 *  Get the command ID as an integer.
	 *  @return	The command ID as an integer.
	 */
	int getCommandIDAsInt();

	// --------------------------------------------------------------------------
	/**
	 *  Get the command's endpoint as an integer.
	 *  @return	The command endpoint as an integer.
	 */
	int getNetEndpointIDAsInt();

	// --------------------------------------------------------------------------
	/**
	 *  Read the command from the stream.
	 *  @param inBitFieldInputStream	The stream to read from.
	 *  @param inCommandByteCount	The number of bytes to read.
	 */
	void fromStream(NetEndpoint inEndpoint, BitFieldInputStream inBitFieldInputStream, int inCommandByteCount);

	// --------------------------------------------------------------------------
	/**
	 *  Write the command to a stream.
	 *  @param inBitFieldOutputStream	The stream to write to.
	 */
	void toStream(BitFieldOutputStream inBitFieldOutputStream);

	// --------------------------------------------------------------------------
	/**
	 *  The size of the command in bytes.
	 *  @return	The size of the command in bytes.
	 */
	int getCommandSize();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the parent packet for this command.
	 * @param inPacket
	 */
	void setPacket(IPacket inPacket);
	
	// --------------------------------------------------------------------------
	/**
	 * Return the ack state of the packet that carried this command.
	 * @return
	 */
	AckStateEnum getAckState();
	
	// --------------------------------------------------------------------------
	/**
	 * The ack packet may contain some result data from the remote.
	 * @return
	 */
	byte[] getAckData();
	
	// --------------------------------------------------------------------------
	/**
	 * Get the resend delay time in ms for this command
	 */
	int getResendDelay();
}
