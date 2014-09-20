/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: CsDeviceManager.java,v 1.19 2013/07/20 00:54:49 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.PropertyUtils;
import com.gadgetworks.codeshelf.util.TwoKeyMap;
import com.gadgetworks.codeshelf.ws.jetty.client.JettyWebSocketClient;
import com.gadgetworks.codeshelf.ws.jetty.client.WebSocketEventListener;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.CompleteWorkInstructionRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.ComputeWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.GetWorkRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.IRadioControllerEventListener;
import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class CsDeviceManager implements ICsDeviceManager, IRadioControllerEventListener, WebSocketEventListener {

	private static final Logger				LOGGER						= LoggerFactory.getLogger(CsDeviceManager.class);

	private static final String							DEVICETYPE_CHE			= "CHE";
	private static final String							DEVICETYPE_LED			= "LED Controller";
	
	private TwoKeyMap<UUID, NetGuid, INetworkDevice> mDeviceMap;
	private IRadioController				mRadioController;
/*
	private String							mOrganizationId;
	private String							mFacilityId;
	private String							mNetworkId;
	*/
	private String							mNetworkCredential;

	/* Device Manager owns websocket configuration too */
	private String							mUri;
	
	@Getter @Setter
	boolean suppressKeepAlive = false;
	
	@Getter @Setter
	boolean idleKill = false;
	
	@Getter
	private JettyWebSocketClient client;
	
	private ConnectionManagerThread connectionManagerThread;

	@Getter
	private long	lastNetworkUpdate=0;
	
	@Getter @Setter
	boolean radioEnabled = true;
	
	@Inject
	public CsDeviceManager(final IRadioController inRadioController) {
		// fetch properties from config file
		radioEnabled = PropertyUtils.getBoolean("radio.enabled",true);
		mUri = PropertyUtils.getString("websocket.uri");
		suppressKeepAlive = PropertyUtils.getBoolean("websocket.idle.suppresskeepalive");
		idleKill = PropertyUtils.getBoolean("websocket.idle.kill");

		mRadioController = inRadioController;
		mDeviceMap = new TwoKeyMap<UUID, NetGuid, INetworkDevice>();

		mNetworkCredential = System.getProperty("networkCredential");
	}

	public final void start() {
		startWebSocketClient();

	}
	
	public final void startRadio(CodeshelfNetwork network) {
		if (mRadioController.isRunning()) {
			LOGGER.warn("Radio controller is already running, cannot start again");
		} else if (this.radioEnabled) {
			// start radio controller
			NetworkId networkId = new NetworkId(network.getNetworkNum().byteValue());
			mRadioController.setNetworkId(networkId);
			mRadioController.startController(network.getChannel().byteValue());
			mRadioController.addControllerEventListener(this);
		} else {			
			LOGGER.warn("Radio controller disabled by setting, cannot start");
			mRadioController.setNetworkId(new NetworkId((byte) 1)); // for test

		}
	}
	
	public final List<AisleDeviceLogic> getAisleControllers() {
		ArrayList<AisleDeviceLogic> aList = new ArrayList<AisleDeviceLogic>();
		for (INetworkDevice theDevice :mDeviceMap.values()) {
			if (theDevice instanceof AisleDeviceLogic)
				aList.add((AisleDeviceLogic) theDevice);			
		}
		return aList;
	}

	public final void startWebSocketClient() {
    	// create response processor and register it with WS client
		SiteControllerMessageProcessor responseProcessor = new SiteControllerMessageProcessor(this);
    	client = new JettyWebSocketClient(mUri,responseProcessor,this);
    	responseProcessor.setWebClient(client);
    	connectionManagerThread = new ConnectionManagerThread(this);
    	connectionManagerThread.start();
	}

	public final void stop() {
		mRadioController.stopController();
		connectionManagerThread.setExit(true);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.device.ICsDeviceManager#getDeviceByGuid(com.gadgetworks.flyweight.command.NetGuid)
	 */
	@Override
	public final INetworkDevice getDeviceByGuid(NetGuid inGuid) {
		return mDeviceMap.get(inGuid);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.flyweight.controller.IRadioControllerEventListener#canNetworkDeviceAssociate(com.gadgetworks.flyweight.command.NetGuid)
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
	 * @see com.gadgetworks.flyweight.controller.IRadioControllerEventListener#deviceLost(com.gadgetworks.flyweight.controller.INetworkDevice)
	 */
	@Override
	public void deviceLost(INetworkDevice inNetworkDevice) {
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.device.ICsDeviceManager#requestCheWork(java.lang.String, java.lang.String, java.lang.String)
	 */
	public final void computeCheWork(final String inCheId, final UUID inPersistentId, final List<String> inContainerIdList) {
		LOGGER.debug("Compute work: Che: " + inCheId + " Container: " + inContainerIdList.toString());
		String cheId = inPersistentId.toString();
		LinkedList<String> containerIds = new LinkedList<String>();
		for (String containerId : inContainerIdList) {
			containerIds.add(containerId);
		}
		ComputeWorkRequest req = new ComputeWorkRequest(cheId,containerIds);
		client.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.device.ICsDeviceManager#requestCheWork(java.lang.String, java.lang.String, java.lang.String)
	 */
	public final void getCheWork(final String inCheId, final UUID inPersistentId, final String inLocationId) {
		LOGGER.debug("Get work: Che: " + inCheId + " Loc: " + inLocationId);
		String cheId = inPersistentId.toString();
		GetWorkRequest req  = new GetWorkRequest(cheId,inLocationId);
		client.sendMessage(req);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.device.ICsDeviceManager#completeWi(java.lang.String, java.util.UUID, com.gadgetworks.codeshelf.model.domain.WorkInstruction)
	 */
	@Override
	public final void completeWi(final String inCheId, final UUID inPersistentId, final WorkInstruction inWorkInstruction) {
		LOGGER.debug("Complete: Che: " + inCheId + " WI: " + inWorkInstruction.toString());
		CompleteWorkInstructionRequest req = new CompleteWorkInstructionRequest(inPersistentId,inWorkInstruction);
		client.sendMessage(req);
	}

	@Override
	public void connected() {
		// connected to server - send attach request
		LOGGER.info("Connected to server");
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUserId(CodeshelfNetwork.DEFAULT_SITECON_SERIAL);
		loginRequest.setPassword(mNetworkCredential);
		
		client.sendMessage(loginRequest);
	}

	@Override
	public void disconnected() {
		// disconnected from server
		LOGGER.info("Disconnected from server");
	}

	private void doCreateUpdateNetDevice(UUID persistentId, NetGuid deviceGuid, String deviceType) {
		// Update the device or create if it does not exist
		// NOTE: it appears CsDeviceManager receives but does not use about the domainId e.g. "CHE1" or "00000013"

		if (persistentId == null || deviceGuid == null || deviceType == null) {
			LOGGER.error("Invalid null parameter in call to doCreateUpdateNetDevice");
			return;
		}
		boolean suppressMapUpdate = false;
		INetworkDevice netDevice = mDeviceMap.get(persistentId);
		
		if (netDevice == null) {
			// new device
			if (deviceType.equals(DEVICETYPE_CHE)) {
				netDevice = new CheDeviceLogic(persistentId, deviceGuid, this, mRadioController);
			} else if (deviceType.equals(DEVICETYPE_LED)) {
				netDevice = new AisleDeviceLogic(persistentId, deviceGuid, this, mRadioController);
			} else {
				LOGGER.error("Don't know how to create new network device of type "+deviceType);
				suppressMapUpdate = true;
			}

			if(!suppressMapUpdate) {
				INetworkDevice oldNetworkDevice = mRadioController.getNetworkDevice(deviceGuid);
				if (oldNetworkDevice != null) {
					LOGGER.warn("Creating " + deviceType + " " + deviceGuid
							+ " but a NetworkDevice already existed with that NetGuid (removing)");
					mRadioController.removeNetworkDevice(oldNetworkDevice);
				} else {
					LOGGER.info("Creating " + deviceType + " " + persistentId + " / " + netDevice.getGuid());
				}
				mRadioController.addNetworkDevice(netDevice);
			}
		} else {
			// update existing device
			if (!netDevice.getGuid().equals(deviceGuid)) {
				// changing NetGuid (deprecated/bad!)
				INetworkDevice oldNetworkDevice = mRadioController.getNetworkDevice(netDevice.getGuid());
				if (oldNetworkDevice != null) {
					LOGGER.warn("Changing NetGuid of " + deviceType + " " + persistentId + " from " + netDevice.getGuid() + " to "
							+ deviceGuid);
					mRadioController.removeNetworkDevice(oldNetworkDevice);
				} else {
					LOGGER.error("Changing NetGuid of " + deviceType + " " + persistentId + " from " + netDevice.getGuid() + " to "
							+ deviceGuid + " but couldn't find original NetworkDevice");
				}
				// can't really change the NetGuid so we will create new device
				if (deviceType.equals(DEVICETYPE_CHE)) {
					netDevice = new CheDeviceLogic(persistentId, deviceGuid, this, mRadioController);
				} else if (deviceType.equals(DEVICETYPE_LED)) {
					netDevice = new AisleDeviceLogic(persistentId, deviceGuid, this, mRadioController);
				} else {
					LOGGER.error("Cannot update existing network device of unrecognized type "+deviceType);
					suppressMapUpdate = true;
				}
				if(!suppressMapUpdate) {
					mRadioController.addNetworkDevice(netDevice);
				}
			} else {
				// if not changing netGuid, there is nothing to change
				LOGGER.debug("No update to " + deviceType + " " + persistentId + " / " + deviceGuid);
				suppressMapUpdate = true;
			}
		}

		// update device map will also remove any mismatches (e.g. other entries with same guid/persistentId - see TwoKeyMap)
		if(!suppressMapUpdate) {
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
				INetworkDevice deviceByNetGuid = mRadioController.getNetworkDevice(deviceGuid);
				if (deviceByNetGuid != null) {
					mRadioController.removeNetworkDevice(deviceByNetGuid);
					LOGGER.error("Removed unmapped " + deviceType + " " + persistentId + " / " + deviceGuid
							+ " from Radio Controller by NetGuid");
				} else {
					LOGGER.error("Failed to remove unmapped " + deviceType + " " + persistentId + " / " + deviceGuid
							+ " from Radio Controller by NetGuid");
				}
			} else {
				deviceType = netDevice.getClass().getSimpleName();
				mRadioController.removeNetworkDevice(netDevice);
				LOGGER.warn("Removed partially unmapped " + deviceType + " " + persistentId + " / " + deviceGuid
						+ " from device map and Radio Controller by NetGuid");
			}
		} else {
			deviceType = netDevice.getClass().getSimpleName();
			mRadioController.removeNetworkDevice(netDevice);
			LOGGER.info("Removed " + deviceType + " " + persistentId + " / " + netDevice.getGuid());
		}
	}
	
	public void updateNetwork(CodeshelfNetwork network) {
		this.lastNetworkUpdate = System.currentTimeMillis();		
		Set<UUID> updateDevices=new HashSet<UUID>();
		// update network devices
		for (Che che : network.getChes().values()) {
			UUID id = che.getPersistentId();
			NetGuid deviceGuid = new NetGuid(che.getDeviceGuid());
			doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_CHE);
			updateDevices.add(id);
		}
		for (LedController ledController : network.getLedControllers().values()) {
			UUID id = ledController.getPersistentId();
			NetGuid deviceGuid = new NetGuid(ledController.getDeviceGuid());
			doCreateUpdateNetDevice(id, deviceGuid, DEVICETYPE_LED);
			updateDevices.add(id);
		}
		
		// now process deletions
		Set<UUID> deleteDevices=new HashSet<UUID>();
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
		
		LOGGER.debug("Network updated: "+updateDevices.size()+" active devices, "+ deleteDevices.size()+" removed");
	}

	public void processComputeWorkResponse(String networkGuid, Integer workInstructionCount) {
		NetGuid cheId = new NetGuid("0x" + networkGuid);
		CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheId);
		if (cheDevice != null) {
			cheDevice.assignComputedWorkCount(workInstructionCount);
		}
		else {
			LOGGER.warn("Unable to assign work count to CHE "+cheId+": CHE not found");
		}
	}
	
	public void processGetWorkResponse(String networkGuid, List<WorkInstruction> workInstructions) {
		NetGuid cheId = new NetGuid("0x" + networkGuid);
		CheDeviceLogic cheDevice = (CheDeviceLogic) mDeviceMap.get(cheId);
		if (cheDevice != null) {
			if (workInstructions!=null && workInstructions.size()>0) {
				cheDevice.assignWork(workInstructions);
			}
			else {
				LOGGER.warn("Unable to assign work to CHE "+cheId+": No work instructions");
			}
		}
		else {
			LOGGER.warn("Unable to assign work to CHE "+cheId+": CHE not found");
		}
	}

	public void processWorkInstructionCompletedResponse(UUID workInstructionId) {
		// do nothing
	}

}
