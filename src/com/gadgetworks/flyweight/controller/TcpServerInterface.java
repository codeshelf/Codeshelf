/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: TcpServerInterface.java,v 1.3 2013/02/27 22:06:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.web.websocket.CsWebSocketClient;

/**
 * @author jeffw
 *
 */
public class TcpServerInterface extends SerialInterfaceABC {

	public static final int		PORT_NUM	= 45000;

	private static final Logger	LOGGER		= LoggerFactory.getLogger(CsWebSocketClient.class);

	private ServerSocket		mServerSocket;
	private List<Remote>		mRemotes	= new ArrayList<Remote>();

	private class Remote {
		public Socket		clientSocket;
		public InputStream	inputStream;
		public OutputStream	outputStream;
	}

	/**
	 * 
	 */
	public TcpServerInterface() {

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#doSetupConnection()
	 */
	@Override
	protected final boolean doSetupConnection() {
		boolean result = true;

		try {
			mServerSocket = new ServerSocket(PORT_NUM);
		} catch (IOException e) {
			LOGGER.error("", e);
			result = false;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#doStartInterface()
	 */
	@Override
	protected final void doStartInterface() {
		Thread interfaceStarterThread = new Thread(new Runnable() {
			public void run() {
				while (true) {

					Socket clientSocket = null;
					try {
						clientSocket = mServerSocket.accept();
						LOGGER.info("Client opened: " + clientSocket.toString());
						Remote remote = new Remote();
						remote.clientSocket = clientSocket;
						remote.inputStream = clientSocket.getInputStream();
						remote.outputStream = clientSocket.getOutputStream();
						//						byte[] putInCharMode = { (byte) 255, (byte) 251, (byte) 1, (byte) 255, (byte) 251, (byte) 3, (byte) 255, (byte) 252, (byte) 34 };
						//						remote.outputStream.write(putInCharMode);
						mRemotes.add(remote);
					} catch (IOException e) {
						System.err.println("Accept failed.");
						System.err.println(e);
						//System.exit(1);
					}
				}
			}
		}, "SocketListener");
		interfaceStarterThread.start();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#doResetInterface()
	 */
	@Override
	protected final void doResetInterface() {
		try {
			mServerSocket.close();
			doSetupConnection();
		} catch (IOException e1) {
			LOGGER.error("", e1);
		}
		for (Remote remote : mRemotes) {
			try {
				remote.clientSocket.close();
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
		mRemotes.clear();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#doStopInterface()
	 */
	@Override
	protected void doStopInterface() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#setRTS()
	 */
	@Override
	protected void setRTS() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#clrRTS()
	 */
	@Override
	protected void clrRTS() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#readByte()
	 */
	@Override
	protected final byte readByte() {
		while (true) {
			for (Remote remote : mRemotes) {
				if (remote.clientSocket.isConnected()) {
					try {
						if (remote.inputStream.available() > 0) {
							return (byte) remote.inputStream.read();
						}
					} catch (IOException e) {
						LOGGER.error("", e);
					}
				}
			}
			try {
				Thread.sleep(READ_SLEEP_MILLIS);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#readBytes(byte[])
	 */
	@Override
	protected final int readBytes(byte[] inBuffer) {
		for (Remote remote : mRemotes) {
			if (remote.clientSocket.isConnected()) {
				try {
					if (remote.inputStream.available() > 0) {
						return remote.inputStream.read(inBuffer);
					}
				} catch (IOException e) {
					LOGGER.error("", e);
					doResetInterface();
				}
			}
		}
		return 0;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#writeByte(byte)
	 */
	@Override
	protected void writeByte(byte inByte) {
		for (Remote remote : mRemotes) {
			if (remote.clientSocket.isConnected()) {
				try {
					remote.outputStream.write(inByte);
				} catch (IOException e) {
					LOGGER.error("", e);
					doResetInterface();
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.SerialInterfaceABC#writeBytes(byte[], int)
	 */
	@Override
	protected void writeBytes(byte[] inBytes, int inLength) {
		for (Remote remote : mRemotes) {
			if (remote.clientSocket.isConnected()) {
				try {
					remote.outputStream.write(inBytes, 0, inLength);
				} catch (IOException e) {
					LOGGER.error("", e);
					doResetInterface();
				}
			}
		}
	}

}
