/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandNetMgmtCheck.java,v 1.4 2013/07/22 04:30:17 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.BitFieldOutputStream;

// --------------------------------------------------------------------------
/**
 *  The network check command is a bidirectional command sent between all devices on a channel.
 *  
 *  
 *  NetworkId = Broadcast
 *  
 *  The controller sends a multi-network broadcast net-check request on a channel in search of other controllers.
 *  Any controllers that receive this broadcast will send a net-check response with that controller's own
 *  networkId.
 *  
 *  
 *  NetworkId = Non-broadcast
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
 *  Also, the dongle should insert it's own GUID into the out-bound net-check packet before it puts it on-the-air.
 *  However, for the net-check responses coming directly from the gateway (dongle), per above, would have a GUID
 *  of '00000000'.  In this way, the controller differentiates net-check responses sent by other controllers
 *  as opposed to net-check responses sent by the gateway (dongle) itself.  (The gateway (dongle) should reject
 *  '00000000' packets received over-the-air.)
 *  
 *  @author jeffw
 */
public final class CommandNetMgmtCheck extends CommandNetMgmtABC {

	public static final byte	NETCHECK_REQ			= 1;
	public static final byte	NETCHECK_RESP			= 2;

	private static final Logger	LOGGER					= LoggerFactory.getLogger(CommandNetMgmtCheck.class);
	/*
	 * 
	 * Command size:
	 * 1B - Net check type.
	 * 1B - Network ID
	 * 8B - GUID
	 * 1B - Channel
	 * 1B - Energy Detect
	 * 1B - Link Quality
	 */
	private static final byte	NETCHECK_COMMAND_BYTES	= 13;

	private byte				mNetCheckType;
	private NetworkId			mNetworkId;
	private String				mGUID;
	private byte				mChannel;
	private NetChannelValue		mChannelEnergy;
	private NetChannelValue		mLastRcvdLinkQuality;

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a new wake command to send to the network.
	 *  @param inDatagramBytes
	 *  @param inEndpoint
	 */
	public CommandNetMgmtCheck(final byte inNetCheckType,
		final NetworkId inNetworkId,
		final String inGUID,
		final byte inChannel,
		final NetChannelValue inChannelEnergy,
		final NetChannelValue inLastRcvdLinkQuality) {
		super(new NetCommandId(NETCHECK_COMMAND));

		mNetCheckType = inNetCheckType;
		mNetworkId = inNetworkId;
		mGUID = inGUID;
		mChannel = inChannel;
		mChannelEnergy = inChannelEnergy;
		mLastRcvdLinkQuality = inLastRcvdLinkQuality;

	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor used to create a wake command received from the network.
	 */
	public CommandNetMgmtCheck() {
		super(new NetCommandId(NETCHECK_COMMAND));
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

		resultStr = "NetCheck type=" + checkType + " GUID=" + mGUID + " chan=" + mChannel + " ED=" + mChannelEnergy + " link=" + mLastRcvdLinkQuality + " netID=" + mNetworkId;

		return resultStr;
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doToStream(com.gadgetworks.bitfields.BitFieldOutputStream)
	 */
	protected void doToStream(BitFieldOutputStream inOutputStream) {
		super.doToStream(inOutputStream);

		try {
			inOutputStream.writeByte(mNetCheckType);
			// Write the network ID requested.
			inOutputStream.writeNBitInteger(mNetworkId);
			inOutputStream.roundOutByte();
			inOutputStream.writeBytes(mGUID.getBytes());
			inOutputStream.writeByte(mChannel);
			inOutputStream.writeNBitInteger(mChannelEnergy);
			inOutputStream.writeNBitInteger(mLastRcvdLinkQuality);
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
			mNetCheckType = inInputStream.readByte();
			// Read the network ID selected.
			mNetworkId = new NetworkId((byte) 0x00);
			inInputStream.readNBitInteger(mNetworkId);
			byte[] temp = new byte[NetGuid.NET_GUID_HEX_CHARS];
			inInputStream.readBytes(temp, NetGuid.NET_GUID_HEX_CHARS);
			mGUID = new String(temp);
			mChannel = inInputStream.readByte();
			// Yowsa!  No support for unsigned bytes in Java.  Gotta convert to short.
			mChannelEnergy = new NetChannelValue();
			inInputStream.readNBitInteger(mChannelEnergy);
			mLastRcvdLinkQuality = new NetChannelValue();
			inInputStream.readNBitInteger(mLastRcvdLinkQuality);

		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.controller.CommandABC#doComputeCommandSize()
	 */
	protected int doComputeCommandSize() {
		return super.doComputeCommandSize() + NETCHECK_COMMAND_BYTES;
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
	public NetworkId getNetworkId() {
		return mNetworkId;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public String getGUID() {
		return mGUID;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public byte getChannel() {
		return mChannel;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public NetChannelValue getChannelEnergy() {
		return mChannelEnergy;
	}

	// --------------------------------------------------------------------------
	/**
	 *  @return
	 */
	public NetChannelValue getLastRcvdLinkQuality() {
		return mLastRcvdLinkQuality;
	}

}
