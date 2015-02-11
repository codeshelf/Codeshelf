/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: PacketTest.java,v 1.2 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;

/** --------------------------------------------------------------------------
 *  Test the Packet class.
 *  @author jeffw
 */
public final class PacketTest {

	private static final byte[]			PACKET_IN_DATA		= { 0x01, 0x00, 0x01, 0x00, 0x31, 0x01, 0x05, 0x54, 0x45, 0x53, 0x54, 0x31, 0x05, 0x54, 0x45, 0x53, 0x54, 0x32, 0x05, 0x54, 0x45, 0x53, 0x54, 0x33, 0x05, 0x54, 0x45, 0x53, 0x54, 0x34 };
	private static final byte[]			PACKET_OUT_DATA		= { 0x01, 0x00, 0x08, 0x00, 0x31, 0x01, 0x05, 0x54, 0x45, 0x53, 0x54, 0x31, 0x05, 0x54, 0x45, 0x53, 0x54, 0x32, 0x05, 0x54, 0x45, 0x53, 0x54, 0x33, 0x05, 0x54, 0x45, 0x53, 0x54, 0x34 };

	private static final String			TEST_MSG1			= "TEST1";
	private static final String			TEST_MSG2			= "TEST2";
	private static final String			TEST_MSG3			= "TEST3";
	private static final String			TEST_MSG4			= "TEST4";

	/**
	 * Test method for {@link com.codeshelf.flyweightcontroller.command.Packet#Packet(com.codeshelf.flyweightcontroller.command.CommandABC, com.codeshelf.flyweightcontroller.command.NetAddress, com.codeshelf.flyweightcontroller.command.NetAddress, byte)}.
	 */
	@Test
	public void testPacketConstructors() {

		// Test the case of a packet created for transmit.
		@SuppressWarnings("unused")
		IPacket packet;

		NetworkId networkId = new NetworkId((byte) 1);
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT, TEST_MSG1, TEST_MSG2, TEST_MSG3, TEST_MSG4);
		NetAddress srcAddr = new NetAddress(IPacket.GATEWAY_ADDRESS);
		NetAddress destAddr = new NetAddress(IPacket.BROADCAST_ADDRESS);

		packet = new Packet(command, networkId, srcAddr, destAddr, false);
		// OK, expected case.

		try {
			packet = new Packet(null, networkId, srcAddr, destAddr, false);
			Assert.fail();
		} catch (NullPointerException e) {
			// Expected case.
		}

		try {
			packet = new Packet(command, networkId, null, destAddr, false);
			Assert.fail();
		} catch (NullPointerException e) {
			// Expected case.
		}

		try {
			packet = new Packet(command, networkId, srcAddr, null, false);
			Assert.fail();
		} catch (NullPointerException e) {
			// Expected case.
		}

	}

	/**
	 * Test method for {@link com.codeshelf.flyweightcontroller.command.Packet#toStream(com.codeshelf.flyweightcontroller.bitfields.BitFieldOutputStream)}.
	 */
	@Test
	public void testFromStream() {

		ByteArrayInputStream byteArray = new ByteArrayInputStream(PACKET_IN_DATA);
		BitFieldInputStream inputStream = new BitFieldInputStream(byteArray, true);

		// Create the packet from the input stream.
		IPacket packet = new Packet();
		packet.fromStream(inputStream, PACKET_IN_DATA.length);

		// Get the command from the packet.
		ICommand command = packet.getCommand();

		// If it is not the datagram command then something went wrong.
		if (!(command instanceof CommandControlDisplayMessage))
			Assert.fail("Not a CommandControlMessage command");

		String message1 = ((CommandControlDisplayMessage) command).getLine1MessageStr();
		if (!TEST_MSG1.equals(message1))
			Assert.fail("Command data is not correct");

		String message2 = ((CommandControlDisplayMessage) command).getLine2MessageStr();
		if (!TEST_MSG2.equals(message2))
			Assert.fail("Command data is not correct");
	}

	/**
	 * Test method for {@link com.codeshelf.flyweightcontroller.command.Packet#fromStream(com.codeshelf.flyweightcontroller.bitfields.BitFieldInputStream)}.
	 */
	@Test
	public void testToStream() {

		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		BitFieldOutputStream outputStream = new BitFieldOutputStream(byteArray);

		// Create a new command.
		ICommand command = new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT, TEST_MSG1, TEST_MSG2, TEST_MSG3, TEST_MSG4);

		// Create the network ID
		NetworkId networkId = new NetworkId((byte) 1);

		// Create the addresses.
		NetAddress srcAddr;
		NetAddress dstAddr;

		srcAddr = new NetAddress(IPacket.GATEWAY_ADDRESS);
		dstAddr = new NetAddress(IPacket.ADDRESS_BITS);

		// Create a new packet to send to the output stream.
		IPacket packet = new Packet(command, networkId, srcAddr, dstAddr, false);

		// Stream the packet out.
		packet.toStream(outputStream);

		// Check the byte values of the stream to verify that it's right.
		byte[] resultBytes = byteArray.toByteArray();
		if (!Arrays.equals(resultBytes, PACKET_OUT_DATA))
			Assert.fail("Command data is not correct");
	}
}
