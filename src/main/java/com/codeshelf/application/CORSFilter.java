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

import com.google.common.base.Strings;

public class CORSFilter implements Filter {

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}
	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		
		String allowOrigin = ((HttpServletRequest)request).getHeader("Origin");
		if (Strings.isNullOrEmpty(allowOrigin)) {
			allowOrigin = "*";
		}
		((HttpServletResponse)response).addHeader(
			"Access-Control-Allow-Credentials", "true" //Have to be specific when the xhr contains credentials
		);
		((HttpServletResponse)response).addHeader(
			"Access-Control-Allow-Origin", allowOrigin //Have to be specific when the xhr contains credentials
		);
		filterChain.doFilter(request, response);

	}


}
