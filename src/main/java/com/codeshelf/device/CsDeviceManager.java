/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CsDeviceManager.java,v 1.19 2013/07/20 00:54:49 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.OutOfRangeException;
import com.codeshelf.flyweight.command.CommandControlPosconBroadcast;
import com.codeshelf.flyweight.command.CommandControlPosconSetup;
import com.codeshelf.flyweight.command.NetEndpoint;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;
import com.codeshelf.flyweight.controller.PacketCaptureListener;
import com.codeshelf.model.DeviceType;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.PcapRecord;
import com.codeshelf.util.PcapRingBuffer;
import com.codeshelf.util.TwoKeyMap;
import com.codeshelf.ws.client.CsClientEndpoint;
import com.codeshelf.ws.client.WebSocketEventListener;
import com.codeshelf.ws.protocol.message.LightLedsInstruction;
import com.codeshelf.ws.protocol.message.NotificationMessage;
import com.codeshelf.ws.protocol.message.PosConLightAddressesMessage;
import com.codeshelf.ws.protocol.message.PosConSetupMessage;
import com.codeshelf.ws.protocol.request.LinkRemoteCheRequest;
import com.codeshelf.ws.protocol.request.CompleteWorkInstructionRequest;
import com.codeshelf.ws.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.protocol.request.ComputePutWallInstructionRequest;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest;
import com.codeshelf.ws.protocol.request.TapeLocationDecodingRequest;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest.ComputeWorkPurpose;
import com.codeshelf.ws.protocol.request.InventoryLightItemRequest;
import com.codeshelf.ws.protocol.request.InventoryLightLocationRequest;
import com.codeshelf.ws.protocol.request.InventoryUpdateRequest;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.request.VerifyBadgeRequest;
import com.codeshelf.ws.protocol.response.FailureResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class CsDeviceManager implements IRadioControllerEventListener, WebSocketEventListener, PacketCaptureListener {

	private static final Logger							LOGGER						= LoggerFactory.getLogger(CsDeviceManager.class);

	static final String									DEVICETYPE_CHE				= "CHE";
	static final String									DEVICETYPE_LED				= "LED Controller";
	static final String									DEVICETYPE_POS_CON_CTRL		= "PosCon Controller";
	static final String									DEVICETYPE_CHE_SETUPORDERS	= "CHE_SETUPORDERS";
	static final String									DEVICETYPE_CHE_LINESCAN		= "CHE_LINESCAN";

	private TwoKeyMap<UUID, NetGuid, INetworkDevice>	mDeviceMap;

	private Map<NetGuid, CheData>						mDeviceDataMap;

	@Getter
	private IRadioController							radioController;

	@Getter
	private PcapRingBuffer								pcapBuffer;

	private String										username;
	private String										password;

	/* Device Manager owns websocket configuration too */
	//	@Getter
	//	private URI											mUri;

	@Getter
	private long										lastNetworkUpdate			= 0;

	private boolean										isAttachedToServer			= false;

	private boolean										autoShortValue				= true;											// log on set (below)
	private boolean										pickMultValue				= false;											// log on set (below)

	@Getter
	@Setter
	private String										pickInfoValue				= "SKU";

	@Getter
	@Setter
	private String										containerTypeValue			= "Order";

	@Getter
	@Setter
	private String										scanTypeValue				= "Disabled";

	@Getter
	@Setter
	private String										sequenceKind				= "BayDistance";

	@Getter
	CsClientEndpoint									clientEndpoint;

	@Inject
	public CsDeviceManager(final IRadioController inRadioController, final CsClientEndpoint clientEndpoint) {
		this.clientEndpoint = clientEndpoint;
		CsClientEndpoint.setEventListener(this);

		radioController = inRadioController;
		mDeviceMap = new TwoKeyMap<UUID, NetGuid, INetworkDevice>();

		mDeviceDataMap = new HashMap<NetGuid, CheData>();

		username = System.getProperty("websocket.username");
		password = System.getProperty("websocket.password");

		if (Boolean.getBoolean("pcapbuffer.enable")) {
			// set up ring buffer
			int pcSize = Integer.getInteger("pcapbuffer.size", PcapRingBuffer.DEFAULT_SIZE);
			int pcSlack = Integer.getInteger("pcapbuffer.slack", PcapRingBuffer.DEFAULT_SLACK);
			this.pcapBuffer = new PcapRingBuffer(pcSize, pcSlack);

			// listen for packets
			radioController.getGatewayInterface().setPacketListener(this);
		}
	}

	private boolean isRadioEnabled() {
		// leaving as a function for now. But currently, no known use case for CsDeviceManager that does not have a radio
		return true;
	}

	public boolean getPickMultValue() {
		return this.pickMultValue;
	}

	public void setPickMultValue(boolean inValue) {
		pickMultValue = inValue;
		LOGGER.info("Site controller setting PICKMULT value = {}", inValue);
	}

	public boolean getAutoShortValue() {
		return this.autoShortValue;
	}

	public void setAutoShortValue(boolean inValue) {
		autoShortValue = inValue;
		LOGGER.info("Site controller setting AUTOSHRT value = {}", inValue);
	}

	private final void startRadio(CodeshelfNetwork network) {
		if (radioController.isRunning()) {
			LOGGER.warn("Radio controller is already running, cannot start again");
		} else if (this.isRadioEnabled()) {
			// start radio controller
			NetworkId networkId = new NetworkId(network.getNetworkNum().byteValue());
			radioController.setNetworkId(networkId);
			radioController.startController(network.getChannel().byteValue());
			radioController.addControllerEventListener(this);
		} else {
			LOGGER.warn("Radio controller disabled by setting, cannot start");
			radioController.setNetworkId(new NetworkId((byte) 1)); // for test

		}
	}

	public final List<AisleDeviceLogic> getAisleControllers() {
		ArrayList<AisleDeviceLogic> aList = new ArrayList<AisleDeviceLogic>();
		for (INetworkDevice theDevice : mDeviceMap.values()) {
			if (theDevice instanceof AisleDeviceLogic)
				aList.add((AisleDeviceLogic) theDevice);
		}
		return aList;
	}

	public final List<PosManagerDeviceLogic> getPosConControllers() {
		ArrayList<PosManagerDeviceLogic> aList = new ArrayList<PosManagerDeviceLogic>();
		for (INetworkDevice theDevice : mDeviceMap.values()) {
			if (theDevice instanceof PosManagerDeviceLogic)
				aList.add((PosManagerDeviceLogic) theDevice);
		}
		return aList;
	}

	public final List<CheDeviceLogic> getCheControllers() {
		ArrayList<CheDeviceLogic> aList = new ArrayList<CheDeviceLogic>();
		for (INetworkDevice theDevice : mDeviceMap.values()) {
			if (theDevice instanceof CheDeviceLogic)
				aList.add((CheDeviceLogic) theDevice);
		}
		return aList;
	}

	/*
	public final void stop() {
		radioController.stopController();
	}
	*/

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#getDeviceByGuid(com.codeshelf.flyweight.command.NetGuid)
	 */
	public INetworkDevice getDeviceByGuid(NetGuid inGuid) {
		return mDeviceMap.get(inGuid);
	}

	public INetworkDevice getDevice(Object deviceIdentifier) {
		INetworkDevice result = null;

		if (deviceIdentifier instanceof NetGuid || deviceIdentifier instanceof UUID) {
			result = mDeviceMap.get(deviceIdentifier);
		} else if (deviceIdentifier instanceof String) {
			// string representation of a NetGuid or UUID
			String id = (String) deviceIdentifier;
			deviceIdentifier = null;
			try {
				if (id.length() == NetGuid.NET_GUID_HEX_CHARS) {
					deviceIdentifier = getNetGuidFromPrefixHexString(id);
				} else {
					deviceIdentifier = UUID.fromString(id);
				}
			} catch (Exception e) {
			}
			if (deviceIdentifier != null) {
				result = mDeviceMap.get(deviceIdentifier);
			}
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* 
	 * Package convenience function for put wall button press. Need to easily find the CheDeviceLogic that asked the position to be lit.
	 * Also used privately for remote CHE association
	 * The inControllerId should have leading "0x" except for unrealistic unit tests
	 */
	CheDeviceLogic getCheDeviceByControllerId(String inControllerId) {
		if (inControllerId == null)
			return null;

		NetGuid theGuid = getNetGuidFromPrefixHexString(inControllerId);
		return getCheDeviceByNetGuid(theGuid);
	}

	// --------------------------------------------------------------------------
	/* 
	 * Package convenience function for put wall button press. Need to easily find the CheDeviceLogic that asked the position to be lit.
	 * Also used privately for remote CHE association
	 */
	CheDeviceLogic getCheDeviceByNetGuid(NetGuid inGuid) {
		if (inGuid == null)
			return null;

		INetworkDevice theDevice = mDeviceMap.get(inGuid);
		if (theDevice == null)
			return null;
		else if (theDevice instanceof CheDeviceLogic)
			return (CheDeviceLogic) theDevice;
		else {
			LOGGER.error("unexpected device type for {} in getCheDeviceByControllerId", inGuid);
			return null;
		}
	}

	// --------------------------------------------------------------------------
	/* 
	 * Public convenience function
	 */
	public final PosManagerDeviceLogic getPosManagerDeviceByControllerId(String controllerId) {
		if (controllerId == null)
			return null;

		NetGuid theGuid = getNetGuidFromPrefixHexString(controllerId);
		if (theGuid == null)
			return null;
		INetworkDevice theDevice = mDeviceMap.get(theGuid);
		if (theDevice == null)
			return null;
		else if (theDevice instanceof PosManagerDeviceLogic)
			return (PosManagerDeviceLogic) theDevice;
		else {
			LOGGER.error("unexpected device type for {} in getPosManagerDeviceByControllerId", controllerId);
			return null;
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.IRadioControllerEventListener#canNetworkDeviceAssociate(com.codeshelf.flyweight.command.NetGuid)
	 */
	@Override
	public final boolean canNetworkDeviceAssociate(final NetGuid inGuid) {
		boolean result = false;

		INetworkDevice networkDevice = mDeviceMap.get(inGuid);
		if (networkDevice != null) {
			result = true;
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.IRadioControllerEventListener#deviceLost(com.codeshelf.flyweight.controller.INetworkDevice)
	 */
	@Override
	public void deviceLost(INetworkDevice inNetworkDevice) {
	}

	@Override
	public void deviceActive(INetworkDevice inNetworkDevice) {
		if (inNetworkDevice instanceof CheDeviceLogic) {
			if (isAttachedToServer) {
				((CheDeviceLogic) inNetworkDevice).connectedToServer();
			} else {
				((CheDeviceLogic) inNetworkDevice).disconnectedFromServer();
			}
		} else if (inNetworkDevice instanceof PosManagerDeviceLogic) {
			if (isAttachedToServer) {
				((PosManagerDeviceLogic) inNetworkDevice).connectedToServer();
			} else {
				((PosManagerDeviceLogic) inNetworkDevice).disconnectedFromServer();
			}
		}

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#requestCheWork(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void computeCheWork(final String inCheId,
		final UUID inPersistentId,
		final Map<String, String> positionToContainerMap,
		final Boolean reverse) {
		LOGGER.debug("Compute work: Che={}; Container={}", inCheId, positionToContainerMap);
		String cheId = inPersistentId.toString();
		ComputeWorkRequest req = new ComputeWorkRequest(ComputeWorkPurpose.COMPUTE_WORK,
			cheId,
			null,
			positionToContainerMap,
			reverse);
		clientEndpoint.sendMessage(req);
	}

	public void computeCheWork(final String inCheId, final UUID inPersistentId, final String orderDetailId) {
		LOGGER.debug("Compute work: Che={}; DetailId={}", inCheId, orderDetailId);
		String cheId = inPersistentId.toString();
		ComputeDetailWorkRequest req = new ComputeDetailWorkRequest(cheId, orderDetailId);
		clientEndpoint.sendMessage(req);
	}

	public void verifyBadge(final String inCheId, final UUID inPersistentId, final String badge) {
		LOGGER.debug("Verify badge: Che={}; badge={}", inCheId, badge);
		String cheId = inPersistentId.toString();
		VerifyBadgeRequest req = new VerifyBadgeRequest(cheId, badge);
		clientEndpoint.sendMessage(req);
	}

	public void computePutWallInstruction(final String inCheId, final UUID inPersistentId, String itemOrUpc, String putWallName) {
		LOGGER.debug("computePutWallInstruction: Che={}; ", inCheId);
		String cheId = inPersistentId.toString();
		ComputePutWallInstructionRequest req = new ComputePutWallInstructionRequest(cheId, itemOrUpc, putWallName);
		clientEndpoint.sendMessage(req);
	}

	public void linkRemoteChe(final String inCheId, final UUID inPersistentId, String cheIdToAssociateTo) {
		LOGGER.debug("associateRemoteChe: Che={}; ", inCheId);
		String cheId = inPersistentId.toString();
		LinkRemoteCheRequest req = new LinkRemoteCheRequest(cheId, cheIdToAssociateTo);
		clientEndpoint.sendMessage(req);
	}

	public void requestTapeDecoding(final String inCheId, final UUID inPersistentId, final String tapeId) {
		LOGGER.debug("Decode tape: Che={}; tape={}", inCheId, tapeId);
		String cheId = inPersistentId.toString();
		TapeLocationDecodingRequest req = new TapeLocationDecodingRequest(cheId, tapeId);
		clientEndpoint.sendMessage(req);
	}

	public void sendNotificationMessage(final NotificationMessage message) {
		LOGGER.debug("Notify: Device={}; type={}", message.getDevicePersistentId(), message.getEventType());
		clientEndpoint.sendMessage(message);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#requestCheWork(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void getCheWork(final String inCheId,
		final UUID inPersistentId,
		final String inLocationId,
		final Map<String, String> positionToContainerMap,
		final Boolean reversePickOrder,
		final Boolean reverseOrderFromLastTime) {
		LOGGER.debug("Get work: Che={}; Loc={}", inCheId, inLocationId);
		String cheId = inPersistentId.toString();
		ComputeWorkRequest req = new ComputeWorkRequest(ComputeWorkPurpose.GET_WORK,
			cheId,
			inLocationId,
			positionToContainerMap,
			reversePickOrder);
		clientEndpoint.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#completeWi(java.lang.String, java.util.UUID, com.codeshelf.model.domain.WorkInstruction)
	 */
	public void completeWi(final String inCheId, final UUID inPersistentId, final WorkInstruction inWorkInstruction) {
		LOGGER.debug("Complete: Che={}; WI={};", inCheId, inWorkInstruction);
		CompleteWorkInstructionRequest req = new CompleteWorkInstructionRequest(inPersistentId.toString(), inWorkInstruction);
		clientEndpoint.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#inventoryScan(final UUID inCheId, final UUID inPersistentId, final String inLocationId, final String inGtin)
	 */
	public void inventoryUpdateScan(final UUID inPersistentId, final String inLocationId, final String inGtin) {
		LOGGER.debug("Inventory update Scan: Che={}; Loc={}; GTIN={};", inPersistentId, inLocationId, inGtin);
		InventoryUpdateRequest req = new InventoryUpdateRequest(inPersistentId.toString(), inGtin, inLocationId);
		clientEndpoint.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#inventoryScan(final String inCheId, final UUID inPersistentId, final String inLocationId, final String inGtin)
	 */
	public void inventoryLightItemScan(final UUID inPersistentId, final String inGtin) {
		LOGGER.debug("Inventory light location request: Che={};  GTIN={};", inPersistentId, inGtin);
		InventoryLightItemRequest req = new InventoryLightItemRequest(inPersistentId.toString(), inGtin);
		clientEndpoint.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#inventoryLightLocationScan(final String inCheId, final UUID inPersistentId, final String inLocation)
	 */
	public void inventoryLightLocationScan(final UUID inPersistentId, final String inLocation, boolean isTape) {
		LOGGER.debug("Inventory light location request: Che={};  Location={};", inPersistentId, inLocation);
		InventoryLightLocationRequest req = new InventoryLightLocationRequest(inPersistentId.toString(), inLocation, isTape);
		clientEndpoint.sendMessage(req);
	}

	/**
	 * Websocket connects then this authenticates and receives the network it should use
	 * @see #attached(CodeshelfNetwork)
	 */
	@Override
	public void connected() {
		// connected to server - send attach request
		LOGGER.info("Connected to server");
		LoginRequest loginRequest = new LoginRequest(username, password);
		loginRequest.setDeviceLogin(true);
		clientEndpoint.sendMessage(loginRequest);
	}

	/**
	 * After connection and authentication this is received to indicate communication for devices is established
	 * @see #connected()
	 */
	public void attached(CodeshelfNetwork network) {
		LOGGER.info("Attached to server");
		this.updateNetwork(network);
		this.startRadio(network);

		isAttachedToServer = true;
		for (INetworkDevice networkDevice : mDeviceMap.values()) {
			if (networkDevice instanceof CheDeviceLogic) {
				((CheDeviceLogic) networkDevice).connectedToServer();
			}
			if (networkDevice instanceof PosManagerDeviceLogic) {
				((PosManagerDeviceLogic) networkDevice).connectedToServer();
			}
		}
	}

	public void unattached() {
		if (!isAttachedToServer)
			return; // don't get stuck in a loop if device manager is requesting disconnection

		LOGGER.info("Unattached from server");
		isAttachedToServer = false;
		for (INetworkDevice networkDevice : mDeviceMap.values()) {
			if (networkDevice instanceof CheDeviceLogic) {
				((CheDeviceLogic) networkDevice).disconnectedFromServer();
			}
			if (networkDevice instanceof PosManagerDeviceLogic) {
				((PosManagerDeviceLogic) networkDevice).disconnectedFromServer();
			}
		}
		if (clientEndpoint.isConnected()) {
			try {
				clientEndpoint.disconnect();
			} catch (IOException e) {
				LOGGER.error("failed to disconnect client", e);
			}
		}
	}

	@Override
	public void disconnected() {
		unattached();
		LOGGER.info("Disconnected from server");
	}

	@SuppressWarnings("unused")
	private boolean needNewDevice(INetworkDevice existingDevice, NetGuid newDeviceGuid, String newDeviceType) {
		if (existingDevice == null) {
			LOGGER.error(" error in needNewDevice");
			return false;
		}
		if (!existingDevice.getGuid().equals(newDeviceGuid)) {
			return true;
		}
		String oldDeviceType = existingDevice.getDeviceType();
		if (!oldDeviceType.equals(newDeviceType)) {
			String oldDeviceType2 = existingDevice.getDeviceType();
			return true;
		}
		return false;
	}

	private void doCreateUpdateNetDevice(UUID persistentId, NetGuid deviceGuid, String deviceType, Che che) {
		// che is often null. Only needed for SetupOrdersDeviceLogic
		Preconditions.checkNotNull(persistentId, "persistentId of device cannot be null");
		Preconditions.checkNotNull(deviceGuid, "deviceGuid of device cannot be null");
		Preconditions.checkNotNull(deviceType, "deviceType of device cannot be null");
		// Update the device or create if it does not exist
		// NOTE: it appears CsDeviceManager receives but does not use about the domainId e.g. "CHE1" or "00000013"
		boolean suppressMapUpdate = false;
		INetworkDevice netDevice = mDeviceMap.get(persistentId);

		if (netDevice == null) {
			// new device
			if (deviceType.equals(DEVICETYPE_CHE)) {
				netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController, che);
			} else if (deviceType.equals(DEVICETYPE_CHE_SETUPORDERS)) {
				netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController, che);
			} else if (deviceType.equals(DEVICETYPE_CHE_LINESCAN)) {
				netDevice = new LineScanDeviceLogic(persistentId, deviceGuid, this, radioController);
			} else if (deviceType.equals(DEVICETYPE_LED)) {
				netDevice = new AisleDeviceLogic(persistentId, deviceGuid, this, radioController);
			} else if (deviceType.equals(DEVICETYPE_POS_CON_CTRL)) {
				netDevice = new PosManagerDeviceLogic(persistentId, deviceGuid, this, radioController);
			} else {
				LOGGER.error("Don't know how to create new network device of type={}", deviceType);
				suppressMapUpdate = true;
			}

			if (!suppressMapUpdate) {
				INetworkDevice oldNetworkDevice = radioController.getNetworkDevice(deviceGuid);
				if (oldNetworkDevice != null) {
					LOGGER.warn("Creating device={}; guid={}; but a NetworkDevice already existed with that NetGuid (removing)",
						deviceType,
						deviceGuid);
					radioController.removeNetworkDevice(oldNetworkDevice);
				} else {
					LOGGER.info("Creating deviceType={}; persistentId={}; guid={}", deviceType, persistentId, netDevice.getGuid());
					// Let's see if we get the CHE's name and associated guid
					if (che != null)
						LOGGER.info("CHE name={}; associatedGuid={};", che.getDomainId(), che.getAssociateToCheGuid());

				}
				radioController.addNetworkDevice(netDevice);
			}
		} else {
			// update existing device
			if (needNewDevice(netDevice, deviceGuid, deviceType)) {
				// if (!netDevice.getGuid().equals(deviceGuid)) {
				// changing NetGuid (deprecated/bad!)
				INetworkDevice oldNetworkDevice = radioController.getNetworkDevice(netDevice.getGuid());
				if (oldNetworkDevice != null) {
					LOGGER.warn("Deleting and remaking prior deviceType={} prior guid={}; new deviceType={} new guid={};",
						oldNetworkDevice.getDeviceType(),
						oldNetworkDevice.getGuid(),
						deviceType,
						deviceGuid);
					// doDeleteNetDevice(persistentId, deviceGuid); // try this?
					radioController.removeNetworkDevice(oldNetworkDevice); // only this originally
					// mDeviceMap.remove(deviceGuid); // or this?
				} else {
					LOGGER.error("Changing NetGuid of deviceType={}; from guid={} to guid={} but couldn't find original network device",
						deviceType,
						netDevice.getGuid(),
						deviceGuid);
				}
				// can't really change the NetGuid so we will create new device
				if (deviceType.equals(DEVICETYPE_CHE)) {
					netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController, che);
				} else if (deviceType.equals(DEVICETYPE_CHE_SETUPORDERS)) {
					netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController, che);
				} else if (deviceType.equals(DEVICETYPE_CHE_LINESCAN)) {
					netDevice = new LineScanDeviceLogic(persistentId, deviceGuid, this, radioController);
				} else if (deviceType.equals(DEVICETYPE_LED)) {
					netDevice = new AisleDeviceLogic(persistentId, deviceGuid, this, radioController);
				} else if (deviceType.equals(DEVICETYPE_POS_CON_CTRL)) {
					netDevice = new PosManagerDeviceLogic(persistentId, deviceGuid, this, radioController);
				} else {
					LOGGER.error("Cannot update existing network device of unrecognized type={}", deviceType);
					suppressMapUpdate = true;
				}
				if (!suppressMapUpdate) {
					radioController.addNetworkDevice(netDevice);
				}

				//TODO if associated to che guid does not match what we have, we need to have the device align itself.
			} else if (che != null && netDevice.needUpdateCheDetails(deviceGuid, che.getDomainId(), che.getAssociateToCheGuid())) {
				LOGGER.debug("No update to. deviceType={}; guid={};", deviceType, deviceGuid);
				suppressMapUpdate = true; // did the update within the existing map. No change to the TwoKeyMap
			} else {
				// if not changing netGuid, there is nothing to change
				LOGGER.debug("No update to. deviceType={}; guid={};", deviceType, deviceGuid);
				suppressMapUpdate = true;
			}
		}

		// update device map will also remove any mismatches (e.g. other entries with same guid/persistentId - see TwoKeyMap)
		if (!suppressMapUpdate) {
			mDeviceMap.put(persistentId, deviceGuid, netDevice);
		}
	}

	private void doDeleteNetDevice(UUID persistentId, NetGuid deviceGuid) {
		// Delete the CHE or LED controller.
		INetworkDevice netDevice = mDeviceMap.remove(persistentId);
		String deviceType = "unknown device";
		if (netDevice == null) {
			LOGGER.error("Failed to remove " + deviceType + " " + persistentId + " / " + deviceGuid
					+ " from device map by persistentId, will try NetGuid");
			netDevice = mDeviceMap.remove(deviceGuid);
			if (netDevice == null) {
				LOGGER.error("Failed to remove " + deviceType + " " + persistentId + " / " + deviceGuid
						+ " from device map by NetGuid");
				// but still try to remove from radio controller
				INetworkDevice deviceByNetGuid = radioController.getNetworkDevice(deviceGuid);
				if (deviceByNetGuid != null) {
					radioController.removeNetworkDevice(deviceByNetGuid);
					LOGGER.error("Removed unmapped " + deviceType + " " + persistentId + " / " + deviceGuid
							+ " from Radio Controller by NetGuid");
				} else {
					LOGGER.error("Failed to remove unmapped " + deviceType + " " + persistentId + " / " + deviceGuid
							+ " from Radio Controller by NetGuid");
				}
			} else {
				deviceType = netDevice.getClass().getSimpleName();
				radioController.removeNetworkDevice(netDevice);
				LOGGER.warn("Removed partially unmapped " + deviceType + " " + persistentId + " / " + deviceGuid
						+ " from device map and Radio Controller by NetGuid");
			}
		} else {
			deviceType = netDevice.getClass().getSimpleName();
			radioController.removeNetworkDevice(netDevice);
			LOGGER.info("Removed deviceType={}; persistentId={}; guid={}", deviceType, persistentId, netDevice.getGuid());
		}
	}

	/**
	 * This API used by the test system only so far, for changing process mode for the picker.
	 * The persistentId and GUID should not change. Only the process mode.
	 */
	public INetworkDevice updateOneDevice(UUID persistentId, NetGuid deviceGuid, String newProcessType) {
		Preconditions.checkNotNull(persistentId, "persistentId cannot be null");
		Preconditions.checkNotNull(deviceGuid, "deviceGuidc annot be null");
		Preconditions.checkNotNull(newProcessType, "newProcessTypecannot be null");
		LOGGER.info("updateOneDevice: " + deviceGuid + " " + newProcessType);
		// make sure this GUID exists.
		INetworkDevice existingDevice = mDeviceMap.get(persistentId);
		if (existingDevice == null || !deviceGuid.equals(existingDevice.getGuid())) {
			LOGGER.error("misuse of updateOneDevice()");
			return existingDevice;
		}
		doCreateUpdateNetDevice(persistentId, deviceGuid, newProcessType, null);
		INetworkDevice newDevice = mDeviceMap.get(persistentId);
		return newDevice;
	}

	public void updateNetwork(CodeshelfNetwork network) {
		Set<UUID> updateDevices = new HashSet<UUID>();
		// update network devices
		LOGGER.info("updateNetwork() called. Creating or updating deviceLogic for each CHE");
		// updateNetwork is called a lot. It does figure out if something needs to change..

		for (Che che : network.getChes().values()) {
			try {
				UUID id = che.getPersistentId();
				NetGuid deviceGuid = new NetGuid(che.getDeviceGuid());

				Che.ProcessMode theMode = che.getProcessMode();

				if (theMode == null)
					doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE_SETUPORDERS, che);
				else {
					switch (theMode) {
						case LINE_SCAN:
							doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE_LINESCAN, null);
							break;
						case SETUP_ORDERS:
							doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE_SETUPORDERS, che);
							break;
						default:
							LOGGER.error("unimplemented case in updateNetwork");
							continue;
					}
				}

				updateDevices.add(id);
			} catch (Exception e) {
				//error in one should not cause issues setting up others
				LOGGER.error("Unable to handle network update for che={}", che, e);
			}
		}
		for (LedController ledController : network.getLedControllers().values()) {
			try {
				UUID id = ledController.getPersistentId();
				NetGuid deviceGuid = new NetGuid(ledController.getDeviceGuid());
				if (ledController.getDeviceType() == DeviceType.Poscons) {
					doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_POS_CON_CTRL, null);
				} else {
					doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_LED, null);
				}
				updateDevices.add(id);
			} catch (Exception e) {
				//error in one should not cause issues setting up others
				LOGGER.error("Unable to handle network update for ledController={}", ledController, e);
			}
		}

		// now process deletions
		Set<UUID> deleteDevices = new HashSet<UUID>();
		for (UUID existingDevice : mDeviceMap.keys1()) {
			if (!updateDevices.contains(existingDevice)) {
				deleteDevices.add(existingDevice);
			}
		}
		for (UUID deleteUUID : deleteDevices) {
			INetworkDevice dev = mDeviceMap.get(deleteUUID);
			NetGuid netGuid = mDeviceMap.getKeys(dev).key2;
			doDeleteNetDevice(deleteUUID, netGuid);
		}
		this.lastNetworkUpdate = System.currentTimeMillis();
		LOGGER.debug("Network updated: {} active devices, {} removed", updateDevices.size(), deleteDevices.size());
	}

	public void processVerifyBadgeResponse(String networkGuid, Boolean verified, String workerNameUI) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			if (verified == null) {
				verified = false;
			}
			setWorkerNameFromGuid(cheDevice.getGuid(), workerNameUI);
			cheDevice.processResultOfVerifyBadge(verified);
		} else {
			LOGGER.warn("Unable to process Verify Badge response for CHE id={} CHE not found", networkGuid);
		}
	}

	public void processComputeWorkResponse(String networkGuid,
		Integer workInstructionCount,
		Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			cheDevice.processWorkInstructionCounts(workInstructionCount, containerToWorkInstructionCountMap);
		} else {
			LOGGER.warn("Unable to assign work count to CHE id={} CHE not found", networkGuid);
		}
	}

	public void processGetWorkResponse(String networkGuid, List<WorkInstruction> workInstructions, String message) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			cheDevice.assignWork(workInstructions, message);
		} else {
			LOGGER.warn("Unable to assign work to CHE id={} CHE not found", networkGuid);
		}
	}

	public void processSetupStateMessage(String networkGuid, HashMap<String, Integer> positionMap) {
		CheDeviceLogic cheDevice = this.getCheDeviceByControllerId(networkGuid);
		if (cheDevice == null)
			LOGGER.error("Did not find device for {} in processSetupStateMessage", networkGuid);
		else
			cheDevice.processStateSetup(positionMap);
	}

	// Works the same as processGetWorkResponse? Good
	public void processOrderDetailWorkResponse(String networkGuid, List<WorkInstruction> workInstructions, String message) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			// Although not done yet, may be useful to return information such as WI already completed, or it shorted, or ....
			LOGGER.info("processOrderDetailWorkResponse calling cheDevice.assignWork()");
			cheDevice.assignWork(workInstructions, message); // will initially use assignWork override, but probably need to add parameters.			
		} else {
			LOGGER.warn("Unable to assign work to CHE id={} CHE not found", networkGuid);
		}
	}

	// Works the same as processGetWorkResponse? Good
	public void processPutWallInstructionResponse(String networkGuid, List<WorkInstruction> workInstructions, String message) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			// Although not done yet, may be useful to return information such as WI already completed, or it shorted, or ....
			LOGGER.info("processPutWallInstructionResponse calling cheDevice.assignWallPuts");
			cheDevice.assignWallPuts(workInstructions, message); // will initially use assignWork override, but probably need to add parameters.			
		} else {
			LOGGER.warn("Device not found in processPutWallInstructionResponse. CHE id={}", networkGuid);
		}
	}
	
	public void processTapeLocationDecodingResponse(String networkGuid, String decodedLocation) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			if (cheDevice instanceof SetupOrdersDeviceLogic){
				((SetupOrdersDeviceLogic)cheDevice).setLocationId(decodedLocation);
				if (cheDevice.getCheStateEnum() == CheStateEnum.SETUP_SUMMARY) {
					LOGGER.info("Tape decoding {} received and saved, refreshing SETUP_SUMMARY. CHE id={}", decodedLocation, networkGuid);
					cheDevice.setState(CheStateEnum.SETUP_SUMMARY);
				} else {
					LOGGER.info("Tape decoding {} received and saved, but device is no longer in SETUP_SUMMARY. CHE id={}", decodedLocation, networkGuid);
				}
			} else {
				LOGGER.warn("Device is not SetupOrdersDeviceLogic in processTapeLocationDecodingResponse. CHE id={}", networkGuid);
			}
		} else {
			LOGGER.warn("Device not found in processPutWallInstructionResponse. CHE id={}", networkGuid);
		}
	}

	/** Two key actions from the associate response
	 * 1) Immediately, in advance of networkUpdate that may come, modify and maintain the association map in the cheDeviceLogic
	 * 2) Update local variables in the cheDeviceLogic so that the immediate screen draw looks right.
	 */
	public void processCheLinkResponse(String networkGuid, String thisCheName, String linkedCheGuidId, String linkedCheName) {
		LOGGER.info("site controller processCheLinkResponse for guid:{} associate to:{}", networkGuid, linkedCheGuidId);
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {

			//Edge case 1. If directly changing association from one cart to another, we need to reset the prior cart.
			CheDeviceLogic priorLinkedDevice = cheDevice.getLinkedCheDevice();
			if (priorLinkedDevice != null) {
				LOGGER.info("processCheLinkResponse: {} was linked to {}", networkGuid, priorLinkedDevice.getGuidNoPrefix());
			}

			NetGuid associateGuid = null;
			if (linkedCheGuidId != null) {
				CheDeviceLogic linkedDevice = getCheDeviceFromPrefixHexString("0x" + linkedCheGuidId);
				// Only allow this if we have it in our device map as a che
				if (linkedDevice == null) {
					LOGGER.error("processCheLinkResponse did not find valid che device for {}", linkedCheGuidId);
					associateGuid = null;
				} else {
					associateGuid = linkedDevice.getGuid();
					//Edge case 2. If the linked CHE is itself remote to another cheDevice, let's detect that set to a consistent state.
					CheDeviceLogic chainLinkedDevice = linkedDevice.getLinkedCheDevice();
					if (chainLinkedDevice != null) {
						LOGGER.warn("breaking link between {} and {} because making new link to {}",
							associateGuid.getHexStringNoPrefix(),
							chainLinkedDevice.getGuidNoPrefix(),
							associateGuid.getHexStringNoPrefix());
						// TODO
						chainLinkedDevice.processUnLinkLocalVariables(associateGuid.getHexStringNoPrefix());
					}
					// Edge case 3. We are linking to linkedCheGuidId/linkedDevice. Is another CHE controlling it now?
					NetGuid otherMobileGuid = linkedDevice.getLinkedFromCheGuid();
					if (otherMobileGuid != null) {
						CheDeviceLogic otherMobileChe = this.getCheDeviceByNetGuid(otherMobileGuid);
						if (otherMobileChe != null) {
							otherMobileChe.forceFromLinkedState(CheStateEnum.REMOTE);
						}
					}
				}
			}
			// perhaps more direct to compute the guid from "0x" + networkGuid, but we did that above and found this device
			this.maintainDeviceData(cheDevice.getGuid(), thisCheName, associateGuid, linkedCheName);

			//Edge case 2. If the linked CHE was itself remote to another cheDevice, its state is wrong. Correct it.
			if (associateGuid != null) {
				CheDeviceLogic assocDevice = this.getCheDeviceByNetGuid(associateGuid);
				if (assocDevice != null && assocDevice.getCheStateEnum().equals(CheStateEnum.REMOTE_LINKED)) {
					assocDevice.forceFromLinkedState(CheStateEnum.REMOTE);
				}
			}

			LOGGER.info("processCheLinkResponse calling cheDevice.maintainLink");
			cheDevice.maintainLink(linkedCheName);

			// Edge case 1. If directly changing association from one cart to another, we need to reset the prior cart.
			if (priorLinkedDevice != null && !priorLinkedDevice.getGuid().equals(associateGuid)) {
				LOGGER.info("breaking link to {} because making new link to {}",
					priorLinkedDevice.getGuidNoPrefix(),
					associateGuid.getHexStringNoPrefix());
				// TODO
				priorLinkedDevice.processUnLinkLocalVariables(cheDevice.getGuidNoPrefix());
			}

		} else {
			LOGGER.error("Device not found in processCheLinkResponse. CHE id={}", networkGuid);
		}
	}

	public void processFailureResponse(FailureResponse failure) {
		String cheGuidStr = failure.getCheId();
		if (cheGuidStr != null) {
			UUID cheGuid = UUID.fromString(cheGuidStr);
			CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheGuid);
			if (cheDevice != null) {
				String message = failure.getStatusMessage();
				cheDevice.sendDisplayCommand("Server Error", message == null ? "" : message);
			} else {
				LOGGER.warn("Unable to process failure response for CHE id={} CHE not found", cheGuid);
			}
		}
	}

	public void processDisplayCheMessage(NetGuid cheId, String line1, String line2, String line3, String line4) {
		CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheId);
		if (cheDevice != null) {
			LOGGER.info("processDisplayCheMessage calling cheDevice.sendDisplayCommand()");
			cheDevice.sendDisplayCommand(line1, line2, line3, line4);
		} else {
			LOGGER.warn("Unable to processDisplayCheMessage for CHE id={} CHE not found", cheId);
		}
	}

	public PosManagerDeviceLogic processPosConControllerMessage(PosControllerInstr instruction, boolean skipUpdate) {
		NetGuid controllerGuid = new NetGuid(instruction.getControllerId());
		String sourceStr = instruction.getSourceId();
		NetGuid sourceGuid = (sourceStr == null) ? controllerGuid : new NetGuid(sourceStr);
		PosManagerDeviceLogic device = (PosManagerDeviceLogic) mDeviceMap.get(controllerGuid);
		if (device != null) {
			LOGGER.info("processPosConControllerMessage calling display function");
			if (instruction.isRemoveAll()) {
				device.removePosConInstrsForSource(sourceGuid);
			} else if (!instruction.getRemovePos().isEmpty()) {
				device.removePosConInstrsForSourceAndPositions(sourceGuid, instruction.getRemovePos());
			} else {
				device.addPosConInstrFor(sourceGuid, instruction);
			}
			if (!skipUpdate) {
				device.updatePosCons();
			}
		} else {
			LOGGER.warn("Unable to assign work to PosCon controller id={}. Device not found", controllerGuid);
		}
		return device;
	}

	public void processPosConSetupMessage(PosConSetupMessage message) {
		NetGuid controllerGuid = new NetGuid(message.getNetGuidStr());
		INetworkDevice device = mDeviceMap.get(controllerGuid);
		if (device == null) {
			LOGGER.warn("Unable to start poscon setup on device {}. Device not found", controllerGuid);
			return;
		}
		if (!(device instanceof PosConDeviceABC)) {
			LOGGER.warn("Unable to start poscon setup on device {}. Device {} is not a PosConDeviceABC", controllerGuid, device);
			return;
		}
		putPosConsInSetupMode((PosConDeviceABC) device);
	}

	/**
	 * Tell device to put all PosCons in the Setup mode
	 */
	public void putPosConsInSetupMode(PosConDeviceABC device) {
		CommandControlPosconSetup command = new CommandControlPosconSetup(NetEndpoint.PRIMARY_ENDPOINT);
		radioController.sendCommand(command, device.getAddress(), true);
	}

	public void processPosConLightAddresses(PosConLightAddressesMessage message) {
		NetGuid controllerGuid = new NetGuid(message.getNetGuidStr());
		INetworkDevice device = mDeviceMap.get(controllerGuid);
		if (device == null) {
			LOGGER.warn("Unable to light poscon addresses on device {}. Device not found", controllerGuid);
			return;
		}
		if (!(device instanceof PosConDeviceABC)) {
			LOGGER.warn("Unable to light poscon addresses on device {}. Device {} is not a PosConDeviceABC", controllerGuid, device);
			return;
		}
		CommandControlPosconBroadcast command = new CommandControlPosconBroadcast(CommandControlPosconBroadcast.POS_SHOW_ADDR,
			NetEndpoint.PRIMARY_ENDPOINT);
		radioController.sendCommand(command, device.getAddress(), true);
	}

	public void processPosConControllerListMessage(PosControllerInstrList instructionList) {
		HashSet<PosManagerDeviceLogic> controllers = new HashSet<>();
		for (PosControllerInstr instruction : instructionList.getInstructions()) {
			controllers.add(processPosConControllerMessage(instruction, true));
		}
		for (PosManagerDeviceLogic controller : controllers) {
			controller.updatePosCons();
		}
	}

	public PosManagerDeviceLogic processOrderLocationFeedbackMessage(OrderLocationFeedbackMessage instruction) {
		String controllerId = instruction.getControllerId();
		NetGuid controllerGuid = new NetGuid(instruction.getControllerId());
		PosManagerDeviceLogic device = getPosManagerDeviceByControllerId(controllerId);
		if (device != null) {
			LOGGER.info("processOrderLocationFeedbackMessage calling display function");
			device.processFeedback(instruction);
		} else {
			LOGGER.warn("Unable to assign work to PosCon controller id={}. Device not found", controllerGuid);
		}
		return device;
	}

	public void processWorkInstructionCompletedResponse(UUID workInstructionId) {
		// do nothing
	}

	public void processInventoryScanRespose(String inResponseMessage) {
		LOGGER.info("Got inventoryscan response: {}", inResponseMessage);
		// TODO - huffa DEV644
	}

	public void lightSomeLeds(final List<LightLedsInstruction> instructions) {
		for (LightLedsInstruction instruction : instructions) {
			if (!LightLedsInstruction.verifyCommandString(instruction.getLedCommands())) {
				LOGGER.error("handleOtherMessage found bad LightLedsMessage");
			} else {
				LOGGER.info("Processing LightLedsInstructions");
				NetGuid deviceGuid = new NetGuid(instruction.getNetGuidStr());
				INetworkDevice aDevice = getDeviceByGuid(deviceGuid);
				if (aDevice != null && aDevice instanceof AisleDeviceLogic) {
					((AisleDeviceLogic) aDevice).lightExtraLeds(instruction.getDurationSeconds(), instruction.getLedCommands());
				} else {
					// By design, the LedInstrListMessage broadcast to all site controllers for this facility. If this site controller does not have the mentioned device, it is an error today
					// but may not be later when we have our multi-controller implementation.
					LOGGER.debug("unknown GUID in lightSomeLeds");
				}
			}
		}
	}

	@Override
	public void capture(byte[] packet) {
		PcapRecord pcap = new PcapRecord(packet);
		try {
			this.pcapBuffer.put(pcap);
		} catch (IOException e) {
			LOGGER.error("Unexpected problem putting packet of size={} in ring buffer", packet.length, e);
		}
	}

	/**
	 * records in the mDeviceDataMap.
	 * Used in the following efficient functions to find the associated che's name and associated che's guid.
	 */
	private class CheData {
		@Getter
		@Setter
		String	cheName;					// the name of this CHE, corresponding to the guid

		@Getter
		@Setter
		NetGuid	associatedToRemoteCheGuid;
		
		@Getter
		@Setter
		String	workerNameUI;				// the ui-friendly name of the logged in worker

		// @Getter
		// @Setter
		// NetGuid	remoteCheAssociatedToThis;

		public CheData(String cheName, NetGuid associatedToCheGuid) {
			setCheName(cheName);
			setAssociatedToRemoteCheGuid(associatedToCheGuid);
		}
	}

	/**
	 * This may populate upon use. If associatedToCheGuid is not null, we know it is valid.
	 * If necessary, make the entry for it.
	 * 
	 */
	private void maintainDeviceData(NetGuid thisCheGuid, String thisCheName, NetGuid associatedToCheGuid, String associatedCheName) {
		if (associatedToCheGuid != null) {
			// Make sure it exists. Add if necessary.
			CheData assocData = mDeviceDataMap.get(associatedToCheGuid);
			if (assocData == null) {
				LOGGER.debug("adding device data element {}:{}", associatedToCheGuid, associatedCheName);
				// Note: if we are associating to another GUID, that may not be associated other.
				assocData = new CheData(associatedCheName, null);
				CheData oldAssocData = mDeviceDataMap.put(associatedToCheGuid, assocData);
				if (oldAssocData != null)
					LOGGER.error("unexpected result_1 in maintainDeviceData");
			}
			// If we did find it, still need to update to a self-consistent state.
			else {
				assocData.setAssociatedToRemoteCheGuid(null);
				assocData.setCheName(associatedCheName); // rarely necessary. The name of this che probably did not change.
			}
		}
		// Above just made sure the associated che has an entry in  mDeviceDataMap.  Guid -> cheName and null associated CHE.
		// Now set or clear the association.
		CheData thisData = mDeviceDataMap.get(thisCheGuid);
		if (thisData == null) {
			LOGGER.debug("adding device data element {}:{}", thisCheGuid, thisCheName);
			thisData = new CheData(thisCheName, null);
			CheData oldDataRecord = mDeviceDataMap.put(thisCheGuid, thisData);
			if (oldDataRecord != null)
				LOGGER.error("unexpected result_2 in maintainDeviceData");
		}
		thisData.setAssociatedToRemoteCheGuid(associatedToCheGuid);

		// Finally, as one mobile links to a CHE, if there was/were already linkage to the CHE, then clear.  This is a linear search.		
		if (associatedToCheGuid != null) {
			for (Map.Entry<NetGuid, CheData> entry : mDeviceDataMap.entrySet()) {
				NetGuid key = entry.getKey();
				CheData value = entry.getValue();
				if (associatedToCheGuid.equals(value.getAssociatedToRemoteCheGuid())) {
					if (!key.equals(thisCheGuid)) {
						LOGGER.info("Removing {} link to {}",
							key.getHexStringNoPrefix(),
							associatedToCheGuid.getHexStringNoPrefix());
						value.setAssociatedToRemoteCheGuid(null);
					}
				}
			}
		}
	}

	/**
	 * From the guid, what is the che's name
	 */
	protected String getCheNameFromGuid(NetGuid thisCheGuid) {
		CheData thisData = mDeviceDataMap.get(thisCheGuid);
		if (thisData == null) {
			return null;
		}
		return thisData.getCheName();
	}

	/**
	 * From the guid, what is the associated che's guid
	 */
	public NetGuid getLinkedCheGuidFromGuid(NetGuid thisCheGuid) {
		CheData thisData = mDeviceDataMap.get(thisCheGuid);
		if (thisData == null) {
			return null;
		}
		return thisData.getAssociatedToRemoteCheGuid();
	}

	/**
	 * From the guid, what is the associated che's name
	 */
	protected String getAssociatedCheNameFromGuid(NetGuid thisCheGuid) {
		NetGuid assocGuid = getLinkedCheGuidFromGuid(thisCheGuid);
		if (assocGuid == null) {
			return null;
		}
		CheData assocData = mDeviceDataMap.get(assocGuid);
		if (assocData == null) {
			return null;
		}
		return assocData.getCheName();
	}

	public String getWorkerNameFromGuid(NetGuid thisCheGuid) {
		CheData thisData = mDeviceDataMap.get(thisCheGuid);
		if (thisData == null) {
			return null;
		}
		String workerName = thisData.getWorkerNameUI();
		return workerName;
	}

	/**
	 * From the guid, set che worker's ui-friendly name
	 */
	public void setWorkerNameFromGuid(NetGuid thisCheGuid, String workerName) {
		CheData thisData = mDeviceDataMap.get(thisCheGuid);
		if (thisData == null) {
			thisData = new CheData(null, null);
			mDeviceDataMap.put(thisCheGuid, thisData);
		}
		thisData.setWorkerNameUI(workerName);
	}

	/**
	 * Fairly trivial function provides useful logging. Common bug is mixup of prefix or not on the hex string.
	 * This catches the the OutOfRangeException, logs, and returns null. Other exceptions are not caught.
	 */
	private NetGuid getNetGuidFromPrefixHexString(String inString) {
		NetGuid deviceGuid = null;
		try {
			deviceGuid = new NetGuid(inString);
		} catch (OutOfRangeException e) {
			LOGGER.error("could not get netGuid in getDevice", e);
		}
		return deviceGuid;
	}

	/**
	 * Common bug is mixup of prefix or not on the hex string, which will log error from OutOfRangeException and return null.
	 * Input as "0x0000008d".
	 */
	private CheDeviceLogic getCheDeviceFromPrefixHexString(String inString) {
		NetGuid theGuid = getNetGuidFromPrefixHexString(inString);
		if (theGuid == null)
			return null;
		else
			return this.getCheDeviceByNetGuid(theGuid);
	}

}
