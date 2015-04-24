/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessor.java,v 1.20 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.AbstractCodeshelfScheduledService;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class EdiProcessorService extends AbstractCodeshelfScheduledService {

	int periodSeconds = 30;

	private static final Logger			LOGGER					= LoggerFactory.getLogger(EdiProcessorService.class);

	private ICsvOrderImporter			mCsvOrderImporter;
	private ICsvOrderLocationImporter	mCsvOrderLocationImporter;
	private ICsvInventoryImporter		mCsvInventoryImporter;
	private ICsvLocationAliasImporter	mCsvLocationAliasImporter;
	private ICsvAislesFileImporter		mCsvAislesFileImporter;
	private ICsvCrossBatchImporter		mCsvCrossBatchImporter;

	private Timer					ediProcessingTimer;
	private Thread ediSignalThread = null;
	
	@Getter
	@Setter
	BlockingQueue<String> ediSignalQueue = null;

	@Inject
	public EdiProcessorService(final ICsvOrderImporter inCsvOrdersImporter,
		final ICsvInventoryImporter inCsvInventoryImporter,
		final ICsvLocationAliasImporter inCsvLocationsImporter,
		final ICsvOrderLocationImporter inCsvOrderLocationImporter,
		final ICsvCrossBatchImporter inCsvCrossBatchImporter,
		final ICsvAislesFileImporter inCsvAislesFileImporter) {

		mCsvOrderImporter = inCsvOrdersImporter;
		mCsvOrderLocationImporter = inCsvOrderLocationImporter;
		mCsvInventoryImporter = inCsvInventoryImporter;
		mCsvLocationAliasImporter = inCsvLocationsImporter;
		mCsvAislesFileImporter = inCsvAislesFileImporter;
		mCsvCrossBatchImporter = inCsvCrossBatchImporter;
	}
	
	List<Facility> getFacilities() {
		return Facility.staticGetDao().getAll();
	}

	@Override
	protected void startUp() throws Exception {
		if(this.ediSignalQueue == null) 
		{
			this.ediSignalQueue = new ArrayBlockingQueue<>(100);
			//throw new NullPointerException("couldn't start EDI processer, signal queue is null");
		}
		ediProcessingTimer		= MetricsService.getInstance().createTimer(MetricsGroup.EDI, "processing-time");
		
		LOGGER.info("starting ediProcessorService");
	}

	@Override
	protected void runOneIteration() throws Exception {

		LOGGER.trace("Begin EDI process.");

		CodeshelfSecurityManager.removeContextIfPresent(); // shared thread, maybe other was aborted
		for (Tenant tenant : TenantManagerService.getInstance().getTenants()) {
			doEdiForTenant(tenant);
		}
	}

	private void doEdiForTenant(Tenant tenant) {
		boolean completed = false;
		int numChecked = 0;
		final Timer.Context timerContext = ediProcessingTimer.time();
		long startTime = System.currentTimeMillis();
		
		try {
			UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
			CodeshelfSecurityManager.setContext(systemUser, tenant);
			LOGGER.trace("Begin EDI process for tenant {}", tenant.getName());
			TenantPersistenceService.getInstance().beginTransaction();

			// Loop through each facility to make sure that it's EDI service processes any queued EDI.
			for (Facility facility : this.getFacilities()) {
				for (IEdiService ediService : facility.getEdiServices()) {
					if (ediService.getServiceState().equals(EdiServiceStateEnum.LINKED)) {
						if (ediService.getUpdatesFromHost(mCsvOrderImporter,
							mCsvOrderLocationImporter,
							mCsvInventoryImporter,
							mCsvLocationAliasImporter,
							mCsvCrossBatchImporter,
							mCsvAislesFileImporter)) {
							numChecked ++;
							// Signal other threads that we've just processed new EDI.
							try {
								ediSignalThread = Thread.currentThread();
								ediSignalQueue.put(ediService.getServiceName());
							} catch (InterruptedException e) {
								LOGGER.error("Failed to signal other threads that we've just processed n EDI", e);
							} finally {
								ediSignalThread = null;
							}
						}
					}
				}
			}
			TenantPersistenceService.getInstance().commitTransaction();
			completed = true;
		} catch (RuntimeException e) {
			LOGGER.error("Unable to process edi for tenant "+tenant.getId(), e);
		} finally {
			long endTime = System.currentTimeMillis();
			if (timerContext != null) timerContext.stop();
			LOGGER.info("Checked for updates from {} EDI services for tenant {} in {}s",numChecked,tenant.getName(),(endTime-startTime)/1000);
			CodeshelfSecurityManager.removeContext();
			if (!completed) {
				TenantPersistenceService.getInstance().rollbackTransaction();
				LOGGER.warn("EDI process did not complete successfully for tenant {}",tenant.getName());
			}
		}

	}

	@Override
	protected void shutDown() throws Exception {
		if(ediSignalThread != null) 
			ediSignalThread.interrupt();
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(Integer.getInteger("service.edi.init.delay",0), this.periodSeconds, TimeUnit.SECONDS);
	}
}
