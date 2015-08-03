package com.codeshelf.device.radio;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.IPacket;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The packet handler service is thread-safe and allows multiple threads to call
 * handle. The handle method will add the packet to a queue to be processed. If
 * that queue is full, the handle method will return false. Each submitted
 * packed is processed in order by the SourceAddr and in parallel across
 * different SourceAddres. The parallelism of this service defaults to the
 * number of available cores*2
 * 
 * @author saba, huffa
 *
 */
public class RadioControllerInboundPacketService {
	private static final Logger						LOGGER			= LoggerFactory.getLogger(RadioControllerInboundPacketService.class);

	private final ExecutorService					executor		= Executors.newFixedThreadPool(Math.max(Runtime.getRuntime()
																									.availableProcessors() * 2, 2),
																		new ThreadFactoryBuilder().setNameFormat("pckt-hndlr-%s")
																			.build());

	private final ConcurrentLinkedQueue<IPacket>	incomingPackets	= new ConcurrentLinkedQueue<IPacket>();

	private final RadioController					radioController;

	public RadioControllerInboundPacketService(RadioController radioController) {
		super();
		this.radioController = radioController;
	}

	public void start() {
		executor.submit(new PacketHandler(incomingPackets));
	}

	public boolean handleInboundPacket(IPacket packet) {
		boolean wasAddedToQueue = true;

		try {
			wasAddedToQueue = incomingPackets.offer(packet);
		} catch (IllegalStateException e) {
			LOGGER.warn("Incoming packet queue is full. Clearing queue.");
			incomingPackets.clear();
		}

		if (wasAddedToQueue) {
			executor.submit(new PacketHandler(incomingPackets));
		} else {
			LOGGER.warn("Incoming packet was dropped. Queue size: {} Packet: {}", incomingPackets.size(), packet.toString());
		}

		return wasAddedToQueue;
	}

	public void shutdown() {
		executor.shutdown();
	}

	private final class PacketHandler implements Runnable {
		private final ConcurrentLinkedQueue<IPacket>	queue;

		public PacketHandler(ConcurrentLinkedQueue<IPacket> queue) {
			super();
			this.queue = queue;
		}

		@Override
		public void run() {

			IPacket packet = null;
			packet = queue.poll();

			if (packet == null) {
				return;
			}

			try {
				radioController.handleInboundPacket(packet);
			} catch (Exception e) {
				// Handle Error
				LOGGER.error("PacketHandler Error. Packet={}", packet, e);
			}
		}
	}
}
