package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class AssociateRemoteCheRequest extends DeviceRequest {

	@Getter
	String remoteCheNameToAssociateTo;
		
	public AssociateRemoteCheRequest() {
	}
	
	public AssociateRemoteCheRequest(String cheId, String remoteCheNameToAssociateTo) {
		setDeviceId(cheId);
		this.remoteCheNameToAssociateTo = remoteCheNameToAssociateTo;
	}
}
