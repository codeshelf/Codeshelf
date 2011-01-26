package com.gadgetworks.codeshelf.controller;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.command.ICommand;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.server.swiftpic.ControlGroupManager;

public final class SnapInterface implements IGatewayInterface {

	private static final Log	LOGGER						= LogFactory.getLog(SnapInterface.class);

	private static final int	E10_SERIAL_TYPE				= 1;
	private static final String	E10_STANDARD_SERIAL_PORT	= "/dev/ttyS1";
	private static final String	E10_RPC_CMD_NAME			= "rpc";
	//	private static final String	E10_MCAST_RPC_NAME			= "macstRpc";

	private static final int	E10_TIMEOUT_MILLIS			= 5000;

	private ControlGroupManager	mControlGroupManager;
	private CodeShelfNetwork	mCodeShelfNetwork;
	private XmlRpcClient		mXmlRpcClient;
	private boolean				mIsStarted;

	public SnapInterface(final CodeShelfNetwork inCodeShelfNetwork) {
		mCodeShelfNetwork = inCodeShelfNetwork;
		mControlGroupManager = new ControlGroupManager(inCodeShelfNetwork);
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void setupXmlRpcClient() {
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(mCodeShelfNetwork.getGatewayUrl()));
			config.setEnabledForExtensions(true);
			config.setReplyTimeout(E10_TIMEOUT_MILLIS);
			config.setConnectionTimeout(E10_TIMEOUT_MILLIS);
			mXmlRpcClient = new XmlRpcClient();
			mXmlRpcClient.setTransportFactory(new XmlRpcCommonsTransportFactory(mXmlRpcClient));
			mXmlRpcClient.setConfig(config);
		} catch (MalformedURLException e) {
			//LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#startInterface()
	 */
	public void startInterface() {

		setupXmlRpcClient();

		try {
			Object[] params = new Object[] { E10_SERIAL_TYPE, E10_STANDARD_SERIAL_PORT, false };
			Object results = (Object) mXmlRpcClient.execute("connectSerial", params);
			if (results instanceof Boolean) {
				Boolean connected = (Boolean) results;
				if (connected) {
					mIsStarted = true;
					mCodeShelfNetwork.setIsConnected(true);

					// Start the interfaces for the control groups.
					mControlGroupManager.start();
				}
			}
		} catch (XmlRpcException e) {
			//LOGGER.error("", e);
		}

		// Push out the changes.
		Util.getSystemDAO().pushNonPersistentUpdates(mCodeShelfNetwork);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#resetInterface()
	 */
	public void resetInterface() {
		stopInterface();
		startInterface();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#stopInterface()
	 */
	public void stopInterface() {
		if (mIsStarted) {
			try {
				Object[] params = new Object[] {};
				Object result = (Object) mXmlRpcClient.execute("disconnect", params);
			} catch (XmlRpcException e) {
				//LOGGER.error("", e);
			}
			mIsStarted = false;
			mXmlRpcClient = null;
			mCodeShelfNetwork.setIsConnected(false);
			Util.getSystemDAO().pushNonPersistentUpdates(mCodeShelfNetwork);

			mControlGroupManager.stop();
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#isStarted()
	 */
	public boolean isStarted() {
		return mIsStarted;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#checkInterfaceOk()
	 */
	public boolean checkInterfaceOk() {
		boolean result = false;

		try {
			Object[] params = new Object[] {};
			Object results = (Object) mXmlRpcClient.execute("gatewayInfo", params);
			if (results instanceof HashMap) {
				HashMap map = (HashMap) results;
				Object object = map.get("connected");
				if (object instanceof Boolean) {
					Boolean connected = (Boolean) object;
					result = connected;
				}
			}
		} catch (XmlRpcException e) {
			//LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#sendCommand(com.gadgetworks.codeshelf.command.ICommand)
	 */
	public void sendCommand(ICommand inCommand) {

		ITransport transport = new SnapTransport();
		inCommand.toTransport(transport);
		sendRpcCommand(transport);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private void sendRpcCommand(ITransport inTransport) {
		try {
			// Create a list to hold the RPC command parameters.
			List<Object> params = new ArrayList<Object>();

			// Src addr
			params.add(mCodeShelfNetwork.getGatewayAddr().getParamValueAsByteArray());

			// Dst addr
			params.add(inTransport.getDstAddr().getParamValueAsByteArray());

			// RPC method name
			params.add(inTransport.getCommandId().getName());

			// Command params
			List<Object> argList = new ArrayList<Object>();
			for (Object param : inTransport.getParams()) {
				argList.add(param);
			}
			params.add(argList.toArray());

			// Send the command.
			Object results = (Object) mXmlRpcClient.execute(E10_RPC_CMD_NAME, params);
			if (results instanceof HashMap) {
				HashMap map = (HashMap) results;
				Object object = map.get("connected");
				if (object instanceof Boolean) {
				}
			}
		} catch (XmlRpcException e) {
			LOGGER.error("", e);
		}

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#receiveCommand(com.gadgetworks.codeshelf.command.NetworkId)
	 */
	public ICommand receiveCommand(NetworkId inMyNetworkId) {
		return null;
	}

}
