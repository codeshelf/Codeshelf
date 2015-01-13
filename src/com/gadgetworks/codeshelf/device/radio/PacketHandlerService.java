package com.gadgetworks.codeshelf.device.radio;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gadgetworks.flyweight.command.IPacket;
import com.gadgetworks.flyweight.command.NetAddress;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The packet handler service is thread-safe and allows multiple threads to call handle. The handle method will add the packet to a queue
 * to be processed. If that queue is full, the handle method will throw an exception. Each submitted packed is processed in order by
 * the SourceAddr and in parallel across different SourceAddres. The parallelism of this service defaults to the number of available cores.
 * 
 * @author saba
 *
 */
public class PacketHandlerService {
	private final ExecutorService									executor				= Executors.newFixedThreadPool(Runtime.getRuntime()
																								.availableProcessors(),
																								new ThreadFactoryBuilder().setNameFormat("pckt-hndlr-%s")
																									.build());
	private final ConcurrentMap<NetAddress, BlockingQueue<IPacket>>	queueMap				= Maps.newConcurrentMap();
	private static final int							MAX_PACKET_QUEUE_SIZE	= 50;

	public boolean handle(IPacket packet) {

		//Get queue from queueMap in thread-safe manner. Multiple threads can call handle at the same time
		BlockingQueue<IPacket> queue = queueMap.get(packet.getSrcAddr());
		if (queue == null) {
			queue = new ArrayBlockingQueue<IPacket>(MAX_PACKET_QUEUE_SIZE);
			BlockingQueue<IPacket> existingQueue = queueMap.putIfAbsent(packet.getSrcAddr(), queue);
			if (existingQueue != null) {
				queue = existingQueue;
			}
		}

		//We synchronize on the queue before modifying it (even though its thread-safe) because we must perform 3 actions atomically
		boolean wasAddedToQueue = false;
		synchronized (queue) {

			boolean wasQueueOriginallyEmpty = queue.isEmpty();

			//OFFER will not block and return boolean for success. If we block here, we will have a deadlock
			wasAddedToQueue = queue.offer(packet);

			//If it was originally empty, then we can resubmit a handler to the executor since no other handler should be running.
			//But we should submit only if we succeeded in adding the packet to the queue!
			if (wasAddedToQueue && wasQueueOriginallyEmpty) {
				executor.submit(new PacketHandler(queue));
			}
		}

		return wasAddedToQueue;

	}

	public void shutdown() {
		executor.shutdown();
	}

	private final class PacketHandler implements Runnable {
		private final BlockingQueue<IPacket>	queue;

		public PacketHandler(BlockingQueue<IPacket> queue) {
			super();
			this.queue = queue;
		}

		@Override
		public void run() {
			//We do not need to use synchronize because we aren't actually modifying the queue
			//We peek because if we poll, we would cause the queue to be empty which could submit another task for this
			//NetAddr in parallel
			IPacket packet = queue.peek();

			try {
				//Handle packet
			} catch (Exception e) {
				//Log Error
			} finally {
				//The finally block ensures that we will always pop this packet off the queue, no matter if we have some uncaught exception or not (like a generic throwable)

				//We synchronize on the queue (even though its thread-safe) because we must perform 2 actions atomically
				synchronized (queue) {
					//Remove packet we JUST finished processing
					queue.poll();

					if (!queue.isEmpty()) {
						//Resubmit this runnable if its not empty yet and let the other submitted PacketHandlers get a fair chance
						executor.submit(this);
					}
				}
			}

		}

	}
}
