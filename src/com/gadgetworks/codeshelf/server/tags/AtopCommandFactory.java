/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCommandFactory.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jeffw
 *
 */
public final class AtopCommandFactory {

	private static final Log	LOGGER					= LogFactory.getLog(AtopCommandFactory.class);

	private static final int	DATA_WAIT_MILLIS		= 10;

	private static final int	HEADER_BYTES			= 8;
	private static final byte	BYTE_MASK				= (byte) 0xff;
	private static final int	SHIFT_BYTE_BITS			= 8;

	private static final int	SHOW_ALPHANUMERIC		= 0x00;
	private static final int	TURN_OFF_ALPHANUMERIC	= 0x01;
	private static final int	SET_MAX_DEVICES			= 0x08;
	private static final int	READ_ALL_TAG_STATUSES	= 0x09;

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
	public static IAtopCommand getNextCommand(DataInputStream inDataInputStream) {
		IAtopCommand result = null;

		short cmdSize = 0;
		short msgType = 0;
		@SuppressWarnings("unused")
		byte reserved = 0;
		short subCommand = 0;
		short subNode = 0;
		byte[] cmdDataBytes = null;
		int cmdBytesRead = 0;

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

			// If we created the command then populate it with data.
			if (result != null) {
				if (result.hasSubNode()) {
					subNode = (short) (BYTE_MASK & inDataInputStream.readByte());
					result.setSubNode(subNode);
				}

				cmdSize -= HEADER_BYTES;
				if (cmdSize < 0) {
					LOGGER.error("ATOP command size smaller than the header!");
				} else {
					cmdDataBytes = new byte[cmdSize];
					// Keep looping until we read all of the bytes.
					cmdBytesRead = 0;
					do {
						cmdBytesRead += inDataInputStream.read(cmdDataBytes);
					} while (cmdBytesRead < cmdSize);
					result.setDataBytes(cmdDataBytes);
				}
			}
		} catch (EOFException e) {
			// EOF is caused by a dropped socket - don't trace it.
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				LOGGER.error("", e);
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

		switch (inSubCommand) {
			case SHOW_ALPHANUMERIC:
				result = new AtopAlphaNumericOnCmd(inMsgType, inSubCommand);
				break;

			case TURN_OFF_ALPHANUMERIC:
				result = new AtopAlphaNumericOffCmd(inMsgType, inSubCommand);
				break;

			case SET_MAX_DEVICES:
				result = new AtopSetMaxDevicesCmd(inMsgType, inSubCommand);
				break;

			case READ_ALL_TAG_STATUSES:
				result = new AtopReadAllTagStatusesCmd(inMsgType, inSubCommand);
				break;

			default:
				break;
		}

		return result;
	}
}
