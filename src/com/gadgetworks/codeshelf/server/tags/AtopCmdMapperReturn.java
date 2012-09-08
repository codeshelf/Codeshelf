/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperReturn.java,v 1.5 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsAckPressed;
import com.gadgetworks.codeshelf.command.CommandCsDisplayClear;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopCmdMapperReturn extends AtopCmdMapper {

	private AtopCmdMapperReturn() {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inResultsList
	 * @param inPickTag
	 * @param inDataBytes
	 */
	public static void mapAtopToCodeShelf(List<ICsCommand> inResultsList, PickTag inPickTag, byte[] inDataBytes) {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 * @param inPickTag
	 * @return
	 */
	public static byte[] mapCodeShelfToAtop(final ICsCommand inCommand, final PickTag inPickTag) {

		byte[] result = null;
		if (inCommand instanceof CommandCsAckPressed) {

			CommandCsAckPressed ackCmd = (CommandCsAckPressed) inCommand;

			String menuText = ackCmd.getMenuText();

			if ((menuText == null) || (menuText.length() == 0)) {
				// Send a simple 0x64 Ack ATOP command.
				result = new byte[10];
				buildHeader(result, (byte) 0x64, (byte) inPickTag.getSerialBusPosition());
				result[8] = 0x00;
				result[9] = 0x16;
			} else {
				// Send an 0x06 menu select ATOP command.
				result = new byte[10 + menuText.length()];
				buildHeader(result, (byte) 0x06, (byte) inPickTag.getSerialBusPosition());
				System.arraycopy(menuText.getBytes(), 0, result, 9, menuText.length());
			}
		}

		return result;
	}
}
