package com.codeshelf.security;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.NewCookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCookieSessionService extends AbstractSessionLoginService {
	static final Logger	LOGGER								= LoggerFactory.getLogger(AbstractCookieSessionService.class);

	abstract String getCookieName();
	abstract String getCookieDomain();
	abstract int getCookieMaxAgeHours();
	abstract boolean isCookieSecure();

	public AbstractCookieSessionService() {
		super();
	}

	/**************************** cookie methods ****************************/

	public TokenSession checkAuthCookie(Cookie[] cookies) {
		if (cookies == null)
			return null;
	
		Cookie match = null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(getCookieName())) {
				if (match == null) {
					match = cookie;
				} else {
					LOGGER.warn("more than one auth cookie found");
					return null;
				}
			}
		}
		if (match != null)
			return checkToken(match.getValue());
		//else
		return null;
	}
	
	public TokenSession checkAuthCookie(Cookie cookie) {
		if (cookie != null && cookie.getName().equals(getCookieName())) {
			return checkToken(cookie.getValue());
		} // else
		return null;
	}

	public TokenSession checkAuthCookie(javax.ws.rs.core.Cookie cookie) {
		if (cookie != null && cookie.getName().equals(getCookieName())) {
			return checkToken(cookie.getValue());
		} // else
		return null;
	}

	public Cookie createAuthCookie(String token) {
		Cookie cookie = new Cookie(getCookieName(), token);
		cookie.setPath("/");
		cookie.setDomain(this.getCookieDomain());
		cookie.setVersion(0);
		cookie.setMaxAge(this.getCookieMaxAgeHours() * 60 * 60);
		cookie.setSecure(this.isCookieSecure());
		return cookie;
	}

	public NewCookie createAuthNewCookie(String token) {
		NewCookie cookie = new NewCookie(getCookieName(), 
			token, 
			"/", // path
			this.getCookieDomain(), 
			0, // version
			null, // no comment 
			this.getCookieMaxAgeHours() * 60 * 60, 
			this.isCookieSecure());
		return cookie;
	}
	
	public String removerCookie() {
		return this.getCookieName()+"=deleted;Domain="+this.getCookieDomain()+";Path=/;Expires=Thu, 01-Jan-1970 00:00:01 GMT";
	}

}