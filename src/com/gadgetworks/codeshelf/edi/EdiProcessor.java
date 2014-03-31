/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessor.java,v 1.20 2013/04/11 18:11:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.IEdiService;
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

	private ICsvOrderImporter			mCsvOrderImporter;
	private ICsvOrderLocationImporter	mCsvOrderLocationImporter;
	private ICsvInventoryImporter		mCsvInventoryImporter;
	private ICsvLocationAliasImporter	mCsvLocationAliasImporter;
	private ICsvCrossBatchImporter		mCsvCrossBatchImporter;
	private ITypedDao<Facility>			mFacilityDao;

	@Inject
	public EdiProcessor(final ICsvOrderImporter inCsvOrdersImporter,
		final ICsvInventoryImporter inCsvInventoryImporter,
		final ICsvLocationAliasImporter inCsvLocationsImporter,
		final ICsvOrderLocationImporter inCsvOrderLocationImporter,
		final ICsvCrossBatchImporter inCsvCrossBatchImporter,
		final ITypedDao<Facility> inFacilityDao) {

		mCsvOrderImporter = inCsvOrdersImporter;
		mCsvOrderLocationImporter = inCsvOrderLocationImporter;
		mCsvInventoryImporter = inCsvInventoryImporter;
		mCsvLocationAliasImporter = inCsvLocationsImporter;
		mCsvCrossBatchImporter = inCsvCrossBatchImporter;
		mFacilityDao = inFacilityDao;

		mShouldRun = false;
		mLastProcessMillis = 0;

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void startProcessor(final BlockingQueue<String> inEdiSignalQueue) {
		mShouldRun = true;
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
		}
		mProcessorThread = new Thread(new Runnable() {
			public void run() {
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

		LOGGER.debug("Begin EDI process.");

		// Loop through each facility to make sure that it's EDI service processes any queued EDI.
		for (Facility facility : mFacilityDao.getAll()) {
			for (IEdiService ediService : facility.getEdiServices()) {
				if (ediService.getServiceStateEnum().equals(EdiServiceStateEnum.LINKED)) {
					if (ediService.getUpdatesFromHost(mCsvOrderImporter,
						mCsvOrderLocationImporter,
						mCsvInventoryImporter,
						mCsvLocationAliasImporter,
						mCsvCrossBatchImporter)) {
						// Signal other threads that we've just processed new EDI.
						try {
							inEdiSignalQueue.put(ediService.getServiceName());
						} catch (InterruptedException e) {
							LOGGER.error("", e);
						}
					}
				}
			}
		}

		LOGGER.debug("End EDI process.");
	}
}
