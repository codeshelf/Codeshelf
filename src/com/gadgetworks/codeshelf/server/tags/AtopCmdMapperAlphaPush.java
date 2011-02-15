/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperAlphaPush.java,v 1.2 2011/02/15 22:16:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsDisplayCount;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopCmdMapperAlphaPush {

//	private static final Log	LOGGER	= LogFactory.getLog(AtopCmdMapperAlphaPush.class);

	private AtopCmdMapperAlphaPush() {

	}

	public static void mapAtopToCodeShelf(List<ICsCommand> inResultsList, PickTag inPickTag, byte[] inDataBytes) {
		int quantity = 0;
		int multiplier = 100000;
		for (int pos = 0; pos < 6; pos++) {
			if (inDataBytes[pos] > 0x30) {
				quantity += (inDataBytes[pos] - 0x30) * multiplier;
			}
			multiplier /= 10;
		}
		inResultsList.add(new CommandCsDisplayCount(inPickTag, quantity));
	}
}
