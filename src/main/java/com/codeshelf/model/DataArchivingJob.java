package com.codeshelf.model;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public class DataArchivingJob implements Job {

	private static final Logger LOGGER	= LoggerFactory.getLogger(DataArchivingJob.class);

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOGGER.info("Starting data archiving job");
		for (Tenant tenant : TenantManagerService.getInstance().getTenants()) {
			archiveTenantData(tenant);
		}
		LOGGER.info("Data archiving job finished");
	}

	private void archiveTenantData(Tenant tenant) {
		boolean completed = false;
		long startTime = System.currentTimeMillis();
		try {
			UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
			CodeshelfSecurityManager.setContext(systemUser, tenant);
			TenantPersistenceService.getInstance().beginTransaction();
			// TODO: do actual archiving...
			TenantPersistenceService.getInstance().commitTransaction();
			completed = true;
		} catch (RuntimeException e) {
			LOGGER.error("Unable to archive data for tenant "+tenant.getId(), e);
		} finally {
			long endTime = System.currentTimeMillis();
			CodeshelfSecurityManager.removeContext();			
			if (completed) {
				LOGGER.info("Data archiving process for tenant {} completed in {}s",tenant.getName(),(endTime-startTime)/1000);
			}
			else {
				TenantPersistenceService.getInstance().rollbackTransaction();
				LOGGER.warn("Data archiving process did not complete successfully for tenant {}",tenant.getName());
			}
		}
	}
}
