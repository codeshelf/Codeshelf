/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperReturn.java,v 1.1 2011/02/16 23:40:40 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsDisplayClear;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.persist.PickTag;

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
		byte[] result = new byte[10];

		buildHeader(result, (byte) 10, (byte) 0x64, (byte) inPickTag.getSerialBusPosition());
		result[8] = 0x00;
		result[9] = 0x16;

		return result;
	}
}
