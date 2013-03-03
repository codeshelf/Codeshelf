/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandABC.java,v 1.2 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  Commands are basic to both systems (toy and hand-held).  Some commands are meant to carry
 *  content that is specific to one or the other system.  Query and response commands, for example,
 *  carry queries and responses that are specific to the system.
 *  
 *  The design goal of the system is to have a command command processing structure for all activities.
 *  System-specific customization should occur at the command content level.
 *  
 *  There are two ways to create a command:
 *  1. You have a stream (from network/disk) that gets decoded into the new command.
 *  	The Packet class contains the command factory for this case:  Packet.generateCommand().
 *  2. You're creating a new command from scratch, and all you know is the command class you want.
 *  	There is no command factory for this case - you create the command adhoc.
 *  
 * The format of a general command stream is:
 * 
 * 4b - Command group
 * 4b - Command endpoint
 * nB - Command data
 *
 * (b = bit, B = byte)
 *
 * @author jeffw
 */

public abstract class CommandABC implements ICommand {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CommandABC.class);

	// Command ID + endpoint
	//public static final byte COMMAND_HDR_SIZE = 1;

	private NetEndpoint			mNetEndpoint;
	private IPacket				mParentPacket;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a command that's coming off of the network.
	 */
	public CommandABC() {
		mNetEndpoint = NetEndpoint.MGMT_ENDPOINT;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a command at the controller to send to the network.
	 *  @param inCommandID	The command type we're creating.
	 *  @param inCommandData	The endpoint where we want to send the command.
	 */
	public CommandABC(final NetEndpoint inEndpoint) {
		mNetEndpoint = inEndpoint;
	}

	// --------------------------------------------------------------------------
	/**
	 *  Computes the size of the command in bytes.
	 *  @return	The size of the command in bytes.
	 */
	protected abstract int doComputeCommandSize();

	// --------------------------------------------------------------------------
	/**
	 *  The "do" method for reading the command off of the input stream.
	 *  @param inInputStream	The input stream to read.
	 *  @param inCommandByteCount	The number of bytes to read.
	 */
	protected abstract void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount);

	// --------------------------------------------------------------------------
	/**
	 *  The 'do" method for writing the command to the output stream.
	 *  @param inOutputStream	The output stream to write.
	 */
	protected abstract void doToStream(BitFieldOutputStream inOutputStream);

	// --------------------------------------------------------------------------
	/**
	 *  The "do" method that creates a debug string for the object.
	 *  @return	A debug string for the object.
	 */
	protected abstract String doToString();

	// --------------------------------------------------------------------------
	/**
	 *  @return	The command ID for this command.
	 */
	public final int getCommandIDAsInt() {
		return getCommandTypeEnum().getValue();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The command's size in bytes.
	 */
	public final int getCommandSize() {
		return COMMAND_HEADER_BYTES + doComputeCommandSize();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The endpoint as an integer.
	 */
	public final int getNetEndpointIDAsInt() {
		return mNetEndpoint.getValue();
	}

	// --------------------------------------------------------------------------
	/**
	 *  The parent method that controls the reading of a command from the input stream.
	 *  @param inEndpoint	The endpoint of the command (read from the input stream in the factory).
	 *  @param inBitFieldInputStream	The input stream to read.
	 *  @param inCommandByteCount	The number of bytes to read from the input stream.
	 */
	public void fromStream(NetEndpoint inEndpoint, BitFieldInputStream inBitFieldInputStream, int inCommandByteCount) {

		// CommandID byte will already have been read in the command factory.

		// Set the endpoint that we read from the stream in the command factory.
		mNetEndpoint = inEndpoint;

		// Have the subclasses get their data from the input stream.
		doFromStream(inBitFieldInputStream, inCommandByteCount);
	}

	// --------------------------------------------------------------------------
	/**
	 *  The parent method that controls the writing of a command to the output stream.
	 *  @param inBitFieldOutputStream	The output stream to write.
	 */
	public void toStream(BitFieldOutputStream inBitFieldOutputStream) {

		try {
			// Put the command ID in the output stream.
			inBitFieldOutputStream.writeNBitInteger(new NetCommandGroup(getCommandTypeEnum()));
			// Round out the byte after the command ID.
			inBitFieldOutputStream.writeNBitInteger(mNetEndpoint);

			// Have the subclasses put their data into the output stream.
			doToStream(inBitFieldOutputStream);

			inBitFieldOutputStream.flush();
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.command.ICommand#setPacket(com.gadgetworks.flyweight.command.IPacket)
	 */
	public final void setPacket(IPacket inPacket) {
		mParentPacket = inPacket;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.command.ICommand#getAckState()
	 */
	public final AckStateEnum getAckState() {
		AckStateEnum result = AckStateEnum.INVALID;

		if (mParentPacket != null) {
			result = mParentPacket.getAckState();
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.command.ICommand#getAckState()
	 */
	public final byte[] getAckData() {
		byte[] result = null;

		if (mParentPacket != null) {
			result = mParentPacket.getAckData();
		}
		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		String resultStr;

		resultStr = "  cmd id:" + getCommandIDAsInt() + " ep:" + getNetEndpoint();
		resultStr += " cmd=" + getCommandName();
		resultStr += " " + doToString();

		return resultStr;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return human-readable command name for this commane.
	 * @return	The command name.
	 */
	private String getCommandName() {
		return getCommandTypeEnum().getName();
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return	The endpoint for this command.
	 */
	private NetEndpoint getNetEndpoint() {
		return mNetEndpoint;
	}

}
