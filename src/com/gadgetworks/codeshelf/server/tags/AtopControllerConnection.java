/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopControllerConnection.java,v 1.1 2011/02/04 02:53:53 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.persist.ControlGroup;

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

	private ControlGroup		mControlGroup;
	private Thread				mServerThread;
	private ServerSocket		mServerSocket;
	private boolean				mShouldRun;

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
				IAtopCommand command = AtopCommandFactory.getNextCommand(dataInputStream);
				if (command != null) {
					LOGGER.info(command.toString());
				}
			}
		}
	}
}
