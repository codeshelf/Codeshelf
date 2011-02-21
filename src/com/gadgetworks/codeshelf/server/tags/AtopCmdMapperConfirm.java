/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperConfirm.java,v 1.2 2011/02/21 21:33:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsDisplayClear;
import com.gadgetworks.codeshelf.command.CommandCsReportPick;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopCmdMapperConfirm extends AtopCmdMapper {

	private AtopCmdMapperConfirm() {

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

		buildHeader(result, (byte) 0x06, (byte) inPickTag.getSerialBusPosition());

		CommandCsReportPick command = (CommandCsReportPick) inCommand;
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
