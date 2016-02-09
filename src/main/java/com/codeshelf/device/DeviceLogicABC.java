/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: DeviceLogicABC.java,v 1.1 2013/05/04 00:30:01 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.device;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;

/**
 * @author jeffw
 *
 */
public abstract class DeviceLogicABC implements INetworkDevice {	
	private static final Logger		LOGGER								= LoggerFactory.getLogger(DeviceLogicABC.class);

	private byte					STARTING_ACK_NUM		= 1;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short					mHardwareVersion;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short					mFirmwareVersion;

	// PersistentId
	@Accessors(prefix = "m")
	@Getter
	private UUID					mPersistentId;

	// MAC address.
	@Accessors(prefix = "m")
	@Getter
	private NetGuid					mGuid;

	// The CHE's net address.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetAddress				mAddress;

	// The network device state.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private NetworkDeviceStateEnum	mDeviceStateEnum;

	// The last known battery level.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private short					mLastBatteryLevel;

	// The last time we had contact.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private long					mLastContactTime;

	// The controller for this device..
	protected IRadioController		mRadioController;

	// The device manager.
	@Accessors(prefix = "m")
	@Getter
	protected CsDeviceManager		mDeviceManager;

	@Accessors(prefix = "m")
	@Getter
	@Setter
	@Transient
	private byte					mLastIncomingAckId;

	@Accessors(prefix = "m")
	@Getter
	private byte					mOutgoingAckId;
	
	@Accessors(prefix = "m")
	@Getter
	@Setter
	long							mLastRadioCommandSendForThisDevice	= 0;
	
	@Accessors(prefix = "m")
	@Getter
	@Setter
	protected IPacket				mLastSentPacket;

	private AtomicLong				mLastPacketReceivedTime	= new AtomicLong(System.currentTimeMillis());
	private AtomicLong				mLastPacketSentTime		= new AtomicLong(System.currentTimeMillis());

	public DeviceLogicABC(final UUID inPersistentId,
		final NetGuid inGuid,
		final CsDeviceManager inDeviceManager,
		final IRadioController inRadioController) {
		mPersistentId = inPersistentId;
		mGuid = inGuid;
		mDeviceManager = inDeviceManager;
		mRadioController = inRadioController;
		mOutgoingAckId = STARTING_ACK_NUM;
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.flyweight.controller.INetworkDevice#doesMatch(com.codeshelf.flyweight.command.NetGuid)
	 */
	@Override
	public final boolean doesMatch(NetGuid inGuid) {
		return ((getGuid() != null) && (getGuid().equals(inGuid)));
	}

	// --------------------------------------------------------------------------
	/* 
	*/
	public boolean isAckIdNew(byte inAckId) {
		int unsignedAckId = inAckId & 0xFF;
		int unsignedLastAckId = mLastIncomingAckId & 0xFF;

		if (unsignedAckId > unsignedLastAckId) {
			return true;
		} else if (unsignedLastAckId > 254 && inAckId < 3) {
			//Overflow case. If ACK last ACK is 256 and inAckID is 0, this IS a new device ID but it won't be greater than the last ACK ID.
			//We give a few ids of buffer in case packets are lost around the overflow.
			return true;
		}

		return false;
	}

	// --------------------------------------------------------------------------
	/* Is device fully associated? Only so if mDeviseStateEnum = STARTED.
	 * See RadioController.networkDeviceBecameActive()
	 */
	@Override
	public final boolean isDeviceAssociated() {
		return (mDeviceStateEnum != null && mDeviceStateEnum.equals(NetworkDeviceStateEnum.STARTED));
	}

	/**
	 * A bottleneck for command so we can look at timing or whatever
	 * Send the command to the the getAddress() of this device
	 */
	private void sendRadioControllerCommand(ICommand inCommand, NetAddress inDstAddr, boolean inAckRequested) {
		if (this.isDeviceAssociated() || mRadioController.testingResendQueueing()) {
			setLastRadioCommandSendForThisDevice(System.currentTimeMillis());
			mRadioController.sendCommand(inCommand, inDstAddr, inAckRequested);
		}
	}

	/**
	 * A bottleneck for command so we can look at timing or whatever
	 * Send the command to the the getAddress() of this device
	 */
	public void sendRadioControllerCommand(ICommand inCommand, boolean inAckRequested) {
		sendRadioControllerCommand(inCommand, getAddress(), inAckRequested);
	}

	/**
	 * Keeps track per device
	 * Sleeps this thread long enough such that radio commands for the same device do not go out too fast.
	 */
	@SuppressWarnings("unused")
	private void waitLongEnough() {
		int delayPeriodMills = 5;

		if (delayPeriodMills > 0) {
			long lastSendMs = getLastRadioCommandSendForThisDevice();
			long nowMs = System.currentTimeMillis();
			long periodSince = nowMs - lastSendMs;
			if (periodSince < delayPeriodMills) {
				try {
					Thread.sleep(delayPeriodMills - periodSince);
				} catch (InterruptedException e) {
				}
				LOGGER.info("waited {} ms to send", delayPeriodMills - periodSince);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Utility function. Should be promoted, and get a cached value.
	 */
	protected String getMyGuidStr() {
		return getGuidNoPrefix();
	}

	// --------------------------------------------------------------------------
	public String getGuidNoPrefix() {
		NetGuid thisGuid = this.getGuid();
		if (thisGuid != null)
			return thisGuid.getHexStringNoPrefix();
		else
			return null;
	}

	// --------------------------------------------------------------------------
	public boolean needUpdateCheDetails(NetGuid cheDeviceGuid, String cheName, byte[] associatedToCheGuid) {
		return false;
	}

	// --------------------------------------------------------------------------
	public void setLastPacketReceivedTime(long inTime) {
		mLastPacketReceivedTime.set(inTime);
	}

	// --------------------------------------------------------------------------
	public long getLastPacketReceivedTime() {
		return mLastPacketReceivedTime.get();
	}

	// --------------------------------------------------------------------------
	public void setLastPacketSentTime(long inTime) {
		mLastPacketSentTime.set(inTime);
	}

	// --------------------------------------------------------------------------
	public long getLastPacketSentTime() {
		return mLastPacketSentTime.get();
	}

	// --------------------------------------------------------------------------
	public synchronized byte getNextAckId() {
		byte curr = mOutgoingAckId;
		int currAckIdUnsigned = mOutgoingAckId & 0xFF;

		if (currAckIdUnsigned == 255) {
			mOutgoingAckId = STARTING_ACK_NUM;
			curr = mOutgoingAckId;
		} else {
			mOutgoingAckId++;
		}

		return curr;
	}
	
	// --------------------------------------------------------------------------
	public void notifyAssociate(String inputString) {
		try {
			org.apache.logging.log4j.ThreadContext.put(ContextLogging.THREAD_CONTEXT_TAGS_KEY, "CHE_EVENT Associate");
			LOGGER.info(inputString);
		} finally {
			org.apache.logging.log4j.ThreadContext.remove(ContextLogging.THREAD_CONTEXT_TAGS_KEY);
		}
	}
}
