package com.codeshelf.device.radio;

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

	// due to lack of netcheck packets
	private final RadioController					radioController;

	public RadioControllerInboundPacketService(RadioController radioController) {
		super();
		this.radioController = radioController;
	}

	public void start() {
		executor.submit(new PacketHandler(null));
	}

	public boolean handleInboundPacket(IPacket packet) {
		executor.submit(new PacketHandler(packet));
		return true;
	}

	public void shutdown() {
		executor.shutdown();
	}

	private final class PacketHandler implements Runnable {
		//private final ConcurrentLinkedQueue<IPacket>	queue;
		private final IPacket packet;

		public PacketHandler(IPacket inPacket) {
			super();
			packet = inPacket;
		}

		@Override
		public void run() {

			if (packet == null) {
				return;
			}

			try {
				//LOGGER.info("Handling pkt: ", packet);
				radioController.handleInboundPacket(packet);
			} catch (Exception e) {
				// Handle Error
				LOGGER.error("PacketHandler Error. Packet={}", packet, e);
			}
		}
	}
}
