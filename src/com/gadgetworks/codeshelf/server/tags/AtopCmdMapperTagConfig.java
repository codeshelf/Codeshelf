/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapperTagConfig.java,v 1.4 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.util.List;

import com.gadgetworks.codeshelf.command.CommandCsIndicatorBlink;
import com.gadgetworks.codeshelf.command.CommandCsIndicatorColor;
import com.gadgetworks.codeshelf.command.CommandCsIndicatorOff;
import com.gadgetworks.codeshelf.command.CommandCsIndicatorOn;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopCmdMapperTagConfig extends AtopCmdMapper {

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
				outboundCommand = createIndicatorColorCmd(inPickTag, inDataBytes);
				break;
			case 1:
				break;
			case 2:
				break;
			case 3:
				break;
			case 4:
				outboundCommand = createIndicatorBlinkCmd(inPickTag, inDataBytes);
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
	private final static ICsCommand createIndicatorColorCmd(PickTag inPickTag, byte[] inDataBytes) {

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
					CommandCsIndicatorColor.RED_OFF,
					CommandCsIndicatorColor.GREEN_ON,
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
	private static ICsCommand createIndicatorBlinkCmd(PickTag inPickTag, byte[] inDataBytes) {

		ICsCommand result = null;

		switch (inDataBytes[1]) {
			case 0x00:
				// On
				result = new CommandCsIndicatorOn(inPickTag);
				break;
			case 0x01:
				// Off
				result = new CommandCsIndicatorOff(inPickTag);
				break;
			case 0x02:
				// 2 second
				result = new CommandCsIndicatorBlink(inPickTag, 2000);
				break;
			case 0x03:
				// 1 second
				result = new CommandCsIndicatorBlink(inPickTag, 1000);
				break;
			case 0x04:
				// 1/2 second
				result = new CommandCsIndicatorBlink(inPickTag, 500);
				break;
			case 0x05:
				// 1/4 second
				result = new CommandCsIndicatorBlink(inPickTag, 250);
				break;
			default:
				// On
				result = new CommandCsIndicatorOn(inPickTag);
		}

		return result;
	}

}
