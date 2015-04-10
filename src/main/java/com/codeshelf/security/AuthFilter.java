package com.codeshelf.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthFilter implements Filter {

	static final private Logger	LOGGER						= LoggerFactory.getLogger(AuthFilter.class);
	
	TokenSessionService tokenSessionService;
	
	public AuthFilter() {
		this.tokenSessionService = TokenSessionService.getInstance();
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		LOGGER.trace("AuthenticationFilter init: {}", filterConfig.getInitParameterNames().toString());
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest; 
		HttpServletResponse response = (HttpServletResponse) servletResponse; 

		Cookie[] cookies = request.getCookies();
		boolean authenticated = false;
		TokenSession tokenSession = tokenSessionService.checkAuthCookie(cookies);
		if(tokenSession != null) {
			if(tokenSession.getStatus().equals(TokenSession.Status.ACCEPTED)) {
				
				authenticated = true;

				String newToken = tokenSession.getNewToken(); // auto refresh if enabled
				if(newToken != null) {
					// offer updated token to keep session active
					response.addCookie(tokenSessionService.createAuthCookie(newToken));
				}
				CodeshelfSecurityManager.setContext(tokenSession.getUser(),tokenSession.getTenant());
				try {
					chain.doFilter(request, response);
				} finally {
					CodeshelfSecurityManager.removeContext();
				}
			} else {
				LOGGER.info("authentication failed: {}",tokenSession.getStatus().toString());
			}
		} else {
			LOGGER.warn("no valid auth cookie, access denied");
		}
		
		if(!authenticated)
			response.setStatus(Status.UNAUTHORIZED.getStatusCode());
	}

	@Override
	public void destroy() {
	}

}
