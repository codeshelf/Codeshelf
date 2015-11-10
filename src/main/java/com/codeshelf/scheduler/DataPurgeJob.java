package com.codeshelf.scheduler;

import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.TenantCallable;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.PurgeProcessor;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public class DataPurgeJob extends AbstractFacilityJob {

	private static final Logger LOGGER	= LoggerFactory.getLogger(DataPurgeJob.class);
	private TenantCallable purgeCallable;
	
	public DataPurgeJob() {
	}

	@Override
	public Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		purgeCallable = new TenantCallable(persistenceService, tenant, systemUser, new PurgeProcessor(facility));
		LOGGER.info("Starting data archiving job");
		purgeCallable.call();
		LOGGER.info("Data archiving job finished");
		return null;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		if (purgeCallable != null) {
			purgeCallable.cancel();
		}
		
	}

}
