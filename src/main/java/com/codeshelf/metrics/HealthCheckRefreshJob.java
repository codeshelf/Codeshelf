package com.codeshelf.metrics;

import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.TenantCallable;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.SingleBatchProcessorABC;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.scheduler.AbstractFacilityJob;
import com.codeshelf.scheduler.CachedHealthCheckResults;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;

public abstract class HealthCheckRefreshJob extends AbstractFacilityJob {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(HealthCheckRefreshJob.class);
	private TenantCallable		callable;

	private String				checkName;

	public HealthCheckRefreshJob() {
		this.checkName = getClass().getSimpleName();
	}

	@Override
	protected Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		callable = new TenantCallable(persistenceService, tenant, systemUser, new HealthCheckRefreshProcessor(facility));
		LOGGER.debug("Starting {} health check job", checkName);
		callable.call();
		return null;
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		if (callable != null) {
			callable.cancel();
		}
	}

	protected abstract void check(Facility facility) throws Exception;

	protected void saveResults(FacilityHealthCheckResult result) {
		saveResults(result.getFacility(), result.isPass(), result.getMessage());
	}

	protected void saveResults(Facility facility, boolean success, String messageFormat, Object...messageArgs ) {
		saveResults(facility, success, String.format(messageFormat, messageArgs));
	}

	protected void saveResults(Facility facility, boolean success, String message) {
		String jobName = getClass().getSimpleName();
		String logMessage = String.format("Job %s completed %ssuccessfully for facility %s: %s",
			jobName,
			success ? "" : "un",
			facility.getDomainId(),
			message);

		// DEV-1387 don't spam on jobs that don't need it. Is production is the classic one.
		if (!CodeshelfHealthCheck.OK.equals(message)) {
			if (success) {
				LOGGER.info(logMessage);
			} else {
				LOGGER.warn(logMessage);
			}
		}
		CachedHealthCheckResults.saveJobResult(jobName, facility, success, message);
	}

	public class HealthCheckRefreshProcessor extends SingleBatchProcessorABC {
		protected Facility	facility;

		public HealthCheckRefreshProcessor(Facility facility) {
			this.facility = facility;
		}

		@Override
		public int doBatch(int batchCount) throws Exception {
			Facility reloadedFacility = facility.reload();
			if (reloadedFacility != null) {
				check(reloadedFacility);
				setDone(true);
				return 1;
			} else {
				LoggerFactory.getLogger(this.getClass()).warn("facility {} no longer exists for healthcheck job {} exiting batch",
					facility,
					this);
				setDone(true);
				return 1;
			}
		}
	}

}