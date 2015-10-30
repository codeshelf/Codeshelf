package com.codeshelf.device.radio;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * 
 * 
 * @author huffa
 */

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.CommandControlABC;
import com.codeshelf.flyweight.command.CommandGroupEnum;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioControllerPacketSchedulerService {
	private static final Logger													LOGGER							= LoggerFactory.getLogger(RadioControllerPacketSchedulerService.class);

	public static final int														MAX_QUEUED_PACKETS_PER_DEVICE	= 50;
	public static final int														MAX_QUEUED_NET_MGMT_PACKETS		= 50;
	public static final int														MAX_NUM_QUEUED_DEVICES			= 150;
	public static final int														MAX_NUM_BLOCKING_PEEKS			= 2;

	public static final int														MAP_INIT_SIZE					= 50;
	public static final float													MAP_LOAD_FACTOR					= (float) 0.75;														// Default Java load factor
	public static final int														MAP_CONCURRENCY_LEVEL			= 4;

	public static final long													NETWORK_PACKET_SPACING_MILLIS	= 3;
	public static final long													DEVICE_PACKET_SPACING_MILLIS	= 30;
	public static final long													DEVICE_ASSOC_SPACING_MILLIS		= 0;

	private static final int													ACK_SEND_RETRY_COUNT			= 20;																	// matching v16. Used to be 20.
	private static final long													MAX_PACKET_AGE_MILLIS			= 8000;

	// DEV-1240 These yield log info messages when hit 
	private static final int													ACK_LOG_MANY_RETRY_1			= 8;
	private static final int													ACK_LOG_MANY_RETRY_2			= 15;

	private final RadioControllerPacketIOService								packetIOService;

	// FIXME - Huffa - It might be interesting to test this with a blocking queues rather than linked ones.
	// Scheduling Queues
	private final BlockingQueue<IPacket>										mPendingNetMgmtPacketsQueue		= new ArrayBlockingQueue<IPacket>(MAX_QUEUED_NET_MGMT_PACKETS);
	private final ConcurrentLinkedQueue<INetworkDevice>							mDeviceQueue					= new ConcurrentLinkedQueue<INetworkDevice>();
	//private final ConcurrentLinkedQueue<INetworkDevice>							mSecondDeviceQueue				= new ConcurrentLinkedQueue<INetworkDevice>();
	private final ConcurrentLinkedDeque<INetworkDevice>							mSecondDeviceQueue				= new ConcurrentLinkedDeque<INetworkDevice>();

	// Scheduling data structures
	private final ConcurrentHashMap<NetAddress, ConcurrentLinkedDeque<IPacket>>	mPendingPacketsMap				= new ConcurrentHashMap<NetAddress, ConcurrentLinkedDeque<IPacket>>(MAP_INIT_SIZE,
																													MAP_LOAD_FACTOR,
																													MAP_CONCURRENCY_LEVEL);
	private final ConcurrentHashMap<NetAddress, Byte>							mLastDeviceAckId				= new ConcurrentHashMap<NetAddress, Byte>(MAP_INIT_SIZE,
																													MAP_LOAD_FACTOR,
																													MAP_CONCURRENCY_LEVEL);
	private final ConcurrentHashMap<NetAddress, Integer>						mDeviceBlockingPeekCount		= new ConcurrentHashMap<NetAddress, Integer>(MAP_INIT_SIZE,
																													MAP_LOAD_FACTOR,
																													MAP_CONCURRENCY_LEVEL);

	private AtomicLong															mLastPacketSentTime				= new AtomicLong(System.currentTimeMillis());
	private long																mLastNetCheckSentTime			= System.currentTimeMillis();

	// Scheduling threads
	private final ScheduledExecutorService										packetSendService				= Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("pckt-schd")
																													.setPriority(Thread.MAX_PRIORITY)
																													.build());

	// Statistics collection
	private static final int													REPORT_INTERVAL_SECS			= 60 * 5;
	private packetSchedulerStatsCollector										statsCollector;
	private final ScheduledExecutorService										schedulerReportService			= Executors.newScheduledThreadPool(1);

	// --------------------------------------------------------------------------
	public RadioControllerPacketSchedulerService(RadioControllerPacketIOService inPacketIOService) {
		this.packetIOService = inPacketIOService;
		statsCollector = new packetSchedulerStatsCollector();
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

		schedulerReportService.scheduleAtFixedRate(statsCollector, REPORT_INTERVAL_SECS, REPORT_INTERVAL_SECS, TimeUnit.SECONDS);
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

		NetAddress deviceAddr = inDevice.getAddress();
		if (deviceAddr == null) {
			LOGGER.warn("Attempting to schedule packet for device without net address: {}", inDevice);
			return;
		}

		// Add the packet to the queue of packets to be sent
		ConcurrentLinkedDeque<IPacket> deque = mPendingPacketsMap.get(deviceAddr);
		if (deque == null) {
			deque = new ConcurrentLinkedDeque<IPacket>();
			mPendingPacketsMap.put(deviceAddr, deque);
		}

		// Add new packet to the end of the queue
		deque.addLast(inPacket);
		addDeviceToQueue(mDeviceQueue, inDevice);
		statsCollector.updateQueueStats(deque.size());
		//LOGGER.warn("Device queue: {}", mDeviceQueue.toString());
	}

	// --------------------------------------------------------------------------
	/**
	 * Adds an ack packet to the schedule. Puts the ack packet at the front of the
	 * queue so that it is sent before any other commands.
	 * 
	 * @param inPacket
	 * 			Is the ack packet
	 * @param inDevice
	 * 			Device to send ack to
	 */
	public void addAckPacketToSchedule(IPacket inPacket, INetworkDevice inDevice) {
		NetAddress deviceAddr = inDevice.getAddress();
		if (deviceAddr == null) {
			LOGGER.warn("Attempting to schedule packet for device without net address: {}", inDevice);
			return;
		}

		// Add the packet to the queue of packets to be sent
		ConcurrentLinkedDeque<IPacket> deque = mPendingPacketsMap.get(deviceAddr);
		if (deque == null) {
			deque = new ConcurrentLinkedDeque<IPacket>();
			mPendingPacketsMap.put(deviceAddr, deque);
		}

		// Need to send ACKs before other packets so add to the front of queue
		deque.addFirst(inPacket);
		addDeviceToQueue(mDeviceQueue, inDevice);
		//LOGGER.warn("Device queue: {}", mDeviceQueue.toString());
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
		NetAddress deviceAddr = inDevice.getAddress();
		if (deviceAddr == null) {
			LOGGER.warn("Attempting to ack packet for device without net address: {}", inDevice);
			return;
		}

		mLastDeviceAckId.put(deviceAddr, inAckNum);
	}

	// --------------------------------------------------------------------------
	/**
	 *	Mark a queued packet as having been acknowledged. Will remove from sending queue.
	 *
	 * @param inDevice - Device associated with packet
	 * @param inAckNum - Ack number of packet
	 */
	public void markPacketAsAcked(INetworkDevice inDevice, byte inAckNum) {
		mLastDeviceAckId.put(inDevice.getAddress(), inAckNum);
	}

	// --------------------------------------------------------------------------
	/**
	 * Try to send packets to the network. Send management packets first if there are any queued.
	 * 
	 */
	private void deliverPackets() {

		try {
			if (!mPendingNetMgmtPacketsQueue.isEmpty()) {
				deliverNetMgmtPacket();
			} else if (!mDeviceQueue.isEmpty() || !mSecondDeviceQueue.isEmpty()) {
				deliverNextCommandPacket();
			}
		} catch (Exception e) {
			LOGGER.error("deliverPackets() {}", e.toString());
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
			LOGGER.debug("No eligible devices to send to");
			return;
		}

		// Get next packet to send and send
		packet = getNextPacketForDevice(device);

		if (packet != null) {
			// Increment stats
			if (packet.getSendCount() == 0) {
				statsCollector.incrementPacketsSent();
			}

			// Send packet
			sendPacket(packet);

			// Update last send time for device
			device.setLastPacketSentTime(System.currentTimeMillis());
			device.setLastSentPacket(packet);
		} else {
			LOGGER.debug("No packet to send");
		}

		// If device still has pending packets add back to the queue
		if (deviceHasPendingPackets(device)) {
			addDeviceToQueue(mDeviceQueue, device);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Get the next eligible packet in queue.
	 *
	 *	@param inDevice
	 *		Device to fetch next eligible packet for
	 */
	private IPacket getNextPacketForDevice(INetworkDevice inDevice) {
		IPacket packet = null;
		ConcurrentLinkedDeque<IPacket> devicePacketdeque = mPendingPacketsMap.get(inDevice.getAddress());

		if (devicePacketdeque == null) {
			return null;
		}

		while (!devicePacketdeque.isEmpty()) {

			packet = devicePacketdeque.peek();

			if (clearToSendCommandPacket(packet, inDevice)) {

				// Remove packet from queue if it does not require an ack
				if (!packet.getRequiresAck()) {
					devicePacketdeque.pollFirst();
				}

				return packet;
			} else {
				LOGGER.debug("Removing packet from pending queue: {}", packet.toString());
				devicePacketdeque.pollFirst();
			}

		}

		return null;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Check if device has more pending packets
	 *
	 *	@param inDevice
	 *		Device to check if it has packets
	 */
	private boolean deviceHasPendingPackets(INetworkDevice inDevice) {

		ConcurrentLinkedDeque<IPacket> devicePacketQueue = mPendingPacketsMap.get(inDevice.getAddress());

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
		int peekCount = 0;

		// Check if the overflow queue has packets to send
		// Check how many times the head of this list has been checked.
		if (!mSecondDeviceQueue.isEmpty()) {
			device = mSecondDeviceQueue.peek();
			//device = mSecondDeviceQueue.poll();
			if (mDeviceBlockingPeekCount.containsKey(device.getAddress())) {
				peekCount = mDeviceBlockingPeekCount.get(device.getAddress());
			}

			if (clearToSendToDevice(device)) {
				device = mSecondDeviceQueue.poll();
				mDeviceBlockingPeekCount.put(device.getAddress(), 0);
				//LOGGER.warn("Returning device");
				return device;
			} else {
				mDeviceBlockingPeekCount.put(device.getAddress(), peekCount = peekCount + 1);
			}

			if (peekCount > 3) {
				device = mSecondDeviceQueue.poll();
				mSecondDeviceQueue.offer(device);
				//mDeviceBlockingPeekCount.put(device.getAddress(), 0);
				//device = mSecondDeviceQueue.poll();
				//INetworkDevice currHead = mSecondDeviceQueue.poll();
				//mSecondDeviceQueue.offerFirst(device);
				//mSecondDeviceQueue.offerFirst(currHead);
			}
		}

		while (!mDeviceQueue.isEmpty()) {
			device = mDeviceQueue.poll();

			if (!clearToSendToDevice(device)) {
				mDeviceBlockingPeekCount.put(device.getAddress(), 0);
				mSecondDeviceQueue.offer(device);
			} else {
				//LOGGER.warn("Returning device");
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

		if (packet != null) {
			sendPacket(packet);
			updateLastSendTimeOfPendingDevices(System.currentTimeMillis());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Update the 'last send time' of all the devices. Indicated the last time a packet
	 *	was sent to the devices. Typical usage after sending net mgmt commands.
	 *
	 *	@param inTime
	 *		The time to set
	 */
	private void updateLastSendTimeOfPendingDevices(long inTime) {
		mLastNetCheckSentTime = inTime;
	}

	// --------------------------------------------------------------------------
	/**
	 *	Send a single packet to the packet IO service
	 *
	 *	@param inPacket
	 *		Packet to send
	 */
	private void sendPacket(IPacket inPacket) {
		//LOGGER.info("Sending packet {}", inPacket.toString());
		try {
			packetIOService.handleOutboundPacket(inPacket);
		} finally {
			mLastPacketSentTime.set(System.currentTimeMillis());
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Check if the device is ready to receive another packet
	 *
	 *	@param inDevice
	 *		Device to check
	 */

	private boolean clearToSendToDevice(INetworkDevice inDevice) {

		long lastReceivedTime = 0;
		long lastSentTime = 0;
		long minDifference = 0;
		long currTime = System.currentTimeMillis();
		int packetDelayRequirement = 0;
		IPacket packet = null;
		ConcurrentLinkedDeque<IPacket> devicePacketdeque;
		
		if (inDevice == null) {
			return false;
		}
		
		devicePacketdeque = mPendingPacketsMap.get(inDevice.getAddress());
		if (devicePacketdeque != null) {
			packet = devicePacketdeque.peek();
		}
		
		// It's always ok to send ack messages
		if (packet != null && packet.getCommand().getCommandTypeEnum() == CommandGroupEnum.CONTROL) {
			CommandControlABC command = (CommandControlABC) packet.getCommand();

			if (command.getExtendedCommandID().getValue() == CommandControlABC.ACK) {
				return true;
			}
		}
		
		IPacket lastPacketSent = inDevice.getLastSentPacket();
		if(lastPacketSent != null) {
			packetDelayRequirement = lastPacketSent.getCommand().getResendDelay();
		}

		lastReceivedTime = 0; //inDevice.getLastPacketReceivedTime();
		lastSentTime = Math.max(inDevice.getLastPacketSentTime(), mLastNetCheckSentTime);
		minDifference = Math.min(currTime - lastReceivedTime, currTime - lastSentTime);
		//5
		//LOGGER.warn("LS: {}, LR: {}, LNChk: {}, Min Diff: {}", inDevice.getLastPacketSentTime(), inDevice.getLastPacketReceivedTime(),mLastNetCheckSentTime, minDifference);
		if (isNextPacketAssocCmd(inDevice)) {
			if (minDifference < DEVICE_ASSOC_SPACING_MILLIS) {
				return false;
			} else {
				return true;
			}
		} else {
			if (minDifference < DEVICE_PACKET_SPACING_MILLIS || minDifference < packetDelayRequirement) {
				return false;
			} else {
				return true;
			}
		}

	}

	private boolean isNextPacketAssocCmd(INetworkDevice inDevice) {
		if (inDevice == null) {
			return false;
		}

		ConcurrentLinkedDeque<IPacket> deviceQueue = mPendingPacketsMap.get(inDevice.getAddress());

		if (deviceQueue == null) {
			return false;
		}

		IPacket headPacket = deviceQueue.peekFirst();

		if (headPacket == null) {
			return false;
		}

		if (headPacket.getCommand().getCommandTypeEnum() == CommandGroupEnum.ASSOC) {
			return true;
		} else {
			return false;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *	Check if it is clear to send packet to device. Logic exists here to drop packets
	 *	from the queue.
	 *
	 *	@param inPacket
	 *		Packet to check
	 *	@param inDevice
	 *		Informative only. Used for error logging
	 */
	private boolean clearToSendCommandPacket(IPacket inPacket, INetworkDevice inDevice) {

		if (inPacket.getCommand().getCommandTypeEnum() == CommandGroupEnum.CONTROL) {
			CommandControlABC command = (CommandControlABC) inPacket.getCommand();

			if (command.getExtendedCommandID().getValue() == CommandControlABC.ACK) {
				return true;
			}
		}

		if (inPacket.getCommand().getCommandTypeEnum() == CommandGroupEnum.ASSOC) {
			return true;
		}

		if (inPacket.getCommand().getCommandTypeEnum() == CommandGroupEnum.NETMGMT) {
			return true;
		}

		if (!packetNewerThanLastAcked(inPacket)) {
			LOGGER.debug("Not sending packet - Has been ack'd. {} Packet {}", inPacket.getAckId(), inPacket.toString());
			statsCollector.updateResendStats(inPacket.getSendCount());
			return false;
		}

		int retryNumber = inPacket.getSendCount();
		if (retryNumber > ACK_SEND_RETRY_COUNT) {
			LOGGER.warn("Not sending packet to {} - Exceeded retry count. (Not acked yet, but sometimes the message made it.) {}",
				getLoggableGuidStr(inDevice),
				inPacket);
			statsCollector.incrementDroppedCount();
			return false;
		}

		if (System.currentTimeMillis() - inPacket.getCreateTimeMillis() > MAX_PACKET_AGE_MILLIS) {
			LOGGER.warn("Not sending packet to {} - Exceeded max packet age. Packet {}", getLoggableGuidStr(inDevice), inPacket);
			statsCollector.incrementDroppedCount();
			return false;
		}

		// DEV-1240 Help us understand a little better if we are doing lots of retries
		if (retryNumber == ACK_LOG_MANY_RETRY_1 || retryNumber == ACK_LOG_MANY_RETRY_2) {
			logManyRetries(retryNumber, inDevice, inPacket); // but still return true
		}

		return true;
	}

	private String getLoggableGuidStr(INetworkDevice inDevice) {
		String guidStr = "?";
		if (inDevice == null) {
			LOGGER.error("null device in getLoggableGuidStr()"); // would we want a stack trace here?
			return guidStr;
		}
		NetGuid addr = inDevice.getGuid();
		if (addr != null)
			guidStr = addr.getHexStringNoPrefix();
		return guidStr;
	}

	private void logManyRetries(int howMany, INetworkDevice inDevice, IPacket inPacket) {
		NetGuid addr = inDevice.getGuid();
		String guidStr = "?";
		if (addr != null)
			guidStr = addr.getHexStringNoPrefix();
		LOGGER.warn("Retry number {} for {}; packet:{}", howMany, guidStr, inPacket);
	}

	// --------------------------------------------------------------------------
	/**
	 * Check if packet has been acked or perhaps skipped
	 * 
	 * 	@param inPacket
	 * 		Packet to check
	 */
	// Could be dangerous if we are on the roll over and loose more than
	// ten packets. Should probably update the map when we drop packets
	// due to timeouts etc
	private boolean packetNewerThanLastAcked(IPacket inPacket) {
		Byte lastAck = 0;
		byte packetAckId = 0;
		INetworkDevice device = null;
		int packetAckIdUnsigned = 0;
		int lastAckIdUnsigned = 0;

		device = inPacket.getDevice();
		packetAckId = inPacket.getAckId();
		lastAck = mLastDeviceAckId.get(device.getAddress());

		packetAckIdUnsigned = packetAckId & 0xFF;

		if (lastAck != null) {
			lastAckIdUnsigned = lastAck.byteValue() & 0xFF;
		} else {
			return true;
		}

		if (packetAckIdUnsigned == 0) {
			return true;
		}

		if ((lastAckIdUnsigned >= 254) && (packetAckIdUnsigned < 10)) {
			return true;
		} else if (packetAckIdUnsigned > lastAckIdUnsigned) {
			return true;
		} else {
			return false;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Add a packet to a queue.
	 * 
	 * 	@param queue
	 * 		Queue to add packet to
	 * 	@param inPacket
	 * 		Packet to add to queue
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
	 * 
	 * 	@param queue
	 * 		Queue to add device to
	 * 	@param inDevice
	 * 		Device to add to queue
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
	 * re-associates to the site controller. Also resets the 'last ack id' to 1 that
	 * the packet scheduler keeps tabs on.
	 * 
	 *	@param inDevice
	 *		Device to clear packet queue 
	 */
	public void clearDevicePacketQueue(INetworkDevice inDevice) {

		NetAddress deviceAddr;

		if (inDevice == null) {
			return;
		}

		deviceAddr = inDevice.getAddress();
		if (deviceAddr == null) {
			LOGGER.warn("Trying to clear packet schedule queue for device without net addres: {}", inDevice);
			return;
		}

		LOGGER.debug("Clearing device {} queue and resetting last ack number", inDevice.getAddress().toString());
		ConcurrentLinkedDeque<IPacket> deviceQueue = mPendingPacketsMap.get(inDevice.getAddress());
		mLastDeviceAckId.put(inDevice.getAddress(), (byte) 1);

		inDevice.setLastPacketSentTime(System.currentTimeMillis());

		if (deviceQueue != null) {
			deviceQueue.clear();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Remove a device from the queue of devices. Typical usual is when a device is
	 * deleted from the backend.
	 * 
	 * 	@param inDevice
	 * 		Device to remove from scheduling
	 */
	public void removeDevice(INetworkDevice inDevice) {
		NetAddress deviceAddr;

		if (inDevice == null) {
			return;
		}

		deviceAddr = inDevice.getAddress();
		if (deviceAddr == null) {
			LOGGER.warn("Trying remove device from schedule without a net address {}", inDevice);
			return;
		}

		LOGGER.debug("Removing device from packet scheduling service {}", inDevice.toString());
		mDeviceQueue.remove(inDevice);
		mSecondDeviceQueue.remove(inDevice);
		mPendingPacketsMap.remove(inDevice.getAddress());
		mLastDeviceAckId.remove(inDevice.getAddress());
	}

	public void updateSendingStats() {

	}

	private class packetSchedulerStatsCollector implements Runnable {

		private Logger	LOGGER		= LoggerFactory.getLogger(RadioControllerPacketSchedulerService.class);

		AtomicInteger	totalPacketsSent;
		AtomicInteger	sumResends, maxResends, minResends;
		AtomicInteger	sumDropped;

		AtomicInteger	sumQueueDepth, maxQueueDepth, minQueueDepth;
		AtomicInteger	queueSampleSize;

		long			st			= 0;
		NumberFormat	formatter	= new DecimalFormat("#0.00");

		packetSchedulerStatsCollector() {

			totalPacketsSent = new AtomicInteger(0);
			sumResends = new AtomicInteger(0);
			maxResends = new AtomicInteger(0);
			minResends = new AtomicInteger(0);
			sumDropped = new AtomicInteger(0);

			sumQueueDepth = new AtomicInteger(0);
			maxQueueDepth = new AtomicInteger(0);
			minQueueDepth = new AtomicInteger(0);
			queueSampleSize = new AtomicInteger(0);

			st = System.currentTimeMillis();
		}

		@Override
		public void run() {
			LOGGER.info(getResendDropStats());
			LOGGER.info(getQueueStats());
			reset();
		}

		// --------------------------------------------------------------------------
		/**
		 * Increment dropped packet counter
		 */
		public void incrementDroppedCount() {
			sumDropped.incrementAndGet();
		}

		// --------------------------------------------------------------------------
		/**
		 * Increment sent packet counter - should not include resends
		 */
		public void incrementPacketsSent() {
			totalPacketsSent.incrementAndGet();
		}

		// Not perfectly thread safe but it's good enough
		// Don't want to introduce too much locking

		// --------------------------------------------------------------------------
		/**
		 * Update resend stats
		 */
		public void updateResendStats(int inResendCount) {
			int currentResends = sumResends.get();

			if (inResendCount > currentResends) {
				maxResends.set(inResendCount);
			} else if (inResendCount < currentResends) {
				minResends.set(inResendCount);
			}

			sumResends.addAndGet(inResendCount);
		}

		// --------------------------------------------------------------------------
		/**
		 * Update queue stats
		 */
		public void updateQueueStats(int inQueueDepth) {
			int currentQueueSum = sumQueueDepth.get();

			if (inQueueDepth > currentQueueSum) {
				maxQueueDepth.set(inQueueDepth);
			} else if (inQueueDepth < currentQueueSum) {
				minQueueDepth.set(inQueueDepth);
			}

			sumQueueDepth.addAndGet(inQueueDepth);
			queueSampleSize.incrementAndGet();
		}

		// --------------------------------------------------------------------------
		/**
		 * Reset collection counters
		 */
		public void reset() {
			totalPacketsSent.set(0);
			sumResends.set(0);
			maxResends.set(0);
			minResends.set(0);
			sumDropped.set(0);

			sumQueueDepth.set(0);
			maxQueueDepth.set(0);
			minQueueDepth.set(0);
			queueSampleSize.set(0);

			st = System.currentTimeMillis();
		}

		// --------------------------------------------------------------------------
		/**
		 *  Get packet success report
		 *  @return String
		 */
		private String getResendDropStats() {
			double meanRetries = 0;
			double time = getMeasuredTimeSec();

			if (totalPacketsSent.get() > 0) {
				meanRetries = sumResends.get() / totalPacketsSent.get();
			}

			return new String("CMD Packet Success Report - Average resend count: " + formatter.format(meanRetries)
					+ " Max resends: " + maxResends.get() + " Min resends: " + minResends.get() + "Dropped: " + sumDropped.get()
					+ "Elapsed time: " + formatter.format(time) + " (secs)");
		}

		// --------------------------------------------------------------------------
		/**
		 *  Get packet queues report
		 *  @return String
		 */
		private String getQueueStats() {
			double meanDepth = 0;
			double time = getMeasuredTimeSec();

			if (queueSampleSize.get() > 0) {
				meanDepth = sumQueueDepth.get() / queueSampleSize.get();
			}

			return new String("CMD Packet Queue Report - Average queue depth: " + formatter.format(meanDepth) + " Max depth: "
					+ maxQueueDepth.get() + " Min depth: " + minQueueDepth.get() + "Elapsed time: " + formatter.format(time)
					+ " (secs)");
		}

		// --------------------------------------------------------------------------
		/**
		 *  Get elapsed time in seconds
		 *  @return double - elapsed time
		 */
		private double getMeasuredTimeSec() {
			double time_seconds = (System.currentTimeMillis() - st) / 1000;
			return time_seconds;
		}
	}
}
