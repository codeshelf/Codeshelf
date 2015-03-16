package com.codeshelf.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.codeshelf.manager.User;

@ToString
@EqualsAndHashCode(of={"status","user","tokenTimestamp","newToken"})
public class AuthResponse {
	public static enum Status {
		ACCEPTED,
		LOGIN_NOT_ALLOWED,
		INVALID_TOKEN,
		INVALID_TIMESTAMP,
		BAD_CREDENTIALS,
		SESSION_IDLE_TIMEOUT,
		LOGGED_OFF
	};
	@Getter
	private Status status; 
	@Getter
	private User user = null;
	@Getter
	private Long tokenTimestamp = null;
	@Getter
	private String newToken = null;
	
	public AuthResponse(Status status,User user, Long tokenTimestamp, String newToken) {
		this.status = status;
		this.user = user;
		this.tokenTimestamp = tokenTimestamp;
		this.newToken = newToken;
	}
	
	public AuthResponse(Status status,User user) {
		this.status = status;
		this.user = user;
	}
	
	public AuthResponse(Status status) {
		this.status = status;
	}
	
}