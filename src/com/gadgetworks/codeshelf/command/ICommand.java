/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ICommand.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.util.List;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface ICommand {

	// --------------------------------------------------------------------------
	/**
	 *  Get the command type enum of this command.
	 *  @return	The command type.
	 */
	CommandGroupEnum getCommandGroupEnum();
	
	// --------------------------------------------------------------------------
	/**
	 * Get the command Id enum.
	 * @return
	 */
	CommandIdEnum getCommandIdEnum();

	// --------------------------------------------------------------------------
	/**
	 *  Get the command ID as an integer.
	 *  @return	The command ID as an integer.
	 */
	int getCommandIDAsInt();
	
	// --------------------------------------------------------------------------
	/**
	 *  Read the command from the transport.
	 *  @param inTransport	The transport to read from.
	 */
	void fromTransport(ITransport inTransport);

	// --------------------------------------------------------------------------
	/**
	 *  Write the command to a stream.
	 *  @param inTransport	The transport to write to.
	 */
	void toTransport(ITransport inTransport);
	
	// --------------------------------------------------------------------------
	/**
	 * Return the number of times we've attempted to send this command.
	 * @return
	 */
	int getSendCount();
	
	// --------------------------------------------------------------------------
	/**
	 * Increment the count of the number of times we've attempted to send this command.
	 */
	void incrementSendCount();
	
	// --------------------------------------------------------------------------
	/**
	 * Return the "sent" time for this command.
	 * @return
	 */
	long getSentTimeMillis();

	
	// --------------------------------------------------------------------------
	/**
	 * Set the "sent" time for this command.
	 */
	void setSentTimeMillis(long inSentTimeMillis);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	byte getAckId();
	
	// --------------------------------------------------------------------------
	/**
	 * @param inAckId
	 */
	void setAckId(byte inAckId);
	
	// --------------------------------------------------------------------------
	/**
	 * Return the ack state of this command.
	 * @return
	 */
	AckStateEnum getAckState();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the ack state of this command.
	 * @return
	 */
	void setAckState(AckStateEnum inAckState);
	
	// --------------------------------------------------------------------------
	/**
	 * The ack command may contain some result data from the remote.
	 * @return
	 */
	byte[] getAckData();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the ack data for the command.
	 */
	void setAckData(byte[] inAckData);
	
	// --------------------------------------------------------------------------
	/**
	 * Get the network ID for this command.
	 * @return
	 */
	NetworkId getNetworkId();

	// --------------------------------------------------------------------------
	/**
	 * Set the network ID for this command.
	 * @param inNetworkId
	 */
	void setNetworkId(NetworkId inNetworkId);
	
	
	// --------------------------------------------------------------------------
	/**
	 * Get the network source address for this command.
	 * @return
	 */
	NetAddress getSrcAddr();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the src address for this command.
	 * @param inSrcAddr
	 */
	void setSrcAddr(NetAddress inSrcAddr);
	
	// --------------------------------------------------------------------------
	/**
	 * Get the network destination address for this command.
	 * @return
	 */
	NetAddress getDstAddr();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the dst address for this command.
	 * @param inDstAddr
	 */
	void setDstAddr(NetAddress inDstAddr);
	
	// --------------------------------------------------------------------------
	/**
	 * Return the scheduled time to send this command (in nanos resolution).
	 * @return
	 */
	long getScheduledTimeNanos();
	
	// --------------------------------------------------------------------------
	/**
	 * Set the send time (in nanos) for this command.
	 * @param inSendTimeNanos
	 */
	void setScheduledTimeNanos(long inSendTimeNanos);
}
