/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceEmbedded.java,v 1.16 2013/04/17 19:54:49 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.CommandControlMessage;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This is the CHE code that runs on the device itself.
 * 
 * @author jeffw
 *
 */
public class CheDeviceEmbedded extends AisleDeviceEmbedded {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(DeviceEmbeddedABC.class);

	private SerialPort			mSerialPort;

	@Inject
	public CheDeviceEmbedded(@Named(IEmbeddedDevice.GUID_PROPERTY) final String inGuidStr,
		@Named(IEmbeddedDevice.CONTROLLER_IPADDR_PROPERTY) final String inIpAddrStr) {
		super(inGuidStr, inIpAddrStr);
	}

	@Override
	public final void doStart() {
		super.doStart();
		
		try {
			mSerialPort = new SerialPort("/dev/ttyACM0");
			mSerialPort.openPort();
			mSerialPort.setParams(38400, 8, 1, 0);
			mSerialPort.writeString("^CHE`CONNECT~");
		} catch (SerialPortException e) {
			LOGGER.error("", e);
		}
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

			case CommandControlABC.LIGHT:
				processControlLightCommand((CommandControlLight) inCommand);
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
		LOGGER.info("Display message: Line1:'" + inCommand.getLine1MessageStr() + "' Line2:'" + inCommand.getLine2MessageStr()
				+ "'");
		try {
			mSerialPort.writeString("^" + inCommand.getLine1MessageStr() + "`" + inCommand.getLine2MessageStr() + "~");
		} catch (SerialPortException e) {
			LOGGER.error("", e);
		}
	}
}
