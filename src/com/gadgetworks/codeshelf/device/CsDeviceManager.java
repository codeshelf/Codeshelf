/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CsDeviceManager.java,v 1.3 2013/03/04 04:47:27 jeffw Exp $
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.AisleController;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdEnum;
import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdNetAttach;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdEnum;
import com.gadgetworks.codeshelf.web.websocket.ICsWebSocketClient;
import com.gadgetworks.codeshelf.web.websocket.ICsWebsocketClientMsgHandler;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.IRadioControllerEventListener;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class CsDeviceManager implements ICsDeviceManager, ICsWebsocketClientMsgHandler, IRadioControllerEventListener {

	private static final Logger				LOGGER		= LoggerFactory.getLogger(CsDeviceManager.class);

	private Map<NetGuid, INetworkDevice>	mCheMap;
	private IRadioController				mRadioController;
	private ICsWebSocketClient				mWebSocketClient;
	private int								mNextMsgNum	= 1;
	private String							mOrganizationId;
	private String							mFacilityId;
	private String							mNetworkId;
	private String							mNetworkCredential;

	@Inject
	public CsDeviceManager(final ICsWebSocketClient inWebSocketClient, final IRadioController inRadioController) {
		mWebSocketClient = inWebSocketClient;
		mRadioController = inRadioController;
		mCheMap = new HashMap<NetGuid, INetworkDevice>();

		mOrganizationId = System.getProperty("organizationId");
		mFacilityId = System.getProperty("facilityId");
		mNetworkId = System.getProperty("networkId");
		mNetworkCredential = System.getProperty("networkCredential");

	}

	public final void start() {
		mWebSocketClient.start();

		// Start the background startup and wait until it's finished.
		mRadioController.startController((byte) 0x01);
		mRadioController.addControllerEventListener(this);

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> propertiesMap = new HashMap<String, Object>();
		propertiesMap.put("organizationId", mOrganizationId);
		propertiesMap.put("facilityId", mFacilityId);
		propertiesMap.put("networkId", mNetworkId);
		propertiesMap.put("credential", mNetworkCredential);
		ObjectNode dataNode = mapper.valueToTree(propertiesMap);

		sendWebSocketMessageNode(WebSessionReqCmdEnum.NET_ATTACH_REQ, dataNode);
	}

	public final void stop() {

	}

	// --------------------------------------------------------------------------
	/**
	 * Send and log messages over the websocket.
	 * @param inMessage
	 */
	private void sendWebSocketMessage(final String inMessage) {
		LOGGER.info("sent: " + inMessage);
		mWebSocketClient.send(inMessage);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inReqCmd
	 * @param inDataNode
	 * @return
	 */
	private void sendWebSocketMessageNode(final WebSessionReqCmdEnum inReqCmd, ObjectNode inDataNode) {
		ObjectNode msgNode = null;

		ObjectMapper treeMapper = new ObjectMapper();
		msgNode = treeMapper.createObjectNode();
		msgNode.put(IWebSessionCmd.COMMAND_ID_ELEMENT, "cid_" + mNextMsgNum++);
		msgNode.put(IWebSessionCmd.COMMAND_TYPE_ELEMENT, inReqCmd.getName());

		msgNode.put("data", inDataNode);

		sendWebSocketMessage(msgNode.toString());
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websocket.ICsWebsocketClientMsgHandler#handleWebSocketMessage(java.lang.String)
	 */
	@Override
	public final void handleWebSocketMessage(String inMessage) {

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
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.web.websocket.ICsWebsocketClientMsgHandler#handleWoebSocketClosed()
	 */
	@Override
	public final void handleWoebSocketClosed() {
		// This will attempt to start the websocket again (and will block).
		start();
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

			requestChes(persistentId);
			requestAisleControllers(persistentId);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Ask the server to keep us informed of CHEs (and any changes to them).
	 * @param inPersistentId
	 */
	private void requestChes(final String inPersistentId) {
		requestDeviceUpdates(inPersistentId, Che.class.getSimpleName());
	}

	// --------------------------------------------------------------------------
	/**
	 * Ask the server to keep us informed of aisle controllers (and any changes to them).
	 * @param inPersistentId
	 */
	private void requestAisleControllers(final String inPersistentId) {
		requestDeviceUpdates(inPersistentId, AisleController.class.getSimpleName());
	}

	// --------------------------------------------------------------------------
	/**
	 * Request updates to device classes that include a GUID.
	 * It's a filter, so the server will send us any creates, updates and deletes.
	 * @param inPersistentId
	 * @param inClassName
	 */
	private void requestDeviceUpdates(final String inPersistentId, final String inClassName) {
		// Build the response Json object.
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode filterParamsArray = mapper.createArrayNode();
		ObjectNode theIdNode = mapper.createObjectNode();
		theIdNode.put("name", "theId");
		theIdNode.put("value", inPersistentId);
		filterParamsArray.add(theIdNode);

		ArrayNode propertiesArray = mapper.createArrayNode();
		propertiesArray.add(IWebSessionReqCmd.SHORT_DOMAIN_ID);
		propertiesArray.add(IWebSessionReqCmd.DEVICE_GUID);

		Map<String, Object> propertiesMap = new HashMap<String, Object>();
		propertiesMap.put(IWebSessionReqCmd.CLASSNAME, inClassName);
		propertiesMap.put(IWebSessionReqCmd.FILTER_CLAUSE, "parent.persistentId = :theId");
		propertiesMap.put(IWebSessionReqCmd.FILTER_PARAMS, filterParamsArray);
		propertiesMap.put(IWebSessionReqCmd.PROPERTY_NAME_LIST, propertiesArray);
		ObjectNode dataNode = mapper.valueToTree(propertiesMap);

		sendWebSocketMessageNode(WebSessionReqCmdEnum.OBJECT_FILTER_REQ, dataNode);
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
				if (classNode != null) {
					if (classNode.asText().equals(Che.class.getSimpleName())) {
						processCheUpdate(objectNode);
					} else if (classNode.asText().equals(AisleController.class.getSimpleName())) {
						processAisleControllerUpdate(objectNode);
					}
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
		INetworkDevice cheDevice = null;
		NetGuid deviceGuid = new NetGuid(inCheUpdateNode.get(IWebSessionReqCmd.DEVICE_GUID).asText());
		if (updateTypeNode != null) {
			switch (updateTypeNode.getTextValue()) {
				case IWebSessionReqCmd.OP_TYPE_CREATE:
					// Create the CHE.
					cheDevice = new CheLighter(deviceGuid, this, mRadioController);

					// Check to see if the Che is already in our map.
					if (!mCheMap.containsValue(cheDevice)) {
						mCheMap.put(deviceGuid, cheDevice);
						mRadioController.addNetworkDevice(cheDevice);
					}

					LOGGER.info("Created che: " + cheDevice.getGuid());
					break;

				case IWebSessionReqCmd.OP_TYPE_UPDATE:
					// Update the CHE.
					cheDevice = mCheMap.get(deviceGuid);

					if (cheDevice == null) {
						cheDevice = new CheLighter(deviceGuid, this, mRadioController);
						mCheMap.put(deviceGuid, cheDevice);
						mRadioController.addNetworkDevice(cheDevice);
					}
					LOGGER.info("Updated che: " + cheDevice.getGuid());
					break;

				case IWebSessionReqCmd.OP_TYPE_DELETE:
					// Delete the CHE.
					cheDevice = mCheMap.remove(deviceGuid);
					mRadioController.removeNetworkDevice(cheDevice);
					LOGGER.info("Deleted che: " + cheDevice.getGuid());
					break;

				default:
					break;
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inAisleControllerUpdateNode
	 */
	private void processAisleControllerUpdate(final JsonNode inAisleControllerUpdateNode) {
		JsonNode updateTypeNode = inAisleControllerUpdateNode.get(IWebSessionReqCmd.OP_TYPE);
		INetworkDevice aisleDevice = null;
		NetGuid deviceGuid = new NetGuid(inAisleControllerUpdateNode.get(IWebSessionReqCmd.DEVICE_GUID).asText());
		if (updateTypeNode != null) {
			switch (updateTypeNode.getTextValue()) {
				case IWebSessionReqCmd.OP_TYPE_CREATE:
					// Create the CHE.
					aisleDevice = new AisleLighter(deviceGuid, this, mRadioController);

					// Check to see if the Che is already in our map.
					if (!mCheMap.containsValue(aisleDevice)) {
						mCheMap.put(deviceGuid, aisleDevice);
						mRadioController.addNetworkDevice(aisleDevice);
					}

					LOGGER.info("Created che: " + aisleDevice.getGuid());
					break;

				case IWebSessionReqCmd.OP_TYPE_UPDATE:
					// Update the CHE.
					aisleDevice = mCheMap.get(deviceGuid);

					if (aisleDevice == null) {
						aisleDevice = new CheLighter(deviceGuid, this, mRadioController);
						mCheMap.put(deviceGuid, aisleDevice);
						mRadioController.addNetworkDevice(aisleDevice);
					}
					LOGGER.info("Updated che: " + aisleDevice.getGuid());
					break;

				case IWebSessionReqCmd.OP_TYPE_DELETE:
					// Delete the CHE.
					aisleDevice = mCheMap.remove(deviceGuid);
					mRadioController.removeNetworkDevice(aisleDevice);
					LOGGER.info("Deleted che: " + aisleDevice.getGuid());
					break;

				default:
					break;
			}
		}
	}

	@Override
	public final boolean canNetworkDeviceAssociate(final NetGuid inGuid) {
		boolean result = false;
		for (INetworkDevice cheDevice : mCheMap.values()) {
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

	@Override
	public final void requestCheWork(String inCheId, String inContainerId, String inLocationId) {
		LOGGER.info("Request for work: Che: " + inCheId + " Container: " + inContainerId + " Loc: " + inLocationId);
	}
}
