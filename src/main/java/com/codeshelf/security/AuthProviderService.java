package com.codeshelf.security;

import javax.servlet.http.Cookie;

import com.google.common.util.concurrent.Service;

public interface AuthProviderService extends Service {
	AuthProviderService initialize(); // start outside of service manager for testing
	
	// tokens
	AuthResponse checkToken(final String value);
	String createToken(final int id, Long sessionStart, SessionFlags flags);

	// tokens wrapped in cookies
	String getCookieName();
	AuthResponse checkAuthCookie(final Cookie[] cookies);
	Cookie createAuthCookie(final String newToken);
	
	// password hashing
	String hashPassword(final String password);
	boolean checkPassword(final String password,final String hash);
	boolean passwordMeetsRequirements(final String password);
	boolean hashIsValid(final String hash);

}