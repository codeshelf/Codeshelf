package com.codeshelf.device.radio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.flyweight.command.CommandGroupEnum;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 
 * @author saba
 *
 */
public class RadioControllerPacketIOService {
	private static final Logger							LOGGER			= LoggerFactory.getLogger(RadioControllerPacketIOService.class);

	private final Counter								packetsSentCounter;
	private final ExecutorService						executorService	= Executors.newFixedThreadPool(1,
																			new ThreadFactoryBuilder().setNameFormat("pckt-io-%s")
																				.setPriority(Thread.MAX_PRIORITY)
																				.build());

	private final IGatewayInterface						gatewayInterface;
	private final RadioControllerPacketHandlerService	packetHandlerService;

	private NetworkId									networkId;
	private volatile boolean							isShutdown		= false;

	public RadioControllerPacketIOService(IGatewayInterface gatewayInterface,
		RadioControllerPacketHandlerService packetHandlerService,
		long writeDelayMs) {
		super();
		this.gatewayInterface = gatewayInterface;
		this.packetHandlerService = packetHandlerService;
		this.packetHandlerService = MetricsService.getInstance().createCounter(MetricsGroup.Radio, "packets.sent");
	}

	public void start() {
		executorService.submit(new PacketReader());
	}

	public void stop() {
		isShutdown = true;
		executorService.shutdown();
	}

	/**
	 * This method is synchronized because the gateway's thread safety is
	 * unknown
	 */
	public synchronized void handleOutboundPacket(IPacket packet) {
		// Send packet
		packet.incrementSendCount();
		gatewayInterface.sendPacket(packet);
		packet.setSentTimeMillis(System.currentTimeMillis());

		if (packetsSentCounter != null) {
			packetsSentCounter.inc();
		}

		if (packet.getCommand().getCommandTypeEnum() != CommandGroupEnum.NETMGMT) {
			LOGGER.debug("OUTBOUND PACKET={}", packet);
		}
	}

	private final class PacketReader implements Runnable {

		@Override
		public void run() {
			while (!isShutdown) {
				try {
					if (gatewayInterface.isStarted()) {

						// Blocks and waits for packet
						// TODO Move gatewayInterface away from polling. This
						// method will sleep for 1ms if we don't have enough
						// packets to read.
						IPacket packet = gatewayInterface.receivePacket(networkId);

						if (packet != null) {
							// Hand packet off to handler service
							boolean success = packetHandlerService.handleInboundPacket(packet);
							LOGGER.info("INBOUND PACKET={}; didGetHandled={}", packet, success);
							if (!success) {

								LOGGER.warn("PacketHandlerService failed to accept packet. Pausing packet reads to retry handlePacket. Packet={}",
									packet);
								// We will stop reading and try to handle this
								// packet with an exponential backoff of up to
								// 500ms
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
		 * Tries to handle a packet with 3 tries and sleeps between. Sleeps will
		 * be 5, 50, and 500ms respectively for each attempt.
		 * 
		 * If all 3 attempts fail. We will log and drop the packet.
		 * 
		 * @throws InterruptedException
		 */
		private void retryHandlePacketWithExponentialBackoff(IPacket packet) throws InterruptedException {
			boolean success = false;
			for (int attempt = 0; attempt < 3; attempt++) {
				long sleepTimeMs = (long) (5 * Math.pow(10, attempt));
				Thread.sleep(sleepTimeMs);

				// Hand packet off to handler service
				success = packetHandlerService.handleInboundPacket(packet);

				if (!success) {
					LOGGER.warn("PacketHandlerService failed to accept packet again. Attempt={}/{}; SleepTimeMs={}; Packet={};",
						attempt + 1,
						3,
						sleepTimeMs,
						packet);
				} else {
					return;
				}
			}

			if (!success) {
				LOGGER.error("PacketHandlerService failed to accept packet after retries. Dropping packet={}", packet);
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
