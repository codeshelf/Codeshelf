/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CsDeviceManager.java,v 1.12 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.ws.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;
import com.gadgetworks.codeshelf.ws.command.req.NetAttachWsReqCmd;
import com.gadgetworks.codeshelf.ws.command.req.WsReqCmdEnum;
import com.gadgetworks.codeshelf.ws.command.resp.WsRespCmdEnum;
import com.gadgetworks.codeshelf.ws.websocket.CsWebSocketClient;
import com.gadgetworks.codeshelf.ws.websocket.ICsWebSocketClient;
import com.gadgetworks.codeshelf.ws.websocket.ICsWebsocketClientMsgHandler;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.IRadioControllerEventListener;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author jeffw
 *
 */
public class CsDeviceManager implements ICsDeviceManager, ICsWebsocketClientMsgHandler, IRadioControllerEventListener {

	private static final Logger						LOGGER						= LoggerFactory.getLogger(CsDeviceManager.class);

	private static final String						WEBSOCKET_CHECK				= "Websocket Checker";
	private static final Integer					WEBSOCKET_OPEN_RETRY_MILLIS	= 5000;

	private Map<NetGuid, INetworkDevice>			mCheMap;
	private IRadioController						mRadioController;
	private ICsWebSocketClient						mWebSocketClient;
	private int										mNextMsgNum					= 1;
	private String									mOrganizationId;
	private String									mFacilityId;
	private String									mNetworkId;
	private String									mNetworkCredential;

	private String									mUri;
	private IUtil									mUtil;
	private ICsWebsocketClientMsgHandler			mMessageHandler;
	private WebSocketClient.WebSocketClientFactory	mWebSocketClientFactory;

	@Inject
	public CsDeviceManager(@Named(CsWebSocketClient.WEBSOCKET_URI_PROPERTY) final String inUriStr,
		final IUtil inUtil,
		final ICsWebsocketClientMsgHandler inMessageHandler,
		final WebSocketClient.WebSocketClientFactory inWebSocketClientFactory,
		//final ICsWebSocketClient inWebSocketClient,
		final IRadioController inRadioController) {

		//mWebSocketClient = inWebSocketClient;
		mRadioController = inRadioController;
		mCheMap = new HashMap<NetGuid, INetworkDevice>();

		mUri = inUriStr;
		mUtil = inUtil;
		mMessageHandler = inMessageHandler;
		mWebSocketClientFactory = inWebSocketClientFactory;

		mOrganizationId = System.getProperty("organizationId");
		mFacilityId = System.getProperty("facilityId");
		mNetworkId = System.getProperty("networkId");
		mNetworkCredential = System.getProperty("networkCredential");

	}

	public final void start() {
		// We used to inject this, but the Java_WebSocket is not re-entrant so we have to create new sockets at runtime if the server connection breaks.
		//mWebSocketClient = new CsWebSocketClient(mUri, mUtil, mMessageHandler, mWebSocketClientFactory);
		//mWebSocketClient.start();
		startWebSocket();

		// Start the background startup and wait until it's finished.
		mRadioController.startController((byte) 0x01);
		mRadioController.addControllerEventListener(this);

	}

	public final void startWebSocket() {

		// Man, the Java_Websocket is good except that the client doesn't auto-reconnect or pause if the server is down.
		// This thread checks the state of the websocket connection and reopens it if it's not open.
		// It would be good to work with the Java_Websockets guys to change the client behavior.
		// THIS IS NOT PRETTY - BUT IT MUST WORK.  IF THE SERVER DROPS THE DEVICE MANAGER MUST AUTO-RECONNECT.

		Thread websocketCheckThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if ((mWebSocketClient == null) || (!mWebSocketClient.isStarted())) {
						// We used to inject this, but the Java_WebSocket is not re-entrant so we have to create new sockets at runtime if the server connection breaks.
						mWebSocketClient = new CsWebSocketClient(mUri, mUtil, mMessageHandler, mWebSocketClientFactory);
						mWebSocketClient.start();

						if (mWebSocketClient.isStarted()) {
							ObjectMapper mapper = new ObjectMapper();
							Map<String, Object> propertiesMap = new HashMap<String, Object>();
							propertiesMap.put("organizationId", mOrganizationId);
							propertiesMap.put("facilityId", mFacilityId);
							propertiesMap.put("networkId", mNetworkId);
							propertiesMap.put("credential", mNetworkCredential);
							ObjectNode dataNode = mapper.valueToTree(propertiesMap);

							sendWebSocketMessageNode(WsReqCmdEnum.NET_ATTACH_REQ, dataNode);
						} else {
							try {
								Thread.sleep(WEBSOCKET_OPEN_RETRY_MILLIS);
							} catch (InterruptedException e) {
								LOGGER.error("", e);
							}
						}
					} else {
						try {
							Thread.sleep(WEBSOCKET_OPEN_RETRY_MILLIS);
						} catch (InterruptedException e) {
							LOGGER.error("", e);
						}
					}
				}
			}
		}, WEBSOCKET_CHECK);
		websocketCheckThread.start();
	}

	public final void stop() {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inReqCmd
	 * @param inDataNode
	 * @return
	 */
	private void sendWebSocketMessageNode(final WsReqCmdEnum inReqCmd, ObjectNode inDataNode) {
		ObjectNode msgNode = null;

		ObjectMapper treeMapper = new ObjectMapper();
		msgNode = treeMapper.createObjectNode();
		msgNode.put(IWebSessionCmd.COMMAND_ID_ELEMENT, "cid_" + mNextMsgNum++);
		msgNode.put(IWebSessionCmd.COMMAND_TYPE_ELEMENT, inReqCmd.getName());

		msgNode.put("data", inDataNode);

		mWebSocketClient.send(msgNode.toString());

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

			WsRespCmdEnum commandEnum = getCommandTypeEnum(rootNode);
			String commandId = rootNode.get(IWebSessionCmd.COMMAND_ID_ELEMENT).getTextValue();
			JsonNode dataNode = rootNode.get(IWebSessionCmd.DATA_ELEMENT);

			switch (commandEnum) {
				case NET_ATTACH_RESP:
					processNetAttachResp(dataNode);
					break;

				case OBJECT_FILTER_RESP:
					processFilterResp(dataNode);
					break;

				case CHE_WORK_RESP:
					processCheWorkResp(dataNode);
					break;

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
	public final void handleWebSocketClosed() {
		// This will attempt to start the websocket again (and will block).
		//mWebSocketClient.stop();
		//startWebSocket();
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataNode
	 */
	private void processNetAttachResp(final JsonNode inDataNode) {
		JsonNode responseNode = inDataNode.get(WsRespCmdEnum.NET_ATTACH_RESP.toString());

		if ((responseNode != null) && (responseNode.getTextValue().equals(NetAttachWsReqCmd.SUCCEED))) {
			JsonNode networkNode = inDataNode.get("codeshelf_network");
			JsonNode networkPersistentIdNode = networkNode.get("persistentId");
			String persistentId = networkPersistentIdNode.getTextValue();

			requestCheDevices(persistentId);
			requestAisleDevices(persistentId);
		}

	}

	// --------------------------------------------------------------------------
	/**
	 * Ask the server to keep us informed of CHEs (and any changes to them).
	 * @param inPersistentId
	 */
	private void requestCheDevices(final String inPersistentId) {
		requestDeviceUpdates(inPersistentId, Che.class.getSimpleName());
	}

	// --------------------------------------------------------------------------
	/**
	 * Ask the server to keep us informed of aisle devices (and any changes to them).
	 * @param inPersistentId
	 */
	private void requestAisleDevices(final String inPersistentId) {
		requestDeviceUpdates(inPersistentId, LedController.class.getSimpleName());
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
		propertiesArray.add(IWsReqCmd.SHORT_DOMAIN_ID);
		propertiesArray.add(IWsReqCmd.DEVICE_GUID);

		Map<String, Object> propertiesMap = new HashMap<String, Object>();
		propertiesMap.put(IWsReqCmd.CLASSNAME, inClassName);
		propertiesMap.put(IWsReqCmd.FILTER_CLAUSE, "parent.persistentId = :theId");
		propertiesMap.put(IWsReqCmd.FILTER_PARAMS, filterParamsArray);
		propertiesMap.put(IWsReqCmd.PROPERTY_NAME_LIST, propertiesArray);
		ObjectNode dataNode = mapper.valueToTree(propertiesMap);

		sendWebSocketMessageNode(WsReqCmdEnum.OBJECT_FILTER_REQ, dataNode);
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataNode
	 */
	private void processFilterResp(final JsonNode inDataNode) {
		JsonNode resultsNode = inDataNode.get(IWsReqCmd.RESULTS);

		if (resultsNode != null) {

			for (JsonNode objectNode : resultsNode) {
				JsonNode classNode = objectNode.get(IWsReqCmd.CLASSNAME);
				if (classNode != null) {
					if (classNode.asText().equals(Che.class.getSimpleName())) {
						processCheUpdate(objectNode);
					} else if (classNode.asText().equals(LedController.class.getSimpleName())) {
						processLedControllerUpdate(objectNode);
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataNode
	 */
	private void processCheWorkResp(final JsonNode inDataNode) {
		JsonNode resultsNode = inDataNode.get(IWsReqCmd.RESULTS);

		NetGuid cheId = new NetGuid("0x" + inDataNode.get("cheId").asText());

		CheDevice cheDevice = (CheDevice) mCheMap.get(cheId);

		if (cheDevice != null) {
			if (resultsNode != null) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					List<WorkInstruction> wiList = mapper.readValue(resultsNode, new TypeReference<List<WorkInstruction>>() {
					});
					//				
					//				for (JsonNode objectNode : resultsNode) {
					//					DeployedWorkInstruction wi = new DeployedWorkInstruction();
					//					wi.setLedController(objectNode.get("acId").asText());
					//					wi.setLedControllerCmd(objectNode.get("acCmd").asText());
					//					wi.setContainerId(objectNode.get("cntrId").asText());
					//					wi.setLocation(objectNode.get("loc").asText());
					//					wi.setQuantity(objectNode.get("qty").asInt());
					//					wi.setSkuId(objectNode.get("sku").asText());
					//					wi.setColor(ColorEnum.valueOf(objectNode.get("color").asText()));
					//					wiList.add(wi);
					//				}
					if (wiList.size() > 0) {
						cheDevice.assignWork(wiList);
					}
				} catch (JsonParseException e) {
					LOGGER.error("", e);
				} catch (JsonMappingException e) {
					LOGGER.error("", e);
				} catch (IOException e) {
					LOGGER.error("", e);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCheUpdateNode
	 */
	private void processCheUpdate(final JsonNode inCheUpdateNode) {
		JsonNode updateTypeNode = inCheUpdateNode.get(IWsReqCmd.OP_TYPE);
		INetworkDevice cheDevice = null;
		NetGuid deviceGuid = new NetGuid(inCheUpdateNode.get(IWsReqCmd.DEVICE_GUID).asText());
		UUID persistentId = UUID.fromString(inCheUpdateNode.get(IWsReqCmd.PERSISTENT_ID).asText());
		if (updateTypeNode != null) {
			switch (updateTypeNode.getTextValue()) {
				case IWsReqCmd.OP_TYPE_CREATE:
					// Create the CHE.
					cheDevice = new CheDevice(persistentId, deviceGuid, this, mRadioController);

					// Check to see if the Che is already in our map.
					if (!mCheMap.containsValue(cheDevice)) {
						mCheMap.put(deviceGuid, cheDevice);
						mRadioController.addNetworkDevice(cheDevice);
					}

					LOGGER.info("Created che: " + cheDevice.getGuid());
					break;

				case IWsReqCmd.OP_TYPE_UPDATE:
					// Update the CHE.
					cheDevice = mCheMap.get(deviceGuid);

					if (cheDevice == null) {
						cheDevice = new CheDevice(persistentId, deviceGuid, this, mRadioController);
						mCheMap.put(deviceGuid, cheDevice);
						mRadioController.addNetworkDevice(cheDevice);
					}
					LOGGER.info("Updated che: " + cheDevice.getGuid());
					break;

				case IWsReqCmd.OP_TYPE_DELETE:
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
	 * @param inLedControllerUpdateNode
	 */
	private void processLedControllerUpdate(final JsonNode inLedControllerUpdateNode) {
		JsonNode updateTypeNode = inLedControllerUpdateNode.get(IWsReqCmd.OP_TYPE);
		INetworkDevice aisleDevice = null;
		NetGuid deviceGuid = new NetGuid(inLedControllerUpdateNode.get(IWsReqCmd.DEVICE_GUID).asText());
		UUID persistentId = UUID.fromString(inLedControllerUpdateNode.get(IWsReqCmd.PERSISTENT_ID).asText());
		if (updateTypeNode != null) {
			switch (updateTypeNode.getTextValue()) {
				case IWsReqCmd.OP_TYPE_CREATE:
					// Create the aisle device.
					aisleDevice = new AisleDevice(persistentId, deviceGuid, this, mRadioController);

					// Check to see if the aisle device is already in our map.
					if (!mCheMap.containsValue(aisleDevice)) {
						mCheMap.put(deviceGuid, aisleDevice);
						mRadioController.addNetworkDevice(aisleDevice);
					}

					LOGGER.info("Created aisle device: " + aisleDevice.getGuid());
					break;

				case IWsReqCmd.OP_TYPE_UPDATE:
					// Update the aisle device.
					aisleDevice = mCheMap.get(deviceGuid);

					if (aisleDevice == null) {
						aisleDevice = new AisleDevice(persistentId, deviceGuid, this, mRadioController);
						mCheMap.put(deviceGuid, aisleDevice);
						mRadioController.addNetworkDevice(aisleDevice);
					}
					LOGGER.info("Updated aisle device: " + aisleDevice.getGuid());
					break;

				case IWsReqCmd.OP_TYPE_DELETE:
					// Delete the aisle device.
					aisleDevice = mCheMap.remove(deviceGuid);
					mRadioController.removeNetworkDevice(aisleDevice);
					LOGGER.info("Deleted aisle device: " + aisleDevice.getGuid());
					break;

				default:
					break;
			}
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IRadioControllerEventListener#canNetworkDeviceAssociate(com.gadgetworks.flyweight.command.NetGuid)
	 */
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

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IRadioControllerEventListener#deviceLost(com.gadgetworks.flyweight.controller.INetworkDevice)
	 */
	@Override
	public void deviceLost(INetworkDevice inNetworkDevice) {

	}

	// --------------------------------------------------------------------------
	/**
	 * @param inCommandAsJson
	 * @return
	 */
	private WsRespCmdEnum getCommandTypeEnum(JsonNode inCommandAsJson) {
		WsRespCmdEnum result = WsRespCmdEnum.INVALID;

		JsonNode commandTypeNode = inCommandAsJson.get(IWebSessionCmd.COMMAND_TYPE_ELEMENT);
		if (commandTypeNode != null) {
			result = WsRespCmdEnum.fromString(commandTypeNode.getTextValue());
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.device.ICsDeviceManager#requestCheWork(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public final void requestCheWork(final String inCheId, final UUID inPersistentId, final String inLocationId, final List<String> inContainerIdList) {
		LOGGER.info("Request for work: Che: " + inCheId + " Container: " + inContainerIdList.toString() + " Loc: " + inLocationId);

		// Build the response Json object.
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode dataNode = mapper.createObjectNode();
		dataNode.put("persistentId", inPersistentId.toString());
		dataNode.put("locationId", inLocationId);

		ArrayNode propertiesArray = mapper.createArrayNode();
		for (String containerId : inContainerIdList) {
			propertiesArray.add(containerId);
		}
		dataNode.put("containerIds", propertiesArray);
		sendWebSocketMessageNode(WsReqCmdEnum.CHE_WORK_REQ, dataNode);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.device.ICsDeviceManager#completeWi(java.lang.String, java.util.UUID, com.gadgetworks.codeshelf.model.domain.WorkInstruction)
	 */
	@Override
	public final void completeWi(final String inCheId, final UUID inPersistentId, final WorkInstruction inWorkInstruction) {
		LOGGER.info("Complete: Che: " + inCheId + " WI: " + inWorkInstruction.toString());

		inWorkInstruction.setStatusEnum(WorkInstructionStatusEnum.COMPLETE);
		
		// Build the response Json object.
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode dataNode = mapper.createObjectNode();
		dataNode.put("persistentId", inPersistentId.toString());
		dataNode.put("wi", mapper.valueToTree(inWorkInstruction));

		sendWebSocketMessageNode(WsReqCmdEnum.CHE_WICOMPLETE_REQ, dataNode);
	}
}
