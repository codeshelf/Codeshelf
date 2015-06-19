package com.codeshelf.device.radio;
/**
 * 
 * 
 * @author huffa
 */

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.AckStateEnum;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioControllerPacketSchedulerService {
	private static final Logger												LOGGER							= LoggerFactory.getLogger(RadioController.class);

	public static final long												NETWORK_PACKET_SPACING_MILLIS	= 10;
	public static final long												DEVICE_PACKET_SPACING_MILLIS	= 20;

	private static final long												ACK_TIMEOUT_MILLIS				= 20;												// matching v16. Used to be 20
	private static final int												ACK_SEND_RETRY_COUNT			= 20;												// matching v16. Used to be 20.
	private static final long												MAX_PACKET_AGE_MILLIS			= 4000;

	private final RadioControllerPacketIOService							packetIOService;

	private final ConcurrentMap<NetAddress, LinkedBlockingQueue<IPacket>>	mPendingPacketsMap				= Maps.newConcurrentMap();

	private final LinkedBlockingQueue<IPacket>								mPendingNetMgmtPacketsQueue		= new LinkedBlockingQueue<IPacket>();

	private final LinkedBlockingQueue<INetworkDevice>						mDeviceQueue					= new LinkedBlockingQueue<INetworkDevice>();

	private AtomicLong														mLastPacketSentTime				= new AtomicLong(System.currentTimeMillis());

	private final ScheduledExecutorService									packetSendService				= Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("radio-bckgrnd-thread")
																												.build());

	// --------------------------------------------------------------------------
	public RadioControllerPacketSchedulerService(RadioControllerPacketIOService inPacketIOService) {
		this.packetIOService = inPacketIOService;
	}

	// --------------------------------------------------------------------------
	public void start() {

		// Start the packet sending service
		packetSendService.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				deliverPackets();
			}
		}, 0, NETWORK_PACKET_SPACING_MILLIS, TimeUnit.MILLISECONDS);
	}

	// --------------------------------------------------------------------------
	public void stop() {
		packetSendService.shutdown();
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
		if (!mDeviceQueue.contains(inDevice)) {
			mDeviceQueue.offer(inDevice);
		}

		// Add the packet to the queue of packets to be sent
		LinkedBlockingQueue<IPacket> queue = mPendingPacketsMap.get(inDevice.getAddress());
		if (queue == null) {
			queue = new LinkedBlockingQueue<IPacket>();
			LinkedBlockingQueue<IPacket> existingQueue = mPendingPacketsMap.putIfAbsent(inDevice.getAddress(), queue);
			if (existingQueue != null) {
				queue = existingQueue;
			}
		}

		// If the pending packet queue for that device is then drop the oldest packet
		// FIXME - huffa not sure if this makes sense with the LinkedBlockingQueues
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

		// FIXME - huffa not sure if this makes sense with LinkedBlockingQueues
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
		LinkedBlockingQueue<IPacket> queue;

		queue = mPendingPacketsMap.get(inDevice.getAddress());

		synchronized (queue) {
			if (queue != null) {
				for (IPacket packet : queue) {
					if (packet != null && packet.getAckId() == inAckNum) {
						queue.remove(packet);
						packet.setAckData(inAckPacket.getAckData());
						packet.setAckState(AckStateEnum.SUCCEEDED);
					}
				}
	
				// TODO - huffa - If the queue is empty remove it from like map of queues? Remove device from queue of devices
				if (queue.isEmpty()) {
					// Remove the queue from the map of queues
					// XXX - huffa - not sure if this is a good idea
					//mPendingPacketsMap.remove(inDevice.getAddress(), queue);
	
					// Remove device from the queue of devices as it has no pending packets to be sent
					mDeviceQueue.remove(inDevice);
				}
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * 
	 */
	private void deliverPackets() {

		if (!mPendingNetMgmtPacketsQueue.isEmpty()) {
			// If there are management packets send one
			deliverNetMgmtPacket();
		} else if (deviceReadyToSend()) {
			// otherwise if there are devices waiting to send send those
			deliverNextCommandPacket();
		}

	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private boolean deviceReadyToSend() {
		for (INetworkDevice device : mDeviceQueue) {
			if (clearToSendToDevice(device)) {
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private void deliverNextCommandPacket() {
		//INetworkDevice device = null;
		BlockingQueue<IPacket> queue = null;
		IPacket packet = null;

		// Get first device to send
		device = getNextDevice();
		if (device == null) {
			return;
		}

		// Get packet queue for the device
		queue = mPendingPacketsMap.get(device.getAddress());
		if (queue == null) {
			return;
		}

		// Get next packet to send and send
		packet = getNextPacket(queue);

		if (packet != null) {

			// Send packet
			sendPacket(packet);

			// Remove packet if it doesn't require an ACK
			if (packet.getAckId() == 0) {
				queue.remove(packet);
			}

			// Update last send time for device
			device.setLastPacketSentTime(System.currentTimeMillis());
		}

		// If device still has pending packets add to the back of the queue
		if (!queue.isEmpty()) {
			putDeviceInQueue(device);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private boolean putDeviceInQueue(INetworkDevice device) {
		return mDeviceQueue.offer(device);
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private IPacket getNextPacket(BlockingQueue<IPacket> queue) {
		for (IPacket packet : queue) {
			if (clearToSendCommandPacket(packet)) {
				return packet;
			} else {
				queue.remove(packet);
				LOGGER.info("Dropping pending packet {}", packet);
			}
		}
		return null;
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private INetworkDevice getNextDevice() {

		for (INetworkDevice device : mDeviceQueue) {
			if (clearToSendToDevice(device)) {
				// XXX - huffa - will the reference live through the remove?
				mDeviceQueue.remove(device);
				return device;
			}
		}

		return null;
	}

	// --------------------------------------------------------------------------
	/**
	 *	
	 */
	private void deliverNetMgmtPacket() {
		IPacket packet = null;
		
		packet = mPendingNetMgmtPacketsQueue.poll();
		
		if (packet != null) {
			sendPacket(packet);

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
