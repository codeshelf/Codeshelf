package com.codeshelf.device.radio;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.IPacket;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

//import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

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
	private static final Logger				LOGGER					= LoggerFactory.getLogger(RadioControllerInboundPacketService.class);

	private final ExecutorService			executor				= Executors.newFixedThreadPool(Math.max(/*Runtime.getRuntime()
																											.availableProcessors() */8,
																		2),
																		new ThreadFactoryBuilder().setNameFormat("pckt-hndlr-%s")
																			.build());

	//private final BlockingQueue<IPacket>	incomingPackets			= new ArrayBlockingQueue<IPacket>(MAX_PACKET_QUEUE_SIZE);
	private final ConcurrentLinkedQueue<IPacket> incomingPackets	= new ConcurrentLinkedQueue<IPacket>();
	

	private static final int				MAX_PACKET_QUEUE_SIZE	= 200;

	private final RadioController			radioController;

	public RadioControllerInboundPacketService(RadioController radioController) {
		super();
		this.radioController = radioController;
	}

	public void start() {
		executor.submit(new PacketHandler(incomingPackets));
	}

	public boolean handleInboundPacket(IPacket packet) {
		boolean wasAddedToQueue = true;

		LOGGER.info("Adding packet {}", packet.toString());
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
			//if (queue.isEmpty()) {
			//	return;
			//}
			
			//LOGGER.info("**** Polling for packet");
			
			IPacket packet = null;
			packet = queue.poll();
			
			if (packet == null) {
				return;
			}
			
			//LOGGER.info("**** Handling packet: {}", packet.toString());

			try {
				//LOGGER.info("~~Handling D: {} P: {}", packet.getDstAddr(), packet.toString());
				// Handle packet
				radioController.handleInboundPacket(packet);
			} catch (Exception e) {
				// Handle Error
				//LOGGER.error("PacketHandler Error. Packet={}", packet, e);
			} //finally {

				// We synchronize on the queue
				//synchronized (queue) {
					//if (!queue.isEmpty()) {
						// Resubmit this runnable if its not empty yet and let
						// the other submitted PacketHandlers get a fair chance
					//	executor.submit(this);
					//}
				//}
				//LOGGER.info("**** Finished with packet: {}", packet.toString());
			//}
		}
	}
}
