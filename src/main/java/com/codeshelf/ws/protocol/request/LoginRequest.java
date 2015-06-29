package com.codeshelf.ws.protocol.request;

import lombok.Getter;
import lombok.Setter;

public class LoginRequest extends RequestABC {

	@Getter @Setter
	private String cstoken;

	@Getter @Setter
	private String userId;

	@Getter @Setter
	private String password;
	
	public LoginRequest() {
	}

	public LoginRequest(String userId, String password) {
		this.userId = userId;
		this.password = password;
	}
}
