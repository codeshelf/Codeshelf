/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsWebSocketClient.java,v 1.4 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdEnum;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdNetAttach;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdEnum;
import com.gadgetworks.flyweight.command.NetMacAddress;
import com.gadgetworks.flyweight.controller.DeviceController;
import com.gadgetworks.flyweight.controller.IController;
import com.gadgetworks.flyweight.controller.IControllerEventListener;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.TcpServerInterface;
import com.gadgetworks.flyweight.controller.WirelessDeviceEventHandler;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CsWebSocketClient extends WebSocketClient implements IWebSocketClient, IControllerEventListener {

	private static final Logger			LOGGER		= LoggerFactory.getLogger(CsWebSocketClient.class);

	private int							mNextMsgNum	= 1;

	private String						mOrganizationId;
	private String						mFacilityId;
	private String						mNetworkId;
	private String						mNetworkCredential;
	private WirelessDeviceEventHandler	mWirelessDeviceEventHandler;
	private IGatewayInterface			mGatewaytInterface;
	private Map<UUID, Che>				mCheMap;
	private IController					mController;

	@Inject
	public CsWebSocketClient(@Named(WEBSOCKET_URI_PROPERTY) final String inUriStr, final IUtil inUtil, final WebSocketClient.WebSocketClientFactory inWebSocketClientFactory) {
		super(URI.create(inUriStr));

		setWebSocketFactory(inWebSocketClientFactory);

		mOrganizationId = System.getProperty("organizationId");
		mFacilityId = System.getProperty("facilityId");
		mNetworkId = System.getProperty("networkId");
		mNetworkCredential = System.getProperty("networkCredential");

		mCheMap = new HashMap<UUID, Che>();

	}

	public final void start() {
		mGatewaytInterface = new TcpServerInterface();
		mController = new DeviceController(mGatewaytInterface);
		mController.addControllerEventListener(this);

		// Start the background startup and wait until it's finished.
		LOGGER.info("Starting controller");
		mController.startController((byte) 0x01);

		WebSocket.DEBUG = true;
		LOGGER.debug("Websocket start");
		try {
			connectBlocking();

			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> propertiesMap = new HashMap<String, Object>();
			propertiesMap.put("organizationId", mOrganizationId);
			propertiesMap.put("facilityId", mFacilityId);
			propertiesMap.put("networkId", mNetworkId);
			propertiesMap.put("credential", mNetworkCredential);
			ObjectNode dataNode = mapper.valueToTree(propertiesMap);

			sendMessageNode(WebSessionReqCmdEnum.NET_ATTACH_REQ, dataNode);

		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}
	}

	public final void stop() {
		close();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inReqCmd
	 * @param inDataNode
	 * @return
	 */
	private void sendMessageNode(final WebSessionReqCmdEnum inReqCmd, ObjectNode inDataNode) {
		ObjectNode msgNode = null;

		ObjectMapper treeMapper = new ObjectMapper();
		msgNode = treeMapper.createObjectNode();
		msgNode.put(IWebSessionCmd.COMMAND_ID_ELEMENT, "cid_" + mNextMsgNum++);
		msgNode.put(IWebSessionCmd.COMMAND_TYPE_ELEMENT, inReqCmd.getName());

		msgNode.put("data", inDataNode);

		sendMessage(msgNode.toString());
	}

	// --------------------------------------------------------------------------
	/**
	 * Send and log messages over the websocket.
	 * @param inMessage
	 */
	private void sendMessage(final String inMessage) {
		LOGGER.info("sent: " + inMessage);
		send(inMessage);
	}

	public final void onOpen(final ServerHandshake inHandshake) {
		LOGGER.debug("Websocket open");
	}

	public final void onClose(final int inCode, final String inReason, final boolean inRemote) {
		LOGGER.debug("Websocket close");
	}

	public final void onMessage(final String inMessage) {
		LOGGER.debug(inMessage);

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(inMessage);

			WebSessionRespCmdEnum commandEnum = getCommandTypeEnum(rootNode);
			String commandId = rootNode.get(IWebSessionCmd.COMMAND_ID_ELEMENT).getTextValue();
			JsonNode dataNode = rootNode.get(IWebSessionCmd.DATA_ELEMENT);

			switch (commandEnum) {
				case NET_ATTACH_RESP:
					processNetAttach(dataNode);
					break;

				case OBJECT_FILTER_RESP: {
					processFilterResp(dataNode);
				}

				default:
					break;
			}
		} catch (JsonProcessingException e) {
			LOGGER.debug("", e);
		} catch (IOException e) {
			LOGGER.debug("", e);
		}
	}

	public final void onError(final Exception inException) {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataNode
	 */
	private void processNetAttach(final JsonNode inDataNode) {
		JsonNode responseNode = inDataNode.get(WebSessionRespCmdEnum.NET_ATTACH_RESP.toString());

		if ((responseNode != null) && (responseNode.getTextValue().equals(WebSessionReqCmdNetAttach.SUCCEED))) {
			JsonNode facilityNode = inDataNode.get("facility");
			JsonNode facilityPersistentIdNode = facilityNode.get("persistentId");
			String persistentId = facilityPersistentIdNode.getTextValue();

			// Build the response Json object.
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode filterParamsArray = mapper.createArrayNode();
			ObjectNode theIdNode = mapper.createObjectNode();
			theIdNode.put("name", "theId");
			theIdNode.put("value", persistentId);
			filterParamsArray.add(theIdNode);

			ArrayNode propertiesArray = mapper.createArrayNode();
			propertiesArray.add("domainId");

			Map<String, Object> propertiesMap = new HashMap<String, Object>();
			propertiesMap.put(IWebSessionReqCmd.CLASSNAME, Che.class.getSimpleName());
			propertiesMap.put(IWebSessionReqCmd.FILTER_CLAUSE, "parent.persistentId = :theId");
			propertiesMap.put(IWebSessionReqCmd.FILTER_PARAMS, filterParamsArray);
			propertiesMap.put(IWebSessionReqCmd.PROPERTY_NAME_LIST, propertiesArray);
			ObjectNode dataNode = mapper.valueToTree(propertiesMap);

			sendMessageNode(WebSessionReqCmdEnum.OBJECT_FILTER_REQ, dataNode);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataNode
	 */
	private void processFilterResp(final JsonNode inDataNode) {
		JsonNode resultsNode = inDataNode.get(IWebSessionReqCmd.RESULTS);

		if (resultsNode != null) {

			for (JsonNode objectNode : resultsNode) {
				JsonNode classNode = objectNode.get(IWebSessionReqCmd.CLASSNAME);
				if ((classNode != null) && (classNode.asText().equals(Che.class.getSimpleName()))) {
					processCheUpdate(objectNode);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param cheUpdateNode
	 */
	private void processCheUpdate(final JsonNode cheUpdateNode) {
		JsonNode updateTypeNode = cheUpdateNode.get(IWebSessionReqCmd.OP_TYPE);
		UUID uuid = null;
		Che che = null;
		if (updateTypeNode != null) {
			switch (updateTypeNode.getTextValue()) {
				case IWebSessionReqCmd.OP_TYPE_CREATE:
					che = new Che();
					uuid = UUID.fromString(cheUpdateNode.get(IWebSessionReqCmd.PERSISTENT_ID).asText());
					che.setPersistentId(uuid);
					che.setDomainId(cheUpdateNode.get(IWebSessionReqCmd.SHORT_DOMAIN_ID).asText());

					// Check to see if the Che is already in our map.
					if (!mCheMap.containsValue(che)) {
						mCheMap.put(uuid, che);
					}

					LOGGER.info("Created che: " + che.getDomainId());
					break;

				case IWebSessionReqCmd.OP_TYPE_UPDATE:
					uuid = UUID.fromString(cheUpdateNode.get(IWebSessionReqCmd.PERSISTENT_ID).asText());
					che = mCheMap.get(uuid);

					if (che == null) {
						che = new Che();
						mCheMap.put(uuid, che);
					}
					che.setPersistentId(uuid);
					che.setDomainId(cheUpdateNode.get(IWebSessionReqCmd.SHORT_DOMAIN_ID).asText());
					WirelessDevice device = new WirelessDevice();
					device.setMacAddress(new NetMacAddress(che.getDomainId().getBytes()));
					mController.addNetworkDevice(device);
					LOGGER.info("Updated che: " + che.getDomainId());
					break;

				case IWebSessionReqCmd.OP_TYPE_DELETE:
					uuid = UUID.fromString(cheUpdateNode.get(IWebSessionReqCmd.PERSISTENT_ID).asText());
					che = mCheMap.remove(uuid);
					LOGGER.info("Deleted che: " + che.getDomainId());
					break;

				default:
					break;
			}
		}
	}

	private WebSessionRespCmdEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WebSessionRespCmdEnum result = WebSessionRespCmdEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCmd.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			result = WebSessionRespCmdEnum.fromString(commandTypeNode.getTextValue());
		}

		return result;
	}

	@Override
	public final boolean canNetworkDeviceAssociate(final String inGUID) {
		boolean result = false;
		for (Che che : mCheMap.values()) {
			if (che.getDomainId().equals(inGUID)) {
				result = true;
			}
		}
		return result;
	}

	@Override
	public void deviceAdded(INetworkDevice inNetworkDevice) {
		
	}

	@Override
	public void deviceRemoved(INetworkDevice inNetworkDevice) {
		
	}

	@Override
	public void deviceLost(INetworkDevice inNetworkDevice) {
		
	}

}
