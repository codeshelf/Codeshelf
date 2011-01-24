package com.gadgetworks.codeshelf.controller;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import com.gadgetworks.codeshelf.command.ICommand;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;

public final class SnapInterface implements IGatewayInterface {

	private static final Log	LOGGER						= LogFactory.getLog(SnapInterface.class);

	private static final int	E10_SERIAL_TYPE				= 1;
	private static final int	E10_STANDARD_SERIAL_PORT	= 1;
	private static final String	E10_RPC_CMD_NAME			= "rpc";
	private static final String	E10_MCAST_RPC_NAME			= "macstRpc";

	private CodeShelfNetwork	mCodeShelfNetwork;
	private XmlRpcClient		mXmlRpcClient;
	private boolean				mIsStarted					= false;

	public SnapInterface(CodeShelfNetwork inCodeShelfNetwork) {
		mCodeShelfNetwork = inCodeShelfNetwork;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IGatewayInterface#startInterface()
	 */
	public void startInterface() {
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL("http://10.0.5.110:8080/RPC2/"));
			config.setEnabledForExtensions(true);
			config.setReplyTimeout(5000);
			config.setConnectionTimeout(5000);
			mXmlRpcClient = new XmlRpcClient();
			mXmlRpcClient.setTransportFactory(new XmlRpcCommonsTransportFactory(mXmlRpcClient));
			mXmlRpcClient.setConfig(config);
		} catch (MalformedURLException e) {
			//LOGGER.error("", e);
		}

		try {
			Object[] params = new Object[] { E10_SERIAL_TYPE, E10_STANDARD_SERIAL_PORT, false };
			Object result = (Object) mXmlRpcClient.execute("connectSerial", params);
			mIsStarted = true;
		} catch (XmlRpcException e) {
			//LOGGER.error("", e);
		}
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
				Object[] params = new Object[] { E10_SERIAL_TYPE, E10_STANDARD_SERIAL_PORT, false };
				Object result = (Object) mXmlRpcClient.execute("disconnect", params);
			} catch (XmlRpcException e) {
				//LOGGER.error("", e);
			}
			mIsStarted = false;
			mXmlRpcClient = null;
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
			params.add(inTransport.getSrcAddr().getParamValueAsByteArray());

			// Dst addr
			params.add(inTransport.getDstAddr().getParamValueAsByteArray());

			// RPC method name
			params.add(inTransport.getCommandId().getName());

			// Command params
			for (Object param : inTransport.getParams()) {
				params.add(param);
			}

			// Send the command.
			Object result = (Object) mXmlRpcClient.execute(E10_RPC_CMD_NAME, params);
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
