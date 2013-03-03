/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleDeviceEmbedded.java,v 1.1 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControlLight;

/**
 * This is the CHE code that runs on the device itself.
 * 
 * @author jeffw
 *
 */
public class AisleDeviceEmbedded extends DeviceEmbeddedABC {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(DeviceEmbeddedABC.class);

	public AisleDeviceEmbedded() {
		super("00000003", "10.47.47.49");
	}
	
	// --------------------------------------------------------------------------
	/**
	 *  The radio controller sent this CHE a command.
	 *  @param inCommand    The control command that we want to process.  (The one just received.)
	 */
	protected final void processControlCmd(CommandControlABC inCommand) {

		// Figure out what kind of control sub-command we have.

		switch (inCommand.getExtendedCommandID().getValue()) {
			case CommandControlABC.MESSAGE:
				break;

			case CommandControlABC.SCAN:
				break;

			case CommandControlABC.LIGHT:
				processControlListCommand((CommandControlLight) inCommand);
				break;

			default:
		}
	}
	
	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processControlListCommand(CommandControlLight inCommand) {
		LOGGER.info("Light message: " + inCommand.toString());
	}
}
