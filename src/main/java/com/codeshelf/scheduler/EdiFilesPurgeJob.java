package com.codeshelf.scheduler;

import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.TenantCallable;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.PurgeProcessorEdiFiles;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public class EdiFilesPurgeJob extends AbstractFacilityJob {

	private static final Logger LOGGER	= LoggerFactory.getLogger(DataPurgeJob.class);
	private TenantCallable purgeCallable;
	
	public EdiFilesPurgeJob() {
	}

	@Override
	public Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		purgeCallable = new TenantCallable(persistenceService, tenant, systemUser, new PurgeProcessorEdiFiles(facility));
		LOGGER.info("Starting edi files purge job");
		purgeCallable.call();
		LOGGER.info("Edi files purge job finished");
		return null;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		if (purgeCallable != null) {
			purgeCallable.cancel();
		}
		
	}
}