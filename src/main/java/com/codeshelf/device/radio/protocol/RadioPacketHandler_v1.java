package com.codeshelf.device.radio.protocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.codeshelf.device.radio.ChannelInfo;
import com.codeshelf.device.radio.RadioControllerPacketIOService;
import com.codeshelf.flyweight.command.IPacket;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.command.NetworkId;
import com.codeshelf.flyweight.controller.INetworkDevice;
import com.codeshelf.flyweight.controller.IRadioControllerEventListener;

public class RadioPacketHandler_v1 extends RadioPacketHandler_v0 {

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

}
