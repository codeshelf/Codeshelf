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
		INVALID_USER_ID,
		INVALID_TOKEN,
		INVALID_TIMESTAMP,
		BAD_CREDENTIALS,
		SESSION_IDLE_TIMEOUT
	};
	@Getter
	private Status status; 
	@Getter
	private User user = null;
	@Getter
	private Long tokenTimestamp = null;
	@Getter
	private Long sessionStartTimestamp = null;
	@Getter
	private SessionFlags sessionFlags = null;
	@Getter
	private String newToken = null;
	
	public AuthResponse(Status status,User user, Long tokenTimestamp, Long sessionStartTimestamp, SessionFlags sessionFlags, String newToken) {
		this.status = status;
		this.user = user;
		this.tokenTimestamp = tokenTimestamp;
		this.sessionStartTimestamp = sessionStartTimestamp;
		this.sessionFlags = sessionFlags;
		this.newToken = newToken;
	}
	
	public AuthResponse(Status status,User user) {
		notAccepted(status);
		this.status = status;
		this.user = user;
	}
	
	public AuthResponse(Status status) {
		notAccepted(status);
		this.status = status;
	}
	
	private void notAccepted(Status status) {
		if(status.equals(Status.ACCEPTED)) {
			throw new RuntimeException("Invalid status");
		}
	}
	
}