package com.gadgetworks.codeshelf.device.radio;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetworkId;
import com.gadgetworks.flyweight.controller.IGatewayInterface;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * This class handles IO for the radio controller with two threads.
 * 
 * TODO Make IO Single Threaded, This requires changes to the gateway interface to support packet availability checks since the read method blocks and waits for packets.
 * 
 * @author saba
 *
 */
public class RadioControllerPacketIOService {
	private static final Logger							LOGGER					= LoggerFactory.getLogger(RadioControllerPacketIOService.class);

	private final ScheduledExecutorService				scheduleExecutorService	= Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder().setNameFormat("pckt-io-%s")
																						.build());
	private final BlockingQueue<IPacket>				packetsPendingWrite		= new ArrayBlockingQueue<>(50);

	private final long									readRateMs;
	private final long									writeRateMs;
	private final IGatewayInterface						gatewayInterface;
	private final RadioControllerPacketHandlerService	packetHandlerService;
	private final NetworkId								networkId;

	public RadioControllerPacketIOService(long readRateMs,
		long writeRateMs,
		IGatewayInterface gatewayInterface,
		RadioControllerPacketHandlerService packetHandlerService,
		NetworkId networkId) {
		super();
		this.readRateMs = readRateMs;
		this.writeRateMs = writeRateMs;
		this.gatewayInterface = gatewayInterface;
		this.packetHandlerService = packetHandlerService;
		this.networkId = networkId;
	}

	public void start() {
		scheduleExecutorService.scheduleAtFixedRate(new PacketReader(), 0, readRateMs, TimeUnit.MILLISECONDS);
		scheduleExecutorService.scheduleAtFixedRate(new PacketWriter(), 0, writeRateMs, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		scheduleExecutorService.shutdown();
	}

	public void queuePacketForWrite(IPacket packet) throws InterruptedException {
		//TODO Reason about put
		packetsPendingWrite.put(packet);
	}

	private final class PacketReader implements Runnable {

		@Override
		public void run() {
			try {
				if (gatewayInterface.isStarted()) {
					IPacket packet = gatewayInterface.receivePacket(networkId);
					boolean success = packetHandlerService.handle(packet);
					//TODO Exponential backoff
				}
			} catch (RuntimeException e) {
				LOGGER.error("Packet Read Error ", e);
			}
		}

	}

	private final class PacketWriter implements Runnable {

		@Override
		public void run() {
			try {
				if (gatewayInterface.isStarted()) {
					//TODO Reason about take
					IPacket packet = packetsPendingWrite.take();
					packet.setSentTimeMillis(System.currentTimeMillis());
					packet.incrementSendCount();

					gatewayInterface.sendPacket(packet);
					//this.packetsSentCounter.inc();
				}
			} catch (Exception e) {
				LOGGER.error("Packet Writer Error ", e);
			}
		}

	}


}
