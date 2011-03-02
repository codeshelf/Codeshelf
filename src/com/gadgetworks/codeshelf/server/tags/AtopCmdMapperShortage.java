/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperShortage.java,v 1.3 2011/03/02 02:34:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsReportShort;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopCmdMapperShortage extends AtopCmdMapper {

	private AtopCmdMapperShortage() {

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
		byte[] result = new byte[15];

		buildHeader(result, (byte) 0x07, (byte) inPickTag.getSerialBusPosition());

		CommandCsReportShort command = (CommandCsReportShort) inCommand;
		byte[] qtyStr = Integer.toString(command.getQuantity()).getBytes();
		int strPos = qtyStr.length;
		for (int resultPos = 13; resultPos > 7; resultPos--) {
			if (strPos > 0) {
				result[resultPos] = qtyStr[strPos-1];
				strPos--;
			}
		}

		return result;
	}
}
