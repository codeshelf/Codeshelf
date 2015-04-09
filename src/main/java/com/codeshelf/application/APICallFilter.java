package com.codeshelf.application;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authz.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;
import com.codeshelf.security.CodeshelfSecurityManager;

public class APICallFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(APICallFilter.class);

	@Override
	public void init(FilterConfig arg0) throws ServletException {}
	
	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			String url = httpRequest.getRequestURL().toString();
			String queryString = httpRequest.getQueryString();
			String method = httpRequest.getMethod();
			
			Object attr = httpRequest.getAttribute("user");
			String attrString = "";
			if(attr!= null) {
				User user = (User) attr;
				attrString = user.getUsername();
			}
			
			LOGGER.info("API CALL: {} {}{} {}", method, url, queryString==null?"":"?"+queryString, attrString);
			try {
				filterChain.doFilter(request, response);
			} catch(UnauthorizedException e) {
				String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
				LOGGER.warn("Access denied to {} {}{} for user {}: {}",method, url, queryString==null?"":"?"+queryString,
						username,e.getMessage());
				httpResponse.setStatus(Status.FORBIDDEN.getStatusCode());
			}
		} else {
			LOGGER.error("API CALL: {}. Can't cast to HttpServletRequest", request);
		}
	}
}
