package com.codeshelf.security;

import lombok.Getter;

public class AuthCookieContents {
	@Getter
	private int id;
	@Getter
	private long timestamp;
	
	public AuthCookieContents(int id, long timestamp) {
		this.id = id;
		this.timestamp = timestamp;
	}
	
}