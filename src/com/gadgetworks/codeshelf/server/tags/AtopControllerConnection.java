/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopControllerConnection.java,v 1.3 2011/02/07 20:11:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.AtopCommandFactory;
import com.gadgetworks.codeshelf.command.IAtopCommand;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopControllerConnection implements IControllerConnection {

	private static final Log		LOGGER						= LogFactory.getLog(AtopControllerConnection.class);

	private static final String		INTERFACE_THREAD_NAME		= "Control Group Socket Listener";
	private static final String		CONNECTION_THREAD_NAME		= "Control Group Connection ";

	private static final int		INTERFACE_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;
	private static final int		CONNECTION_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;

	private static final int		SOCKET_TIMEOUT_MILLIS		= 500;
	private static final short		BROADCAST_NODE				= 0x00fc;

	private ControlGroup			mControlGroup;
	private Thread					mServerThread;
	private ServerSocket			mServerSocket;
	private boolean					mShouldRun;
	private Map<Short, PickTag>	mSerialBusMap;

	public AtopControllerConnection(final ControlGroup inControlGroup) {
		mControlGroup = inControlGroup;

		// Atop tags do not have unique IDs.  Instead they are "numbered" on a serial bus from 1-to-200.
		// The host s/w must remember the bus number for each device on the controller.
		// We, on the other hand, have a MAC for each device.  We address commands to the MAC address
		// of the device (anywhere on the network).

		// To deal with the mismatch here, we maintain a mapping from the serial bus order to the MAC address.
		mSerialBusMap = new HashMap<Short, PickTag>();
		for (PickTag tag : mControlGroup.getPickTags()) {
			mSerialBusMap.put(tag.getSerialBusPosition(), tag);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void start() {
		try {
			mServerSocket = new ServerSocket(mControlGroup.getInterfacePortNum());
			LOGGER.info("Server socket started: " + mServerSocket.toString());
			mShouldRun = true;

			mServerThread = new Thread(new Runnable() {
				public void run() {
					processConnectAttempts();
				}
			}, INTERFACE_THREAD_NAME + mControlGroup.getId().toString());
			mServerThread.setPriority(INTERFACE_THREAD_PRIORITY);
			mServerThread.setDaemon(true);
			mServerThread.start();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void stop() {
		mShouldRun = false;
		if (mServerSocket != null) {
			try {
				mServerSocket.close();
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void processConnectAttempts() {
		while (mShouldRun) {
			try {
				final Socket socket = mServerSocket.accept();
				//socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);
				LOGGER.info("Socket connected: " + socket.toString());

				Thread connectionThread = new Thread(new Runnable() {
					public void run() {
						processConnectionData(socket);
					}
				}, CONNECTION_THREAD_NAME + socket.getRemoteSocketAddress());
				connectionThread.setPriority(CONNECTION_THREAD_PRIORITY);
				connectionThread.setDaemon(true);
				connectionThread.start();

			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inSocket
	 */
	private void processConnectionData(Socket inSocket) {
		DataInputStream dataInputStream = null;
		try {
			dataInputStream = new DataInputStream(inSocket.getInputStream());
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		if (dataInputStream != null) {
			while (!inSocket.isClosed()) {
				IAtopCommand command = AtopCommandFactory.getNextCommand(inSocket, dataInputStream);
				if (command != null) {
					if (command.hasSubNode()) {
						if (command.getSubNode() == BROADCAST_NODE) {
							// Broadcast the command to all tags.
						} else {
							// Unicast the command to a single tag.
							PickTag tag = mSerialBusMap.get(command.getSubNode());
							if (tag != null) {
								command.setDstAddr(tag.getNetAddress());
								CodeShelfNetwork network = mControlGroup.getParentCodeShelfNetwork();
								IWirelessInterface wirelessInterface = network.getWirelessInterface();
								wirelessInterface.sendCommand(command);
							}
						}
					}
				}
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					LOGGER.error(command.toString());
				}
			}
		}
	}
}
