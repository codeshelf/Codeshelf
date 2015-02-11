/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CsDeviceManager.java,v 1.19 2013/07/20 00:54:49 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.websocket.WebSocketContainer;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;
import com.codeshelf.flyweight.controller.PacketCaptureListener;
import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.IConfiguration;
import com.codeshelf.util.PcapRecord;
import com.codeshelf.util.PcapRingBuffer;
import com.codeshelf.util.ThreadUtils;
import com.codeshelf.util.TwoKeyMap;
import com.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.codeshelf.ws.jetty.client.WebSocketEventListener;
import com.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.codeshelf.ws.jetty.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.jetty.protocol.request.ComputeWorkRequest;
import com.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.codeshelf.ws.jetty.protocol.response.FailureResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class CsDeviceManager implements
	ICsDeviceManager,
	IRadioControllerEventListener,
	WebSocketEventListener,
	PacketCaptureListener {

	private static final Logger							LOGGER						= LoggerFactory.getLogger(CsDeviceManager.class);

	static final String									DEVICETYPE_CHE				= "CHE";
	static final String									DEVICETYPE_LED				= "LED Controller";
	static final String									DEVICETYPE_CHE_SETUPORDERS	= "CHE_SETUPORDERS";
	static final String									DEVICETYPE_CHE_LINESCAN		= "CHE_LINESCAN";

	private TwoKeyMap<UUID, NetGuid, INetworkDevice>	mDeviceMap;

	@Getter
	private IRadioController							radioController;

	@Getter
	private PcapRingBuffer								pcapBuffer;

	private String										username;
	private String										password;

	/* Device Manager owns websocket configuration too */
	@Getter
	private URI											mUri;

	@Getter
	@Setter
	boolean												suppressKeepAlive			= false;

	@Getter
	@Setter
	boolean												idleKill					= false;

	@Getter
	private JettyWebSocketClient						client;

	private ConnectionManagerThread						connectionManagerThread;

	@Getter
	private long										lastNetworkUpdate			= 0;

	private boolean										isAttachedToServer			= false;

	@Getter
	@Setter
	boolean												radioEnabled				= true;

	private boolean										autoShortValue				= true;											// getter needed to be in the interface. Cannot use lomboc getter. Want to log on the set

	@Getter
	@Setter
	private String										pickInfoValue				= "SKU";

	@Getter
	@Setter
	private String										containerTypeValue			= "Order";

	private WebSocketContainer							webSocketContainer;

	@Inject
	public CsDeviceManager(final IRadioController inRadioController,
		final IConfiguration configuration,
		final WebSocketContainer inWebSocketContainer) {
		// fetch properties from config file
		radioEnabled = configuration.getBoolean("radio.enabled", true);
		mUri = URI.create(configuration.getString("websocket.uri"));
		suppressKeepAlive = configuration.getBoolean("websocket.idle.suppresskeepalive", false);
		idleKill = configuration.getBoolean("websocket.idle.kill", false);

		this.webSocketContainer = inWebSocketContainer;

		radioController = inRadioController;
		mDeviceMap = new TwoKeyMap<UUID, NetGuid, INetworkDevice>();

		username = configuration.getString("websocket.username");
		password = configuration.getString("websocket.password");

		if (configuration.getBoolean("pcapbuffer.enable", false)) {
			// set up ring buffer
			int pcSize = configuration.getInt("pcapbuffer.size", PcapRingBuffer.DEFAULT_SIZE);
			int pcSlack = configuration.getInt("pcapbuffer.slack", PcapRingBuffer.DEFAULT_SLACK);
			this.pcapBuffer = new PcapRingBuffer(pcSize, pcSlack);

			// listen for packets
			radioController.getGatewayInterface().setPacketListener(this);
		}
	}

	@Override
	public final void start() {
		startWebSocketClient();

	}

	@Override
	public boolean getAutoShortValue() {
		return autoShortValue;
	}

	public void setAutoShortValue(boolean inValue) {
		autoShortValue = inValue;
		LOGGER.info("Site controller setting AUTOSHRT value = {}", inValue);
	}

	private final void startRadio(CodeshelfNetwork network) {
		if (radioController.isRunning()) {
			LOGGER.warn("Radio controller is already running, cannot start again");
		} else if (this.radioEnabled) {
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

	@Override
	public final List<AisleDeviceLogic> getAisleControllers() {
		ArrayList<AisleDeviceLogic> aList = new ArrayList<AisleDeviceLogic>();
		for (INetworkDevice theDevice : mDeviceMap.values()) {
			if (theDevice instanceof AisleDeviceLogic)
				aList.add((AisleDeviceLogic) theDevice);
		}
		return aList;
	}

	@Override
	public final List<CheDeviceLogic> getCheControllers() {
		ArrayList<CheDeviceLogic> aList = new ArrayList<CheDeviceLogic>();
		for (INetworkDevice theDevice : mDeviceMap.values()) {
			if (theDevice instanceof CheDeviceLogic)
				aList.add((CheDeviceLogic) theDevice);
		}
		return aList;
	}

	public final void startWebSocketClient() {
		// create response processor and register it with WS client
		SiteControllerMessageProcessor responseProcessor = new SiteControllerMessageProcessor(this);
		client = new JettyWebSocketClient(webSocketContainer, mUri, responseProcessor, this);
		responseProcessor.setWebClient(client);
		connectionManagerThread = new ConnectionManagerThread(this);
		connectionManagerThread.start();
	}

	@Override
	public final void stop() {
		radioController.stopController();
		connectionManagerThread.setExit(true);
		while (connectionManagerThread.isAlive()) {
			LOGGER.debug("Waiting for connection manager thread to exit...");
			ThreadUtils.sleep(2000);
		}
		try {
			client.disconnect();
		} catch (IOException e) {
			LOGGER.error("Failed to disconnect from server", e);
		}
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.ICsDeviceManager#getDeviceByGuid(com.codeshelf.flyweight.command.NetGuid)
	 */
	@Override
	public final INetworkDevice getDeviceByGuid(NetGuid inGuid) {
		return mDeviceMap.get(inGuid);
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
		}

	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.ICsDeviceManager#requestCheWork(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public final void computeCheWork(final String inCheId, final UUID inPersistentId, final List<String> inContainerIdList) {
		LOGGER.debug("Compute work: Che={}; Container={}", inCheId, inContainerIdList);
		String cheId = inPersistentId.toString();
		LinkedList<String> containerIds = new LinkedList<String>();
		for (String containerId : inContainerIdList) {
			containerIds.add(containerId);
		}
		ComputeWorkRequest req = new ComputeWorkRequest(cheId, containerIds);
		client.sendMessage(req);
	}

	@Override
	public final void computeCheWork(final String inCheId, final UUID inPersistentId, final String orderDetailId) {
		LOGGER.debug("Compute work: Che={}; DetailId={}", inCheId, orderDetailId);
		String cheId = inPersistentId.toString();
		ComputeDetailWorkRequest req = new ComputeDetailWorkRequest(cheId, orderDetailId);
		client.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.ICsDeviceManager#requestCheWork(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public final void getCheWork(final String inCheId, final UUID inPersistentId, final String inLocationId) {
		LOGGER.debug("Get work: Che={}; Loc={}", inCheId, inLocationId);
		String cheId = inPersistentId.toString();
		GetWorkRequest req = new GetWorkRequest(cheId, inLocationId);
		client.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.device.ICsDeviceManager#completeWi(java.lang.String, java.util.UUID, com.codeshelf.model.domain.WorkInstruction)
	 */
	@Override
	public final void completeWi(final String inCheId, final UUID inPersistentId, final WorkInstruction inWorkInstruction) {
		LOGGER.debug("Complete: Che={}; WI={};", inCheId, inWorkInstruction);
		CompleteWorkInstructionRequest req = new CompleteWorkInstructionRequest(inPersistentId.toString(), inWorkInstruction);
		client.sendMessage(req);
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
		client.sendMessage(loginRequest);
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
		}
	}

	public void unattached() {
		LOGGER.info("Unattached from server");
		isAttachedToServer = false;
		for (INetworkDevice networkDevice : mDeviceMap.values()) {
			if (networkDevice instanceof CheDeviceLogic) {
				((CheDeviceLogic) networkDevice).disconnectedFromServer();
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

	private void doCreateUpdateNetDevice(UUID persistentId, NetGuid deviceGuid, String deviceType) {
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
				netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController);
			} else if (deviceType.equals(DEVICETYPE_CHE_SETUPORDERS)) {
				netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController);
			} else if (deviceType.equals(DEVICETYPE_CHE_LINESCAN)) {
				netDevice = new LineScanDeviceLogic(persistentId, deviceGuid, this, radioController);
			} else if (deviceType.equals(DEVICETYPE_LED)) {
				netDevice = new AisleDeviceLogic(persistentId, deviceGuid, this, radioController);
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
					netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController);
				} else if (deviceType.equals(DEVICETYPE_CHE_SETUPORDERS)) {
					netDevice = new SetupOrdersDeviceLogic(persistentId, deviceGuid, this, radioController);
				} else if (deviceType.equals(DEVICETYPE_CHE_LINESCAN)) {
					netDevice = new LineScanDeviceLogic(persistentId, deviceGuid, this, radioController);
				} else if (deviceType.equals(DEVICETYPE_LED)) {
					netDevice = new AisleDeviceLogic(persistentId, deviceGuid, this, radioController);
				} else {
					LOGGER.error("Cannot update existing network device of unrecognized type={}", deviceType);
					suppressMapUpdate = true;
				}
				if (!suppressMapUpdate) {
					radioController.addNetworkDevice(netDevice);
				}
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
		doCreateUpdateNetDevice(persistentId, deviceGuid, newProcessType);
		INetworkDevice newDevice = mDeviceMap.get(persistentId);
		return newDevice;
	}

	public void updateNetwork(CodeshelfNetwork network) {
		Set<UUID> updateDevices = new HashSet<UUID>();
		// update network devices
		LOGGER.info("updateNetwork() called. Creating or updating deviceLogic for each CHE");
		for (Che che : network.getChes().values()) {
			try {
				UUID id = che.getPersistentId();
				NetGuid deviceGuid = new NetGuid(che.getDeviceGuid());

				Che.ProcessMode theMode = che.getProcessMode();
				if (theMode == null)
					doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE_SETUPORDERS);
				else {
					switch (theMode) {
						case LINE_SCAN:
							doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE_LINESCAN);
							break;
						case SETUP_ORDERS:
							doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE_SETUPORDERS);
							break;
						default:
							LOGGER.error("unimplemented case in updateNetwork");
							continue;
					}
				}
				// doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE); // comment this in favor of block above

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
				doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_LED);
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

	public void processComputeWorkResponse(String networkGuid,
		Integer workInstructionCount,
		Map<String, WorkInstructionCount> containerToWorkInstructionCountMap) {
		NetGuid cheId = new NetGuid("0x" + networkGuid);
		CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheId);
		if (cheDevice != null) {
			cheDevice.processWorkInstructionCounts(workInstructionCount, containerToWorkInstructionCountMap);
		} else {
			LOGGER.warn("Unable to assign work count to CHE id={} CHE not found", cheId);
		}
	}

	public void processGetWorkResponse(String networkGuid, List<WorkInstruction> workInstructions, String message) {
		NetGuid cheId = new NetGuid("0x" + networkGuid);
		CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheId);
		if (cheDevice != null) {
			cheDevice.assignWork(workInstructions, message);
		} else {
			LOGGER.warn("Unable to assign work to CHE id={} CHE not found", cheId);
		}
	}

	public void processOrderDetailWorkResponse(String networkGuid, List<WorkInstruction> workInstructions, String message) {
		NetGuid cheId = new NetGuid("0x" + networkGuid);
		CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheId);
		if (cheDevice != null) {
			// Although not done yet, may be useful to return information such as WI already completed, or it shorted, or ....
			LOGGER.info("processOrderDetailWorkResponse calling cheDevie.assignWork()");
			cheDevice.assignWork(workInstructions, message); // will initially use assignWork override, but probably need to add parameters.			
		} else {
			LOGGER.warn("Unable to assign work to CHE id={} CHE not found", cheId);
		}
	}
	
	public void processFailureResponse(FailureResponse failure) {
		String cheGuidStr = failure.getCheId();
		if (cheGuidStr != null) {
			UUID cheGuid = UUID.fromString(cheGuidStr);
			CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheGuid);
			if (cheDevice != null) {
				String message = failure.getStatusMessage();
				cheDevice.sendDisplayCommand("Server Error", message==null?"":message);
			} else {
				LOGGER.warn("Unable to process failure response for CHE id={} CHE not found", cheGuid);
			}
		}
	}

	public void processWorkInstructionCompletedResponse(UUID workInstructionId) {
		// do nothing
	}

	public void lightSomeLeds(NetGuid inGuid, int inSeconds, String inCommands) {
		INetworkDevice aDevice = getDeviceByGuid(inGuid);
		if (aDevice != null && aDevice instanceof AisleDeviceLogic) {
			((AisleDeviceLogic) aDevice).lightExtraLeds(inSeconds, inCommands);
		} else {
			// By design, the LightLedsMessage broadcast to all site controllers for this facility. If this site controller does not have the mentioned device, it is an error today
			// but may not be later when we have our multi-controller implementation.
			LOGGER.debug("unknown GUID in lightSomeLeds");
		}
	}

	@Override
	public JettyWebSocketClient getWebSocketCient() {
		return this.client;
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

}