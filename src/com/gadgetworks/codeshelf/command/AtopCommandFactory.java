/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCommandFactory.java,v 1.2 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jeffw
 *
 */
public final class AtopCommandFactory {

	private static final Log	LOGGER					= LogFactory.getLog(AtopCommandFactory.class);

	private static final int	SHORT_HEADER_BYTES		= 7;
	private static final int	LONG_HEADER_BYTES		= 8;
	private static final short	BYTE_MASK				= (short) 0xff;
	private static final int	SHIFT_BYTE_BITS			= 8;

	/**
	 * Prevent utility class from getting instantiated.
	 */
	private AtopCommandFactory() {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataInputStream
	 * @return
	 */
	public static IAtopCommand getNextCommand(Socket inSocket, DataInputStream inDataInputStream) {
		IAtopCommand result = null;

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

				// Create the command.
				result = createCommand(msgType, subCommand);

				if (result == null) {
					// If we didn't create the command then it is a command we don't support.
					// We need to read (and discard) the remaining cmd bytes.
					cmdSize -= 7;
					cmdDataBytes = new byte[cmdSize];
					inDataInputStream.readFully(cmdDataBytes);
				} else {
					// If we created the command then populate it with data.
					if (result.hasSubNode()) {
						subNode = (short) (BYTE_MASK & inDataInputStream.readByte());
						result.setSubNode(subNode);
						cmdSize -= LONG_HEADER_BYTES;
					} else {
						cmdSize -= SHORT_HEADER_BYTES;
					}
					if (cmdSize < 0) {
						LOGGER.error("ATOP command size smaller than the header!");
					} else {
						cmdDataBytes = new byte[cmdSize];
						inDataInputStream.readFully(cmdDataBytes);
						result.setDataBytes(cmdDataBytes);
					}
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
	 * @param inSubCommand
	 * @return
	 */
	private static IAtopCommand createCommand(short inMsgType, short inSubCommand) {
		IAtopCommand result = null;

		AtopCommandEnum cmdEnum = AtopCommandEnum.getCommandEnum(inSubCommand);

		switch (cmdEnum) {
			case ALPHA_NUM_PUSH:
				result = new CommandAtopAlphaPush(inMsgType, inSubCommand);
				break;

			case ALPHA_NUM_CLEAR:
				result = new CommandAtopAlphaClear(inMsgType, inSubCommand);
				break;

			case LED_ON:
				result = new CommandAtopLedOn(inMsgType, inSubCommand);
				break;

			case LED_OFF:
				result = new CommandAtopLedOff(inMsgType, inSubCommand);
				break;

			case SET_MAX_DEVICES:
				result = new CommandAtopSetMaxDevices(inMsgType, inSubCommand);
				break;

			case READ_ALL_STATUS:
				result = new CommandAtopReadAllStatus(inMsgType, inSubCommand);
				break;

			case PICK_MODE:
				result = new CommandAtopPickMode(inMsgType, inSubCommand);
				break;

			case DIGIT_LIMIT:
				result = new CommandAtopDigitLimit(inMsgType, inSubCommand);
				break;

			case TAG_CONFIG:
				result = new CommandAtopTagConfig(inMsgType, inSubCommand);
				break;

			default:
				LOGGER.error("Unsupported ATOP sub-command: " + Integer.toHexString(inSubCommand));
				break;
		}

		return result;
	}
}
