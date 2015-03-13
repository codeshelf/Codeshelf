package com.codeshelf.security;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.NewCookie;

import com.google.common.util.concurrent.Service;

public interface AuthProviderService extends Service {
	AuthProviderService initialize();
	
	AuthCookieContents checkAuthCookie(final Cookie[] cookies);
	NewCookie createAuthCookie(final int id, final int maxAgeSeconds);
	int	getDefaultCookieExpirationSeconds();
	
	String hashPassword(final String password);
	boolean checkPassword(final String password,final String hash);
	boolean passwordMeetsRequirements(final String password);
	boolean hashIsValid(final String hash);
}
