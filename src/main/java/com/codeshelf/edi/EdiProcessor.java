/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessor.java,v 1.20 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.util.concurrent.BlockingQueue;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codeshelf.metrics.MetricsGroup;
import com.codeshelf.metrics.MetricsService;
import com.codeshelf.model.EdiServiceStateEnum;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.IEdiService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.google.inject.Inject;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class EdiProcessor implements IEdiProcessor {

	public static final long			PROCESS_INTERVAL_MILLIS	= 1 * 30 * 1000;

	private static final Logger			LOGGER					= LoggerFactory.getLogger(EdiProcessor.class);

	private long						mLastProcessMillis;
	private boolean						mShouldRun;
	private Thread						mProcessorThread;

	@Getter
	private TenantPersistenceService			tenantPersistenceService;

	private ICsvOrderImporter			mCsvOrderImporter;
	private ICsvOrderLocationImporter	mCsvOrderLocationImporter;
	private ICsvInventoryImporter		mCsvInventoryImporter;
	private ICsvLocationAliasImporter	mCsvLocationAliasImporter;
	private ICsvAislesFileImporter		mCsvAislesFileImporter;
	private ICsvCrossBatchImporter		mCsvCrossBatchImporter;
	private ITypedDao<Facility>			mFacilityDao;

	private final Timer					ediProcessingTimer		= MetricsService.addTimer(MetricsGroup.EDI, "processing-time");

	@Inject
	public EdiProcessor(final ICsvOrderImporter inCsvOrdersImporter,
		final ICsvInventoryImporter inCsvInventoryImporter,
		final ICsvLocationAliasImporter inCsvLocationsImporter,
		final ICsvOrderLocationImporter inCsvOrderLocationImporter,
		final ICsvCrossBatchImporter inCsvCrossBatchImporter,
		final ICsvAislesFileImporter inCsvAislesFileImporter,
		final ITypedDao<Facility> inFacilityDao,
		final TenantPersistenceService tenantPersistenceService) {

		mCsvOrderImporter = inCsvOrdersImporter;
		mCsvOrderLocationImporter = inCsvOrderLocationImporter;
		mCsvInventoryImporter = inCsvInventoryImporter;
		mCsvLocationAliasImporter = inCsvLocationsImporter;
		mCsvAislesFileImporter = inCsvAislesFileImporter;
		mCsvCrossBatchImporter = inCsvCrossBatchImporter;
		mFacilityDao = inFacilityDao;

		mShouldRun = false;
		mLastProcessMillis = 0;

		this.tenantPersistenceService = tenantPersistenceService;

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void startProcessor(final BlockingQueue<String> inEdiSignalQueue) {
		mShouldRun = true;
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
		}
		mProcessorThread = new Thread(new Runnable() {
			public void run() {
				//ContextLogging.setUser("SYSTEM");
				process(inEdiSignalQueue);
			}
		}, EDIPROCESSOR_THREAD_NAME);
		mProcessorThread.setDaemon(true);
		mProcessorThread.setPriority(Thread.MIN_PRIORITY);
		mProcessorThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void stopProcessor() {
		mShouldRun = false;
		if (mProcessorThread != null) {
			mProcessorThread.interrupt();
			long timeout = 5000;
			try {
				mProcessorThread.join(timeout);
			} catch (InterruptedException e) {
				LOGGER.error("EdiProcessor thread did not stop within " + timeout, e);
			}
		} else {
			LOGGER.warn("EdiProcessor has not been started");
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void process(final BlockingQueue<String> inEdiSignalQueue) {
		while (mShouldRun) {
			try {
				if (System.currentTimeMillis() > (mLastProcessMillis + PROCESS_INTERVAL_MILLIS)) {
					// Time to check EDI.
					checkEdiServices(inEdiSignalQueue);
					mLastProcessMillis = System.currentTimeMillis();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.error("", e);
				}
			} catch (Exception e) {
				// We don't want the thread to exit on some weird, uncaught errors in the processor.
				LOGGER.error("", e);
				mLastProcessMillis = System.currentTimeMillis();
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void checkEdiServices(BlockingQueue<String> inEdiSignalQueue) {
		boolean completed = false;
		int numChecked = 0;

		LOGGER.trace("Begin EDI process.");
		final Timer.Context context = ediProcessingTimer.time();
		try {
			this.getTenantPersistenceService().beginTransaction();


			// Loop through each facility to make sure that it's EDI service processes any queued EDI.
			for (Facility facility : mFacilityDao.getAll()) {
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
								inEdiSignalQueue.put(ediService.getServiceName());
							} catch (InterruptedException e) {
								LOGGER.error("Failed to signal other threads that we've just processed n EDI", e);
							}
						}
					}
				}
			}
			this.getTenantPersistenceService().commitTransaction();
			completed = true;
		} catch (RuntimeException e) {
			this.getTenantPersistenceService().rollbackTenantTransaction();
			LOGGER.error("Unable to process edi", e);
		} finally {
			context.stop();
			if(completed) {
				LOGGER.info("Checked for updates from "+numChecked+" EDI services");
			} else {
				LOGGER.warn("EDI process did not complete successfully.");
			}
		}

	}
}