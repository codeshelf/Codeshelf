/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ControlGroupInterface.java,v 1.1 2011/01/26 00:30:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.swiftpic;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.persist.ControlGroup;

/**
 * @author jeffw
 *
 */
public final class ControlGroupInterface {

	private static final Log	LOGGER						= LogFactory.getLog(ControlGroupInterface.class);

	private static final String	INTERFACE_THREAD_NAME		= "Control Group Socket ";
	private static final String	CONNECTION_THREAD_NAME		= "Control Group Connection ";

	private static final int	INTERFACE_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;
	private static final int	CONNECTION_THREAD_PRIORITY	= Thread.NORM_PRIORITY - 1;

	private ControlGroup		mControlGroup;
	private Thread				mServerThread;
	private ServerSocket		mServerSocket;
	private boolean				mShouldRun;

	public ControlGroupInterface(final ControlGroup inControlGroup) {
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
				LOGGER.info("Socket connected: " + socket.toString());

				Thread connectionThread = new Thread(new Runnable() {
					public void run() {
						processConnection(socket);
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

	private void processConnection(Socket inSocket) {
		InputStream inputStream = null;
		try {
			inputStream = inSocket.getInputStream();
		} catch (IOException e) {
			LOGGER.error("", e);
		}

		if (inputStream != null) {
			while (!inSocket.isClosed()) {
				try {
					int data = inputStream.read();
					LOGGER.info(data);
					if (data == '#') {
						inSocket.close();
					}
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			}
		}
	}

}
