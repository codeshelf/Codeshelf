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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthzFilter implements Filter {

	static final private Logger	LOGGER						= LoggerFactory.getLogger(AuthzFilter.class);
	

	private String permission;
	
	public AuthzFilter() {
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		permission = filterConfig.getInitParameter("permission");
		LOGGER.debug("AuthzFilter path, permission: {}, {}", filterConfig.getServletContext().getContextPath(), permission);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest; 
		HttpServletResponse response = (HttpServletResponse) servletResponse; 
		Subject subject = SecurityUtils.getSubject();
		if (subject  != null && subject.isPermitted(permission)) {
			chain.doFilter(request, response);
		} else {
			response.setStatus(Status.FORBIDDEN.getStatusCode());			
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}


}
