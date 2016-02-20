package com.codeshelf.flyweight.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.controller.INetworkDevice;

public class PacketFactory {
	private static final Logger		LOGGER				= LoggerFactory.getLogger(PacketFactory.class);		
	
	/**
	 * Returns the correct packet version for this device.
	 * 
	 */
	public IPacket getPacketForDevice(
		INetworkDevice inDevice,
		final ICommand inCommand,
		final NetworkId inNetworkId,
		final NetAddress inSrcAddr,
		final NetAddress inDstAddr,
		final boolean inAckRequested) {
		
		IPacket packet = null;
		NBitInteger protocolVersion = inDevice.getPacketVersion();
		
		if (protocolVersion == null) {
			LOGGER.error("Device does not have protocol version defined. Returning null packet.");
			return packet;
		}
		
		if (protocolVersion.equals(IPacket.PACKET_VERSION_0)) {
			packet = new PacketV0(inCommand, inNetworkId, inSrcAddr, inDstAddr, inAckRequested);
		} else if (protocolVersion.equals(IPacket.PACKET_VERSION_1)) {
			packet = new PacketV1(inCommand, inNetworkId, inSrcAddr, inDstAddr, inAckRequested);
		} else {
			LOGGER.error("Unknown protocol version. Returning null packet.");
		}
		
		return packet;
	}
	
	/**
	 * Returns the correct packet version for defined protocol version.
	 * 
	 */
	public IPacket getPacketForProtocol(
		final byte inProtocol,
		final ICommand inCommand,
		final NetworkId inNetworkId,
		final NetAddress inSrcAddr,
		final NetAddress inDstAddr,
		final boolean inAckRequested) {
		
		IPacket packet = null;
		
		if (inProtocol == IPacket.PACKET_VERSION_0) {
			packet = new PacketV0(inCommand, inNetworkId, inSrcAddr, inDstAddr, inAckRequested);
		} else if (inProtocol == IPacket.PACKET_VERSION_1) {
			packet = new PacketV1(inCommand, inNetworkId, inSrcAddr, inDstAddr, inAckRequested);
		} else {
			LOGGER.error("Unknown protocol version. Returning null packet.");
		}
		
		return packet;
	}
	
	/**
	 * Returns the correct packet version for defined protocol version.
	 * 
	 */
	public IPacket getPacketForProtocol(final byte inProtocol) {
		
		IPacket packet = null;
		
		if (inProtocol == IPacket.PACKET_VERSION_0) {
			packet = new PacketV0();
		} else if (inProtocol == IPacket.PACKET_VERSION_1) {
			packet = new PacketV1();
		} else {
			LOGGER.error("Unknown protocol version. Returning null packet.");
		}
		
		return packet;
	}
	
	public byte getAddressBitsForProtocol(final byte inProtocol) {
		if (inProtocol == IPacket.PACKET_VERSION_0) {
			return PacketV0.ADDRESS_BITS;
		} else if (inProtocol == IPacket.PACKET_VERSION_1) {
			return PacketV1.ADDRESS_BITS;
		} else {
			LOGGER.error("Unknown protocol version. Returning zero.");
			return 0;
		}
	}
	
	/**
	 * Returns the maximum number of payload bytes in a packet for
	 * this device.
	 * 
	 */
	public int getPayloadSizeForDevice(INetworkDevice inDevice) {
		
		int packetPayloadSize = 0;
		NBitInteger protocolVersion = inDevice.getPacketVersion();
		
		if (protocolVersion == null) {
			LOGGER.error("Device does not have protocol version defined. Returning 0.");
			return packetPayloadSize;
		}
		
		if (protocolVersion.equals(IPacket.PACKET_VERSION_0)) {
			
		} else if (protocolVersion.equals(IPacket.PACKET_VERSION_1)) {
			
		} else {
			LOGGER.error("Unknown protocol version. Returning 0.");
		}
		
		return packetPayloadSize;
	}
	
	/**
	 * Returns the maximum number of payload bytes in a packet for
	 * this device.
	 * 
	 */
	public byte getAddressBitsForDevice(INetworkDevice inDevice) {
		
		byte packetAddressBits = 0;
		NBitInteger protocolVersion = inDevice.getPacketVersion();
		
		if (protocolVersion == null) {
			LOGGER.error("Device does not have protocol version defined. Returning 0.");
			return packetAddressBits;
		}
		
		if (protocolVersion.equals(IPacket.PACKET_VERSION_0)) {
			packetAddressBits = PacketV0.ADDRESS_BITS;
		} else if (protocolVersion.equals(IPacket.PACKET_VERSION_1)) {
			packetAddressBits = PacketV1.ADDRESS_BITS;
		} else {
			LOGGER.error("Unknown protocol version. Returning 0.");
		}
		
		return packetAddressBits;
	}
}
