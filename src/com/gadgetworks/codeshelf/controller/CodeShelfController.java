/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfController.java,v 1.3 2011/02/15 02:39:46 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.command.CommandControlABC;
import com.gadgetworks.codeshelf.command.CommandControlButton;
import com.gadgetworks.codeshelf.command.CommandCsABC;
import com.gadgetworks.codeshelf.query.IQuery;
import com.gadgetworks.codeshelf.query.IResponse;
import com.gadgetworks.codeshelf.query.QueryActorDescriptor;
import com.gadgetworks.codeshelf.query.QueryActorKVP;
import com.gadgetworks.codeshelf.query.ResponseActorDescriptor;
import com.gadgetworks.codeshelf.query.ResponseActorKVP;
import com.gadgetworks.codeshelf.query.ResponseTypeEnum;
import com.gadgetworks.codeshelf.server.tags.AtopStreamProcessor;

public final class CodeShelfController extends ControllerABC {

	public static final String	BEAN_ID					= "CodeShelfController";
	public static final String	CONTROLLER_THREAD_NAME	= "Controller";

	private static final Log	LOGGER					= LogFactory.getLog(CodeShelfController.class);

	private static final int	STD_SLEEP_TIME			= 50;
	// Timeout after 5 minutes.
	private static final long	SESSION_CHECK_MILLIS	= 20 * 1000;
	private static final long	SESSION_TIMEOUT_MILLIS	= 10 * 60 * 1000;							// 10 minutes

	private long				mLastTimeChecked		= System.currentTimeMillis();

	// --------------------------------------------------------------------------
	/**
	 *  @param inSessionManager
	 */
	public CodeShelfController(final IDeviceMaintainer inDeviceMaintainer, final List<IWirelessInterface> inInterfaceList) {
		super(inDeviceMaintainer, inInterfaceList);

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.ControllerABC#startController()
	 */
	public void doStartController() {

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.ControllerABC#doProcessEvents()
	 */
	protected boolean doBackgroundProcessing() {

		if (System.currentTimeMillis() > mLastTimeChecked + SESSION_CHECK_MILLIS) {
			mLastTimeChecked = System.currentTimeMillis();

			// Check the state of each device, and see if there is any work that we need to do to those devices.
			for (INetworkDevice networkDevice : this.getNetworkDevices()) {

				// Check to see if we've not heard from the remote device in a while.
				//				if (!networkDevice.getNetworkDeviceState().equals(NetworkDeviceStateEnum.LOST)) {
				if (networkDevice.getLastContactTime() != 0) {
					long lastHeardMillis = System.currentTimeMillis() - networkDevice.getLastContactTime();
					if (lastHeardMillis > SESSION_TIMEOUT_MILLIS) {

						// Indicate to listeners we've removed the actor.
						for (IControllerEventListener listener : getControllerEventListeners()) {
							listener.deviceLost(networkDevice);
						}
					}
				}
				//				}

				// Check the state of the session, and see what we need to do.
				switch (networkDevice.getNetworkDeviceState()) {
					case ASSIGNACK_RCVD:
						break;
					default:
						break;

				}
			}
		} else {
			//			try {
			//				Thread.sleep(5);
			//			} catch (InterruptedException e) {
			//				// TODO: handle exception
			//			}
		}

		return true;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inSession
	 */
	protected void doNetworkDeviceBecameActive(INetworkDevice inNetworkDevice) {

		// Delete any outstanding queries for this device.
		clearQueriesForDevice(inNetworkDevice);

		// When the remote actor session becomes active we ask the actor for its descriptor.
		//QueryActorDescriptor query = (QueryActorDescriptor) FlyWeightBeanFactory.getBean(QueryActorDescriptor.BEAN_ID);
		QueryActorDescriptor query = new QueryActorDescriptor();
		this.sendQuery(query, inNetworkDevice);
		inNetworkDevice.setNetworkDeviceState(NetworkDeviceStateEnum.SETUP);
		mDeviceMaintainer.deviceUpdated(inNetworkDevice, true);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.ControllerABC#doProcessQueryCmd(com.gadgetworks.query.IQuery)
	 */
	protected void doProcessQuery(IQuery inQuery) {

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.ControllerABC#doProcessResponseCmd(com.gadgetworks.command.CommandQueryABC, com.gadgetworks.command.CommandResponseABC)
	 */
	protected void doProcessResponse(IResponse inResponse, IQuery inQuery) {

		ResponseTypeEnum responseType = inResponse.getResponseType();

		// Figure out what session the response is for.
		INetworkDevice networkDevice = inQuery.getQueryNetworkDevice();

		if (networkDevice != null) {

			switch (responseType) {
				case ACTOR_DESCRIPTOR:
					this.processActorDescriptorResponse((ResponseActorDescriptor) inResponse, networkDevice);
					break;

				case ACTOR_KVP:
					this.processActorKVPResponse((ResponseActorKVP) inResponse, networkDevice);
					break;

				case INVALID:
				default:
					break;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inResponse
	 */
	private void processActorDescriptorResponse(ResponseActorDescriptor inActorDescResp, INetworkDevice inNetworkDevice) {

		// We're starting to build a profile of a new remote device actor we've discovered on the network.

		inNetworkDevice.setDeviceType(inActorDescResp.getDeviceType());
		inNetworkDevice.setDesc(inActorDescResp.getDescStr());
		inNetworkDevice.setKVPCount(inActorDescResp.getKVPCount());

		// Now send a query for the actor's KVPs.
		for (byte i = 0; i < inActorDescResp.getKVPCount(); i++) {
			//QueryActorKVP query = (QueryActorKVP) FlyWeightBeanFactory.getBean(QueryActorKVP.BEAN_ID);
			QueryActorKVP query = new QueryActorKVP(i);
			this.sendQuery(query, inNetworkDevice);
			// Give the radio a bit of time to catch up.
			try {
				Thread.sleep(STD_SLEEP_TIME);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Process the Actor KVP response from the remote.
	 *  @param inActorKVPResp	The response received from the network.
	 */
	private void processActorKVPResponse(ResponseActorKVP inActorKVPResp, INetworkDevice inNetworkDevice) {

		// Add this new KVP to the actor.
		inNetworkDevice.addKeyValuePair(inActorKVPResp.getKeyStr(), inActorKVPResp.getValueStr());

		// Check to see if we've completed the acquisition of actor KVPs.
		if (inNetworkDevice.getStoredKVPCount() == inNetworkDevice.getExpectedKVPCount()) {
		}

	}

	// --------------------------------------------------------------------------
	/**
	 *  Handle the request of a remote device that wants to associate to our controller.
	 *  @param inCommand    The association command that we want to process.  (The one just received.)
	 */
	protected void doProcessControlCmd(CommandControlABC inCommand, INetworkDevice inNetworkDevice) {

		switch (inCommand.getCommandIdEnum()) {
			case BUTTON:
				processControlButtonCmd((CommandControlButton) inCommand, inNetworkDevice);
				break;

			default:
				break;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Handle the request of a remote device that wants to associate to our controller.
	 *  @param inCommand    The association command that we want to process.  (The one just received.)
	 */
	protected void doProcessCodeShelfCmd(CommandCsABC inCommand) {
		AtopStreamProcessor.sendCsCommandsToAtopConnection(inCommand);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inCommand
	 *  @param inActor
	 *  @param inNetworkDevice
	 */
	private void processControlButtonCmd(CommandControlButton inCommand, INetworkDevice inNetworkDevice) {
		inNetworkDevice.buttonCommandReceived(inCommand.getButtonPressed(), inCommand.getFunctionType());
	}
}
