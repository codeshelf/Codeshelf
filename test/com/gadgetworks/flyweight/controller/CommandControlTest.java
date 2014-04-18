/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlTest.java,v 1.3 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.Test;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;
import com.gadgetworks.flyweight.command.CommandControlButton;
import com.gadgetworks.flyweight.command.CommandControlMessage;
import com.gadgetworks.flyweight.command.CommandControlRequestQty;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.command.Packet;

public final class CommandControlTest extends CommandABCTest {

	private static final byte[]	REQUEST_PACKET_IN_DATA	= { 0x01, 0x00, 0x01, 0x00, 0x31, 0x03, 0x05, 0x02, 0x01, 0x03 };
	private static final byte[]	REQUEST_PACKET_OUT_DATA	= { 0x01, 0x00, 0x08, 0x00, 0x31, 0x03, 0x05, 0x02, 0x01, 0x03 };

	private static final byte[]	BUTTON_PACKET_IN_DATA	= { 0x01, 0x00, 0x01, 0x00, 0x31, 0x04, 0x05, 0x02 };

	private static final String	TEST_MSG1				= "TEST1";
	private static final String	TEST_MSG2				= "TEST2";

	private static final Byte	POS_NUM					= 5;
	private static final Byte	MIN_VALUE				= 1;
	private static final Byte	REQ_VALUE				= 2;
	private static final Byte	MAX_VALUE				= 3;
	private static final Byte	FREQ_VALUE				= 4;
	private static final Byte	DUTYCYCLE_VALUE			= 5;

	public CommandControlTest(final String inName) {
		super(inName);
	}

	@Override
	protected ICommand createCommandABC() throws Exception {
		return new CommandControlMessage(NetEndpoint.PRIMARY_ENDPOINT, TEST_MSG1, TEST_MSG2);
	}

	@Test
	public void testRequestCommandFromStream() {

		ByteArrayInputStream byteArray = new ByteArrayInputStream(REQUEST_PACKET_IN_DATA);
		BitFieldInputStream inputStream = new BitFieldInputStream(byteArray, true);

		// Create the packet from the input stream.
		IPacket packet = new Packet();
		packet.fromStream(inputStream, REQUEST_PACKET_IN_DATA.length);

		// Get the command from the packet.
		ICommand command = packet.getCommand();

		// If it is not the datagram command then something went wrong.
		if (!(command instanceof CommandControlRequestQty))
			fail("Not a CommandControlRequestQty command");

		Byte posNum = ((CommandControlRequestQty) command).getPosNum();
		if (!POS_NUM.equals(posNum))
			fail("Command data is not correct");

		Byte req = ((CommandControlRequestQty) command).getReqValue();
		if (!REQ_VALUE.equals(req))
			fail("Command data is not correct");

		Byte min = ((CommandControlRequestQty) command).getMinValue();
		if (!MIN_VALUE.equals(min))
			fail("Command data is not correct");

		Byte max = ((CommandControlRequestQty) command).getMaxValue();
		if (!MAX_VALUE.equals(max))
			fail("Command data is not correct");

	}

	@Test
	public void testRequestCommandToStream() {

		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		BitFieldOutputStream outputStream = new BitFieldOutputStream(byteArray);

		// Create a new command.
		ICommand command = new CommandControlRequestQty(NetEndpoint.PRIMARY_ENDPOINT,
			POS_NUM,
			REQ_VALUE,
			MIN_VALUE,
			MAX_VALUE,
			FREQ_VALUE,
			DUTYCYCLE_VALUE);

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
		if (!Arrays.equals(resultBytes, REQUEST_PACKET_OUT_DATA))
			fail("Command data is not correct");
	}

	@Test
	public void testButtonCommandFromStream() {

		ByteArrayInputStream byteArray = new ByteArrayInputStream(BUTTON_PACKET_IN_DATA);
		BitFieldInputStream inputStream = new BitFieldInputStream(byteArray, true);

		// Create the packet from the input stream.
		IPacket packet = new Packet();
		packet.fromStream(inputStream, REQUEST_PACKET_IN_DATA.length);

		// Get the command from the packet.
		ICommand command = packet.getCommand();

		// If it is not the datagram command then something went wrong.
		if (!(command instanceof CommandControlButton))
			fail("Not a CommandControlRequestQty command");

		Byte posNum = ((CommandControlButton) command).getPosNum();
		if (!POS_NUM.equals(posNum))
			fail("Command data is not correct");

		Byte req = ((CommandControlButton) command).getValue();
		if (!REQ_VALUE.equals(req))
			fail("Command data is not correct");

	}
}
