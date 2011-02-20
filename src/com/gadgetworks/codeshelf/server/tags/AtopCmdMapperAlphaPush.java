/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperAlphaPush.java,v 1.4 2011/02/20 00:18:34 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.Arrays;
import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsDisplayText;
import com.gadgetworks.codeshelf.command.CommandCsSetCount;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopCmdMapperAlphaPush extends AtopCmdMapper {

	//	private static final Log	LOGGER	= LogFactory.getLog(AtopCmdMapperAlphaPush.class);

	private AtopCmdMapperAlphaPush() {

	}

	public static void mapAtopToCodeShelf(List<ICsCommand> inResultsList, PickTag inPickTag, byte[] inDataBytes) {

		boolean isInteger = true;

		int quantity = 0;
		int multiplier = 100000;
		for (int pos = 0; pos < 6; pos++) {
			if ((inDataBytes[pos] >= 0x30) && (inDataBytes[pos] <= 0x39)) {
				quantity += (inDataBytes[pos] - 0x30) * multiplier;
			} else {
				isInteger = false;
			}
			multiplier /= 10;
		}

		if (isInteger) {
			inResultsList.add(new CommandCsSetCount(inPickTag, quantity));
		} else {
			String text = new String(Arrays.copyOfRange(inDataBytes, 1, inDataBytes.length));
			inResultsList.add(new CommandCsDisplayText(inPickTag, text));
		}
	}
}
