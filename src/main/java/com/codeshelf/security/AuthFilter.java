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
	
	AuthProviderService authProviderService;
	
	public AuthFilter() {
		this.authProviderService = HmacAuthService.getInstance();
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
		AuthResponse authResponse = authProviderService.checkAuthCookie(cookies);
		if(authResponse != null) {
			if(authResponse.getStatus().equals(AuthResponse.Status.ACCEPTED)) {
				
				//User is on the thread, not in the request context
				//request.setAttribute(REQUEST_ATTR, authResponse.getUser());

				String newToken = authResponse.getNewToken(); // auto refresh if enabled
				if(newToken != null) {
					// offer updated token to keep session active
					response.addCookie(authProviderService.createAuthCookie(newToken));
				}
				CodeshelfSecurityManager.setContext(authResponse.getUser(),authResponse.getTenant());
				try {
					chain.doFilter(request, response);
				} finally {
					CodeshelfSecurityManager.removeContext();
				}
			}
		} else {
			LOGGER.warn("no valid auth cookie, access denied");
		}
		response.setStatus(Status.FORBIDDEN.getStatusCode());
	}

	@Override
	public void destroy() {
	}

}
