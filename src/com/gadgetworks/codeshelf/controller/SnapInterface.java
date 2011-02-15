package com.gadgetworks.codeshelf.controller;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.command.CommandCsAckPressed;
import com.gadgetworks.codeshelf.command.CommandIdEnum;
import com.gadgetworks.codeshelf.command.ICommand;
import com.gadgetworks.codeshelf.command.ICsCommand;
import com.gadgetworks.codeshelf.model.TagProtocolEnum;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.server.tags.AtopControllerConnection;
import com.gadgetworks.codeshelf.server.tags.IControllerConnection;

public final class SnapInterface implements IWirelessInterface {

	private static final Log	LOGGER						= LogFactory.getLog(SnapInterface.class);

	private static final int	E10_SERIAL_TYPE				= 1;
	private static final String	E10_STANDARD_SERIAL_PORT	= "/dev/ttyS1";
	private static final String	E10_RPC_CMD_NAME			= "rpc";
	//	private static final String	E10_MCAST_RPC_NAME			= "macstRpc";

	private static final int	OUTBOUND_TIMEOUT_MILLIS		= 5000;

	private CodeShelfNetwork	mCodeShelfNetwork;
	private XmlRpcClient		mInboundXmlRpcClient;
	private XmlRpcClient		mOutboundXmlRpcClient;
	private boolean				mIsStarted;

	public SnapInterface(final CodeShelfNetwork inCodeShelfNetwork) {
		mCodeShelfNetwork = inCodeShelfNetwork;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void setupXmlRpcClients() {
		try {

			// Setup the inbound XML RPC client config.
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(mCodeShelfNetwork.getGatewayUrl()));
			config.setEnabledForExtensions(true);
			//			config.setReplyTimeout(INBOUND_TIMEOUT_MILLIS);
			//			config.setConnectionTimeout(INBOUND_TIMEOUT_MILLIS);
			mInboundXmlRpcClient = new XmlRpcClient();
			mInboundXmlRpcClient.setTransportFactory(new XmlRpcCommonsTransportFactory(mInboundXmlRpcClient));
			mInboundXmlRpcClient.setConfig(config);

			// Setup the outbound XML RPC client config.
			config = new XmlRpcClientConfigImpl();
			config.setServerURL(new URL(mCodeShelfNetwork.getGatewayUrl()));
			config.setEnabledForExtensions(true);
			config.setReplyTimeout(OUTBOUND_TIMEOUT_MILLIS);
			config.setConnectionTimeout(OUTBOUND_TIMEOUT_MILLIS);
			mOutboundXmlRpcClient = new XmlRpcClient();
			mOutboundXmlRpcClient.setTransportFactory(new XmlRpcCommonsTransportFactory(mOutboundXmlRpcClient));
			mOutboundXmlRpcClient.setConfig(config);
		} catch (MalformedURLException e) {
			//LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IWirelessInterface#startInterface()
	 */
	public void startInterface() {

		setupXmlRpcClients();

		try {
			Object[] params = new Object[] { E10_SERIAL_TYPE, E10_STANDARD_SERIAL_PORT, false };
			Object xmlRpcResult = (Object) mOutboundXmlRpcClient.execute("connectSerial", params);
			if (xmlRpcResult instanceof Boolean) {
				Boolean connected = (Boolean) xmlRpcResult;
				if (connected) {
					mIsStarted = true;
					mCodeShelfNetwork.setIsConnected(true);

					// Start the interfaces for the control groups.
					for (ControlGroup controlGroup : mCodeShelfNetwork.getControlGroups()) {
						IControllerConnection connection = controlGroup.getControllerConnection();
						if (connection == null) {
							if (controlGroup.getTagProtocol().equals(TagProtocolEnum.ATOP)) {
								connection = new AtopControllerConnection(controlGroup);
							}
							controlGroup.setControllerConnection(connection);
						}
						if (connection != null) {
							connection.start();
						}
					}
				}
			}
		} catch (XmlRpcException e) {
			// Excetions may happen in normal cases such as the remote controller being down.
			//LOGGER.error("", e);
			// But we should sleep here to reduce CPU usage in cases where we cannot connect.
			try {
				Thread.sleep(OUTBOUND_TIMEOUT_MILLIS);
			} catch (InterruptedException e1) {
				LOGGER.error("", e1);
			}
		}

		// Push out the changes.
		Util.getSystemDAO().pushNonPersistentUpdates(mCodeShelfNetwork);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IWirelessInterface#resetInterface()
	 */
	public void resetInterface() {
		stopInterface();
		startInterface();
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IWirelessInterface#stopInterface()
	 */
	public void stopInterface() {
		if (mIsStarted) {
			try {
				Object[] params = new Object[] {};
				Object xmlRpcResult = (Object) mOutboundXmlRpcClient.execute("disconnect", params);
				if (xmlRpcResult instanceof Boolean) {
					Boolean connected = (Boolean) xmlRpcResult;
					if (connected) {
					}
				}
			} catch (XmlRpcException e) {
				//LOGGER.error("", e);
			}
			mIsStarted = false;
			mOutboundXmlRpcClient = null;
			mCodeShelfNetwork.setIsConnected(false);
			Util.getSystemDAO().pushNonPersistentUpdates(mCodeShelfNetwork);

			for (ControlGroup controlGroup : mCodeShelfNetwork.getControlGroups()) {
				IControllerConnection connection = controlGroup.getControllerConnection();
				if (connection != null) {
					connection.stop();
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IWirelessInterface#isStarted()
	 */
	public boolean isStarted() {
		return mIsStarted;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IWirelessInterface#checkInterfaceOk()
	 */
	public boolean checkInterfaceOk() {
		boolean result = false;

		try {
			Object[] params = new Object[] {};
			Object xmlRpcResult = (Object) mOutboundXmlRpcClient.execute("gatewayInfo", params);
			if (xmlRpcResult instanceof Map<?, ?>) {
				Map<String, Object> map = (Map<String, Object>) xmlRpcResult;
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
	 * @see com.gadgetworks.codeshelf.controller.IWirelessInterface#sendCommand(com.gadgetworks.codeshelf.command.ICommand)
	 */
	public void sendCommand(ICommand inCommand) {

		ITransport transport = new SnapTransport();
		inCommand.toTransport(transport);
		if (!sendRpcCommand(transport)) {
			LOGGER.info("RPC failed: " + inCommand.toString());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommand
	 */
	private boolean sendRpcCommand(ITransport inTransport) {

		boolean result = false;

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
			Object xmlRpcResult = (Object) mOutboundXmlRpcClient.execute(E10_RPC_CMD_NAME, params);
			if (xmlRpcResult instanceof Boolean) {
				Boolean success = (Boolean) xmlRpcResult;
				if (success) {
					result = true;
				}
			}
		} catch (XmlRpcException e) {
			LOGGER.error("", e);
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.controller.IWirelessInterface#receiveCommand(com.gadgetworks.codeshelf.command.NetworkId)
	 */
	public ICommand receiveCommand(NetworkId inMyNetworkId) {
		ICommand result = null;

		try {
			String methodName = null;
			NetAddress netAddr = null;
			Object object;

			//			Object[] params = new Object[] { mCodeShelfNetwork.getGatewayAddr().getParamValueAsByteArray(), false,
			//					Integer.valueOf(0), Integer.valueOf(0), new Float(1.0) };
			Object[] params = new Object[] { mCodeShelfNetwork.getGatewayAddr().getParamValueAsByteArray(), false };
			Object xmlRpcResult = (Object) mOutboundXmlRpcClient.execute("waitOnEvent", params);
			if (xmlRpcResult instanceof HashMap) {
				HashMap map = (HashMap) xmlRpcResult;
				object = map.get("methodName");
				if (object instanceof String) {
					methodName = (String) object;
				}
				object = map.get("netAddr");
				if (object instanceof byte[]) {
					netAddr = new NetAddress((byte[]) object);
				}
				if ((methodName != null) && (netAddr != null)) {
					result = createCommand(methodName, netAddr);
				}
			}
		} catch (XmlRpcException e) {
			if (e.linkedException instanceof SocketTimeoutException) {
				// Don't worry about socket timeouts.
			} else {
				LOGGER.error("", e);
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inMethodName
	 * @param inNetAddr
	 * @return
	 */
	private ICsCommand createCommand(String inMethodName, NetAddress inNetAddr) {
		ICsCommand result = null;

		PickTag pickTag = Util.getSystemDAO().findPickTagByNetAddr(inNetAddr);

		if (inMethodName.equals(CommandIdEnum.CS_ACK_PRESSED.getName())) {
			result = new CommandCsAckPressed(pickTag);
		}

		return result;
	}
}
