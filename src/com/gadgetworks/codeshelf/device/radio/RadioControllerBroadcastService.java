package com.gadgetworks.codeshelf.device.radio;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.CommandNetMgmtCheck;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetChannelValue;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioControllerBroadcastService {
	private static final Logger						LOGGER					= LoggerFactory.getLogger(RadioControllerPacketIOService.class);

	private final ScheduledExecutorService	scheduleExecutorService	= Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("radio-broadcast-thread")
		.build());
	private final NetworkId 						broadcastNetworkId 		= new NetworkId(IPacket.BROADCAST_NETWORK_ID);
	private final NetAddress						broadcastAddress 		= new NetAddress(IPacket.BROADCAST_ADDRESS);

	private final IRadioController			radioController;
	private final long						broadcastRateMs;

	public RadioControllerBroadcastService(IRadioController radioController, long broadcastRateMs) {
		super();
		this.radioController = radioController;
		this.broadcastRateMs = broadcastRateMs;
	}

	public void start() {
		//Broadcast at fixed rate
		scheduleExecutorService.scheduleAtFixedRate(new Broadcaster(), 0, broadcastRateMs, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		scheduleExecutorService.shutdown();
	}

	private final class Broadcaster implements Runnable {

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

	public NetworkId getBroadcastNetworkId() {
		return broadcastNetworkId;
	}

	public NetAddress getBroadcastAddress() {
		return broadcastAddress;
	}

}
