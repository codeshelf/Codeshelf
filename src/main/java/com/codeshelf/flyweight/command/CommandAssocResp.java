/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocResp.java,v 1.5 2013/07/22 04:30:18 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.bitfields.BitFieldInputStream;
import com.codeshelf.flyweight.bitfields.BitFieldOutputStream;
import com.codeshelf.flyweight.bitfields.NBitInteger;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *  
 *  1B - assigned address
 *  4b - space
 *  4b - assigned network ID
 *  1B - wait until sleep seconds
 *  
 *  The controller sends the associate response command to any device whose GUID is under management by this controller.  
 *  The controller maps the GUID string to an unused address slot in the network address table.
 *  The controller then broadcasts the response (that includes this address and the controller's network ID.)
 *  
 *  There is a possibility that no controller will have the remote device's GUID under management.  It's also possible
 *  that the controller that *does* manage the remote device's GUID is not running.  In either case the remote will
 *  not receive any response.  The remote must periodically retry as necessary.
 */
public final class CommandAssocResp extends CommandAssocABC {

	private static final Logger	LOGGER					= LoggerFactory.getLogger(CommandAssocResp.class);
	private static final int	ADDRESS_ASSIGNED_BYTES	= 1;

	private NBitInteger			mAddressSpacing;
	private NetAddress			mAddressAssigned;
	private NBitInteger			mNetworkSpacing;
	private NetworkId			mNetworkId;
	private short				mSleepSeconds;
	private byte				mScannerType = 0;

	// --------------------------------------------------------------------------
	/**
	 *  @param inEndpoint	The endpoint to send the command.
	 *  @param inUniqueID	The GUID of the device. 
	 *  @param inAddressToAssign	The network address to assign to the device.
	 */
	public CommandAssocResp(final String inUniqueID,
		final NetworkId inNetworkId,
		final NetAddress inAddressToAssign,
		final short inSleepSeconds) {
		super(new NetCommandId(ASSOC_RESP_COMMAND), inUniqueID);
		mAddressSpacing = new NBitInteger((byte) IPacket.ADDRESS_SPACING_BITS, (byte) 0);
		mAddressAssigned = inAddressToAssign;
		mNetworkSpacing = new NBitInteger((byte) IPacket.NETWORK_NUM_SPACING_BITS, (byte) 0);
		mNetworkId = inNetworkId;
		mSleepSeconds = inSleepSeconds;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public CommandAssocResp() {
		super(new NetCommandId(ASSOC_RESP_COMMAND));
		mAddressSpacing = new NBitInteger((byte) IPacket.ADDRESS_SPACING_BITS);
		mAddressAssigned = new NetAddress(IPacket.BROADCAST_ADDRESS);
		mNetworkSpacing = new NBitInteger((byte) IPacket.NETWORK_NUM_SPACING_BITS);
		mNetworkId = new NetworkId(IPacket.BROADCAST_NETWORK_ID);
		mSleepSeconds = 0;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return Integer.toHexString(ASSOC_RESP_COMMAND) + " RESP" + super.doToString() + " net:" + mNetworkId.toString() + " addr="
				+ mAddressAssigned.toString();
	}

	// --------------------------------------------------------------------------
	/**
	 *  Return the address assigned by this command.
	 *  @return	The assigned address.
	 */
	public NetAddress getNetAdress() {
		return mAddressAssigned;
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public NetworkId getNetworkId() {
		return mNetworkId;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		// First write the unique ID.
		try {
			inOutputStream.writeNBitInteger(mAddressSpacing);
			inOutputStream.writeNBitInteger(mAddressAssigned);
			inOutputStream.writeNBitInteger(mNetworkSpacing);
			inOutputStream.writeNBitInteger(mNetworkId);
			inOutputStream.writeShort(mSleepSeconds);
			//inOutputStream.roundOutByte();
			inOutputStream.writeByte(mScannerType);
		} catch (IOException e) {
			LOGGER.error("", e);
		}

	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromStream(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromStream(BitFieldInputStream inInputStream, int inCommandByteCount) {
		super.doFromStream(inInputStream, inCommandByteCount);

		try {
			inInputStream.readNBitInteger(mAddressSpacing);
			inInputStream.readNBitInteger(mAddressAssigned);
			inInputStream.readNBitInteger(mNetworkSpacing);
			inInputStream.readNBitInteger(mNetworkId);
			mSleepSeconds = inInputStream.readByte();
			mScannerType = inInputStream.readByte();
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + ADDRESS_ASSIGNED_BYTES;
	}
	
	/* --------------------------------------------------------------------------
	 * setScannerType
	 * 
	 * Sets a byte value indicating the type of scanner attached to device.
	 * 
	 * @param inScannerType
	 */
	public void setScannerType(byte inScannerType) {
		mScannerType = inScannerType;
	}
	
	/* --------------------------------------------------------------------------
	 * getScannerType
	 * 
	 * Returns a byte value indicating the type of scanner attached to device.
	 * 
	 * @return byte inScannerType
	 */
	public byte getScannerType(){
		return mScannerType;
	}

}
