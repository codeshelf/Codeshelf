/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperTagConfig.java,v 1.1 2011/02/15 02:39:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsIndicatorColor;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopCmdMapperTagConfig {

	private AtopCmdMapperTagConfig() {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inResultsList
	 * @param inPickTag
	 * @param inDataBytes
	 */
	public static void mapAtopToCodeShelf(List<ICsCommand> inResultsList, PickTag inPickTag, byte[] inDataBytes) {
		ICsCommand outboundCommand = null;

		switch (inDataBytes[0]) {
			case 0:
				outboundCommand = getColorCsCmd(inPickTag, inDataBytes);
				break;
			case 1:
				break;
			case 2:
				break;
			case 3:
				break;
			case 4:
				outboundCommand = getIndicatorBlink(inPickTag, inDataBytes);
				break;
			case 5:
				break;
			case 6:
				break;
			case 0x0a:
				break;
			default:
		}

		if (outboundCommand != null) {
			inResultsList.add(outboundCommand);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTransport
	 */
	private final static ICsCommand getColorCsCmd(PickTag inPickTag, byte[] inDataBytes) {

		// Create a CommandCsIndColor.
		CommandCsIndicatorColor result = null;

		switch (inDataBytes[1]) {
			case 0x00:
				// Red
				result = new CommandCsIndicatorColor(inPickTag,
					CommandCsIndicatorColor.RED_ON,
					CommandCsIndicatorColor.GREEN_OFF,
					CommandCsIndicatorColor.BLUE_OFF);
				break;
			case 0x01:
				// Green
				result = new CommandCsIndicatorColor(inPickTag,
					CommandCsIndicatorColor.RED_ON,
					CommandCsIndicatorColor.GREEN_OFF,
					CommandCsIndicatorColor.BLUE_OFF);
				break;
			case 0x02:
				// Orange
				result = new CommandCsIndicatorColor(inPickTag,
					CommandCsIndicatorColor.RED_ON,
					CommandCsIndicatorColor.GREEN_ON,
					CommandCsIndicatorColor.BLUE_OFF);
				break;
			case 0x03:
				// Blue
				result = new CommandCsIndicatorColor(inPickTag,
					CommandCsIndicatorColor.RED_OFF,
					CommandCsIndicatorColor.GREEN_OFF,
					CommandCsIndicatorColor.BLUE_ON);
				break;
			case 0x04:
				// Pink
				result = new CommandCsIndicatorColor(inPickTag,
					CommandCsIndicatorColor.RED_ON,
					CommandCsIndicatorColor.GREEN_ON,
					CommandCsIndicatorColor.BLUE_ON);
				break;
			case 0x05:
				// Cyan
				result = new CommandCsIndicatorColor(inPickTag,
					CommandCsIndicatorColor.RED_OFF,
					CommandCsIndicatorColor.GREEN_ON,
					CommandCsIndicatorColor.BLUE_ON);
				break;
			default:
				// Red
				result = new CommandCsIndicatorColor(inPickTag,
					CommandCsIndicatorColor.RED_ON,
					CommandCsIndicatorColor.GREEN_OFF,
					CommandCsIndicatorColor.BLUE_OFF);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTransport
	 */
	private static ICsCommand getIndicatorBlink(PickTag inPickTag, byte[] inDataBytes) {

		ICsCommand result = null;

		switch (inDataBytes[1]) {
			case 0x00:
				// Off
				break;
			case 0x01:
				// Red
				break;
			default:
		}

		return result;
	}

}
