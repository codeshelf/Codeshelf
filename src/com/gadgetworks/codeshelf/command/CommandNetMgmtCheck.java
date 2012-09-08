/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtCheck.java,v 1.5 2012/09/08 03:03:22 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.controller.ITransport;
import com.gadgetworks.codeshelf.controller.NetworkId;

// --------------------------------------------------------------------------
/**
 *  The network check command is a bidirectional command sent between all devices on a channel.
 *  
 *  
 *  NetworkID = Broadcast
 *  
 *  The controller sends a multi-network broadcast net-check request on a channel in search of other controllers.
 *  Any controllers that receive this broadcast will send a net-check response with that controller's own
 *  networkID.
 *  
 *  
 *  NetworkID = Non-broadcast
 *  
 *  Remotes associated to a controller in an established network will send the net-check request from time-to-time
 *  to verify that they are still in contact with their controller.  The controller will send a net-check response to
 *  the remote.
 *  
 *  Should the remote receive no net-check response it should increase broadcast power and re-attempt to contact
 *  the controller.  If the remote has reached maximum broadcast power it should then assume that the controller
 *  has either quit or restarted on another channel.  At this point the remote should re-enter the associate
 *  negotiation cycle.  (This cycle has the remote scan the channels in search of its controller.)
 *  
 *  
 *  A note on the role of the gateway (dongle) in net-check:
 *  
 *  Normally, the gateway (dongle) does nothing but pass-through packets from the serial port to the air and visa-versa.
 *  However, whenever the gateway (dongle) sees a net-check req for the broadcast network ID pass over the serial link
 *  (not the OTA link) then it should respond with it's own net-check response.  This net-check response will contain
 *  the energy detect value for the channel nominated in the out-bound net-check packet.  The controller will use
 *  this ED value along with any other net-check responses to determine the most suitable channel 
 *  for the new network.
 *  
 *  Also, the dongle should insert it's own MacAddr into the out-bound net-check packet before it puts it on-the-air.
 *  However, for the net-check responses coming directly from the gateway (dongle), per above, would have a MacAddr
 *  of '00000000'.  In this way, the controller differentiates net-check responses sent by other controllers
 *  as opposed to net-check responses sent by the gateway (dongle) itself.  (The gateway (dongle) should reject
 *  '00000000' packets received over-the-air.)
 *  
 *  @author jeffw
 */
public final class CommandNetMgmtCheck extends CommandNetMgmtABC {

	public static final String	BEAN_ID			= "CommandNetCheck";

	public static final byte	NETCHECK_REQ	= 1;
	public static final byte	NETCHECK_RESP	= 2;

	private static final Log	LOGGER			= LogFactory.getLog(CommandNetMgmtCheck.class);

	private byte				mNetCheckType;
	private NetworkId			mNetworkId;
	private String				mMacAddr;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtCheck(final byte inNetCheckType, final NetworkId inNetworkId, final String inMacAddr) {
		super(CommandIdEnum.NET_CHECK);

		mNetCheckType = inNetCheckType;
		mNetworkId = inNetworkId;
		mMacAddr = inMacAddr;
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a wake command received from the network.
	 */
	public CommandNetMgmtCheck() {
		super(CommandIdEnum.NET_CHECK);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToString()
	 */
	public String doToString() {

		String resultStr;

		String checkType = "";
		switch (mNetCheckType) {
			case NETCHECK_REQ:
				checkType = "REQ";
				break;
			case NETCHECK_RESP:
				checkType = "RESP";
				break;
			default:
		}

		resultStr = CommandIdEnum.NET_CHECK + " type=" + checkType + " MacAddr=" + mMacAddr + " netID=" + mNetworkId;

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToTransport(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToTransport(ITransport inTransport) {
		super.doToTransport(inTransport);

		inTransport.setNextParam(Byte.toString(mNetCheckType));
		inTransport.setNextParam(mNetworkId.toString());
		inTransport.setNextParam(mMacAddr);
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doFromTransport(com.gadgetworks.bitfields.BitFieldInputStream, int)
	 */
	protected void doFromTransport(ITransport inTransport) {
		super.doFromTransport(inTransport);

		mNetCheckType = ((Byte) inTransport.getParam(1)).byteValue();
		mNetworkId = ((NetworkId) inTransport.getParam(2));
		mMacAddr = (String) inTransport.getParam(3);
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getNetCheckType() {
		return mNetCheckType;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public NetworkId getNetworkID() {
		return mNetworkId;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getMacAddr() {
		return mMacAddr;
	}
}
