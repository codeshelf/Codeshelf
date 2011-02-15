/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperAlphaClear.java,v 1.1 2011/02/15 02:39:46 jeffw Exp $
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
public final class AtopCmdMapperAlphaClear {

	private AtopCmdMapperAlphaClear() {

	}

	public static void mapAtopToCodeShelf(List<ICsCommand> inResultsList, PickTag inPickTag, byte[] inDataBytes) {
		inResultsList.add(new CommandCsDisplayClear(inPickTag));
	}
}
