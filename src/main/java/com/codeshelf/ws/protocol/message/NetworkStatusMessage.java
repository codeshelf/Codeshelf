package com.codeshelf.ws.protocol.message;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.domain.CodeshelfNetwork;

public class NetworkStatusMessage extends MessageABC {
	@Getter @Setter
	CodeshelfNetwork network;

	public NetworkStatusMessage() {	
	}
	
	public NetworkStatusMessage(CodeshelfNetwork network) {
		network.getDomainId(); // ensure object loaded
		this.network = network;
	}
}
