package com.codeshelf.device.radio;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codeshelf.flyweight.command.AckStateEnum;
import com.codeshelf.flyweight.command.CommandGroupEnum;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.controller.IGatewayInterface;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 
 * @author saba
 *
 */
public class RadioControllerPacketIOService {
	private static final Logger							LOGGER							= LoggerFactory.getLogger(RadioControllerPacketIOService.class);

	private final ExecutorService						executorService					= Executors.newFixedThreadPool(1,
																							new ThreadFactoryBuilder().setNameFormat("pckt-io-%s")
																								.setPriority(Thread.MAX_PRIORITY)
																								.build());

	private final ConcurrentMap<NetAddress, AtomicLong>	mLastPacketSentTimestampMsMap	= Maps.newConcurrentMap();

	/**
	 * We use a read-write lock to prevent any threads from sending out packets when we want to send a packet out on the broadcast address (i.e. to alldevices). 
	 * All normal packets will aquire a read lock (which will only be possible if there are no writers). Furthermore, aquiring a write lock means all read locks 
	 * have been released. This way we can send the broadcast packet out and update all the lastSentTimestamps for every destination addr before resuming.
	 */
	private final ReadWriteLock							broadcastReadWriteLock			= new ReentrantReadWriteLock();
	private volatile boolean							isShutdown						= false;

	private final IGatewayInterface						gatewayInterface;
	private final RadioControllerPacketHandlerService	packetHandlerService;
	private final NetAddress							broadcastAddress;
	private final long									packetSpacingMillis;

	private NetworkId									networkId;
	private Counter										packetsSentCounter;

	public RadioControllerPacketIOService(IGatewayInterface gatewayInterface,
		RadioControllerPacketHandlerService packetHandlerService,
		NetAddress broadcastAddress,
		long packetSpacingMillis) {
		super();
		this.gatewayInterface = gatewayInterface;
		this.packetHandlerService = packetHandlerService;
		this.broadcastAddress = broadcastAddress;
		this.packetSpacingMillis = packetSpacingMillis;
	}

	public void start() {
		executorService.submit(new PacketReader());
		this.packetsSentCounter = MetricsService.getInstance().createCounter(MetricsGroup.Radio, "packets.sent");
	}

	public void stop() {
		isShutdown = true;
		gatewayInterface.stopInterface();
		executorService.shutdown();
	}

	/** Properly spaces outbound packets
	 * @param inPacket
	 */
	public void handleOutboundPacket(IPacket inPacket) {

		if (broadcastAddress.equals(inPacket.getDstAddr())) {
			// If we are broadcasting a packet. We must make sure no other
			// threads are sending packet to their respective dstAddress before
			// proceeding.
			Lock writeLock = broadcastReadWriteLock.writeLock();
			try {
				writeLock.lock();

				// At this point we are the only one that can send a packet.
				long maxLastPacketSentTimestampMs = 0;

				// A concurrentHashMap will never throw
				// ConcurrentModificationException. Concurrent modications
				// could happen since further below, we (potentially) modify this map
				// before obtaining a read lock. In that scenario,
				// we would send a broadcast to that addrr and potentially fail
				// to set its lastSentTimestamp as it may
				// not become visible in the collection below. Later, this could
				// result in a poorly spaced packet, however that
				// would mean the device would have to have never received a
				// packet from us before and thus it would just
				// have a slightly delayed assocation which isn't too bad.
				for (AtomicLong lastPacketSentTimestamp : mLastPacketSentTimestampMsMap.values()) {
					maxLastPacketSentTimestampMs = Math.max(maxLastPacketSentTimestampMs, lastPacketSentTimestamp.get());
				}

				long differenceMs = System.currentTimeMillis() - maxLastPacketSentTimestampMs;

				// Sleep if needed to ensure all device are ready to read our packet.
				// We use a while loop here because sleep is not gaurunteed to
				// sleep for even at least the specified time.
				while (differenceMs < packetSpacingMillis) {
					try {
						Thread.sleep(Math.max(1, differenceMs));
					} catch (InterruptedException e) {
						LOGGER.error("SendPckt ", e);
					}
					differenceMs = System.currentTimeMillis() - maxLastPacketSentTimestampMs;
				}

				// Send the broadcast packet
				sendOutboundPacket(inPacket);

				// Update everybody's last sent timestamp. Similar race conditions could occur here (as described above) that would result in
				//potential non-fatally spaced packets for new devices.
				for (AtomicLong lastPacketSentTimestamp : mLastPacketSentTimestampMsMap.values()) {
					lastPacketSentTimestamp.set(inPacket.getSentTimeMillis());
				}

			} finally {
				writeLock.unlock();
			}
		} else {

			// Get the last packet sent timestamp for this destination address
			AtomicLong lastPacketSentTimestampMs = getLastPacketSentTimestamp(inPacket.getDstAddr());

			// Lock on the timestamp to ensure only one thread can proceed per
			// destination address.
			// We lock on this timestamp before the read lock to minimize time
			// spent holding the read lock so that the
			// broadcast thread can send a packet asap. Furthermore, this
			// timestamp does not need to be atomic but a normal
			// Long object does not provide a setter, so we use atomicLong
			// instead.
			synchronized (lastPacketSentTimestampMs) {

				// Since we are not broadcasting, we need to obtain a read lock
				// before proceeding.
				Lock readLock = broadcastReadWriteLock.readLock();
				try {
					readLock.lock();
					// At this point we are allowed to write to the radio

					// Sleep as need to ensure proper packet spacing
					long differenceMs = System.currentTimeMillis() - lastPacketSentTimestampMs.get();

					// We use a while loop here because sleep is not gaurunteed
					// to sleep for even at least the specified time.
					while (differenceMs < packetSpacingMillis) {
						try {
							Thread.sleep(Math.max(1, differenceMs));
						} catch (InterruptedException e) {
							LOGGER.error("SendPckt ", e);
						}
						differenceMs = System.currentTimeMillis() - lastPacketSentTimestampMs.get();
					}

					if (inPacket.getAckState() != AckStateEnum.SUCCEEDED) {
						// Write out the packet
						sendOutboundPacket(inPacket);
					} else {
						//It is possible for BG Thread to submit a packet to be sent, then whilst waiting for the lock, we receive an ACK.
						//If thats the case just return without updating the lastSentTimestamp. The lock will be released since it's in a finally block.
						LOGGER.info("Not Sending Packet={} that was ACKED", inPacket);
						return;
					}

					// Upate the timestamp
					lastPacketSentTimestampMs.set(inPacket.getSentTimeMillis());

				} finally {
					readLock.unlock();
				}

			}

		}
	}

	/**
	 * Gaurtuneed to return the same timestamp object for every thread.
	 */
	private AtomicLong getLastPacketSentTimestamp(NetAddress remoteAddr) {
		AtomicLong lastIOTimestmapMs = mLastPacketSentTimestampMsMap.get(remoteAddr);
		if (lastIOTimestmapMs == null) {
			// Initialize as current timestamp as 0
			lastIOTimestmapMs = new AtomicLong(0);
			AtomicLong currentValue = mLastPacketSentTimestampMsMap.putIfAbsent(remoteAddr, lastIOTimestmapMs);
			if (currentValue != null) {
				lastIOTimestmapMs = currentValue;
			}
		}
		return lastIOTimestmapMs;
	}

	/**
	 * This method is synchronized because the gateway's thread safety is
	 * unknown
	 */
	public synchronized void sendOutboundPacket(IPacket packet) {
		// Send packet
		packet.incrementSendCount();
		gatewayInterface.sendPacket(packet);
		packet.setSentTimeMillis(System.currentTimeMillis());

		if (packetsSentCounter != null) {
			packetsSentCounter.inc();
		}

		if (packet.getCommand().getCommandTypeEnum() != CommandGroupEnum.NETMGMT) {
			LOGGER.info("Outbound packet={}", packet);
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
							LOGGER.debug("Inbound packet={}; didGetHandled={}", packet, success);
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
