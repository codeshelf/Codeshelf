package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class LinkRemoteCheRequest extends DeviceRequest {

	@Getter
	String remoteCheNameToLinkTo;
		
	public LinkRemoteCheRequest() {
	}
	
	public LinkRemoteCheRequest(String cheId, String remoteCheNameToAssociateTo) {
		setDeviceId(cheId);
		this.remoteCheNameToLinkTo = remoteCheNameToAssociateTo;
	}
}
