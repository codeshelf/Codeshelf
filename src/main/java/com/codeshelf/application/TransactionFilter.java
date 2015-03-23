package com.codeshelf.application;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;

public class TransactionFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionFilter.class);

	@Override
	public void init(FilterConfig arg0) throws ServletException {}
	
	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		try {
			persistenceService.beginTransaction();
			filterChain.doFilter(request, response);
		} catch(Exception e) {
			LOGGER.warn("Rolling back transaction for exception: " + e);
			persistenceService.rollbackTransaction();
			throw e;
		} finally {
			persistenceService.commitTransaction();
		}
	}
}
