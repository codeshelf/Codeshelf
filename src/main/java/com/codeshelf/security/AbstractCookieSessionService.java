package com.codeshelf.security;

import javax.servlet.http.Cookie;

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

	public Cookie createAuthCookie(String token) {
		Cookie cookie = new Cookie(getCookieName(), token);
		cookie.setPath("/");
		cookie.setDomain(this.getCookieDomain());
		cookie.setVersion(0);
		cookie.setMaxAge(this.getCookieMaxAgeHours() * 60 * 60);
		cookie.setSecure(this.isCookieSecure());
		return cookie;
	}

}