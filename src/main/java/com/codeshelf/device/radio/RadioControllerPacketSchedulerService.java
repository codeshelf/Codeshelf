package com.codeshelf.device.radio;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.AckStateEnum;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioControllerPacketSchedulerService implements Runnable {
	private static final Logger										LOGGER							= LoggerFactory.getLogger(RadioController.class);

	public static final long										NETWORK_PACKET_SPACING_MILLIS	= 10;
	public static final long										DEVICE_PACKET_SPACING_MILLIS	= 20;

	private static final long										ACK_TIMEOUT_MILLIS				= 20;													// matching v16. Used to be 20
	private static final int										ACK_SEND_RETRY_COUNT			= 20;													// matching v16. Used to be 20.
	private static final long										MAX_PACKET_AGE_MILLIS			= 4000;

	public static final int											DEFAULT_QUEUE_SIZE				= 200;

	private final RadioControllerPacketIOService					packetIOService;

	private final ConcurrentMap<NetAddress, BlockingQueue<IPacket>>	mPendingPacketsMap				= Maps.newConcurrentMap();

	private final BlockingQueue<IPacket>							mPendingNetMgmtPacketsQueue		= new ArrayBlockingQueue<IPacket>(DEFAULT_QUEUE_SIZE);

	private AtomicLong												mLastPacketSentTime				= new AtomicLong(System.currentTimeMillis());

	private boolean													shouldRun						= false;

	public RadioControllerPacketSchedulerService(RadioControllerPacketIOService inPacketIOService) {
		this.packetIOService = inPacketIOService;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		shouldRun = true;
		
		deliverPackets();
	}
	
	public void stop() {
		shouldRun = false;
	}


	// --------------------------------------------------------------------------
	/**
	 * Adds a device packet to a queue of out going packets for that device
	 * 
	 * @param inPacket
	 * 			Is the network management packet
	 *  @param inPacket
	 * 			Is the network management packet
	 */
	public void addCommandPacketToSchedule(IPacket inPacket, INetworkDevice inDevice) {
		// Add the packet to the queue of packets to be sent
		BlockingQueue<IPacket> queue = mPendingPacketsMap.get(inDevice.getAddress());
		if (queue == null) {
			queue = new ArrayBlockingQueue<IPacket>(DEFAULT_QUEUE_SIZE);
			BlockingQueue<IPacket> existingQueue = mPendingPacketsMap.putIfAbsent(inDevice.getAddress(), queue);
			if (existingQueue != null) {
				queue = existingQueue;
			}
		}

		// If the pending packet queue for that device is then drop the oldest packet
		boolean success = queue.offer(inPacket);
		while (!success) {
			IPacket packetToDrop = queue.poll();
			LOGGER.warn("Dropping oldest command packet because command packet queue for device is full. Size={}; DroppedPacket={}",
				queue.size(),
				packetToDrop);
			success = queue.offer(inPacket);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Adds a network management packet to the queue of out going
	 * network management packets.
	 * 
	 * @param inPacket
	 * 			Is the network management packet
	 */
	public void addNetMgmtPacketToSchedule(IPacket inPacket) {

		boolean success = mPendingNetMgmtPacketsQueue.offer(inPacket);
		while (!success) {
			IPacket packetToDrop = mPendingNetMgmtPacketsQueue.poll();
			LOGGER.warn("Dropping oldest net mgmt packet because netmgmt packet queue is full. Size={}; DroppedPacket={}",
				mPendingNetMgmtPacketsQueue.size(),
				packetToDrop);
			mPendingNetMgmtPacketsQueue.remove();
			success = mPendingNetMgmtPacketsQueue.offer(inPacket);
		}
	}
	
	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	public void markPacketAsAcked(INetworkDevice inDevice, byte inAckNum, IPacket inAckPacket) {
		BlockingQueue<IPacket> queue;

		queue = mPendingPacketsMap.get(inDevice.getAddress());

		if (queue != null) {
			for (IPacket packet : queue) {
				if (packet != null && packet.getAckId() == inAckNum) {
					queue.remove(packet);
					packet.setAckData(inAckPacket.getAckData());
					packet.setAckState(AckStateEnum.SUCCEEDED);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * 
	 */
	private void deliverPackets() {
		
		while (shouldRun) {
			
			if (mPendingNetMgmtPacketsQueue.size() > 0) {
				deliverNetMgmtPackets();
			}
			
			//FIXME - huffa not sure if this makes sense. We never remove the queues?
			if (mPendingPacketsMap.size() > 0) {
				deliverCommandPackets();
			}
		}
	}

	
	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private void deliverCommandPackets() {
		IPacket packet = null;
		INetworkDevice device = null;
		long timeDiff = 0;

		if (mPendingPacketsMap.size() > 0) {

			for (BlockingQueue<IPacket> queue : mPendingPacketsMap.values()) {

				packet = queue.peek();

				if (packet != null && clearToSendToDevice(packet.getDevice())) {
					if (clearToSendCommandPacket(packet)) {
						sendPacket(packet);
						packet.getDevice().setLastPacketSentTime(System.currentTimeMillis());
						
						// Remove packet that does not require ack
						if (packet.getAckId() == (byte) 0) {
							queue.remove(packet);
						}
						
					} else {
						LOGGER.warn("Removing packet from pending packets");
						queue.remove(packet);
					}
					
					// Stop sending command packets if there are mgmt packets
					if (mPendingNetMgmtPacketsQueue.size() > 0) {
						break;
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private void deliverNetMgmtPackets() {
		long timeDiff;
		
		// Send all the networkMgmt packets that have queued up
		for (IPacket packet : mPendingNetMgmtPacketsQueue) {
			
			timeDiff = System.currentTimeMillis() - mLastPacketSentTime.get();
			
			// Guarantee minimum of DEVICE_PACKET_SPACING_MILLIS between mgmt packets
			if (timeDiff < DEVICE_PACKET_SPACING_MILLIS) {
				try {
					Thread.sleep(DEVICE_PACKET_SPACING_MILLIS - timeDiff);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			}
			
			sendPacket(packet);
			
			//TODO - huffa - Update send time of each device
			updateLastSendTimeOfPendingDevices(System.currentTimeMillis());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private void updateLastSendTimeOfPendingDevices(long currentTimeMillis) {
		IPacket packet = null;
		
		for (BlockingQueue<IPacket> queue : mPendingPacketsMap.values()) {
			packet = queue.peek();
			
			if (packet.getDevice() != null) {
				packet.getDevice().setLastPacketSentTime(currentTimeMillis);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private void sendPacket(IPacket inPacket) {
		long timeDiff;
		
		timeDiff = System.currentTimeMillis() - mLastPacketSentTime.get();
		
		// Guarantee minimum of NETWORK_PACKET_SPACING_MILLIS between all packets
		if (timeDiff < NETWORK_PACKET_SPACING_MILLIS) {
			try {
				Thread.sleep(NETWORK_PACKET_SPACING_MILLIS - timeDiff);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
		

		try {
			packetIOService.handleOutboundPacket(inPacket);
		} finally {
			mLastPacketSentTime.set(System.currentTimeMillis());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	// FIXME - need to consider last time a packet was sent to the network?
	// The issue here is that a netmgmt packet may have been sent to a device that
	// was not in the pending packets list. This means that it may get a mgmt packet
	// and a command packet with only the NETWORK_PACKET_SPACING_MILLIS
	private boolean clearToSendToDevice(INetworkDevice inDevice) {
		long lastReceivedTime = System.currentTimeMillis();
		long lastSentTime = System.currentTimeMillis();
		long minDifference = 0;
		long currTime = System.currentTimeMillis();

		if (inDevice != null) {
			lastReceivedTime = inDevice.getLastPacketReceivedTime();
			lastSentTime = inDevice.getLastPacketSentTime();

			minDifference = Math.min(currTime - lastReceivedTime, currTime - lastSentTime);
		}

		if (minDifference > DEVICE_PACKET_SPACING_MILLIS) {
			return true;
		} else {
			return false;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private boolean clearToSendCommandPacket(IPacket inPacket) {

		if (inPacket.getAckState() == AckStateEnum.SUCCEEDED) {
			LOGGER.warn("Not sending packet - Has been ack'd. Packet {}", inPacket);
			return false;
		}

		// FIXME - huffa not sure if this makes much sense.
		if (System.currentTimeMillis() - inPacket.getSentTimeMillis() < ACK_TIMEOUT_MILLIS) {
			LOGGER.warn("Not sending packet - Timed out waiting for response. Packet {}", inPacket);
			return false;
		}

		if (inPacket.getSendCount() > ACK_SEND_RETRY_COUNT) {
			LOGGER.warn("Not sending packet - Exceeded retry count. Packet {}", inPacket);
			return false;
		}

		if (System.currentTimeMillis() - inPacket.getCreateTimeMillis() > MAX_PACKET_AGE_MILLIS) {
			LOGGER.warn("Not sending packet - Exceeded max packet age. Packet {}", inPacket);
			return false;
		}

		return true;
	}	
}
