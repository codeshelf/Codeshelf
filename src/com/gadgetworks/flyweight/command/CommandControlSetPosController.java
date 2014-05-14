/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlRequestQty.java,v 1.1 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.PosControllerInstr;
import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  Set position contoller.
 *  
 *  1B - Instruction count
 *  repeat:
 *  1B - Position Number
 *  1B - Requested Value
 *  1B - Min Value
 *  1B - Max Value
 *  1B - LED PWM freq
 *  1B - LED PWM duty cycle
 *
 *	}

 *  @author jeffw
 */
public final class CommandControlSetPosController extends CommandControlABC {

	private static final Logger			LOGGER					= LoggerFactory.getLogger(CommandControlSetPosController.class);

	private static final int			INSTRUCTION_COUNT_BYTES	= 1;
	private static final int			ONE_INSTRUCTION_BYTES	= 6;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private List<PosControllerInstr>	mInstructions;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlSetPosController(final NetEndpoint inEndpoint, List<PosControllerInstr> inInstructions) {
		super(inEndpoint, new NetCommandId(CommandControlABC.SET_POSCONTROLLER));
		mInstructions = inInstructions;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlSetPosController() {
		super(new NetCommandId(CommandControlABC.SET_POSCONTROLLER));
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		String result = "Pos controller set: ";

		for (PosControllerInstr instruction : mInstructions) {
			result += " pos:" + instruction.getPosition() + " qty: " + instruction.getReqQty() + " min: " + instruction.getMinQty()
					+ " max: " + instruction.getMaxQty() + " freq: " + instruction.getFreq() + " duty: "
					+ instruction.getDutyCycle();
		}

		return result;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localDecode(com.gadgetworks.bitfields.BitFieldOutputStream, int)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte((byte) mInstructions.size());
			for (PosControllerInstr instruction : mInstructions) {
				inOutputStream.writeByte(instruction.getPosition());
				inOutputStream.writeByte(instruction.getReqQty());
				inOutputStream.writeByte(instruction.getMinQty());
				inOutputStream.writeByte(instruction.getMaxQty());
				inOutputStream.writeByte(instruction.getFreq());
				inOutputStream.writeByte(instruction.getDutyCycle());
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#localEncode(com.gadgetworks.bitfields.BitFieldInputStream)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			short instructionCnt = inInputStream.readByte();
			mInstructions = new ArrayList<PosControllerInstr>();
			for (int instructionNum = 0; instructionNum < instructionCnt; instructionNum++) {
				byte position = inInputStream.readByte();
				byte reqQty = inInputStream.readByte();
				byte minQty = inInputStream.readByte();
				byte maxQty = inInputStream.readByte();
				byte freq = inInputStream.readByte();
				byte dutyCycle = inInputStream.readByte();
				PosControllerInstr instruction = new PosControllerInstr(position, reqQty, minQty, maxQty, freq, dutyCycle);
				mInstructions.add(instruction);
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.command.CommandABC#doComputeCommandSize()
	 */
	@Override
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + INSTRUCTION_COUNT_BYTES + (mInstructions.size() * ONE_INSTRUCTION_BYTES);
	}
}
