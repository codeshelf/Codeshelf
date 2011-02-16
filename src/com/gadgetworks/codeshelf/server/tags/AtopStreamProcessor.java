/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopStreamProcessor.java,v 1.3 2011/02/16 23:40:40 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.CommandCsReportPick;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopStreamProcessor {

	private static final Log	LOGGER				= LogFactory.getLog(AtopStreamProcessor.class);

	private static final int	SHORT_HEADER_BYTES	= 7;
	private static final int	LONG_HEADER_BYTES	= 8;
	private static final short	BYTE_MASK			= (short) 0xff;
	private static final int	SHIFT_BYTE_BITS		= 8;

	/**
	 * Prevent utility class from getting instantiated.
	 */
	private AtopStreamProcessor() {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataInputStream
	 * @return
	 */
	public static List<ICsCommand> getNextCsCommands(Socket inSocket, DataInputStream inDataInputStream, ControlGroup inControlGroup) {
		List<ICsCommand> result = new ArrayList<ICsCommand>();

		short cmdSize = 0;
		short msgType = 0;
		@SuppressWarnings("unused")
		byte reserved = 0;
		short subCommand = 0;
		short subNode = 0;
		byte[] cmdDataBytes = null;

		try {
			try {
				// Read the commmand size.
				short loByte = inDataInputStream.readByte();
				short hiByte = inDataInputStream.readByte();
				cmdSize = (short) ((hiByte << SHIFT_BYTE_BITS) + loByte);

				// Read the message type
				msgType = (short) (BYTE_MASK & inDataInputStream.readByte());

				// Read the reserved bytes.
				reserved = inDataInputStream.readByte();
				reserved = inDataInputStream.readByte();
				reserved = inDataInputStream.readByte();

				// Read the sub-command
				subCommand = (short) (BYTE_MASK & inDataInputStream.readByte());
				AtopCommandEnum commandEnum = AtopCommandEnum.getCommandEnum(subCommand);

				if (hasSubNode(commandEnum)) {
					subNode = (short) (BYTE_MASK & inDataInputStream.readByte());
					cmdSize -= LONG_HEADER_BYTES;
				} else {
					cmdSize -= SHORT_HEADER_BYTES;
				}
				if (cmdSize < 0) {
					LOGGER.error("ATOP command size smaller than the header!");
				} else {
					cmdDataBytes = new byte[cmdSize];
					inDataInputStream.readFully(cmdDataBytes);
				}

				if (commandEnum == AtopCommandEnum.INVALID) {
					LOGGER.error("Unsupported ATOP sub-command: " + subCommand);
				} else {
					convertToCsCommands(result, inControlGroup, commandEnum, subNode, cmdDataBytes);
				}
			} catch (EOFException e) {
				// EOF is caused by a dropped socket - don't trace it - just close the socket.
				inSocket.close();
			}
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAtopCommandEnum
	 * @return
	 */
	private static boolean hasSubNode(AtopCommandEnum inAtopCommandEnum) {
		boolean result = true;

		switch (inAtopCommandEnum) {
			case READ_ALL_STATUS:
				// These Atop commands have no sub-node.
				result = false;
				break;
			default:
				// All others do.
				result = true;
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inResultsList
	 * @param inCommandEnum
	 * @param inSubCommand
	 * @param inDataBytes
	 */
	private static void convertToCsCommands(List<ICsCommand> inResultsList,
		ControlGroup inControlGroup,
		AtopCommandEnum inCommandEnum,
		short inSubNode,
		byte[] inDataBytes) {

		PickTag pickTag = inControlGroup.getPickTagBySerialBusNumber(inSubNode);
		if (pickTag != null) {

			LOGGER.info("Atop cmd: " + inCommandEnum.getName() + " node:" + inSubNode + " data:" + Arrays.toString(inDataBytes));

			switch (inCommandEnum) {
				case ALPHA_NUM_PUSH:
					AtopCmdMapperAlphaPush.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case ALPHA_NUM_CLEAR:
					AtopCmdMapperAlphaClear.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case LED_ON:
					AtopCmdMapperLedOn.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case LED_OFF:
					AtopCmdMapperLedOff.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case SET_MAX_DEVICES:
					AtopCmdMapperSetMaxDevices.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case READ_ALL_STATUS:
					AtopCmdMapperReadAllStatus.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case PICK_MODE:
					AtopCmdMapperPickMode.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case DIGIT_LIMIT:
					AtopCmdMapperDigitLimit.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				case TAG_CONFIG:
					AtopCmdMapperTagConfig.mapAtopToCodeShelf(inResultsList, pickTag, inDataBytes);
					break;

				default:
					break;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCsCommands
	 */
	public static void sendCsCommandToAtopConnection(ICsCommand inCsCommand) {

		PickTag pickTag = inCsCommand.getPickTag();
		ControlGroup controlGroup = pickTag.getParentControlGroup();
		IControllerConnection connection = controlGroup.getControllerConnection();

		if (connection != null) {
			byte[] dataBytes = null;
			switch (inCsCommand.getCommandIdEnum()) {
				case CS_ACK_PRESSED:
					dataBytes = AtopCmdMapperReturn.mapCodeShelfToAtop(inCsCommand, pickTag);
					break;
				case CS_REPORT_PICK:
					dataBytes = AtopCmdMapperConfirm.mapCodeShelfToAtop(inCsCommand, pickTag);
					break;
				case CS_REPORT_SHORT:
					dataBytes = AtopCmdMapperShortage.mapCodeShelfToAtop(inCsCommand, pickTag);
					break;
				default:
			}
			if (dataBytes != null) {
				connection.sendDataBytes(dataBytes);
			}
		}
	}
}
