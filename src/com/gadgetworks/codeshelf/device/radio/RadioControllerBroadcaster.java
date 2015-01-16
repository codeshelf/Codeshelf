package com.gadgetworks.codeshelf.device.radio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandNetMgmtCheck;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetChannelValue;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.controller.IRadioController;

public class RadioControllerBroadcaster implements Runnable {
	private static final Logger		LOGGER				= LoggerFactory.getLogger(RadioControllerBroadcaster.class);

	private static final NetworkId	broadcastNetworkId	= new NetworkId(IPacket.BROADCAST_NETWORK_ID);
	private static final NetAddress	broadcastAddress	= new NetAddress(IPacket.BROADCAST_ADDRESS);

	private final IRadioController	radioController;

	public RadioControllerBroadcaster(IRadioController radioController) {
		super();
		this.radioController = radioController;
	}

	@Override
	public void run() {
		try {
			ICommand netCheck = new CommandNetMgmtCheck(CommandNetMgmtCheck.NETCHECK_REQ,
				broadcastNetworkId,
				RadioController.PRIVATE_GUID,
				radioController.getRadioChannel(),
				new NetChannelValue((byte) 0),
				new NetChannelValue((byte) 0));

			radioController.sendCommand(netCheck, broadcastAddress, false);
		} catch (Exception e) {
			LOGGER.error("Broadcast Error ", e);
		}

	}

}
