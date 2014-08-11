package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;

public class GetWorkRequest extends RequestABC {

	@Getter
	String cheId;
	
	@Getter
	String locationId;
	
	public GetWorkRequest() {
	}
	
	public GetWorkRequest(String cheId, String locationId) {
		this.cheId = cheId;
		this.locationId = locationId;
	}

}
