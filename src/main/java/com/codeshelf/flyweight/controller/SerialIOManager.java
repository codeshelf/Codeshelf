/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: SerialIOManager.java,v 1.1 2013/02/20 08:28:25 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.controller;


/**
 * --------------------------------------------------------------------------
 * 
 * This class encodes/decodes FlyWeightController Packets to/from the FlyWeightController network.
 * @author jeffw
 * 
 */

public final class SerialIOManager { //extends SerialInterfaceABC {

//	private static final int	TX_TIMEOUT_MS	= 5;
//	private static final int	RX_TIMEOUT_MS	= 5;
//
//	private static final Log	logger			= LogFactory.getLog(SerialIOManager.class);
//
//	private SerInputStream		mSerialInputStream;
//	private SerialPortLocal		mSerialPortLocal;
//
//	// --------------------------------------------------------------------------
//	/**
//	 *  The constructor tries to setup the serial connection.
//	 */
//	SerialIOManager() {
//
//	}
//
//	/* --------------------------------------------------------------------------
//	 * (non-Javadoc)
//	 * @see com.gadgetworks.controller.SerialInterfaceABC#doSetupConnection()
//	 */
//	protected boolean doSetupConnection() {
//
//		boolean result = false;
//
//		String devName = "";
//		//"Bluetooth-PDA-Sync";
//
//		try {
//			String[] devNames = SerialPortLocal.getPortList();
//			for (int i = 0; i < devNames.length; i++) {
//				logger.debug(devNames[i]);
//				if (devNames[i].contains("usbserial")) {
//					devName = devNames[i];
//				}
//			}
//
//			SerialConfig serCfg = new SerialConfig(devName);
//			// settings
//			mSerialPortLocal = new SerialPortLocal(serCfg);
//			mSerialPortLocal.setBitRate(SerialConfig.BR_115200);
//			mSerialPortLocal.setDataBits(SerialConfig.LN_8BITS);
//			mSerialPortLocal.setStopBits(SerialConfig.ST_1BITS);
//			mSerialPortLocal.setParity(SerialConfig.PY_NONE);
//			mSerialPortLocal.setHandshake(SerialConfig.HS_CTSRTS);
//			mSerialPortLocal.setTimeoutTx(TX_TIMEOUT_MS);
//			mSerialPortLocal.setTimeoutRx(RX_TIMEOUT_MS);
//
//			mSerialPortLocal.open();
//
//			mSerialPortLocal.setDTR(true); // some modems will not respond if DTR is low
//
//			mSerialInputStream = new SerInputStream(mSerialPortLocal);
//			//			mSerialOutputStream = new SerOutputStream(mSerialPortLocal);
//
//			result = true;
//
//		} catch (IOException ioe) {
//			//ioe.printStackTrace();
//			logger.error("Serial I/O init problem", ioe);
//		}
//
//		return result;
//	}
//
//	/* --------------------------------------------------------------------------
//	 * (non-Javadoc)
//	 * @see com.gadgetworks.controller.SerialInterfaceABC#doStartInterface()
//	 */
//	protected void doStartInterface() {
//	};
//
//	/* --------------------------------------------------------------------------
//	 * (non-Javadoc)
//	 * @see com.gadgetworks.controller.SerialInterfaceABC#doStopInterface()
//	 */
//	protected void doStopInterface() {
//	};
//
//	/* --------------------------------------------------------------------------
//	 * (non-Javadoc)
//	 * @see com.gadgetworks.controller.SerialInterfaceABC#readByte()
//	 */
//	protected byte readByte() {
//		byte result = 0;
//
//		try {
//			result = (byte) mSerialInputStream.read();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return result;
//	};
//
//	/* --------------------------------------------------------------------------
//	 * (non-Javadoc)
//	 * @see com.gadgetworks.controller.SerialInterfaceABC#readBytes()
//	 */
//	protected int readBytes(byte[] inBuffer) {
//		int result = 0;
//
//		try {
//			result = mSerialInputStream.read(inBuffer);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return result;
//	};
//
//	/* --------------------------------------------------------------------------
//	 * (non-Javadoc)
//	 * @see com.gadgetworks.controller.SerialInterfaceABC#writeByte(byte)
//	 */
//	protected void writeByte(byte inByte) {
//	};
//
//	/* --------------------------------------------------------------------------
//	 * (non-Javadoc)
//	 * @see com.gadgetworks.controller.SerialInterfaceABC#writeBytes(byte[])
//	 */
//	protected void writeBytes(byte[] inBytes, int inLength) {
//		try {
//			mSerialPortLocal.putData(inBytes, 0, inLength);
//			while (mSerialPortLocal.txBufCount() > 0) {
//				//				try {
//				//					Thread.sleep(1);
//				//				} catch (InterruptedException e) {
//				//				}
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	};

}
