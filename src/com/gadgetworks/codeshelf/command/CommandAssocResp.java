/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandAssocResp.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
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

	public static final String	BEAN_ID	= "CommandAssocResp";

	private static final Log	LOGGER	= LogFactory.getLog(CommandAssocResp.class);

	private NetAddress			mAddressAssigned;
	private NetworkId			mNetworkId;

	// --------------------------------------------------------------------------
	/**
	 *  @param inEndpoint	The endpoint to send the command.
	 *  @param inUniqueID	The GUID of the device. 
	 *  @param inAddressToAssign	The network address to assign to the device.
	 */
	public CommandAssocResp(final String inUniqueID, final NetworkId inNetworkId, final NetAddress inAddressToAssign) {
		super(CommandIdEnum.ASSOC_RESP, inUniqueID);
		mAddressAssigned = inAddressToAssign;
		mNetworkId = inNetworkId;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public CommandAssocResp() {
		super(CommandIdEnum.ASSOC_RESP);
		mAddressAssigned = IController.BROADCAST_ADDRESS;
		mNetworkId = IController.BROADCAST_NETWORK_ID;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {
		return CommandIdEnum.ASSOC_RESP + super.doToString() + " net:" + mNetworkId.toString() + " addr="
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
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);
	}
}
