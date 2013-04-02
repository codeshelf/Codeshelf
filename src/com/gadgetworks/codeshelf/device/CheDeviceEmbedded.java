/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceEmbedded.java,v 1.12 2013/04/02 04:29:02 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControlMessage;

/**
 * This is the CHE code that runs on the device itself.
 * 
 * @author jeffw
 *
 */
public class CheDeviceEmbedded extends DeviceEmbeddedABC {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(DeviceEmbeddedABC.class);

	public CheDeviceEmbedded() {
		super("00000002", "10.47.47.49");
	}
	
	public void doStart() {
		
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
				processControlMessageCommand((CommandControlMessage) inCommand);
				break;

			case CommandControlABC.SCAN:
				break;

			default:
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void processControlMessageCommand(CommandControlMessage inCommand) {
		LOGGER.info("Display message: Line1:'" + inCommand.getLine1MessageStr() + "' Line2:'" + inCommand.getLine2MessageStr() + "'");
	}
}
