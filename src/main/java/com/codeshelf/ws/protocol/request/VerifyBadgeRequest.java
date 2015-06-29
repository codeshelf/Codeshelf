package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class VerifyBadgeRequest extends RequestABC {
	@Getter
	private String badge;
	
	public VerifyBadgeRequest() {
	}
	
	public VerifyBadgeRequest(String cheId, String badge) {
		setDeviceId(cheId);
		this.badge = badge;
	}
}
