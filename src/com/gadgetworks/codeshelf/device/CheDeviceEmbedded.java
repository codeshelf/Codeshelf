/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CheDeviceEmbedded.java,v 1.18 2013/04/19 23:23:25 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandControlABC;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.CommandControlMessage;
import com.gadgetworks.flyweight.command.CommandControlScan;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.Packet;
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
	public CheDeviceEmbedded(@Named(IEmbeddedDevice.GUID_PROPERTY) final String inGuidStr, @Named(IEmbeddedDevice.CONTROLLER_IPADDR_PROPERTY) final String inIpAddrStr) {
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

		processScans();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void processScans() {
		Thread eventThread = new Thread(new Runnable() {
			public void run() {
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				while (true) {
					try {
						String scanValue = reader.readLine();

						ICommand command = new CommandControlScan(NetEndpoint.PRIMARY_ENDPOINT, scanValue);
						IPacket packet = new Packet(command, getNetworkId(), getNetAddress(), new NetAddress(IPacket.GATEWAY_ADDRESS), false);
						command.setPacket(packet);
						sendPacket(packet);
					} catch (IOException e) {
						LOGGER.error("", e);
					}
				}
			}
		});
		eventThread.start();
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
		LOGGER.info("Display message: Line1:'" + inCommand.getLine1MessageStr() + "' Line2:'" + inCommand.getLine2MessageStr() + "'");
		try {
			mSerialPort.writeString("^" + inCommand.getLine1MessageStr() + "`" + inCommand.getLine2MessageStr() + "~");
		} catch (SerialPortException e) {
			LOGGER.error("", e);
		}
	}
}
