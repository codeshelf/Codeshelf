package com.codeshelf.device.radio;

/**
 * 
 * 
 * @author huffa
 */

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandGroupEnum;
//import com.codeshelf.flyweight.command.AckStateEnum;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioControllerPacketSchedulerService {
	private static final Logger													LOGGER							= LoggerFactory.getLogger(RadioController.class);

	public static final int														MAX_QUEUED_PACKETS_PER_DEVICE	= 50;
	public static final int														MAX_QUEUED_NET_MGMT_PACKETS		= 50;
	public static final int														MAX_BUFFERED_INCOMING_PACKETS	= 50;
	public static final int														MAX_NUM_QUEUED_DEVICES			= 150;
	public static final long													MAX_QUEUE_OFFER_TIMEOUT_MILLIS	= 5;

	public static final int														MAP_INIT_SIZE					= 50;
	public static final float													MAP_LOAD_FACTOR					= (float) 0.75;												// Default load factor
	public static final int														MAP_CONCURRENCY_LEVEL			= 4;

	public static final long													NETWORK_PACKET_SPACING_MILLIS	= 5;
	public static final long													DEVICE_PACKET_SPACING_MILLIS	= 30;

	//private static final long												ACK_TIMEOUT_MILLIS				= 20;												// matching v16. Used to be 20
	private static final int													ACK_SEND_RETRY_COUNT			= 20;															// matching v16. Used to be 20.
	private static final long													MAX_PACKET_AGE_MILLIS			= 4000;
	private static final long													MAXIMUM_NETCHECK_SPACING_MILLIS = 2000;

	private final RadioControllerPacketIOService								packetIOService;

	// Incoming packet buffering
	//private final ConcurrentLinkedQueue<IPacket>								mPacketsToBeScheduled			= new ConcurrentLinkedQueue<IPacket>();
	//private final ConcurrentLinkedQueue<IPacket>								mAcksToBeScheduled				= new ConcurrentLinkedQueue<IPacket>();

	//ConcurrentLinkedQueue
	// Scheduling Queues
	private final BlockingQueue<IPacket>										mPendingNetMgmtPacketsQueue		= new ArrayBlockingQueue<IPacket>(MAX_QUEUED_NET_MGMT_PACKETS);
	//private final BlockingQueue<INetworkDevice>							mDeviceQueue					= new ArrayBlockingQueue<INetworkDevice>(MAX_NUM_QUEUED_DEVICES);
	//private final BlockingQueue<INetworkDevice>							mSecondDeviceQueue				= new ArrayBlockingQueue<INetworkDevice>(MAX_NUM_QUEUED_DEVICES);
	//private final ConcurrentLinkedQueue<IPacket>										mPendingNetMgmtPacketsQueue		= new ConcurrentLinkedQueue<IPacket>();
	private final ConcurrentLinkedQueue<INetworkDevice>							mDeviceQueue					= new ConcurrentLinkedQueue<INetworkDevice>();
	private final ConcurrentLinkedQueue<INetworkDevice>							mSecondDeviceQueue				= new ConcurrentLinkedQueue<INetworkDevice>();

	// Scheduling data structures
	private final ConcurrentHashMap<NetAddress, ConcurrentLinkedDeque<IPacket>>	mPendingPacketsMap				= new ConcurrentHashMap<NetAddress, ConcurrentLinkedDeque<IPacket>>(MAP_INIT_SIZE,
																													MAP_LOAD_FACTOR,
																													MAP_CONCURRENCY_LEVEL);
	private final ConcurrentHashMap<NetAddress, Byte>							mLastDeviceAckId				= new ConcurrentHashMap<NetAddress, Byte>(MAP_INIT_SIZE,
																													MAP_LOAD_FACTOR,
																													MAP_CONCURRENCY_LEVEL);
	private final ConcurrentHashMap<NetAddress, Integer>						mNumPacketsQueuedMap			= new ConcurrentHashMap<NetAddress, Integer>(MAP_INIT_SIZE,
																													MAP_LOAD_FACTOR,
																													MAP_CONCURRENCY_LEVEL);

	private AtomicLong															mLastPacketSentTime				= new AtomicLong(System.currentTimeMillis());
	private long																mLastNetCheckSentTime			= System.currentTimeMillis();

	// Scheduling threads
	private final ScheduledExecutorService										packetSendService				= Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("pckt-schd")
																													.build());

	private final ExecutorService												executor						= Executors.newFixedThreadPool(2,
																													new ThreadFactoryBuilder().setNameFormat("pckt-hndlr-%s")
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
		LOGGER.info("RadioControllerPacketSchedulerSerivce shutting down.");
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

		// Add the packet to the queue of packets to be sent
		ConcurrentLinkedDeque<IPacket> deque = mPendingPacketsMap.get(inDevice.getAddress());
		if (deque == null) {
			deque = new ConcurrentLinkedDeque<IPacket>();
			mPendingPacketsMap.put(inDevice.getAddress(), deque);
		}

		// Add new packet to the end of the queue
		deque.addLast(inPacket);
		if (!deviceIsQueued(inDevice)) {
			addDeviceToQueue(mDeviceQueue, inDevice);
		}
	}

	public void addAckPacketToSchedule(IPacket inPacket, INetworkDevice inDevice) {

		// Add the packet to the queue of packets to be sent
		ConcurrentLinkedDeque<IPacket> deque = mPendingPacketsMap.get(inDevice.getAddress());
		if (deque == null) {
			deque = new ConcurrentLinkedDeque<IPacket>();
			mPendingPacketsMap.put(inDevice.getAddress(), deque);
		}

		// Need to send ACKs before other packets so add to the front of queue
		deque.addFirst(inPacket);
		if (!deviceIsQueued(inDevice)) {
			addDeviceToQueue(mDeviceQueue, inDevice);
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
	// FIXME - huffa refactor this and maybe make it a linked queue
	public void addNetMgmtPacketToSchedule(IPacket inPacket) {

		boolean success = addPacketToQueue(mPendingNetMgmtPacketsQueue, inPacket);

		// If the pending packet queue is full drop the oldest
		while (!success) {
			if (!mPendingNetMgmtPacketsQueue.isEmpty()) {
				IPacket packetToDrop = mPendingNetMgmtPacketsQueue.poll();
				LOGGER.warn("Dropping oldest net mgmt packet because netmgmt packet queue is full. Size={}; DroppedPacket={}",
					mPendingNetMgmtPacketsQueue.size(),
					packetToDrop);
				success = mPendingNetMgmtPacketsQueue.add(inPacket);
			} else {
				LOGGER.warn("Error adding packet to queue. Dropping packet: {}", inPacket.toString());
				return;
			}
		}

	}

	// --------------------------------------------------------------------------
	/**
	 *	Mark a queued packet as having been acknowledged. Will remove from sending queue.
	 *
	 * @param inDevice - Device associated with packet
	 * @param inAckNum - Ack number of packet
	 * @param inAckPacket - Ack packet from device
	 */
	public void markPacketAsAcked(INetworkDevice inDevice, byte inAckNum, IPacket inAckPacket) {

		mLastDeviceAckId.put(inDevice.getAddress(), inAckNum);

	}

	// --------------------------------------------------------------------------
	/**
	 * Try to send packets to the network. Send management packets first if there are any queued.
	 * 
	 */
	private void deliverPackets() {

		
		// FIXME - huffa must guarantee a minimum number of net checks
		if (!mPendingNetMgmtPacketsQueue.isEmpty()) {
			// If there are management packets send one
			//LOGGER.warn("~~~~ Net mgmt packet len: {}", mPendingNetMgmtPacketsQueue.size());
			deliverNetMgmtPacket();
		} else if (!mDeviceQueue.isEmpty() || !mSecondDeviceQueue.isEmpty()) {
			// otherwise if there are devices waiting to send send those
			//LOGGER.info("Device queue:", mDeviceQueue.toString());
			//LOGGER.info("Second Device queue:", mSecondDeviceQueue.toString());
			//LOGGER.info("{}", mDeviceQueue.toString());
			deliverNextCommandPacket();
		}
		

//		if ((!mDeviceQueue.isEmpty() || !mSecondDeviceQueue.isEmpty()) /*&& mustSendNetCheck()*/) {
//			// otherwise if there are devices waiting to send send those
//			deliverNextCommandPacket();
//		} else if (!mPendingNetMgmtPacketsQueue.isEmpty()) {
//			// If there are management packets send one
//		//	LOGGER.warn("~~~~ Net mgmt packet len: {}", mPendingNetMgmtPacketsQueue.size());
//			deliverNetMgmtPacket();
//		}

	}
	
	private boolean mustSendNetCheck() {
		long currTime = System.currentTimeMillis();
		long diff = currTime - mLastNetCheckSentTime;
		
		if (diff >= MAXIMUM_NETCHECK_SPACING_MILLIS) {
			return true;
		} else {
			return false;
		}
		
	}

	// --------------------------------------------------------------------------
	/**
	 *	Deliver the next packet to next eligible device.
	 */
	private void deliverNextCommandPacket() {
		INetworkDevice device = null;
		IPacket packet = null;

		// Get next eligible device to send to
		device = getNextDevice();
		if (device == null) {
			// Although there are queued devices non are eligible to be sent to
			//LOGGER.warn("~~ Device was null");
			return;
		}

		// Get next packet to send and send
		packet = getNextPacketForDevice(device);

		if (packet != null) {
			LOGGER.info("Sending packet");
			// Send packet
			sendPacket(packet);

			// Update last send time for device
			device.setLastPacketSentTime(System.currentTimeMillis());
		} else {
			//LOGGER.warn("~~ Packet was null");
		}

		// If device still has pending packets add back to the queue
		if (deviceHasPendingPackets(device)) {
			addDeviceToQueue(mDeviceQueue, device);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Get the next eligible packet in queue.
	 */
	// FIXME - HUFFA might want to look at something better than this for loop
	// synchronizing on the queue isn't the best :(
	private IPacket getNextPacketForDevice(INetworkDevice device) {

		int size = 0;
		IPacket packet = null;
		ConcurrentLinkedDeque<IPacket> devicePacketdeque = mPendingPacketsMap.get(device.getAddress());

		if (devicePacketdeque == null) {
			return null;
		}

		while (!devicePacketdeque.isEmpty()) {

			packet = devicePacketdeque.peek();

			if (clearToSendCommandPacket(packet)) {

				// Remove packet from queue if it does not require an ack
				if (!packet.getRequiresAck()) {
					//devicePacketdeque.removeFirst();
					devicePacketdeque.pollFirst();
				}

				return packet;
			} else {
				LOGGER.warn("Droping packet from pending queue: {}", packet);
				//devicePacketdeque.removeFirst();
				devicePacketdeque.pollFirst();
			}

		}
		/*
		synchronized (devicePacketdeque) {
			// Size is not guaranteed to be accurate concurrentdeque as requires
			// an iteration over entire deque
			size = devicePacketdeque.size();
		}

		// Find next eligible packet
		for (int i = 0; i < size; i++) {
			packet = devicePacketdeque.peek();

			if (clearToSendCommandPacket(packet)) {

				// Remove packet from queue if it does not require an ack
				if (!packet.getRequiresAck()) {
					//devicePacketdeque.removeFirst();
					devicePacketdeque.pollFirst();
				}
				
				return packet;
			} else {
				LOGGER.warn("Droping packet from pending queue: {}", packet);
				//devicePacketdeque.removeFirst();
				devicePacketdeque.pollFirst();
			}
		}*/

		return null;
	}

	private boolean deviceIsQueued(INetworkDevice device) {
		/*
		if ( mDeviceQueue.contains(device)) {
			return true;
		}
		
		if (mSecondDeviceQueue.contains(device)) {
			return true;
		}
		*/

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Check if device has more pending packets
	 */
	private boolean deviceHasPendingPackets(INetworkDevice device) {

		ConcurrentLinkedDeque<IPacket> devicePacketQueue = mPendingPacketsMap.get(device.getAddress());

		if (devicePacketQueue != null && !devicePacketQueue.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Get the next eligible device in queue.
	 */
	private INetworkDevice getNextDevice() {
		INetworkDevice device = null;

		// Check if the overflow queue has packets to send
		// FIXME - huffa need to check how many times the head of this list has been checked.
		if (!mSecondDeviceQueue.isEmpty()) {

			if (clearToSendToDevice(mSecondDeviceQueue.peek())) {
				device = mSecondDeviceQueue.poll();
				return device;
			}
		}

		// Find next eligible device
		for (int i = 0; i < mDeviceQueue.size(); i++) {

			device = mDeviceQueue.poll();

			if (!clearToSendToDevice(device)) {
				mSecondDeviceQueue.offer(device);
			} else {
				return device;
			}
		}

		return null;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Deliver the next netMgmtPacket.
	 */
	private void deliverNetMgmtPacket() {
		IPacket packet = null;

		try {
			packet = mPendingNetMgmtPacketsQueue.poll(1, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}

		//packet = mPendingNetMgmtPacketsQueue.poll();

		if (packet != null) {
			sendPacket(packet);
			updateLastSendTimeOfPendingDevices(System.currentTimeMillis());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Update the 'last send time' of all the devices. Indicated the last time a packet
	 *	was sent to the devices. Typical usage after sending net mgmt commands.
	 */
	private void updateLastSendTimeOfPendingDevices(long currentTimeMillis) {
		mLastNetCheckSentTime = System.currentTimeMillis();
	}

	// --------------------------------------------------------------------------
	/**
	 *	Send a single packet to the packet IO service
	 */
	private void sendPacket(IPacket inPacket) {
		LOGGER.warn("Sending packet with ack id: {}", inPacket.getAckId());
		try {
			packetIOService.handleOutboundPacket(inPacket);
		} finally {
			mLastPacketSentTime.set(System.currentTimeMillis());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Check if the device is ready to receive another packet
	 */

	private boolean clearToSendToDevice(INetworkDevice inDevice) {

		long lastReceivedTime = System.currentTimeMillis();
		long lastSentTime = System.currentTimeMillis();
		long minDifference = 0;
		long currTime = System.currentTimeMillis();

		if (inDevice != null) {
			lastReceivedTime = inDevice.getLastPacketReceivedTime();
			lastSentTime = Math.max(inDevice.getLastPacketSentTime(), mLastNetCheckSentTime);

			minDifference = Math.min(currTime - lastReceivedTime, currTime - lastSentTime);
		}

		if (minDifference < DEVICE_PACKET_SPACING_MILLIS) {
			return false;
		} else {
			return true;
		}

	}

	// --------------------------------------------------------------------------
	/**
	 *	Check if it is clear to send packet to device. Logic exists here to drop packets
	 *	from the queue.
	 */
	private boolean clearToSendCommandPacket(IPacket inPacket) {
		
		if(inPacket.getCommand().getCommandTypeEnum() == CommandGroupEnum.ASSOC) {
			return true;
		}
		
		if(inPacket.getCommand().getCommandTypeEnum() == CommandGroupEnum.NETMGMT) {
			return true;
		}

		if (!packetOlderThanLastAcked(inPacket)) {
			LOGGER.warn("Not sending packet - Has been ack'd. {} Packet {}", inPacket.getAckId(), inPacket.toString());
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

	// --------------------------------------------------------------------------
	/**
	 * Check if packet has been acked or perhaps skipped
	 */
	// FIXME - huffa
	// Could be dangerous if we are on the roll over and loose more than
	// ten packets. Should probably update the map when we drop packets
	// due to timeouts etc
	private boolean packetOlderThanLastAcked(IPacket packet) {
		Byte lastAck = 0;
		byte packetAckId = 0;
		INetworkDevice device = null;
		
		int packetAckIdUnsigned = 0;
		int lastAckIdUnsigned = 0;

		device = packet.getDevice();
		packetAckId = packet.getAckId();
		lastAck = mLastDeviceAckId.get(device.getAddress());
		
		packetAckIdUnsigned = packetAckId & 0xFF;
		lastAckIdUnsigned = lastAck.byteValue() & 0xFF;

		LOGGER.warn("Last packetId({}) > lastAckId({})?", packetAckIdUnsigned, lastAckIdUnsigned);
		
		if (packetAckId == (byte)0) {
			return true;
		}
		
		if (lastAck == null || lastAckIdUnsigned == 0) {
			//mLastDeviceAckId.put(device.getAddress(), (byte)0);
			LOGGER.warn("-- returning -1");
			return true;
		}

		if ((lastAckIdUnsigned >= 254) && (packetAckIdUnsigned < 10)) {
			LOGGER.warn("-- returning 1");
			return true;
		} else if (packetAckIdUnsigned > lastAckIdUnsigned) {
			LOGGER.warn("-- returning 2");
			return true;
		} else {
			LOGGER.warn("-- returning 3");
			return false;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Add a packet to a queue.
	 */
	private boolean addPacketToQueue(Queue<IPacket> queue, IPacket inPacket) {
		boolean success = false;

		try {
			success = queue.add(inPacket);
		} catch (IllegalStateException e) {
			queue.clear();
		} catch (Exception e) {
			LOGGER.error("Exception {}", e);
		}

		return success;
	}

	// --------------------------------------------------------------------------
	/**
	 * Add a device to a queue.
	 */
	private boolean addDeviceToQueue(Queue<INetworkDevice> queue, INetworkDevice inDevice) {
		boolean success = false;

		try {
			success = queue.add(inDevice);
		} catch (IllegalStateException e) {
			LOGGER.warn("Adding to queue error. Clearing Queue. Exception: {}", e);
		} catch (Exception e) {
			LOGGER.error("Exception {}", e);
		}

		return success;
	}

	// --------------------------------------------------------------------------
	/**
	 * Remove all pending packets from a devices queue. Typical usage is when a device
	 * re-associates to the site controller.
	 */
	public void clearDevicePacketQueue(INetworkDevice foundDevice) {
		if (foundDevice == null) {
			return;
		}

		ConcurrentLinkedDeque<IPacket> deviceQueue = mPendingPacketsMap.get(foundDevice.getAddress());
		mLastDeviceAckId.put(foundDevice.getAddress(), (byte) 0);

		if (deviceQueue != null) {
			deviceQueue.clear();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Remove a device from the queue of devices. Typical usual is when a device is
	 * deleted from the backend.
	 */
	public void removeDevice(INetworkDevice inNetworkDevice) {

		if (inNetworkDevice == null) {
			return;
		}

		mDeviceQueue.remove(inNetworkDevice);
		mSecondDeviceQueue.remove(inNetworkDevice);
		mPendingPacketsMap.remove(inNetworkDevice.getAddress());
		mLastDeviceAckId.remove(inNetworkDevice.getAddress());
	}
}
