package com.codeshelf.ws.protocol.request;

import com.codeshelf.application.JvmProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest extends RequestABC {

	@Getter @Setter
	private String cstoken;

	@Getter @Setter
	private String userId;

	@Getter @Setter
	private String password;
	
	@Getter @Setter
	private String clientVersion;

	public LoginRequest() {
		// default constructor does NOT set the client app version 
	}
	
	public LoginRequest(String userId, String password) {
		this.userId = userId;
		this.password = password;

		// this constructor is used by the client and automatically sets the version to the client's app version.
		this.clientVersion = JvmProperties.getVersionStringShort();
	}
}
