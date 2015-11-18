package com.codeshelf.behavior;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.BatchProcessor;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class TenantCallable implements Callable<BatchReport> {
	private static final Logger			LOGGER						= LoggerFactory.getLogger(TenantCallable.class);
	private BatchProcessor				delegate;
	private Stopwatch					runTiming;
	private Thread						runningThread;
	private Tenant						tenant;
	private UserContext					userContext;
	private SettableFuture<Void>		cancelled					= null;
	private TenantPersistenceService	persistenceService;
	private long						lastLoggedTimeMs = 0;

	public TenantCallable(TenantPersistenceService persistenceService, Tenant tenant, BatchProcessor delegate) {
		this(persistenceService, tenant, CodeshelfSecurityManager.getUserContextSYSTEM(), delegate);
	}

	public TenantCallable(TenantPersistenceService persistenceService,
		Tenant tenant,
		UserContext userContext,
		BatchProcessor delegate) {
		this.persistenceService = persistenceService;
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

	public ListenableFuture<Void> cancel() {
		cancelled = SettableFuture.create();
		if (runningThread != null) {
			runningThread.interrupt();
		}
		return cancelled;
	}

	/**
	 * This always logs. Called this way at start and end and upon exception
	 */
	private void saveReport(BatchReport report) {
		LOGGER.info("Saving batch report {}", report);
		//persistenceService.saveOrUpdate(report);
	}

	/**
	 * For our incremental reporting, do not log too often, even if we persist results more frequently
	 */
	private void saveReport(BatchReport report, int batchNumber) {
		
		long nowMs = System.currentTimeMillis();
		if (lastLoggedTimeMs == 0) // a rather weak one-time initialization
			lastLoggedTimeMs = nowMs;
		if (nowMs - lastLoggedTimeMs > 10000) {
			LOGGER.info("Batch {}. Saving batch report {}", batchNumber, report);
			lastLoggedTimeMs = nowMs;
		}
		//persistenceService.saveOrUpdate(report);
	}

	public BatchReport call() {
		runTiming.start();
		BatchReport report = new BatchReport(DateTime.now());
		try {
			runningThread = Thread.currentThread();
			CodeshelfSecurityManager.setContext(userContext, tenant);
			persistenceService.beginTransaction();
			try {
				int totalTodo = delegate.doSetup();
				report.setTotal(totalTodo);
				saveReport(report);
				persistenceService.commitTransaction();
			} catch (Exception e) {
				persistenceService.rollbackTransaction();
				report.setException(e);
				return report;
			}

			int batchCount = 0;
			while (cancelled == null && !runningThread.isInterrupted() && !delegate.isDone()) {
				batchCount++; // pre-increment. First batch is 1, not zero
				persistenceService.beginTransaction();
				try {
					int completeCount = delegate.doBatch(batchCount);
					report.setCount(completeCount);
					saveReport(report, batchCount);
					persistenceService.commitTransaction();
				} catch (Exception e) {
					persistenceService.rollbackTransaction();
					LOGGER.warn("Received exception during batch: {}", batchCount, e);
					report.setException(e);
					return report;
				}
			}
			if (delegate.isDone()) {
				report.setComplete();
			}
		} finally {
			try {
				runningThread = null;
				runTiming.stop();
				if (cancelled != null) {
					report.setCancelled();
				}
				persistenceService.beginTransaction();
				delegate.doTeardown();
				saveReport(report);
				persistenceService.commitTransaction();
			} catch (Exception e) {
				persistenceService.rollbackTransaction();
				LOGGER.warn("Exception during final cleanup of {}", delegate, e);
			} finally {
				if (cancelled != null) {
					cancelled.set(null); //signal cancel is complete
				}
				CodeshelfSecurityManager.removeContextIfPresent();
			}
		}
		return report;
	}

	public String toString() {
		return String.format("TenantCallable: tenant: %s, user: %s, processor: %s", tenant, userContext, delegate);
	}

}
