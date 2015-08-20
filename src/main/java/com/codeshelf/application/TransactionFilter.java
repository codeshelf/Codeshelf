package com.codeshelf.application;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.persistence.TenantPersistenceService;

public class TransactionFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionFilter.class);

	@Override
	public void init(FilterConfig arg0) throws ServletException {}
	
	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		boolean threw=false;
		try {
			persistenceService.beginTransaction();
			filterChain.doFilter(request, response);
			if (response instanceof HttpServletResponse) {
				int status = ((HttpServletResponse)response).getStatus();
				if (status < 200 || status >= 400){
					LOGGER.warn("Rolling back transaction for non-200s-300s response: " + status);
					threw = true;
					persistenceService.rollbackTransaction();
				}
			}
		} catch(Exception e) {
			LOGGER.warn("Rolling back transaction for exception: " + e);
			threw=true;
			persistenceService.rollbackTransaction();
			throw e;
		} finally {
			if(!threw)
				persistenceService.commitTransaction();
		}
	}
}
