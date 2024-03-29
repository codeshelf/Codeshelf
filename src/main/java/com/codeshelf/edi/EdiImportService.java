/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiImportService.java,v 1.20 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.AbstractCodeshelfScheduledService;
import com.google.inject.Inject;
import com.google.inject.Provider;

import lombok.Getter;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class EdiImportService extends AbstractCodeshelfScheduledService {

	@Getter
	int											periodSeconds			= 30;

	private static final Logger					LOGGER					= LoggerFactory.getLogger(EdiImportService.class);

	private Provider<ICsvOrderImporter>			mCsvOrderImporter;
	private Provider<ICsvOrderLocationImporter>	mCsvOrderLocationImporter;
	private Provider<ICsvInventoryImporter>		mCsvInventoryImporter;
	private Provider<ICsvLocationAliasImporter>	mCsvLocationAliasImporter;
	private Provider<ICsvAislesFileImporter>	mCsvAislesFileImporter;
	private Provider<ICsvCrossBatchImporter>	mCsvCrossBatchImporter;

	private Timer								ediProcessingTimer;

	Integer										lastNumTenants			= 0;
	int											lastSuccessfulTenants	= 0;
	Long										lastSuccessTime			= 0L;

	@Inject
	public EdiImportService(final Provider<ICsvOrderImporter> inCsvOrdersImporter,
		final Provider<ICsvInventoryImporter> inCsvInventoryImporter,
		final Provider<ICsvLocationAliasImporter> inCsvLocationsImporter,
		final Provider<ICsvOrderLocationImporter> inCsvOrderLocationImporter,
		final Provider<ICsvCrossBatchImporter> inCsvCrossBatchImporter,
		final Provider<ICsvAislesFileImporter> inCsvAislesFileImporter) {

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
		ediProcessingTimer = MetricsService.getInstance().createTimer(MetricsGroup.EDI, "processing-time");

		LOGGER.info("starting EDI import check");
	}

	@Override
	protected void runOneIteration() throws Exception {
		try {
			CodeshelfSecurityManager.removeContextIfPresent(); // shared thread, maybe other was aborted
			LOGGER.trace("Begin EDI import check for all tenants.");
	
			int numTenants = 0;
			int successfulTenants = 0;
	
			for (Tenant tenant : TenantManagerService.getInstance().getTenants()) {
				try {
					numTenants++;
					if(doEdiForTenant(tenant)) {
						successfulTenants++;
					}
				} catch(Exception e) {
					LOGGER.warn("Unable to do EDI import check for tenant {}", tenant, e);
				}
			}
			if(numTenants == successfulTenants) {
				synchronized(this.lastSuccessTime) {
					this.lastSuccessTime = System.currentTimeMillis();
				}
			}
			synchronized(this.lastNumTenants) {
				this.lastNumTenants = numTenants;
				this.lastSuccessfulTenants = successfulTenants;
			}
		} catch(Exception e) {
			LOGGER.warn("failed during EDI import check", e);
		}
	}
	
	public long getLastSuccessTime() {
		synchronized(this.lastSuccessTime) {
			return this.lastSuccessTime;
		}	
	}
	
	public String getErrorStatus() {
		if(this.lastNumTenants == this.lastSuccessfulTenants) {
			return null;
		} //else
		return String.format("%d/%d EDI working", this.lastSuccessfulTenants, this.lastNumTenants);
	}

	private boolean doEdiForTenant(Tenant tenant) {
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
				numChecked += doEdiForFacility(facility);
			}
			TenantPersistenceService.getInstance().commitTransaction();
			completed = true;
		} catch (RuntimeException e) {
			LOGGER.error("Unable to process edi for tenant " + tenant.getId(), e);
		} finally {
			long endTime = System.currentTimeMillis();
			if (timerContext != null) {
				timerContext.stop();
			}
			LOGGER.info("Checked for updates from {} EDI services for tenant {} in {}s",
					numChecked,
					tenant.getName(),
					(endTime - startTime) / 1000);
			CodeshelfSecurityManager.removeContext();

			if (!completed) {
				LOGGER.warn("EDI process did not complete successfully for tenant {}", tenant.getName());
				TenantPersistenceService.getInstance().rollbackTransaction();
			}
		}
		return completed;
	}

	//package level for testing
	int doEdiForFacility(Facility facility) {
		int numChecked = 0;
		for (IEdiImportGateway ediGateway : facility.getLinkedEdiImportGateways()) { 
			if (ediGateway.isActive()){
				try {
					if (ediGateway.getUpdatesFromHost(mCsvOrderImporter.get(),
						mCsvOrderLocationImporter.get(),
						mCsvInventoryImporter.get(),
						mCsvLocationAliasImporter.get(),
						mCsvCrossBatchImporter.get(),
						mCsvAislesFileImporter.get())) {
						ediGateway.updateLastSuccessTime();
						numChecked++;
					}
				} catch(Exception e) {
					LOGGER.warn("EDI import update failed for service {}", ediGateway, e);
				}
			}
		}
		return numChecked;
	}

	@Override
	protected void shutDown() throws Exception {
		LOGGER.info("{} is being shutdown", this);
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(Integer.getInteger("service.edi.init.delay", 0),
			this.periodSeconds,
			TimeUnit.SECONDS);
	}
}
