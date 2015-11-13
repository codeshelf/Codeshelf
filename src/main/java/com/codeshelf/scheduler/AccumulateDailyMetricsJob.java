package com.codeshelf.scheduler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FacilityMetric;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public class AccumulateDailyMetricsJob  extends AbstractFacilityJob {

	private static final Logger LOGGER	= LoggerFactory.getLogger(AccumulateDailyMetricsJob.class);

	@Override
	protected Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException {
		LOGGER.info("AccumulateDailyMetricsJob job started");
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		CodeshelfSecurityManager.setContext(systemUser, tenant);
		persistenceService.beginTransaction();
		try {
			SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			TimeZone facilityTimeZone = facility.getTimeZone();
			Calendar cal = Calendar.getInstance(facilityTimeZone);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.DATE, -1);
			
			String dateStr = out.format(cal.getTime());
			FacilityMetric metrics = facility.computeMetrics(dateStr, true);
			LOGGER.info("AccumulateDailyMetricsJob job finished successfully");
			persistenceService.commitTransaction();
			return metrics;
		} catch (Exception e) {
			persistenceService.rollbackTransaction();
			throw new JobExecutionException(e);
		}
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
	}
}
