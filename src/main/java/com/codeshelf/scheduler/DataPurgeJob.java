package com.codeshelf.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.TenantCallable;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.PurgeProcessor;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public class DataPurgeJob implements Job {

	private static final Logger LOGGER	= LoggerFactory.getLogger(DataPurgeJob.class);
	
	public DataPurgeJob() {
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Facility facility = (Facility) context.get("facility");
		Tenant tenant = (Tenant) context.get("tenant");
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		TenantCallable purgeCallable = new TenantCallable(persistenceService, tenant, systemUser, new PurgeProcessor(facility));
		LOGGER.info("Starting data archiving job");
		purgeCallable.call();
		LOGGER.info("Data archiving job finished");
	}

}
