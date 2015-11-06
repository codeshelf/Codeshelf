package com.codeshelf.scheduler;

import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;

public abstract class AbstractFacilityJob implements Job, InterruptableJob {

	@Override
	final public void execute(JobExecutionContext context) throws JobExecutionException {
		Facility facility = (Facility) context.getMergedJobDataMap().get("facility");
		Tenant tenant = (Tenant) context.getMergedJobDataMap().get("tenant");
		doFacilityExecute(tenant, facility);
	}

	protected abstract Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException;
	

}
