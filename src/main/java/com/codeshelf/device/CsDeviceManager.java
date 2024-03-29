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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.InfoBehavior.InfoPackage;
import com.codeshelf.behavior.PalletizerBehavior.PalletizerInfo;
import com.codeshelf.behavior.PalletizerBehavior.PalletizerRemoveInfo;
import com.codeshelf.device.PosControllerInstr.PosConInstrGroupSerializer;
import com.codeshelf.flyweight.bitfields.OutOfRangeException;
import com.codeshelf.flyweight.command.CommandControlPosconBroadcast;
import com.codeshelf.flyweight.command.CommandControlPosconSetupStart;
import com.codeshelf.flyweight.command.ICommand;
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
import com.codeshelf.model.domain.SiteController.SiteControllerRole;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.model.domain.Che.CheLightingEnum;
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
import com.codeshelf.ws.protocol.request.InfoRequest;
import com.codeshelf.ws.protocol.request.TapeLocationDecodingRequest;
import com.codeshelf.ws.protocol.request.ComputeWorkRequest.ComputeWorkPurpose;
import com.codeshelf.ws.protocol.request.InfoRequest.InfoRequestType;
import com.codeshelf.ws.protocol.request.InventoryLightItemRequest;
import com.codeshelf.ws.protocol.request.InventoryLightLocationRequest;
import com.codeshelf.ws.protocol.request.InventoryUpdateRequest;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.request.LogoutRequest;
import com.codeshelf.ws.protocol.request.PalletizerCompleteItemRequest;
import com.codeshelf.ws.protocol.request.PalletizerItemRequest;
import com.codeshelf.ws.protocol.request.PalletizerNewOrderRequest;
import com.codeshelf.ws.protocol.request.PalletizerRemoveOrderRequest;
import com.codeshelf.ws.protocol.request.SkuWallLocationDisambiguationRequest;
import com.codeshelf.ws.protocol.request.VerifyBadgeRequest;
import com.codeshelf.ws.protocol.response.CompleteWorkInstructionResponse;
import com.codeshelf.ws.protocol.response.FailureResponse;
import com.codeshelf.ws.protocol.response.GetPutWallInstructionResponse;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.protocol.response.VerifyBadgeResponse;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
	static final String									DEVICETYPE_CHE_PALLETIZER	= "CHE_PALLETIZER";

	static final String									WI_COMPLETE_FAIL			= "WI_COMPLETE_FAIL";										

	private TwoKeyMap<UUID, NetGuid, INetworkDevice>	mDeviceMap;

	private Map<NetGuid, CheData>						mDeviceDataMap;

	private Map<String, String>							mTotalCntrVsDeviceMap;

	@Accessors(prefix = "m")
	@Getter
	private HashMap<String, PalletizerInfo>				mPalletizerCache			= new HashMap<>();

	@Getter
	private IRadioController							radioController;

	@Getter
	private PcapRingBuffer								pcapBuffer;
	
	@Getter @Setter
	private String										tenantName;

	@Getter @Setter
	private String										facilityDomainId;

	@Getter
	private String										username;
	private String										password;
	
	@Getter
	@Setter
	private Short										channel;

	/* Device Manager owns websocket configuration too */
	//	@Getter
	//	private URI											mUri;

	@Getter
	private long										lastNetworkUpdate			= 0;

	private boolean										isAttachedToServer			= false;

	private boolean										autoShortValue				= true;											// log on set (below)
	private boolean										pickMultValue				= false;											// log on set (below)
	private boolean										productionValue				= false;											// log on set (below)

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
	@Setter
	private String										ordersubValue				= "Disabled";

	@Getter
	@Setter
	private SiteControllerRole							siteControllerRole			 = SiteControllerRole.NETWORK_PRIMARY;

	@Getter
	CsClientEndpoint									clientEndpoint;
		
	@Inject
	public CsDeviceManager(final IRadioController inRadioController, final CsClientEndpoint clientEndpoint) {
		this.clientEndpoint = clientEndpoint;
		CsClientEndpoint.setEventListener(this);

		radioController = inRadioController;
		mDeviceMap = new TwoKeyMap<UUID, NetGuid, INetworkDevice>();

		mDeviceDataMap = new HashMap<NetGuid, CheData>();

		mTotalCntrVsDeviceMap = new HashMap<String, String>();

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

	public boolean getPickMultValue(CheLightingEnum	cheLightingEnum) {
		if (cheLightingEnum == CheLightingEnum.LABEL_V1){
			return false;
		}
		return this.pickMultValue;
	}

	public void setPickMultValue(boolean inValue) {
		pickMultValue = inValue;
		LOGGER.info("Site controller setting PICKMULT value = {}", inValue);
	}

	public boolean getProductionValue() {
		return this.productionValue;
	}

	public void setProductionValue(boolean inValue) {
		productionValue = inValue;
		LOGGER.info("Site controller setting PRODUCTIN value = {}", inValue);
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
			setChannel(network.getChannel());
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

	public final List<ChePalletizerDeviceLogic> getPalletizers() {
		ArrayList<ChePalletizerDeviceLogic> aList = new ArrayList<ChePalletizerDeviceLogic>();
		for (INetworkDevice theDevice : mDeviceMap.values()) {
			if (theDevice instanceof ChePalletizerDeviceLogic)
				aList.add((ChePalletizerDeviceLogic) theDevice);
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
		} else if (inNetworkDevice instanceof AisleDeviceLogic) {
			if (isAttachedToServer) {
				((AisleDeviceLogic) inNetworkDevice).connectedToServer();
			} else {
				((AisleDeviceLogic) inNetworkDevice).disconnectedFromServer();
			}
		}
	}

	// --------------------------------------------------------------------------
	/* 
	 * For DEV-1347 find in our local storage. Or find by asking each device logic
	 * Return null if not found
	 */
	private String findDeviceNameWithContainer(String inOrderCntrId) {
		return mTotalCntrVsDeviceMap.get(inOrderCntrId);
	}

	private void removeOldAndRememberNew(final String inCheId, final Map<String, String> positionToContainerMap) {
		if (inCheId == null || positionToContainerMap == null) {
			LOGGER.error("removeOldAndRememberNew had null input");
			return;
		}

		// remove old
		//   Java 8 this approach is cool!   mTotalCntrVsDeviceMap.entrySet().removeIf(e-> <boolean expression> );
		LOGGER.debug("mTotalCntrVsDeviceMap removing all values for {}", inCheId);
		for (Iterator<Map.Entry<String, String>> it = mTotalCntrVsDeviceMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String> entry = it.next();
			if (inCheId.equals(entry.getValue())) {
				it.remove();
			}
		}

		// put back new
		for (Map.Entry<String, String> entry : positionToContainerMap.entrySet()) {
			LOGGER.debug("mTotalCntrVsDeviceMap.put {}/{}", entry.getValue(), inCheId);
			mTotalCntrVsDeviceMap.put(entry.getValue(), inCheId);
		}
	}

	// --------------------------------------------------------------------------
	/* 
	 * For DEV-1347 this is where we find the device logic and tell it to remove that cntr/order from its positionToContainerMap
	 */
	private void tellDeviceRemoveCntr(final String inCheGuid, final String inCntr) {
		// The value is as "000001bf"
		CheDeviceLogic logic = this.getCheDeviceByControllerId(inCheGuid);
		if (logic == null) {
			LOGGER.error("Expected to find deviceLogic for {}", inCheGuid);
		} else {
			logic.removeStolenCntr(inCntr);
		}

	}

	// --------------------------------------------------------------------------
	/* 
	 * For DEV-1347 if another CHE puts the container on, remove from other CHE.
	 * This is the "simple" use case. Might be overly simple. What if two CHE are running the same order for picks on different routes? 
	 * If so, this implementation is wrong. (mTotalCntrVsDeviceMap says only one CHE at a time as the container.) To support that, change
	 * mTotalCntrVsDeviceMap to mTotalCntrOnRouteVsDeviceMap, keyed by Cntr and route or area.
	 */
	private void evaluateContainerConflictsAndRememberNew(final String inCheId, final Map<String, String> positionToContainerMap) {

		// 1) Iterate over the new container assignments. We care if any are on another CHE. No op necessary if on this CHE.	
		for (Map.Entry<String, String> entry : positionToContainerMap.entrySet()) {
			String deviceName = findDeviceNameWithContainer(entry.getValue());
			if (deviceName != null && !deviceName.equals(inCheId)) {
				LOGGER.warn("Removing {} from {} as we are adding it to {} at position {}",
					entry.getValue(),
					deviceName,
					inCheId,
					entry.getKey());
				// Tell the device to remove this container. And remove from our map
				tellDeviceRemoveCntr(deviceName, entry.getValue());
				mTotalCntrVsDeviceMap.remove(entry.getValue());
			}
		}

		// 2) Replace all of this CHEs entries with this map's values. Not necessary if just using the individual device logic data.
		// See findDeviceNameWithContainer() for what we are doing.
		removeOldAndRememberNew(inCheId, positionToContainerMap);
	}

	// --------------------------------------------------------------------------
	/* This is the "Computing Work" call
	 * 
	 */
	public void computeCheWork(final String inCheId,
		final UUID inPersistentId,
		final Map<String, String> positionToContainerMap,
		final Boolean reverse) {
		// DEV-1331 part 2 logging . need the guid
		LOGGER.info("COMPUTE_WORK from {}", inCheId);
		String cheId = inPersistentId.toString();

		// DEV-1347 we are sending this request to server, locking in these containers to a device.  If another device has a contaienr,
		// can we pull it off?
		evaluateContainerConflictsAndRememberNew(inCheId, positionToContainerMap);

		ComputeWorkRequest req = new ComputeWorkRequest(ComputeWorkPurpose.COMPUTING_WORK,
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
		//no longer sending worker events messages.  Using completeWorkInstruction on WorkBehavior instead
		EventType type = message.getEventType();
		if (type != WorkerEvent.EventType.COMPLETE 
				&& type != WorkerEvent.EventType.SUBSTITUTION
				&& type != WorkerEvent.EventType.SHORT) {
			LOGGER.debug("Notify: Device={}; type={}", message.getDevicePersistentId(), type);
			clientEndpoint.sendMessage(message);
		}
	}

	public void sendLogoutRequest(final String inCheId, final UUID inPersistentId, final String workerId) {
		LOGGER.debug("Logout: Che={};", inCheId);
		String cheId = inPersistentId.toString();
		LogoutRequest req = new LogoutRequest(workerId);
		req.setDeviceId(cheId);
		clientEndpoint.sendMessage(req);
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
		// DEV-1331 part 2 logging . need the guid
		LOGGER.info("GET_WORK from {}; Loc={}", inCheId, inLocationId);
		String cheId = inPersistentId.toString();
		ComputeWorkRequest req = new ComputeWorkRequest(ComputeWorkPurpose.GETTING_WORK,
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

	public void completePalletizerItem(final String inCheId,
		final UUID cheId,
		final PalletizerInfo info,
		final Boolean shorted,
		final String userId) {
		LOGGER.debug("Complete Palletizer Item: Item={}; Che={}; Short={}; UserId={}", info.getItem(), inCheId, shorted, userId);
		PalletizerCompleteItemRequest req = new PalletizerCompleteItemRequest(cheId.toString(), info, shorted, userId);
		clientEndpoint.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.CsDeviceManager#inventoryScan(final UUID inCheId, final UUID inPersistentId, final String inLocationId, final String inGtin)
	 */
	public void inventoryUpdateScan(final UUID inPersistentId,
		final String inLocationId,
		final String inGtin,
		final String skuWallName) {
		LOGGER.debug("Inventory update Scan: Che={}; Loc={}; GTIN={};", inPersistentId, inLocationId, inGtin);
		InventoryUpdateRequest req = new InventoryUpdateRequest(inPersistentId.toString(), inGtin, inLocationId, skuWallName);
		clientEndpoint.sendMessage(req);
	}

	public void skuWallLocationDisambiguation(final UUID inPersistentId,
		final String inLocationId,
		final String inGtin,
		final String skuWallName) {
		LOGGER.debug("Inventory update Scan: Che={}; Loc={}; GTIN={};", inPersistentId, inLocationId, inGtin);
		SkuWallLocationDisambiguationRequest req = new SkuWallLocationDisambiguationRequest(inPersistentId.toString(),
			inGtin,
			inLocationId,
			skuWallName);
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

	public void performInfoOrRemoveAction(InfoRequestType actionType, String location, String cheGuid, String chePersistentId) {
		performInfoOrRemoveAction(actionType, location, cheGuid, chePersistentId, null);
	}

	public void performInfoOrRemoveAction(InfoRequestType actionType,
		String location,
		String cheGuid,
		String chePersistentId,
		UUID removeItemId) {
		LOGGER.debug("sendInfoOrRemoveCommand: Che={};  Type ={}; Location={};", cheGuid, actionType, location);
		InfoRequest req = new InfoRequest(actionType, chePersistentId, location, removeItemId);
		clientEndpoint.sendMessage(req);
	}

	public void palletizerItemRequest(String cheGuid, String chePersistentId, String item, String userId) {
		LOGGER.debug("palletizerItemRequest: Che={};  Item={}; UserId={}", cheGuid, item, userId);
		PalletizerItemRequest req = new PalletizerItemRequest(chePersistentId, item, userId);
		clientEndpoint.sendMessage(req);
	}

	public void palletizerNewOrderRequest(String cheGuid, String chePersistentId, String item, String location, String userId) {
		LOGGER.debug("palletizerNewOrderRequest: Che={}, Item={}, Location={}, UserId={}", cheGuid, item, location, userId);
		PalletizerNewOrderRequest req = new PalletizerNewOrderRequest(chePersistentId, item, location, userId);
		clientEndpoint.sendMessage(req);
	}

	public void palletizerRemoveOrderRequest(String cheGuid, String chePersistentId, String prefix, String scan) {
		LOGGER.debug("palletizerRemoveOrderRequest: Che={}, Prefix={}, Scan={}", cheGuid, prefix, scan);
		PalletizerRemoveOrderRequest req = new PalletizerRemoveOrderRequest(chePersistentId, prefix, scan);
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
		clientEndpoint.sendMessage(loginRequest);
	}

	/**
	 * After connection and authentication this is received to indicate communication for devices is established
	 * @see #connected()
	 */
	public void attached(CodeshelfNetwork network) {
		LOGGER.info("Attached to server");
		this.updateNetwork(network);
		if (getSiteControllerRole() == SiteControllerRole.NETWORK_PRIMARY){
			this.startRadio(network);
		} else {
			LOGGER.warn("Site Controller " + getUsername() + " is " + getSiteControllerRole() + ". Skipping startRadio() call");
		}

		isAttachedToServer = true;
		for (INetworkDevice networkDevice : mDeviceMap.values()) {
			if (networkDevice instanceof CheDeviceLogic) {
				((CheDeviceLogic) networkDevice).connectedToServer();
			}
			if (networkDevice instanceof PosManagerDeviceLogic) {
				((PosManagerDeviceLogic) networkDevice).connectedToServer();
			}
			if (networkDevice instanceof AisleDeviceLogic) {
				((AisleDeviceLogic) networkDevice).connectedToServer();
			}
		}
	}

	public void unattached() {
		if (!isAttachedToServer)
			return; // don't get stuck in a loop if device manager is requesting disconnection

		LOGGER.warn("Unattached from server. Sending out 'Server unavailable; please wait...' messages");
		isAttachedToServer = false;
		for (INetworkDevice networkDevice : mDeviceMap.values()) {
			if (networkDevice instanceof CheDeviceLogic) {
				((CheDeviceLogic) networkDevice).disconnectedFromServer();
			}
			if (networkDevice instanceof PosManagerDeviceLogic) {
				((PosManagerDeviceLogic) networkDevice).disconnectedFromServer();
			}
			if (networkDevice instanceof AisleDeviceLogic) {
				((AisleDeviceLogic) networkDevice).disconnectedFromServer();
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
		LOGGER.warn("Disconnected from server");
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
				netDevice = new LineScanDeviceLogic(persistentId, deviceGuid, this, radioController, che);
			} else if (deviceType.equals(DEVICETYPE_CHE_PALLETIZER)) {
				netDevice = new ChePalletizerDeviceLogic(persistentId, deviceGuid, this, radioController, che);
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
					netDevice = new LineScanDeviceLogic(persistentId, deviceGuid, this, radioController, che);
				} else if (deviceType.equals(DEVICETYPE_CHE_PALLETIZER)) {
					netDevice = new ChePalletizerDeviceLogic(persistentId, deviceGuid, this, radioController, che);
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

				// TODO! Update scanner type on the fly!
				//TODO if associated to che guid does not match what we have, we need to have the device align itself.
			} else if (che != null && netDevice.needUpdateCheDetails(deviceGuid, che)) {
				LOGGER.debug("Update deviceType={}; guid={};", deviceType, deviceGuid);
				netDevice.updateCheDetails(deviceGuid, che);
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
		
		if (getSiteControllerRole() != SiteControllerRole.NETWORK_PRIMARY){
			this.lastNetworkUpdate = System.currentTimeMillis();
			LOGGER.warn("Site Controller " + getUsername() + " is " + getSiteControllerRole() + ". Skipping updateNetwork() call");
			return;
		}
		
		Set<UUID> updateDevices = new HashSet<UUID>();
		// update network devices
		LOGGER.info("updateNetwork() called. Creating or updating deviceLogic for each CHE");
		// updateNetwork is called a lot. It does figure out if something needs to change..

		for (Che che : network.getChes().values()) {
			LOGGER.info("Processing CHE " + che.getDeviceGuidStrNoPrefix());
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
						case PALLETIZER:
							doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE_PALLETIZER, che);
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

	public void processVerifyBadgeResponse(VerifyBadgeResponse response) {
		String cheGuid = response.getNetworkGuid();
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + cheGuid);
		Boolean verified = response.getVerified();
		if (cheDevice != null) {
			if (verified == null) {
				verified = false;
			}
			if (cheDevice.getCheLightingEnum() == CheLightingEnum.NOLIGHTING){
				LOGGER.warn("CHE " + cheDevice.getMyGuidStr() + " has lighting mode NOLIGHTING. Will behave as if it was POSCON");
			}
			setWorkerNameFromGuid(cheDevice.getGuid(), response.getWorkerNameUI());
			cheDevice.processResultOfVerifyBadge(verified, response.getWorkerId());
			setCheNameFromGuid(new NetGuid(cheGuid), response.getCheName());
		} else {
			LOGGER.warn("Unable to process Verify Badge response for CHE id={} CHE not found", cheGuid);
		}
	}

	public void processComputeWorkResponse(String networkGuid,
		Integer workInstructionCount,
		Map<String, WorkInstructionCount> containerToWorkInstructionCountMap,
		boolean isReplenishRun) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			LOGGER.info("accepting server's work instruction counts, but not the wis, into device {}", cheDevice.getGuidNoPrefix());
			cheDevice.setReplenishRun(isReplenishRun);
			cheDevice.processWorkInstructionCounts(workInstructionCount, containerToWorkInstructionCountMap);
		} else {
			LOGGER.warn("Unable to assign work count to CHE id={} CHE not found", networkGuid);
		}
	}

	public void processGetWorkResponse(String networkGuid, List<WorkInstruction> workInstructions, String message) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			LOGGER.info("accepting server's work instructions into device {}", cheDevice.getGuidNoPrefix());
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
	public void processPutWallInstructionResponse(GetPutWallInstructionResponse wallResponse) {
		String networkGuid = wallResponse.getNetworkGuid();
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			// Although not done yet, may be useful to return information such as WI already completed, or it shorted, or ....
			LOGGER.info("processPutWallInstructionResponse calling cheDevice.assignWallPuts");
			cheDevice.assignWallPuts(wallResponse.getWorkInstructions(), wallResponse.getWallType(), wallResponse.getWallName()); // will initially use assignWork override, but probably need to add parameters.			
		} else {
			LOGGER.warn("Device not found in processPutWallInstructionResponse. CHE id={}", networkGuid);
		}
	}

	public void processTapeLocationDecodingResponse(String networkGuid, String decodedLocation) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			if (cheDevice instanceof SetupOrdersDeviceLogic) {
				((SetupOrdersDeviceLogic) cheDevice).setLocationId(decodedLocation);
				if (cheDevice.getCheStateEnum() == CheStateEnum.SETUP_SUMMARY) {
					LOGGER.info("Tape decoding {} received and saved, refreshing SETUP_SUMMARY. CHE id={}",
						decodedLocation,
						networkGuid);
					cheDevice.setState(CheStateEnum.SETUP_SUMMARY);
				} else {
					LOGGER.info("Tape decoding {} received and saved, but device is no longer in SETUP_SUMMARY. CHE id={}",
						decodedLocation,
						networkGuid);
				}
			} else {
				LOGGER.warn("Device is not SetupOrdersDeviceLogic in processTapeLocationDecodingResponse. CHE id={}", networkGuid);
			}
		} else {
			LOGGER.warn("Device not found in processPutWallInstructionResponse. CHE id={}", networkGuid);
		}
	}

	public void processInfoResponse(String networkGuid, InfoPackage info) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			if (cheDevice instanceof SetupOrdersDeviceLogic) {
				if (info == null) {
					LOGGER.info("INFO request returned with null info. This is normal. Thus, do not change CHE display");
				} else {
					((SetupOrdersDeviceLogic) cheDevice).setInfo(info);
					cheDevice.setState(CheStateEnum.INFO_DISPLAY);
				}
			} else {
				LOGGER.warn("Device is not SetupOrdersDeviceLogic in processInfoResponse. CHE id={}", networkGuid);
			}
		} else {
			LOGGER.warn("Device not found in processInfoResponse. CHE id={}", networkGuid);
		}
	}

	public void processPalletizerItemResponse(String networkGuid, PalletizerInfo info) {
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			if (cheDevice instanceof ChePalletizerDeviceLogic) {
				// Not symmetrical. Could add to cache here, but it is done in the cheDevice
				((ChePalletizerDeviceLogic) cheDevice).processItemResponse(info);
			} else {
				LOGGER.warn("Device is not ChePalletizerDeviceLogic in processInfoResponse. CHE id={}", networkGuid);
			}
		} else {
			LOGGER.warn("Device not found in processPalletizerItemResponse. CHE id={}", networkGuid);
		}
	}

	public void processPalletizerRemoveResponse(String networkGuid, PalletizerRemoveInfo info) {
		synchronized (getPalletizerCache()) {
			for (String order : info.getOrders()){
				// could log the remove from cache here.
				getPalletizerCache().remove(order);
			}
		}
		
		CheDeviceLogic cheDevice = getCheDeviceFromPrefixHexString("0x" + networkGuid);
		if (cheDevice != null) {
			if (cheDevice instanceof ChePalletizerDeviceLogic) {
				((ChePalletizerDeviceLogic) cheDevice).processRemoveResponse(info.getError());
			} else {
				LOGGER.warn("Device is not ChePalletizerDeviceLogic in processRemoveResponse. CHE id={}", networkGuid);
			}
		} else {
			LOGGER.warn("Device not found in processPalletizerRemoveResponse. CHE id={}", networkGuid);
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
		CommandControlPosconSetupStart command = new CommandControlPosconSetupStart(NetEndpoint.PRIMARY_ENDPOINT);
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
		ICommand command = new CommandControlPosconBroadcast(NetEndpoint.PRIMARY_ENDPOINT, CommandControlPosconBroadcast.POSCON_DSP_ADDRESS);
		device.sendRadioControllerCommand(command, true);
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

	public void processWorkInstructionCompletedResponse(CompleteWorkInstructionResponse response) {
		if (response.getStatus() == ResponseStatus.Success) {
		// do nothing
		}
		else {
			// DEV-1331 v26 improvement. Force the CHE back to compute so we do not get a whole string of bad ones.
			NetGuid logicsGuid = new NetGuid(response.getNetworkGuid());
			CheDeviceLogic logic = this.getCheDeviceByNetGuid(logicsGuid);
			// A kludge. Not removing. But trigger the need to START and recompute again just like after a stolen container.
			logic.removeStolenCntr(WI_COMPLETE_FAIL);
		}
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
	public void setWorkerNameFromGuid(NetGuid cheGuid, String workerName) {
		CheData cheData = getOrCreateCheData(cheGuid);
		cheData.setWorkerNameUI(workerName);
	}

	public void setCheNameFromGuid(NetGuid cheGuid, String cheName) {
		CheData cheData = getOrCreateCheData(cheGuid);
		cheData.setCheName(cheName);
	}

	private CheData getOrCreateCheData(NetGuid cheGuid) {
		CheData cheData = mDeviceDataMap.get(cheGuid);
		if (cheData == null) {
			cheData = new CheData(null, null);
			mDeviceDataMap.put(cheGuid, cheData);
		}
		return cheData;
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

	/**
	 * This function determines if any CHEs other that @sourceChe are using Poscons needed by @requestedWi
	 */
	protected String getPosconHolders(NetGuid sourceChe, String requestedPosconStream) {
		StringBuilder posconHolders = new StringBuilder();
		if (requestedPosconStream == null || requestedPosconStream.isEmpty()) {
			return null;
		}
		List<PosControllerInstr> requestedPosconInstructions = PosConInstrGroupSerializer.deserializePosConInstrString(requestedPosconStream);
		if (requestedPosconInstructions.isEmpty()) {
			return null;
		}
		List<String> requestedPoscons = Lists.newArrayList();
		for (PosControllerInstr requestedPosconInstruction : requestedPosconInstructions) {
			String requestedPoscon = posconKeygen(requestedPosconInstruction.getControllerId(),
				requestedPosconInstruction.getPosition());
			requestedPoscons.add(requestedPoscon);
		}
		for (INetworkDevice networkDevice : mDeviceMap.values()) {
			if (networkDevice instanceof CheDeviceLogic) {
				CheDeviceLogic che = (CheDeviceLogic) networkDevice;
				if (sourceChe.equals(che.getGuid())) {
					continue;
				}
				List<WorkInstruction> wisOnChe = che.getActivePickWiList();
				for (WorkInstruction wi : wisOnChe) {
					String posconStream = wi.getPosConCmdStream();
					if (posconStream == null || posconStream.isEmpty()) {
						continue;
					}
					List<PosControllerInstr> posconInstructions = PosConInstrGroupSerializer.deserializePosConInstrString(posconStream);
					for (PosControllerInstr posconInstruction : posconInstructions) {
						String usedPoscon = posconKeygen(posconInstruction.getControllerId(), posconInstruction.getPosition());
						if (requestedPoscons.contains(usedPoscon)) {
							String cheDescription = getCheDescription(che);
							posconHolders.append(che.getUserId()).append(" on ").append(cheDescription).append(", ");
						}
					}
				}
			}
		}
		int len = posconHolders.length();
		if (len == 0) {
			return null;
		} else {
			return posconHolders.substring(0, len - 2);
		}
	}

	private String getCheDescription(CheDeviceLogic che) {
		CheData cheData = mDeviceDataMap.get(che.getGuid());
		if (cheData != null) {
			String cheName = cheData.getCheName();
			if (cheName != null) {
				return cheName;
			}
		}
		return che.getMyGuidStr();
	}

	private String posconKeygen(String controllerId, int posConIndex) {
		return controllerId + "-" + posConIndex;
	}

}
