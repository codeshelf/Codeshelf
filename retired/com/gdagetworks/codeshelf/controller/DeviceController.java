/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DeviceController.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import java.util.List;

import com.gadgetworks.codeshelf.command.ICommand;

/**
 * @author jeffw
 *
 */
public class DeviceController implements IController {

	/**
	 * 
	 */
	public DeviceController() {
		// TODO Auto-generated constructor stub
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#receiveCommand(com.gadgetworks.codeshelf.command.ICommand, com.gadgetworks.codeshelf.controller.NetAddress)
	 */
	@Override
	public void receiveCommand(ICommand inCommand, NetAddress inSrcAddr) {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#sendCommandNow(com.gadgetworks.codeshelf.command.ICommand, com.gadgetworks.codeshelf.controller.NetAddress, boolean)
	 */
	@Override
	public void sendCommandNow(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#sendCommandTimed(com.gadgetworks.codeshelf.command.ICommand, com.gadgetworks.codeshelf.controller.NetAddress, long, boolean)
	 */
	@Override
	public void sendCommandTimed(ICommand inCommand, NetAddress inDstAddr, long InSendTimeNanos, boolean inAckRequested) {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#sendCommandTimed(com.gadgetworks.codeshelf.command.ICommand, com.gadgetworks.codeshelf.controller.NetworkId, com.gadgetworks.codeshelf.controller.NetAddress, long, boolean)
	 */
	@Override
	public void sendCommandTimed(ICommand inCommand, NetworkId inNetworkId, NetAddress inDstAddr, long inSendTime, boolean inAckRequested) {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#startController()
	 */
	@Override
	public void startController() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#stopController()
	 */
	@Override
	public void stopController() {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#setRadioChannel(byte)
	 */
	@Override
	public void setRadioChannel(byte inChannel) {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#getRadioChannel()
	 */
	@Override
	public byte getRadioChannel() {
		// TODO Auto-generated method stub
		return 0;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#addControllerEventListener(com.gadgetworks.codeshelf.controller.IControllerEventListener)
	 */
	@Override
	public void addControllerEventListener(IControllerEventListener inControllerEventListener) {
		// TODO Auto-generated method stub

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#getInterfaces()
	 */
	@Override
	public List<IWirelessInterface> getInterfaces() {
		// TODO Auto-generated method stub
		return null;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#getNetworkDevice(com.gadgetworks.codeshelf.controller.NetAddress)
	 */
	@Override
	public INetworkDevice getNetworkDevice(NetAddress inAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IController#getNetworkDevices()
	 */
	@Override
	public List<INetworkDevice> getNetworkDevices() {
		// TODO Auto-generated method stub
		return null;
	}

}
