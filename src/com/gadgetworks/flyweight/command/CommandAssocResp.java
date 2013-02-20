/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocResp.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;
import com.gadgetworks.flyweight.bitfields.NBitInteger;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 *  
 *  1B - assigned address
 *  4b - space
 *  4b - assigned network ID
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

	public static final String	BEAN_ID					= "CommandAssocResp";

	private static final Log	LOGGER					= LogFactory.getLog(CommandAssocResp.class);
	private static final int	ADDRESS_ASSIGNED_BYTES	= 1;

	private NBitInteger			mAddressSpacing;
	private NetAddress			mAddressAssigned;
	private NBitInteger			mNetworkSpacing;
	private NetworkId			mNetworkId;

	// --------------------------------------------------------------------------
	/**
	 *  @param inEndpoint	The endpoint to send the command.
	 *  @param inUniqueID	The GUID of the device. 
	 *  @param inAddressToAssign	The network address to assign to the device.
	 */
	public CommandAssocResp(final String inUniqueID, final NetworkId inNetworkId, final NetAddress inAddressToAssign) {
		super(new NetCommandId(ASSOC_RESP_COMMAND), inUniqueID);
		mAddressSpacing = new NBitInteger((byte) IPacket.ADDRESS_SPACING_BITS, NBitInteger.INIT_VALUE);
		mAddressAssigned = inAddressToAssign;
		mNetworkSpacing = new NBitInteger((byte) IPacket.NETWORK_NUM_SPACING_BITS, NBitInteger.INIT_VALUE);
		mNetworkId = inNetworkId;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public CommandAssocResp() {
		super(new NetCommandId(ASSOC_RESP_COMMAND));
		mAddressSpacing = new NBitInteger((byte) IPacket.ADDRESS_SPACING_BITS, NBitInteger.INIT_VALUE);
		mAddressAssigned = IPacket.BROADCAST_ADDRESS;
		mNetworkSpacing = new NBitInteger((byte) IPacket.NETWORK_NUM_SPACING_BITS, NBitInteger.INIT_VALUE);
		mNetworkId = IPacket.BROADCAST_NETWORK_ID;
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
			inOutputStream.roundOutByte();
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

}
