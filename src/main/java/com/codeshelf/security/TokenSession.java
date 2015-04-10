package com.codeshelf.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;

@ToString
@EqualsAndHashCode(of={"status","user","tokenTimestamp","newToken"})
public class TokenSession {
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
	private Tenant tenant = null;
	@Getter
	private Long tokenTimestamp = null;
	@Getter
	private Long sessionStartTimestamp = null;
	@Getter
	private SessionFlags sessionFlags = null;
	@Getter
	private String newToken = null;
	
	public TokenSession(Status status, User user, Tenant tenant, Long tokenTimestamp, Long sessionStartTimestamp, SessionFlags sessionFlags, String newToken) {
		if(user != null) {
			if(!user.getTenant().equals(tenant)) { // validate that user can access tenant
				throw new RuntimeException("Token validation error: user "+user.getUsername()+" cannot access tenant "+tenant.getId());
			}
		}
		this.user = user;
		this.tenant = tenant;
		this.status = status;
		this.tokenTimestamp = tokenTimestamp;
		this.sessionStartTimestamp = sessionStartTimestamp;
		this.sessionFlags = sessionFlags;
		this.newToken = newToken;
	}
	
	public TokenSession(Status status,User user) {
		notAccepted(status);
		this.status = status;
		this.user = user;
	}
	
	public TokenSession(Status status) {
		notAccepted(status);
		this.status = status;
	}
	
	private void notAccepted(Status status) {
		if(status.equals(Status.ACCEPTED)) {
			throw new RuntimeException("Invalid status");
		}
	}
	
}