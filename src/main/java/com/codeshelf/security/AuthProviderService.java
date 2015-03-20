package com.codeshelf.security;

import javax.servlet.http.Cookie;

import com.codeshelf.service.CodeshelfService;

public interface AuthProviderService extends CodeshelfService {
	AuthProviderService initialize(); // start outside of service manager for testing
	
	// tokens
	AuthResponse checkToken(final String value);
	String createToken(int id, Long timestamp, Long sessionStart, SessionFlags sessionFlags);
	String createToken(int id);

	// tokens wrapped in cookies
	String getCookieName();
	AuthResponse checkAuthCookie(final Cookie[] cookies);
	Cookie createAuthCookie(final String newToken);
	
	// password authentication
	AuthResponse authenticate(String username, String password);
	boolean passwordMeetsRequirements(final String password);
	String hashPassword(final String password);
//	boolean checkPassword(final String password,final String hash);
	boolean hashIsValid(final String hash);
	String describePasswordRequirements();

	// etc
	boolean usernameMeetsRequirements(String username);

}
