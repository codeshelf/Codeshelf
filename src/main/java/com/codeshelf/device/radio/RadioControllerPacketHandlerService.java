package com.codeshelf.device.radio;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.radio.protocol.IRadioPacketHandler;
import com.codeshelf.flyweight.command.CommandAssocABC;
import com.codeshelf.flyweight.command.CommandAssocReq;
import com.codeshelf.flyweight.command.CommandGroupEnum;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The packet handler service is thread-safe and allows multiple threads to call
 * handle. The handle method will add the packet to a queue to be processed. If
 * that queue is full, the handle method will return false. Each submitted
 * packed is processed in order by the SourceAddr and in parallel across
 * different SourceAddres. The parallelism of this service defaults to the
 * number of available cores*2
 * 
 * @author saba
 *
 */
public class RadioControllerPacketHandlerService {
	private static final Logger										LOGGER					= LoggerFactory.getLogger(RadioControllerPacketHandlerService.class);

	private final ExecutorService									executor				= Executors.newFixedThreadPool(Math.max(Runtime.getRuntime()
																								.availableProcessors() * 2,
																								2),
																								new ThreadFactoryBuilder().setNameFormat("pckt-hndlr-%s")
																									.build());

	private final ConcurrentMap<NetAddress, BlockingQueue<IPacket>>	queueMap				= Maps.newConcurrentMap();

	private static final int										MAX_PACKET_QUEUE_SIZE	= 50;

	private final Map<Byte, IRadioPacketHandler>					protocolVersionToPacketHandlerMap;
	private final Map<NetAddress, INetworkDevice>					netAddrToDeviceMap;
	private final Map<String, INetworkDevice>						guidToDeviceMap;

	public RadioControllerPacketHandlerService(Map<Byte, IRadioPacketHandler> protocolVersionToPacketHandlerMap,
		Map<NetAddress, INetworkDevice> netAddrToDeviceMap,
		Map<String, INetworkDevice> guidToDeviceMap) {
		super();
		this.protocolVersionToPacketHandlerMap = protocolVersionToPacketHandlerMap;
		this.netAddrToDeviceMap = netAddrToDeviceMap;
		this.guidToDeviceMap = guidToDeviceMap;
	}

	public boolean handleInboundPacket(IPacket packet) {
		// Get queue from queueMap in thread-safe manner. Multiple threads can
		// call handle at the same time
		BlockingQueue<IPacket> queue = queueMap.get(packet.getSrcAddr());
		if (queue == null) {
			queue = new ArrayBlockingQueue<IPacket>(MAX_PACKET_QUEUE_SIZE);
			BlockingQueue<IPacket> existingQueue = queueMap.putIfAbsent(packet.getSrcAddr(), queue);
			if (existingQueue != null) {
				queue = existingQueue;
			}
		}

		// We synchronize on the queue before modifying it (even though its
		// thread-safe) because we must perform 3 actions atomically
		boolean wasAddedToQueue = false;
		synchronized (queue) {

			boolean wasQueueOriginallyEmpty = queue.isEmpty();

			// OFFER will not block and return boolean for success. If we block
			// here, we will have a deadlock
			wasAddedToQueue = queue.offer(packet);

			// If it was originally empty, then we can resubmit a handler to the
			// executor since no other handler should be running.
			// But we should submit only if we succeeded in adding the packet to
			// the queue!
			if (wasAddedToQueue && wasQueueOriginallyEmpty) {
				executor.submit(new InboundPacketHandler(queue));
			}
		}

		return wasAddedToQueue;
	}

	private final class InboundPacketHandler implements Runnable {
		private final BlockingQueue<IPacket>	queue;

		public InboundPacketHandler(BlockingQueue<IPacket> queue) {
			super();
			this.queue = queue;
		}

		@Override
		public void run() {
			// We do not need to use synchronize because we aren't actually
			// modifying the queue
			// We peek because if we poll, we would cause the queue to be empty
			// which could submit another task for this
			// NetAddr in parallel
			IPacket packet = queue.peek();

			try {
				// Handle packet -- TODO perhaps move this to the radio controller? It might be more appropriate there
				if (CommandGroupEnum.ASSOC.equals(packet.getCommand().getCommandTypeEnum())) {
					CommandAssocABC assocCmd = (CommandAssocABC) packet.getCommand();

					//Make sure device is registered
					INetworkDevice device = guidToDeviceMap.get(assocCmd.getGUID());
					if (device == null) {
						LOGGER.info("Ignoring AssocRequest from unregistered GUID assocCmd={}", assocCmd);
						return;
					}

					if (CommandAssocABC.ASSOC_REQ_COMMAND == assocCmd.getExtendedCommandID().getValue()) {
						CommandAssocReq assocReq = (CommandAssocReq) assocCmd;

						//Update the radio protocol version for the GUID from the version in the assoc req
						byte radioProtocolVersion = assocReq.getRadioProtocolVersion();
						device.setRadioProtocolVersion(radioProtocolVersion);

						//Lookup handler for RP version
						IRadioPacketHandler handler = protocolVersionToPacketHandlerMap.get(radioProtocolVersion);

						if (handler == null) {
							LOGGER.warn("Ignoring AssocRequest with unsupported radio protocol version. rpVersion={}; assocReq={}",
								radioProtocolVersion,
								assocReq);
							return;
						}

						//Handle packet
						handler.handleInboundPacket(packet);

					} else {
						//This is a different AssocCmd.
						Byte radioProtocolVersion = device.getRadioProtocolVersion();

						if (radioProtocolVersion == null) {
							LOGGER.info("Ignoring AssocCmd from device that has not yet registered its radio protocol version with a AssocReq. Cmd={}",
								assocCmd);
							return;
						}

						IRadioPacketHandler handler = protocolVersionToPacketHandlerMap.get(radioProtocolVersion);

						if (handler == null) {
							LOGGER.warn("Ignoring AssocRequest with unsupported radio protocol version. rpVersion={}; assocReq={}",
								radioProtocolVersion,
								assocCmd);
							return;
						}

						//Handle Packet
						handler.handleInboundPacket(packet);

					}
				} else {

					//Now we can lookup the INetworkDevice from the NetAddr
					//Make sure device is registered
					INetworkDevice device = netAddrToDeviceMap.get(packet.getSrcAddr());
					if (device == null) {
						LOGGER.info("Ignoring Packet from unregistered device packet={}", packet);
						return;
					}

					//Check version
					Byte radioProtocolVersion = device.getRadioProtocolVersion();

					if (radioProtocolVersion == null) {
						LOGGER.info("Ignoring packet from device that has not yet registered its radio protocol version with an AssocReq. packet={}",
							packet);
						return;
					}

					IRadioPacketHandler handler = protocolVersionToPacketHandlerMap.get(radioProtocolVersion);

					if (handler == null) {
						LOGGER.warn("Ignoring packet with unsupported radio protocol version. rpVersion={}; packet={}",
							radioProtocolVersion,
							packet);
						return;
					}

					//Handle Packet
					handler.handleInboundPacket(packet);
				}

			} catch (Exception e) {
				// Handle Error
				LOGGER.error("PacketHandler Error. Packet={}", packet, e);
			} finally {
				// The finally block ensures that we will always pop this packet
				// off the queue, no matter if we have some uncaught exception
				// or not (like a generic throwable)

				// We synchronize on the queue (even though its thread-safe)
				// because we must perform 2 actions atomically
				synchronized (queue) {
					// Remove packet we JUST finished processing
					queue.poll();

					if (!queue.isEmpty()) {
						// Resubmit this runnable if its not empty yet and let
						// the other submitted PacketHandlers get a fair chance
						executor.submit(this);
					}
				}
			}
		}
	}

	public void shutdown() {
		executor.shutdown();
	}

}
