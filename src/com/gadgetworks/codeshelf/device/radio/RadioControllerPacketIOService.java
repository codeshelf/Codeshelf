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
 * TODO Make IO Single Threaded, This requires changes to the gateway interface to support callbacks
 * 
 * @author saba
 *
 */
public class RadioControllerPacketIOService {
	private static final Logger							LOGGER					= LoggerFactory.getLogger(RadioControllerPacketIOService.class);
	private static final int							MAX_PACKET_WRITE_QUEUE_SIZE	= 50;

	private final ScheduledExecutorService				scheduleExecutorService		= Executors.newScheduledThreadPool(2,
																						new ThreadFactoryBuilder().setNameFormat("pckt-io-%s")
																							.build());
	private final BlockingQueue<IPacket>				packetsPendingWrite			= new ArrayBlockingQueue<>(MAX_PACKET_WRITE_QUEUE_SIZE);
	private final IGatewayInterface						gatewayInterface;
	private final RadioControllerPacketHandlerService	packetHandlerService;
	private final long						writeDelayMs;

	private NetworkId						networkId;
	private volatile boolean							isShutdown					= false;

	public RadioControllerPacketIOService(IGatewayInterface gatewayInterface,
		RadioControllerPacketHandlerService packetHandlerService,
		long writeDelayMs) {
		super();
		this.gatewayInterface = gatewayInterface;
		this.packetHandlerService = packetHandlerService;
		this.writeDelayMs = writeDelayMs;
	}

	public void start() {
		//We do not need any delays here. TODO is it okay to use 0?
		scheduleExecutorService.submit(new PacketReader());

		//We want there to be a delay between every write. PacketWriter waits for packets then sends them immediately after they are ready. It could wait for longer than writeRateMs.
		scheduleExecutorService.scheduleWithFixedDelay(new PacketWriter(), 0, writeDelayMs, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		isShutdown = true;
		scheduleExecutorService.shutdown();
	}

	/**
	 * This method will block until there is space in the queue.
	 */
	public void queuePacketForWrite(IPacket packet) throws InterruptedException {
		LOGGER.info("SABA Queueing packet for gateway. Dst={}; QueueSize={}; Max={}",
			packet,
			packetsPendingWrite.size(),
			MAX_PACKET_WRITE_QUEUE_SIZE);

		//Block and wait for space in queue
		packetsPendingWrite.put(packet);
	}

	private final class PacketReader implements Runnable {

		@Override
		public void run() {
			while (!isShutdown) {
				try {
					if (gatewayInterface.isStarted()) {

						//Blocks and waits for packet
						//TODO Move gatewayInterface away from polling. This method will sleep for 1ms if we don't have enough packets to read.
						IPacket packet = gatewayInterface.receivePacket(networkId);

						if (packet != null) {
							//Hand packet off to handler service
							boolean success = packetHandlerService.handle(packet);

							if (!success) {

								LOGGER.warn("PacketHandlerService failed to accept packet. Pausing packet reads to retry packet. Packet={}",
									packet);
								//We will stop reading and try to handle this packet with an exponential backoff of up to 500ms
								this.retryHandlePacketWithExponentialBackoff(packet);
							}
						}
					}
				} catch (Exception e) {
					LOGGER.error("Packet Read Error. Potential packet loss has occured. ", e);
				}
			}
		}

		/**
		 * Tries to handle a packet with 3 tries and sleeps between.
		 * Sleeps will be 5, 50, and 500ms respectively for each attempt.
		 * 
		 * If all 3 attempts fail. We will log and drop the packet.
		 * @throws InterruptedException 
		 */
		private void retryHandlePacketWithExponentialBackoff(IPacket packet) throws InterruptedException {
			boolean success = false;
			for (int attempt = 0; attempt < 3; attempt++) {
				long sleepTimeMs = (long) (5 * Math.pow(10, attempt));
				Thread.sleep(sleepTimeMs);

				//Hand packet off to handler service
				success = packetHandlerService.handle(packet);

				if (!success) {
					LOGGER.warn("PacketHandlerService failed to accept packet again. Attempt={}/{}; SleepTimeMs={}; Packet={};",
						attempt + 1,
						3,
						sleepTimeMs,
						packet);
				} else {
					break;
				}
			}

			if (!success) {
				LOGGER.error("PacketHandlerService failed to accept packet after retries. Dropping packet={}", packet);
			}
		}

	}
	


	private final class PacketWriter implements Runnable {

		@Override
		public void run() {
			try {
				if (gatewayInterface.isStarted()) {

					//Non-busy block and wait for packet to send. 
					IPacket packet = packetsPendingWrite.take();
					packet.setSentTimeMillis(System.currentTimeMillis());
					packet.incrementSendCount();

					//Send packet
					gatewayInterface.sendPacket(packet);
					//this.packetsSentCounter.inc();
				}
			} catch (Exception e) {
				LOGGER.error("Packet Writer Error ", e);
			}
		}

	}

	public void setNetworkId(NetworkId networkId) {
		this.networkId = networkId;
	}

	public NetworkId getNetworkId() {
		return networkId;
	}

}
