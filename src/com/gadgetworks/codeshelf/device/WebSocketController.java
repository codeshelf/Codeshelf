/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSocketController.java,v 1.4 2013/02/27 22:06:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.java_websocket.client.IWebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdEnum;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdNetAttach;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdEnum;
import com.gadgetworks.codeshelf.web.websocket.ICsWebsocketClientMsgHandler;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IController;
import com.gadgetworks.flyweight.controller.IControllerEventListener;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.TcpServerInterface;

/**
 * @author jeffw
 *
 */
public class WebSocketController implements ICsWebsocketClientMsgHandler, IControllerEventListener {

	private static final Logger				LOGGER		= LoggerFactory.getLogger(WebSocketController.class);

	private Map<NetGuid, CheDevice>	mCheMap;
	private IController						mRadioController;
	private IGatewayInterface				mGatewaytInterface;
	private IWebSocketClient				mWebSocketClient;
	private int								mNextMsgNum	= 1;
	private String							mOrganizationId;
	private String							mFacilityId;
	private String							mNetworkId;
	private String							mNetworkCredential;

	public WebSocketController() {
		mCheMap = new HashMap<NetGuid, CheDevice>();

		mOrganizationId = System.getProperty("organizationId");
		mFacilityId = System.getProperty("facilityId");
		mNetworkId = System.getProperty("networkId");
		mNetworkCredential = System.getProperty("networkCredential");

	}

	public final void start(IWebSocketClient inWebSocketClient) {
		mWebSocketClient = inWebSocketClient;
		mGatewaytInterface = new TcpServerInterface();
		mRadioController = new RadioController(mGatewaytInterface);
		mRadioController.addControllerEventListener(this);

		// Start the background startup and wait until it's finished.
		LOGGER.info("Starting controller");
		mRadioController.startController((byte) 0x01);

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> propertiesMap = new HashMap<String, Object>();
		propertiesMap.put("organizationId", mOrganizationId);
		propertiesMap.put("facilityId", mFacilityId);
		propertiesMap.put("networkId", mNetworkId);
		propertiesMap.put("credential", mNetworkCredential);
		ObjectNode dataNode = mapper.valueToTree(propertiesMap);

		sendMessageNode(WebSessionReqCmdEnum.NET_ATTACH_REQ, dataNode);
	}

	public final void stop() {

	}

	// --------------------------------------------------------------------------
	/**
	 * Send and log messages over the websocket.
	 * @param inMessage
	 */
	private void sendMessage(final String inMessage) {
		LOGGER.info("sent: " + inMessage);
		mWebSocketClient.send(inMessage);
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

	@Override
	public final void handleMessage(String inMessage) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(inMessage);

			WebSessionRespCmdEnum commandEnum = getCommandTypeEnum(rootNode);
			String commandId = rootNode.get(IWebSessionCmd.COMMAND_ID_ELEMENT).getTextValue();
			JsonNode dataNode = rootNode.get(IWebSessionCmd.DATA_ELEMENT);

			switch (commandEnum) {
				case NET_ATTACH_RESP:
					processNetAttachResp(dataNode);
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

	// --------------------------------------------------------------------------
	/**
	 * @param inDataNode
	 */
	private void processNetAttachResp(final JsonNode inDataNode) {
		JsonNode responseNode = inDataNode.get(WebSessionRespCmdEnum.NET_ATTACH_RESP.toString());

		if ((responseNode != null) && (responseNode.getTextValue().equals(WebSessionReqCmdNetAttach.SUCCEED))) {
			JsonNode networkNode = inDataNode.get("codeshelfNetwork");
			JsonNode networkPersistentIdNode = networkNode.get("persistentId");
			String persistentId = networkPersistentIdNode.getTextValue();

			// Build the response Json object.
			ObjectMapper mapper = new ObjectMapper();
			ArrayNode filterParamsArray = mapper.createArrayNode();
			ObjectNode theIdNode = mapper.createObjectNode();
			theIdNode.put("name", "theId");
			theIdNode.put("value", persistentId);
			filterParamsArray.add(theIdNode);

			ArrayNode propertiesArray = mapper.createArrayNode();
			propertiesArray.add(IWebSessionReqCmd.SHORT_DOMAIN_ID);
			propertiesArray.add(IWebSessionReqCmd.DEVICE_GUID);

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
	 * @param inCheUpdateNode
	 */
	private void processCheUpdate(final JsonNode inCheUpdateNode) {
		JsonNode updateTypeNode = inCheUpdateNode.get(IWebSessionReqCmd.OP_TYPE);
		CheDevice cheDevice = null;
		NetGuid deviceGuid = new NetGuid(inCheUpdateNode.get(IWebSessionReqCmd.DEVICE_GUID).asText());
		if (updateTypeNode != null) {
			switch (updateTypeNode.getTextValue()) {
				case IWebSessionReqCmd.OP_TYPE_CREATE:
					cheDevice = new CheDevice(deviceGuid);

					// Check to see if the Che is already in our map.
					if (!mCheMap.containsValue(cheDevice)) {
						mCheMap.put(deviceGuid, cheDevice);
					}

					LOGGER.info("Created che: " + cheDevice.getGuid());
					break;

				case IWebSessionReqCmd.OP_TYPE_UPDATE:
					cheDevice = mCheMap.get(deviceGuid);

					if (cheDevice == null) {
						cheDevice = new CheDevice(deviceGuid);
						mCheMap.put(deviceGuid, cheDevice);
					}
					mRadioController.addNetworkDevice(cheDevice);
					LOGGER.info("Updated che: " + cheDevice.getGuid());
					break;

				case IWebSessionReqCmd.OP_TYPE_DELETE:
					cheDevice = mCheMap.remove(deviceGuid);
					LOGGER.info("Deleted che: " + cheDevice.getGuid());
					break;

				default:
					break;
			}
		}
	}

	@Override
	public final boolean canNetworkDeviceAssociate(final NetGuid inGuid) {
		boolean result = false;
		for (CheDevice cheDevice : mCheMap.values()) {
			if (cheDevice.getGuid().equals(inGuid)) {
				result = true;
			}
		}
		return result;
	}

	@Override
	public void deviceLost(INetworkDevice inNetworkDevice) {

	}

	private WebSessionRespCmdEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WebSessionRespCmdEnum result = WebSessionRespCmdEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCmd.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			result = WebSessionRespCmdEnum.fromString(commandTypeNode.getTextValue());
		}

		return result;
	}
}
