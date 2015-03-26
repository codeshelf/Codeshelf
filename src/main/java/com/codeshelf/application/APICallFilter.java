package com.codeshelf.application;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.User;

public class APICallFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(APICallFilter.class);

	@Override
	public void init(FilterConfig arg0) throws ServletException {}
	
	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
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
		} else {
			LOGGER.info("API CALL: {}. Can't cast to HttpServletRequest", request);
		}
		filterChain.doFilter(request, response);
	}
}
