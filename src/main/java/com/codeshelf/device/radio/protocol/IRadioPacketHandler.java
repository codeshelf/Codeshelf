package com.codeshelf.device.radio.protocol;

import com.codeshelf.flyweight.command.IPacket;

public interface IRadioPacketHandler {

	public void handleInboundPacket(IPacket packet);

}
