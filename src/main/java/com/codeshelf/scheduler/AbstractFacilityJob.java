package com.codeshelf.scheduler;

import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.codeshelf.application.ContextLogging;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;

public abstract class AbstractFacilityJob implements Job, InterruptableJob {

	public static void disabled(Facility facility, Class<? extends Job> healthCheckClass) {
		CachedHealthCheckResults.saveJobResult(healthCheckClass.getSimpleName(), facility, true, "check disabled");
	}
	
	@Override
	final public void execute(JobExecutionContext context) throws JobExecutionException {
		Facility facility = (Facility) context.getMergedJobDataMap().get("facility");
		Tenant tenant = (Tenant) context.getMergedJobDataMap().get("tenant");
		try {
			ContextLogging.setTenantNameAndFacilityId(tenant.getName(), facility.getDomainId());
			doFacilityExecute(tenant, facility);
		} finally {
			ContextLogging.setTenantNameAndFacilityId(null, null);
		}
	}

	protected abstract Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException;
	

}
