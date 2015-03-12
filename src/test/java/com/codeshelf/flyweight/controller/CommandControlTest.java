/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlTest.java,v 1.3 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.CommandControlSetPosController;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;
import com.codeshelf.testframework.MinimalTest;

public final class CommandControlTest extends MinimalTest {

	private static final String	TEST_MSG1				= "TEST1";
	private static final String	TEST_MSG2				= "TEST2";
	private static final String	TEST_MSG3				= "TEST3";
	private static final String	TEST_MSG4				= "TEST4";

	private static final Byte	INSTRUCTION_CNT			= 1;
	private static final Byte	POS_NUM					= 5;
	private static final Byte	MIN_VALUE				= 1;
	private static final Byte	REQ_VALUE				= 2;
	private static final Byte	MAX_VALUE				= 3;
	private static final Byte	FREQ					= 4;
	private static final Byte	DUTYCYCLE				= 5;

	private static final byte[]	REQUEST_PACKET_IN_DATA	= { 0x01, 0x00, 0x01, 0x00, 0x31, CommandControlButton.SET_POSCONTROLLER,
			INSTRUCTION_CNT, POS_NUM, REQ_VALUE, MIN_VALUE, MAX_VALUE, FREQ, DUTYCYCLE };
	private static final byte[]	REQUEST_PACKET_OUT_DATA	= { 0x01, 0x00, 0x08, 0x00, 0x31, CommandControlButton.SET_POSCONTROLLER,
			INSTRUCTION_CNT, POS_NUM, REQ_VALUE, MIN_VALUE, MAX_VALUE, FREQ, DUTYCYCLE };

	private static final byte[]	BUTTON_PACKET_IN_DATA	= { 0x01, 0x00, 0x01, 0x00, 0x31, CommandControlButton.BUTTON, POS_NUM,
			REQ_VALUE, MIN_VALUE, MAX_VALUE, FREQ, DUTYCYCLE };

	protected ICommand createCommandABC() throws Exception {
		return new CommandControlDisplayMessage(NetEndpoint.PRIMARY_ENDPOINT, TEST_MSG1, TEST_MSG2, TEST_MSG3, TEST_MSG4);
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
		if (!(command instanceof CommandControlSetPosController))
			Assert.fail("Not a CommandControlRequestQty command");

		List<PosControllerInstr> instructions = ((CommandControlSetPosController) command).getInstructions();
		Assert.assertEquals(1, instructions.size());

		PosControllerInstr instruction = instructions.get(0);

		Assert.assertEquals(POS_NUM, instruction.getPosition());
		Assert.assertEquals(REQ_VALUE, instruction.getReqQty());
		Assert.assertEquals(MIN_VALUE, instruction.getMinQty());
		Assert.assertEquals(MAX_VALUE, instruction.getMaxQty());
		Assert.assertEquals(FREQ, instruction.getFreq());
		Assert.assertEquals(DUTYCYCLE, instruction.getDutyCycle());
	}

	@Test
	public void testRequestCommandToStream() {

		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		BitFieldOutputStream outputStream = new BitFieldOutputStream(byteArray);

		// Create a new command.
		List<PosControllerInstr> instructions = new ArrayList<PosControllerInstr>();
		PosControllerInstr instruction = new PosControllerInstr(POS_NUM, REQ_VALUE, MIN_VALUE, MAX_VALUE, FREQ, DUTYCYCLE);
		instructions.add(instruction);
		ICommand command = new CommandControlSetPosController(NetEndpoint.PRIMARY_ENDPOINT, instructions);

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
			Assert.fail("Command data is not correct");
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
			Assert.fail("Not a CommandControlRequestQty command");

		Byte posNum = ((CommandControlButton) command).getPosNum();
		if (!POS_NUM.equals(posNum))
			Assert.fail("Command data is not correct");

		Byte req = ((CommandControlButton) command).getValue();
		if (!REQ_VALUE.equals(req))
			Assert.fail("Command data is not correct");

	}
}
