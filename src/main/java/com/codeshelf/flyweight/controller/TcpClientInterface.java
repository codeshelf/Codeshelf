/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: TcpClientInterface.java,v 1.4 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */
public class TcpClientInterface extends SerialInterfaceABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(TcpClientInterface.class);

	private Socket				mSocket;
	private InputStream			mInputStream;
	private OutputStream		mOutputStream;
	private String				mHostname;

	public TcpClientInterface(final String inHostname) {
		mHostname = inHostname;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#doSetupConnection()
	 */
	@Override
	protected final boolean doSetupConnection() {
		boolean result = true;

		try {
			mSocket = new Socket(mHostname, TcpServerInterface.PORT_NUM);
			mInputStream = mSocket.getInputStream();
			mOutputStream = mSocket.getOutputStream();
		} catch (IOException e) {
			LOGGER.error("", e);
			result = false;
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#doStartInterface()
	 */
	@Override
	protected final void doStartInterface() {

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#doResetInterface()
	 */
	@Override
	final protected void doResetInterface() {
		try {
			mSocket.close();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#doStopInterface()
	 */
	@Override
	protected void doStopInterface() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#setRTS()
	 */
	@Override
	protected void setRTS() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#clrRTS()
	 */
	@Override
	protected void clrRTS() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#readByte()
	 */
	@Override
	protected final byte readByte() {
		byte result = -1;
		if (mSocket.isConnected()) {
			try {
				int readByte = mInputStream.read();
				if (readByte == -1) {
					stopInterface();
				} else {
					result = (byte) readByte;
				}
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#readBytes(byte[])
	 */
	@Override
	protected final int readBytes(byte[] inBuffer) {
		if (mSocket.isConnected()) {
			try {
				return (byte) mInputStream.read(inBuffer);
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
		return 0;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#writeByte(byte)
	 */
	@Override
	protected final void writeByte(byte inByte) {
		if (mSocket.isConnected()) {
			try {
				mOutputStream.write((byte) inByte);
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.SerialInterfaceABC#writeBytes(byte[], int)
	 */
	@Override
	protected final void writeBytes(byte[] inBytes, int inLength) {
		if (mSocket.isConnected()) {
			try {
				mOutputStream.write(inBytes, 0, inLength);
			} catch (IOException e) {
				LOGGER.error("", e);
			}
		}
	}
}
