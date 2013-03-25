/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsCheApplication.java,v 1.4 2013/03/25 10:37:44 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.application;

import jssc.SerialPort;
import jssc.SerialPortException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.IEmbeddedDevice;
import com.google.inject.Inject;

public final class CsCheApplication extends ApplicationABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CsCheApplication.class);

	private IEmbeddedDevice		mCheDevice;

	@Inject
	public CsCheApplication(final IEmbeddedDevice inCheDevice, final IUtil inUtil) {
		super(inUtil);
		mCheDevice = inCheDevice;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doStartup() {
		// Start the device.
		mCheDevice.start();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				SerialPort serialPort = new SerialPort("/dev/tty.usbmodem1421");
				try {
					serialPort.openPort();
					serialPort.setParams(38400, 8, 1, 0);

					while (true) {
						serialPort.writeString("^HELLO`WORLD~");
						LOGGER.debug(serialPort.readString());
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							LOGGER.error("", e);
						}
					}
				} catch (SerialPortException e) {
					LOGGER.error("", e);
				}
			}
		}, "Serial Port");
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	protected void doShutdown() {
		mCheDevice.stop();
	}

	// --------------------------------------------------------------------------
	/**
	 *	Reset some of the persistent object fields to a base state at start-up.
	 */
	protected void doInitializeApplicationData() {

	}
}
