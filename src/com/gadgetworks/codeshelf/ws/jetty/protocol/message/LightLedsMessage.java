package com.gadgetworks.codeshelf.ws.jetty.protocol.message;

import lombok.Getter;
import lombok.Setter;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.flyweight.command.NetGuid;

public class LightLedsMessage extends MessageABC {
	@Getter @Setter
	String ledCommands;
	

	@Getter @Setter
	NetGuid netGuid;

	@Getter @Setter
	int durationSeconds;

	public LightLedsMessage() {	
	}
	public LightLedsMessage(NetGuid inGuid, int inDurationSeconds, String inCommands) {	
		ledCommands = inCommands;
		netGuid = inGuid;
		durationSeconds = inDurationSeconds;
	}

}
