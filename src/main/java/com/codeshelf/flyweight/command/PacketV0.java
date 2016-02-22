/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: Packet.java,v 1.5 2013/07/22 04:30:18 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;
import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.controller.INetworkDevice;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *
 *  There are two ways to create a packet:
 *  1. You have a stream (from network/disk) that gets decoded into the new packet.
 *  	The Packet constructor can build the packet directly from the stream.
 *  2. You're creating a new packet from scratch, and you have the addresses and command to go with it.
 *  	There is no packet factory for this case - you create the command ad-hoc.	 /**
 *
 * 
 *   2b - Packet format version
 *   1b - Packet type (std or ack)
 *   5b - Network ID
 *   1B - Src address
 *	 1B - Dst address
 *   1B - Ack Id (if ack wanted on std packet then non-zero)
 *	 nB - Command data
 *	
 *	 (b = bit, B = byte)
 *
 *	A note about our simplistic packetization and framing.
 *
 *	We use the SLIP framing protocol as recorded in RFC 1055.  The packet does not participate in this.  The serial interface
 *  manager frames the packets prior to transmission onto the serial interface.  The packets that go onto the radio DO NOT
 *  contain SLIP framing bytes since 802.15.4 has its own framing system.
 *
 *  I am aware that there are many (good) ways to frame a packet.  My favorite is frame marking and byte-stuffing 
 *  as used in Ethernet.  Sure, it would be great to have "the ultimate, flexible" packet sizing method here too, but
 *  there is a good reason to do something simpler.  For starters, one side of the link is an 8-bit MCU
 *  with little processing power and memory, so we need to keep things small/simple.  The second
 *  reason is that every packet sent over 802.15.4 is 125 bytes or smaller.  Given this upper limit (and 
 *  little possibility that it will ever change due to massive pain in the hardware itself) a single byte to 
 *  specify the frame/size of a packet will always take less space, run faster, and reduce complexity than byte-stuffing.
 *  
 *  So, there you have it: a fixed byte packet sizing.
 *
 */

public final class PacketV0 implements IPacket {

	private static final Logger		LOGGER			= LoggerFactory.getLogger(PacketV0.class);
	
	// Packet header structure sizes.  (See Packet.java)
	public final static byte	PROTOCOL_VERSION		= IPacket.PACKET_VERSION_0;
	public final static byte	PROTOCOL_VERSION_BITS	= 2;
	public final static byte	ACK_REQUIRED_BITS		= 1;
	public final static byte	RESERVED_HEADER_BITS	= 1;
	public final static byte	NETWORK_NUMBER_BITS		= 4;
	public final static byte	ADDRESS_BITS			= 8;
	
	public final static byte	NETWORK_NUM_SPACING_BITS	= 4;
	public final static byte	ADDRESS_SPACING_BITS		= 0;

	public final static byte	STD_PACKET		= 0;
	public final static byte	ACK_PACKET		= 1;
	public final static byte	EMPTY_ACK_ID	= 0;
	public final static int		ACK_DATA_BYTES	= 8;

	public static final NetAddress	GATEWAY_ADDRESS				= new NetAddress((byte) 0x00, ADDRESS_BITS);
	//byte	GATEWAY_ADDRESS		= 0x0000;
	// Broadcast address is all 1's for each address bit.
	public static final NetAddress	BROADCAST_ADDRESS			= new NetAddress((byte) (Math.pow(2, ADDRESS_BITS) - 1), ADDRESS_BITS);
	//short	BROADCAST_ADDRESS	= (short) 0xffff; //(Math.pow(2, ADDRESS_BITS) - 1);

	// This is the network ID used to send network mgmt commands to all devices on a channel regardless of network ID.
	//byte	BROADCAST_NETWORK_ID	= (byte) (Math.pow(2, NETWORK_NUMBER_BITS) - 1);
	//byte	DEFAULT_NETWORK_ID		= (byte) 0x01;
	//byte	ZERO_NETWORK_ID			= (byte) 0x00;
	public static final NetworkId	BROADCAST_NETWORK_ID		= new NetworkId((byte) (Math.pow(2, NETWORK_NUMBER_BITS) - 1));
	public static final NetworkId	DEFAULT_NETWORK_ID			= new NetworkId((byte) 0x01);
	public static final NetworkId	ZERO_NETWORK_ID				= new NetworkId((byte) 0x00);
	
	byte	SMAC_FRAME_BYTES	= 2;
	byte	PACKET_SIZE			= 125; 
	byte	MAX_PACKET_BYTES	= (byte) (PACKET_SIZE - SMAC_FRAME_BYTES);
	
	byte PACKET_HEADER_BYTES = 4;
	
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private INetworkDevice			mDevice;
	
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Boolean					mRequiresAck;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private PacketVersion			mPacketVersion	= new PacketVersion(PROTOCOL_VERSION, PROTOCOL_VERSION_BITS);
	private NBitInteger				mPacketType;
	private NBitInteger				mReservedHeaderBits;
	private NetworkId				mNetworkId;
	private NetAddress				mSrcAddr;
	private NetAddress				mDstAddr;
	private ICommand				mCommand;
	private long					mCreateTimeMillis;
	private long					mSentTimeMillis;
	private int						mSendCount;
	private byte					mAckId;
	private volatile AckStateEnum	mAckState;
	private byte[]					mAckData;
	private byte					mLQI;

	// --------------------------------------------------------------------------
	/**
	 * This is how we create a packet that we want to transmit.
	 *  @param inCommand
	 *  @param inSrcAddr
	 *  @param inDstAddr
	 *  @throws NullPointerException
	 */
	public PacketV0(final ICommand inCommand,
		final NetworkId inNetworkId,
		final NetAddress inSrcAddr,
		final NetAddress inDstAddr,
		final boolean inAckRequested) {

		mNetworkId = inNetworkId;
		mPacketType = new NBitInteger(ACK_REQUIRED_BITS, STD_PACKET);
		mReservedHeaderBits = new NBitInteger(RESERVED_HEADER_BITS, (byte) 0);

		if (inCommand == null)
			throw new NullPointerException("inCommand is null");

		if (inSrcAddr == null)
			throw new NullPointerException("inSrcAddr is null");

		if (inDstAddr == null)
			throw new NullPointerException("inDstAddr is null");

		mCommand = inCommand;
		mSrcAddr = inSrcAddr;
		mDstAddr = inDstAddr;
		mAckId = EMPTY_ACK_ID;
		mAckState = AckStateEnum.INVALID;
		mSendCount = 0;
		mCreateTimeMillis = System.currentTimeMillis();
		setRequiresAck(inAckRequested);
	}

	// --------------------------------------------------------------------------
	/**
	 * This is how we create a packet object when we only have it's raw data from the input stream.
	 *  @param inInputStream
	 */
	public PacketV0() {
		mPacketVersion = new PacketVersion(PROTOCOL_VERSION, PROTOCOL_VERSION_BITS);
		mReservedHeaderBits = new NBitInteger(RESERVED_HEADER_BITS, (byte) 0);
		mNetworkId = BROADCAST_NETWORK_ID;
		mSrcAddr = new NetAddress((byte)0x00, ADDRESS_BITS);
		mDstAddr = new NetAddress((byte)0x00, ADDRESS_BITS);
		mPacketType = new NBitInteger(ACK_REQUIRED_BITS, STD_PACKET);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String resultStr;

		resultStr = "net:" + mNetworkId.toString() + " src:" + mSrcAddr.toString() + " dst:" + mDstAddr.toString();

		if (mAckState != AckStateEnum.INVALID) {
			resultStr += " ackid:" + getAckId() + " ackState:" + getAckState() + " sendcnt:" + getSendCount();
		}

		if (mCommand != null) {
			resultStr += " command:" + mCommand.toString();
		}

		resultStr += " creationTimeMs:" + mCreateTimeMillis;

		return resultStr;
	}

	// --------------------------------------------------------------------------
	/**
	 * This function serializes the command into a raw data stream for network transmission.
	 *  @param inBitFieldOutputStream
	 */
	@Override
	public void toStream(BitFieldOutputStream inBitFieldOutputStream) {

		try {
			//this.computePacketSize();
			inBitFieldOutputStream.writeNBitInteger(mPacketVersion);
			inBitFieldOutputStream.writeNBitInteger(mPacketType);
			inBitFieldOutputStream.writeNBitInteger(mReservedHeaderBits);
			inBitFieldOutputStream.writeNBitInteger(mNetworkId);
			inBitFieldOutputStream.writeNBitInteger(mSrcAddr);
			inBitFieldOutputStream.writeNBitInteger(mDstAddr);
			inBitFieldOutputStream.writeByte(mAckId);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		mCommand.toStream(inBitFieldOutputStream);
		try {
			inBitFieldOutputStream.flush();
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * This function takes a serialization string of a raw command (from network transmission) and sets the packet structures.
	 *  @param inInputStream
	 */
	@Override
	public boolean fromStream(BitFieldInputStream inInputStream, int inFrameSize) {

		try {
			//mSize = inInputStream.readByte();
			inInputStream.readNBitInteger(mPacketVersion);
			
			if (mPacketVersion.getValue() != (int) PROTOCOL_VERSION) {
				mCommand = null;
				return false;
			}
			
			inInputStream.readNBitInteger(mPacketType);
			inInputStream.readNBitInteger(mReservedHeaderBits);
			inInputStream.readNBitInteger(mNetworkId);
			inInputStream.readNBitInteger(mSrcAddr);
			inInputStream.readNBitInteger(mDstAddr);
			mAckId	= inInputStream.readByte();

			if (mPacketType.getValue() == STD_PACKET) {
				mCommand = CommandFactory.createCommand(inInputStream, this.packetPayloadSize(inFrameSize));
			} else {
				mAckData = new byte[ACK_DATA_BYTES];
				inInputStream.readByte();	// Read out command byte and disregard
				inInputStream.readBytes(mAckData, ACK_DATA_BYTES);
			}
			
			mCreateTimeMillis = System.currentTimeMillis();
			return true;
			
		} catch (IOException e) {
			LOGGER.error("", e);
			return false;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	private int packetPayloadSize(int inFrameSize) {

		return (inFrameSize - PACKET_HEADER_BYTES);

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getCommand()
	 */
	@Override
	public ICommand getCommand() {
		return mCommand;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getSrcAddr()
	 */
	@Override
	public NetAddress getSrcAddr() {

		return mSrcAddr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getDstAddr()
	 */
	@Override
	public NetAddress getDstAddr() {

		return mDstAddr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#setCommand(com.codeshelf.flyweight.command.ICommand)
	 */
	@Override
	public void setCommand(ICommand inCommand) {
		mCommand = inCommand;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.IPacket#getSentTimeMillis()
	 */
	@Override
	public long getSentTimeMillis() {
		return mSentTimeMillis;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCreateTimeMillis
	 * @return
	 */
	@Override
	public long getCreateTimeMillis() {
		return mCreateTimeMillis;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.IPacket#setSendTime(long)
	 */
	@Override
	public void setSentTimeMillis(long inSentTimeMillis) {
		mSentTimeMillis = inSentTimeMillis;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getNetworkId()
	 */
	@Override
	public NetworkId getNetworkId() {
		return mNetworkId;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#setNetworkId(com.codeshelf.flyweight.command.NetworkId)
	 */
	@Override
	public void setNetworkId(NetworkId inNetworkId) {
		mNetworkId = inNetworkId;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	@Override
	public byte getAckId() {
		return mAckId;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAckId
	 */
	@Override
	public void setAckId(final byte inAckId) {
		mAckId = inAckId;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#incrementSendCount()
	 */
	@Override
	public void incrementSendCount() {
		mSendCount++;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getSendCount()
	 */
	@Override
	public int getSendCount() {
		return mSendCount;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#setSendCount(int)
	 */
	@Override
	public void setSendCount(int inResendCount) {
		mSendCount = inResendCount;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getAckState()
	 */
	@Override
	public AckStateEnum getAckState() {
		return mAckState;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#setAckedState(com.codeshelf.flyweight.command.AckedStateEnum)
	 */
	@Override
	public void setAckState(AckStateEnum inAckedState) {
		mAckState = inAckedState;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getAckData()
	 */
	@Override
	public byte[] getAckData() {
		return mAckData;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#setAckData(int)
	 */
	@Override
	public void setAckData(byte[] inAckData) {
		mAckData = inAckData;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#getPacketType()
	 */
	@Override
	public byte getPacketType() {
		// This bit of weirdness is to deal with the lack of unsigned bytes in Java.
		if (mPacketType.getValue() == ACK_PACKET) {
			return ACK_PACKET;
		} else {
			return STD_PACKET;
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.command.IPacket#setPacketType(byte)
	 */
	@Override
	public void setPacketType(byte inPacketType) {
		mPacketType.setValue(inPacketType);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Set the LQI of the packet 
	 * @param lqi
	 */
	public void setLQI(byte inLQI) {
		mLQI = inLQI;
	}

	// --------------------------------------------------------------------------
	/**
	 * Get the lqi of the packet
	 */
	public byte getLQI() {
		return mLQI;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Get the maximum packet payload for packet
	 */
	public byte getMaxPacketBytes() {
		return (byte) (MAX_PACKET_BYTES - PACKET_HEADER_BYTES);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Get the maximum packet payload for packet
	 */
	public byte getHeaderByteCount() {
		return PACKET_HEADER_BYTES;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Get the bytes of the payload (everything after ackid)
	 */
	public byte[] getPayloadBytes(){
		byte[] packetBytes;
		byte[] payloadBytes;
		
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
		BitFieldOutputStream bitFieldOutStream = new BitFieldOutputStream(byteArrayStream);
		byteArrayStream.reset();
		this.toStream(bitFieldOutStream);
		
		packetBytes = byteArrayStream.toByteArray();
		
		if (packetBytes == null){
			return null;
		}
		
		payloadBytes = Arrays.copyOfRange(packetBytes, PACKET_HEADER_BYTES, packetBytes.length - 1);
		
		return payloadBytes;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Get empty ack id
	 */
	public byte getEmptyAckId() {
		return EMPTY_ACK_ID;
	}
}
