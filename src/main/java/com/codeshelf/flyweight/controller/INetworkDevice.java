/*******************************************************************************
 *  OmniBox
 *  Copyright (c) 2005-2007, Jeffrey B. Williams, All rights reserved
 *  $Id: INetworkDevice.java,v 1.8 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.flyweight.controller;

import com.codeshelf.device.DeviceRestartCauseEnum;
import com.codeshelf.flyweight.command.CommandControlButton;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetGuid;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface INetworkDevice {

	// --------------------------------------------------------------------------
	/**
	 *  Every device has a globally unique ID.
	 *  @return	The GUID for this device.
	 */
	NetGuid getGuid();

	// --------------------------------------------------------------------------
	/**
	 * Set the GUID for this device.
	 * @param inGuid
	 */
	// You can't set this - it's immutable for the object, so set it at construction.
	// void setGuid(NetGuid inGuid);

	// --------------------------------------------------------------------------
	/**
	 *  Get the network address assigned to this device's session.
	 *  @return	The network address
	 */
	NetAddress getAddress();

	// --------------------------------------------------------------------------
	/**
	 *  Set the network address for this device.
	 *  @param inNetAddr	The network address
	 */
	void setAddress(NetAddress inNetAddr);

	// --------------------------------------------------------------------------
	/**
	 *  Set the state for this device.
	 *  @param inSessionState	The device's session state.
	 */
	void setDeviceStateEnum(NetworkDeviceStateEnum inStatus);

	// --------------------------------------------------------------------------
	/**
	 *  Get the network state for this device.
	 *  @return	The device's network status.
	 */
	NetworkDeviceStateEnum getDeviceStateEnum();

	// --------------------------------------------------------------------------
	/**
	 *  Determine if this device's session matches a device GUID.
	 *  @param inGuid	The GUID
	 *  @return	True if there is a match.
	 */
	boolean doesMatch(NetGuid inGuid);

	// --------------------------------------------------------------------------
	/**
	 *  Get the time that we last heard from this device during this session.
	 *  @return	The last contact time.
	 */
	long getLastContactTime();

	// --------------------------------------------------------------------------
	/**
	 *  Set the time that we last heard from this device during this session.
	 *  @param inContactTime	The last contact time.
	 */
	void setLastContactTime(long inContactTime);

	// --------------------------------------------------------------------------
	/**
	 *  Get the last battery level (0 - 100).
	 *  @return	The last battery level reading.
	 */
	short getLastBatteryLevel();

	// --------------------------------------------------------------------------
	/**
	 *  Set the last battery level reading (0 - 100).
	 *  @param inLastBatteryLevel	The last battery level reading.
	 */
	void setLastBatteryLevel(short inLastBatteryLevel);

	// --------------------------------------------------------------------------
	/**
	 * We get this message when the device first starts on the network. And after reconnects
	 * Warning: restartEnum may be null. Often is as used in unit tests
	 */
	void startDevice(DeviceRestartCauseEnum restartEnum);

	// --------------------------------------------------------------------------
	/**
	 *  The user scanned a command on the remote.
	 *  @param inButtonNumberPressed
	 */
	void scanCommandReceived(String inCommandStr);

	// --------------------------------------------------------------------------
	/**
	 *  The userpressed a button on the remote.
	 *  @param inButtonNumberPressed
	 */
	void buttonCommandReceived(CommandControlButton inButtonCommand);

	// --------------------------------------------------------------------------
	/**
	 * Figure out if the ack ID is new (later) or not.
	 * @param inAckId
	 * @return
	 */
	boolean isAckIdNew(byte inAckId);

	// --------------------------------------------------------------------------
	/**
	 * Get the last ack ID that we processed.
	 */
	byte getLastIncomingAckId();

	// --------------------------------------------------------------------------
	/**
	 * Set the last ack ID that we processed.
	 * @param inAckId mOutgoingAckId
	 */
	void setLastIncomingAckId(byte inAckId);

	// --------------------------------------------------------------------------
	/**
	 * This is the number of seconds the remote device should sleep after the last user action.
	 * @return
	 */
	short getSleepSeconds();

	// --------------------------------------------------------------------------
	/**
	 * Was this device associated?
	 */
	public boolean isDeviceAssociated();

	// --------------------------------------------------------------------------
	/**
	 * What is the string name of this kind of device? Gives answers such as CsDeviceManager.DEVICETYPE_LED 
	 */
	public String getDeviceType();

	//public short getHardwareVersion();

	public void setHardwareVersion(short hardwareVersion);

	//public short getFirmwareVersion();

	public void setFirmwareVersion(short firmwareVersion);

	public boolean needUpdateCheDetails(NetGuid cheDeviceGuid, String cheName, byte[] associatedToCheGuid);
	
	/**
	 * getLastPacketReceivedTime();
			lastSentTime = inDevice.getLastPacketSentTime();
	 */
	
	/**
	 * Set the last time a packet was received from this device
	 * @param inTime
	 */
	public void setLastPacketReceivedTime(long inTime);
	
	/**
	 * Get the last time a packet was received from this device
	 */
	public long getLastPacketReceivedTime();
	
	/**
	 * Set the last time a packet was received from this device
	 * @param inTime
	 */
	public void setLastPacketSentTime(long inTime);
	
	/**
	 * Get the last time a packet was received from this device
	 */
	public long getLastPacketSentTime();
	
	/**
	 * Get the next ack id for this device
	 */
	public byte getNextAckId();
	
	/**
	 * Get the last outgoing ack id used
	 */
	public byte getOutgoingAckId();

	/**
	 * If this device is a Che type with scanner, get its type.
	 */
	byte getScannerTypeCode();
	
	/**
	 * Notify device association
	 */
	void notifyAssociate(String inString);
	
	/**
	 * Get last packet sent to device
	 * @return Ipacket of last packt or NULL if no previous packet
	 */
	IPacket getLastSentPacket();
	
	/**
	 * Set last packet sent
	 */
	void setLastSentPacket(IPacket inPacket);
	
	/**
	 * Bottleneck to tell this device to send the command through. Typically does not actually try to send if device is not associated yet.
	 */
	public void sendRadioControllerCommand(ICommand inCommand, boolean inAckRequested);
}
