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

public class APICallFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(APICallFilter.class);

	@Override
	public void init(FilterConfig arg0) throws ServletException {}
	
	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			String url = ((HttpServletRequest)request).getRequestURL().toString();
			String queryString = ((HttpServletRequest)request).getQueryString();
			LOGGER.info("API CALL: {}?{}", url, queryString==null?"":queryString);
		} else {
			LOGGER.info("API CALL: {}. Can't cast to HttpServletRequest", request);
		}
		filterChain.doFilter(request, response);
	}
}
