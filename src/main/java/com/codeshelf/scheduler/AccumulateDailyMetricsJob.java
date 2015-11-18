package com.codeshelf.scheduler;

import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.TenantCallable;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.AccumulateDailyMetricsProcessor;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public class AccumulateDailyMetricsJob  extends AbstractFacilityJob {

	private static final Logger LOGGER	= LoggerFactory.getLogger(AccumulateDailyMetricsJob.class);
	private TenantCallable callable;
	
	@Override
	public Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		callable = new TenantCallable(persistenceService, tenant, systemUser, new AccumulateDailyMetricsProcessor(facility));
		LOGGER.info("Starting daily facility metric accumulating job");
		callable.call();
		return null;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		if (callable != null) {
			callable.cancel();
		}
	}
}