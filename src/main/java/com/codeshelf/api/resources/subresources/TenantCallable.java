package com.codeshelf.api.resources.subresources;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.google.common.base.Stopwatch;

public abstract class TenantCallable implements Callable<Report>{
	private static final Logger LOGGER	= LoggerFactory.getLogger(TenantCallable.class);
	private BatchProccessor	delegate;
	private Stopwatch runTiming;
	private Thread runningThread;
	private Tenant tenant;
	private UserContext userContext;
	private boolean cancelled = false;
	
	public TenantCallable(BatchProccessor delegate, Tenant tenant) {
		this(delegate, tenant, CodeshelfSecurityManager.getUserContextSYSTEM());
	}

	public TenantCallable(BatchProccessor delegate, Tenant tenant, UserContext userContext) {
		this.tenant = tenant;
		this.userContext = userContext;
		this.delegate = delegate;
		this.runTiming = Stopwatch.createUnstarted();
	}

	public long getTotalTime() {
		return runTiming.elapsed(TimeUnit.MILLISECONDS);
	}
	
	public boolean isRunning() {
		return runTiming.isRunning();
	}
	
	public boolean cancel() {
		//TODO review thread safety
		if (runningThread != null) {
			runningThread.interrupt();
		} 
		cancelled = true;
		return cancelled;
	}

	private void saveReport(Report report) {
		LOGGER.info("Saving batch report {}", report);
		//TenantPersistenceService.getInstance().getSession().save(report);
	}
	
	public Report call() throws Exception {
		Report report = new Report(DateTime.now());
		try {
			runningThread = Thread.currentThread();
			CodeshelfSecurityManager.setContext(userContext, tenant);
			TenantPersistenceService.getInstance().beginTransaction();
			try {
				int totalTodo = delegate.doSetup();
				report.setTotal(totalTodo);
				saveReport(report);
				TenantPersistenceService.getInstance().commitTransaction();
			} catch (Exception e) {
				TenantPersistenceService.getInstance().rollbackTransaction();
				report.setException(e);
				throw e;
			}

			int batchCount = 0;
			while(!cancelled && !runningThread.isInterrupted() && !delegate.isDone()) {
				TenantPersistenceService.getInstance().beginTransaction();
				try {
					int completeCount = delegate.doBatch(batchCount);
					report.setComplete(completeCount);
					saveReport(report);
					TenantPersistenceService.getInstance().commitTransaction();
				} catch (Exception e) {
					TenantPersistenceService.getInstance().rollbackTransaction();
					report.setException(e);
					throw e;
				}
			} 
		} finally {
			try {
				TenantPersistenceService.getInstance().beginTransaction();
				delegate.doTeardown();
				saveReport(report);
				TenantPersistenceService.getInstance().commitTransaction();
				
			} catch (Exception e) {
				TenantPersistenceService.getInstance().rollbackTransaction();
				throw e;
			}
		}
		CodeshelfSecurityManager.removeContextIfPresent();
		runningThread = null;
		runTiming.stop();
		return report;
	}

}
