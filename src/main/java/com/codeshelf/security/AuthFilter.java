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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class AuthFilter implements Filter {

	private static final Logger	LOGGER						= LoggerFactory.getLogger(AuthFilter.class);
	
	private static final int	MAX_FUTURE_TIMESTAMP_SECONDS = 30; // do not allow timestamps significantly in the future
	private static final int	MAX_IDLE_MINUTES = 5;
	private static final int	MIN_IDLE_MINUTES = 1;

	//private static final String	AUTHENTICATED_USER_HEADER	= "X-Codeshelf-User";

	AuthProviderService authProviderService;
	
	@Inject
	public AuthFilter(AuthProviderService authProviderService) {
		this.authProviderService = authProviderService;
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
		AuthCookieContents authCookieContents = authProviderService.checkAuthCookie(cookies);
		
		// TODO: finish authentication
		
//		if(authCookieContents.getTimestamp()+)

		/* option: external authentication, header passed in:
		 * 
		String authUsername = request.getHeader(AUTHENTICATED_USER_HEADER);
		if(authUsername != null) {
			User user = TenantManagerService.getInstance().getUser(authUsername);
			if(user != null) {
				request.setAttribute("user",user);
				LOGGER.debug("Externally authenticated user: {}",user);
			} else {
				LOGGER.warn("Unknown user: {}",authUsername);
			}
		}
		 * 
		 */
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}

}
