package com.codeshelf.device.radio.protocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.device.radio.ChannelInfo;
import com.codeshelf.device.radio.RadioControllerPacketIOService;
import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.command.CommandAssocAck;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.command.Packet;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;

public class RadioPacketHandler_v1 extends RadioPacketHandler_v0 {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(RadioPacketHandler_v1.class);

	public RadioPacketHandler_v1(NetAddress mServerAddress,
		ConcurrentMap<NetAddress, BlockingQueue<IPacket>> mPendingAcksMap,
		Map<NetAddress, INetworkDevice> mDeviceNetAddrMap,
		NetworkId broadcastNetworkId,
		NetAddress broadcastAddress,
		AtomicBoolean mChannelSelected,
		List<IRadioControllerEventListener> mEventListeners,
		ChannelInfo[] mChannelInfo,
		Map<String, INetworkDevice> mDeviceGuidMap,
		RadioControllerPacketIOService packetIOService) {

		super(mServerAddress,
			mPendingAcksMap,
			mDeviceNetAddrMap,
			broadcastNetworkId,
			broadcastAddress,
			mChannelSelected,
			mEventListeners,
			mChannelInfo,
			mDeviceGuidMap,
			packetIOService);
	}

	/**
	 * @param inAckId
	 * @param inNetId
	 * @param inSrcAddr
	 */

	@Override
	protected void respondToAck(INetworkDevice device, final byte inAckId, final NetworkId inNetId, final NetAddress inSrcAddr) {
		ContextLogging.setNetGuid(device.getGuid());
		try {

			LOGGER.info("ACKing packet: ackId={}; netId={}; srcAddr={}", inAckId, inNetId, inSrcAddr);

			device.setLastAckId(inAckId);

			//The difference between v0 and v1 is that we're sending device.getGuid().getHexStringNoPrefix() on an ACK instead of all 0's 
			CommandAssocAck ackCmd = new CommandAssocAck(device.getGuid().getHexStringNoPrefix(),
				new NBitInteger(CommandAssocAck.ASSOCIATE_STATE_BITS, (byte) 0));

			IPacket ackPacket = new Packet(ackCmd, inNetId, mServerAddress, inSrcAddr, false);
			ackCmd.setPacket(ackPacket);
			ackPacket.setAckId(inAckId);
			handleOutboundPacket(ackPacket);

		} finally {
			ContextLogging.clearNetGuid();
		}

	}

}
