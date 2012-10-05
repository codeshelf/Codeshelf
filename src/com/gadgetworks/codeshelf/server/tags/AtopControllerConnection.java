/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopControllerConnection.java,v 1.10 2012/10/05 21:01:41 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.controller.IWirelessInterface;
import com.gadgetworks.codeshelf.model.domain.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.domain.ControlGroup;
import com.gadgetworks.codeshelf.model.domain.PickTag;

/**
 * @author jeffw
 *
 */
public final class AtopControllerConnection implements IControllerConnection {

	private static final Log	LOGGER						= LogFactory.getLog(AtopControllerConnection.class);

	private static final String	INTERFACE_THREAD_NAME		= "Control Group Socket Listener";
	private static final String	CONNECTION_THREAD_NAME		= "Control Group Connection ";

	private static final int	INTERFACE_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;
	private static final int	CONNECTION_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;

	private static final int	SOCKET_TIMEOUT_MILLIS		= 500;
	private static final short	BROADCAST_NODE				= 0x00fc;

	private ControlGroup		mControlGroup;
	private Thread				mServerThread;
	private ServerSocket		mServerSocket;
	private boolean				mShouldRun;
	private Socket				mSocket;
	private DataInputStream		mDataInputStream;
	private DataOutputStream	mDataOutputStream;

	public AtopControllerConnection(final ControlGroup inControlGroup) {
		mControlGroup = inControlGroup;
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
			}, INTERFACE_THREAD_NAME + mControlGroup.getShortDomainId().toString());
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
				mSocket = mServerSocket.accept();
				//socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);
				LOGGER.info("Socket connected: " + mSocket.toString());

				Thread connectionThread = new Thread(new Runnable() {
					public void run() {
						processConnectionData();
					}
				}, CONNECTION_THREAD_NAME + mSocket.getRemoteSocketAddress());
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
	private void processConnectionData() {
		try {
			mDataInputStream = new DataInputStream(mSocket.getInputStream());
			mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		if (mDataInputStream != null) {
			while (!mSocket.isClosed()) {
				for (ICsCommand csCommand : AtopStreamProcessor.getNextCsCommands(mSocket, mDataInputStream, mControlGroup)) {
					PickTag tag = csCommand.getPickTag();
					if (tag == null) {
						// Broadcast the command to all tags.
					} else {
						// Unicast the command to a single tag.
						// Map to a set of commands for the CodeShelf network.
						CodeShelfNetwork network = mControlGroup.getParentCodeShelfNetwork();
						IWirelessInterface wirelessInterface = network.getWirelessInterface();
						csCommand.setDstAddr(tag.getNetAddress());
						wirelessInterface.sendCommand(csCommand);
						LOGGER.info("Cmd sent:" + csCommand.toString());
					}
				}
			}
		}
	}
	
	// --------------------------------------------------------------------------
	/**
	 */
	public void sendDataBytes(byte[] inDataBytes) {
		if ((mSocket != null) && (!mSocket.isClosed()) && (mDataOutputStream != null)) {
			try {
				mDataOutputStream.write(inDataBytes);
			} catch (IOException e) {
				LOGGER.debug("", e);
			}
		}
	}
}
