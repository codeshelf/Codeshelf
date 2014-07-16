package com.gadgetworks.codeshelf.ws.jetty.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class LoginRequest extends RequestABC {

	@Getter @Setter
	String organizationId;
	
	@Getter @Setter
	String userId;
	
	@Getter @Setter
	String password;

	public LoginRequest() {
	}
	
	public LoginRequest(String organizationId, String userId, String password) {
		this.organizationId = organizationId;
		this.userId = userId;
		this.password = password;
	}
}
