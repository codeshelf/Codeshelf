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
		HttpServletResponse httpResponse = (HttpServletResponse)response;
		httpResponse.addHeader(
			"Access-Control-Allow-Credentials", "true" //Have to be specific when the xhr contains credentials
		);
		httpResponse.addHeader(
			"Access-Control-Allow-Origin", allowOrigin //Have to be specific when the xhr contains credentials
		);
		httpResponse.addHeader(
			"Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS"
		);
		httpResponse.addHeader(
			"Vary", "Accept-Encoding, Origin"
		);
		httpResponse.addHeader(
			"Access-Control-Allow-Headers", "accept, content-type"
		);

		String method = ((HttpServletRequest) request).getMethod();
		if (!method.equals("OPTIONS")) {
			filterChain.doFilter(request, response);
		}
	}


}
