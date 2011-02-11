/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAtopTagConfig.java,v 1.1 2011/02/11 23:23:57 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.command;

import java.util.ArrayList;
import java.util.List;

import com.gadgetworks.codeshelf.controller.ITransport;

/**
 * @author jeffw
 *
 */
public class CommandAtopTagConfig extends CommandAtopABC {

	public CommandAtopTagConfig(final short inMsgType, final short inSubCommand) {
		super(CommandIdEnum.ATOP_TAG_CONFIG, inMsgType, inSubCommand);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	protected final boolean doHasSubNode() {
		return true;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doSetupOutboundCsCommands()
	 */
	public final List<ICsCommand> doSetupOutboundCsCommands() {
		
		List<ICsCommand> result = new ArrayList<ICsCommand>();;
		
		ICsCommand outboundCommand = null;
		
		switch (getDataBytes()[0]) {
			case 0:
				outboundCommand = getColorCsCmd();
				break;
			case 1:
				break;
			case 2:
				break;
			case 3:
				break;
			case 4:
				outboundCommand = getIndicatorBlink();
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
			result.add(outboundCommand);
		}
		
		return result;
	}
	
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.command.CommandAtopABC#doToTransport(com.gadgetworks.codeshelf.controller.ITransport)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTransport
	 */
	private ICsCommand getColorCsCmd() {
		
		// Create a CommandCsIndColor.
		CommandCsIndicatorColor result = null;
		
		switch (getDataBytes()[1]) {
			case 0x00:
				// Red
				result = new CommandCsIndicatorColor(CommandCsIndicatorColor.RED_ON, CommandCsIndicatorColor.GREEN_OFF, CommandCsIndicatorColor.BLUE_OFF);
				break;
			case 0x01:
				// Green
				result = new CommandCsIndicatorColor(CommandCsIndicatorColor.RED_OFF, CommandCsIndicatorColor.GREEN_ON, CommandCsIndicatorColor.BLUE_OFF);
				break;
			case 0x02:
				// Orange
				result = new CommandCsIndicatorColor(CommandCsIndicatorColor.RED_ON, CommandCsIndicatorColor.GREEN_ON, CommandCsIndicatorColor.BLUE_OFF);
				break;
			case 0x03:
				// Blue
				result = new CommandCsIndicatorColor(CommandCsIndicatorColor.RED_OFF, CommandCsIndicatorColor.GREEN_OFF, CommandCsIndicatorColor.BLUE_ON);
				break;
			case 0x04:
				// Pink
				result = new CommandCsIndicatorColor(CommandCsIndicatorColor.RED_ON, CommandCsIndicatorColor.GREEN_ON, CommandCsIndicatorColor.BLUE_ON);
				break;
			case 0x05:
				// Cyan
				result = new CommandCsIndicatorColor(CommandCsIndicatorColor.RED_OFF, CommandCsIndicatorColor.GREEN_ON, CommandCsIndicatorColor.BLUE_ON);
				break;
			default:
				// Red
				result = new CommandCsIndicatorColor(CommandCsIndicatorColor.RED_ON, CommandCsIndicatorColor.GREEN_OFF, CommandCsIndicatorColor.BLUE_OFF);
		}
		
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inTransport
	 */
	private ICsCommand getIndicatorBlink() {
		
		ICsCommand result = null;
		
		switch (getDataBytes()[1]) {
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

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.server.tags.CommandAtopABC#toString()
	 */
	public final String doToString() {
		StringBuffer result = new StringBuffer(super.doToString());

		result.append(" Config Op: ");
		switch (getDataBytes()[0]) {
			case 0:
				result.append("SetColor");
				break;
			case 1:
				result.append("ValidDigits");
				break;
			case 2:
				result.append("EnableModeConfig");
				break;
			case 3:
				result.append("DisableModeConfig");
				break;
			case 4:
				result.append("SetIndicatorBlink");
				break;
			case 5:
				result.append("SetDigitsBlink");
				break;
			case 6:
				result.append("SetDigitsBright");
				break;
			case 0x0a:
				result.append("SpecialFun");
				break;
			default:
		}
		return result.toString();
	}
}
