/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IPacket.java,v 1.3 2013/03/03 23:27:20 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.command;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;
import com.codeshelf.flyweight.controller.INetworkDevice;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface IPacket {

	byte	PACKET_VERSION_0			= 0;

	// Packet header structure sizes.  (See Packet.java)
	byte	PROTOCOL_VERSION_BITS		= 2;
	byte	ACK_REQUIRED_BITS			= 1;
	byte	RESERVED_HEADER_BITS		= 1;
	byte	NETWORK_NUMBER_BITS			= 4;
	byte	ADDRESS_BITS				= 8;

	byte	NETWORK_NUM_SPACING_BITS	= 4;
	byte	ADDRESS_SPACING_BITS		= 0;

	byte	STD_PACKET					= 0;
	byte	ACK_PACKET					= 1;
	byte	EMPTY_ACK_ID				= 0;
	int		ACK_DATA_BYTES				= 8;

	//NetAddress	GATEWAY_ADDRESS				= new NetAddress((byte) 0x0);
	byte	GATEWAY_ADDRESS				= 0x00;
	// Broadcast address is all 1's for each address bit.
	//NetAddress	BROADCAST_ADDRESS			= new NetAddress((byte) (Math.pow(2, ADDRESS_BITS) - 1));
	byte	BROADCAST_ADDRESS			= (byte) (Math.pow(2, ADDRESS_BITS) - 1);

	// This is the network ID used to send network mgmt commands to all devices on a channel regardless of network ID.
	byte	BROADCAST_NETWORK_ID		= (byte) (Math.pow(2, NETWORK_NUMBER_BITS) - 1);
	byte	DEFAULT_NETWORK_ID			= (byte) 0x01;
	byte	ZERO_NETWORK_ID				= (byte) 0x00;
	//NetworkId	BROADCAST_NETWORK_ID		= new NetworkId((byte) (Math.pow(2, NETWORK_NUMBER_BITS) - 1));
	//NetworkId	DEFAULT_NETWORK_ID			= new NetworkId((byte) 0x01);
	//NetworkId	ZERO_NETWORK_ID				= new NetworkId((byte) 0x00);

	byte	SMAC_FRAME_BYTES			= 2;
	byte	MAX_PACKET_BYTES			= 125 - SMAC_FRAME_BYTES;

	byte	PACKET_HEADER_BYTES			= 4;

	// --------------------------------------------------------------------------
	/**
	 *  This function serializes the packet to an output stream.
	 *  @param inBitFieldOutputStream
	 */
	void toStream(BitFieldOutputStream inBitFieldOutputStream);

	// --------------------------------------------------------------------------
	/**
	 *  This function takes a serialization string of a raw command (from network transmission) and sets the packet structures.
	 *  @param inInputStream
	 */
	void fromStream(BitFieldInputStream inInputStream, int inFrameSize);

	// --------------------------------------------------------------------------
	/**
	 *  Get the command contained in this packet.
	 *  @return	The command
	 */
	ICommand getCommand();

	// --------------------------------------------------------------------------
	/**
	 *  Get the source address of this packet.
	 *  @return	The source address
	 */
	NetAddress getSrcAddr();

	// --------------------------------------------------------------------------
	/**
	 *  Get the destination address of this packet.
	 *  @return	The destination address
	 */
	NetAddress getDstAddr();

	// --------------------------------------------------------------------------
	/**
	 *  Set the command contained in this packet.
	 *  @param inCommand	The command to put into the packet.
	 */
	void setCommand(ICommand inCommand);

	// --------------------------------------------------------------------------
	/**
	 * Get the time that the packet was created.
	 */
	long getCreateTimeMillis();

	// --------------------------------------------------------------------------
	/**
	 * The time that the packet was actually sent (last if resent).
	 * @return	The actual send time in millis
	 */
	long getSentTimeMillis();

	// --------------------------------------------------------------------------
	/**
	 * Set the time that the packet was actually send (last if resent).
	 */
	void setSentTimeMillis(long inSendTimeMillis);

	// --------------------------------------------------------------------------
	/**
	 *  Get the network ID for this packet.
	 *  @return	The network ID of the packet.
	 */
	NetworkId getNetworkId();

	// --------------------------------------------------------------------------
	/**
	 *  Set the network ID of the packet.
	 *  @param inNetworkId
	 */
	void setNetworkId(NetworkId inNetworkId);

	// --------------------------------------------------------------------------
	/**
	 *  Get a unique command number for this command.
	 *  @return
	 */
	byte getAckId();

	// --------------------------------------------------------------------------
	/**
	 *  Set a unique command number for this command.
	 *  @return
	 */
	void setAckId(byte inCommandAckId);

	// --------------------------------------------------------------------------
	/**
	 * Get the packet type (Ack or standard)
	 * @return
	 */
	byte getPacketType();

	// --------------------------------------------------------------------------
	/**
	 * Set the packet type (Ack or standard)
	 * @param inPacketType
	 */
	void setPacketType(byte inPacketType);

	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  Determine if this command requires an ACK.
	//	 *  @return
	//	 */
	//	boolean getAckRequested();
	//
	//	// --------------------------------------------------------------------------
	//	/**
	//	 *  Set if this command requires an ACK.
	//	 *  @param inNeedsAck	whether the command needs an ACK.
	//	 */
	//	void setAckRequested(boolean inNeedsAck);

	// --------------------------------------------------------------------------
	/**
	 *  Each time we resend a command the requires ACK we increment the send count.
	 */
	void incrementSendCount();

	// --------------------------------------------------------------------------
	/**
	 *  Get the number of times the command was sent.
	 *  @return	The number of times we already sent the command.
	 */
	int getSendCount();

	// --------------------------------------------------------------------------
	/**
	 *  Set the number of times the command was sent.
	 *  @param	The number of times we already sent the command.
	 */
	void setSendCount(int inResendCount);

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	AckStateEnum getAckState();

	// --------------------------------------------------------------------------
	/**
	 *  @param inAckedState
	 */
	void setAckState(AckStateEnum inAckedState);

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	byte[] getAckData();

	// --------------------------------------------------------------------------
	/**
	 * @param inAckData
	 */
	void setAckData(byte[] inAckData);

	// --------------------------------------------------------------------------
	/**
	 * Get the network device associated with this packet
	 */
	INetworkDevice getDevice();

	// --------------------------------------------------------------------------
	/**
	 * Set the network device associated with this packet
	 * @param inDevice
	 */
	void setDevice(INetworkDevice inDevice);
	
	// --------------------------------------------------------------------------
	/**
	 * Returns whether the packet requires an acknowledgment 
	 */
	Boolean getRequiresAck();
	
	// --------------------------------------------------------------------------
	/**
	 * Set that the packet requires an acknowledgment 
	 * @param requiresAck
	 */
	void setRequiresAck(Boolean requiresAck);

}
