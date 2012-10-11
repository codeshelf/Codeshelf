/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessor.java,v 1.12 2012/10/11 02:42:39 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

	public static final long	PROCESS_INTERVAL_MILLIS	= 1 * 10 * 1000;

	private static final Log	LOGGER					= LogFactory.getLog(EdiProcessor.class);

	private long				mLastProcessMillis;
	private boolean				mShouldRun;
	private Thread				mProcessorThread;

	private IOrderImporter		mOrderImporter;
	private ITypedDao<Facility>	mFacilityDao;

	@Inject
	public EdiProcessor(final IOrderImporter inOrderImporter, final ITypedDao<Facility> inFacilityDao) {

		mOrderImporter = inOrderImporter;
		mFacilityDao = inFacilityDao;

		mShouldRun = false;
		mLastProcessMillis = 0;

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void startProcessor() {
		mShouldRun = true;
		mProcessorThread = new Thread(new Runnable() {
			public void run() {
				process();
			}
		}, EDIPROCESSOR_THREAD_NAME);
		mProcessorThread.setDaemon(true);
		mProcessorThread.setPriority(Thread.MIN_PRIORITY);
		mProcessorThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void restartProcessor() {
		mLastProcessMillis = 0;

		stopProcessor();
		// Loop until the existing threads stop.
		while (mProcessorThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
		startProcessor();
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
	private void process() {
		while (mShouldRun) {
			try {
				if (System.currentTimeMillis() > (mLastProcessMillis + PROCESS_INTERVAL_MILLIS)) {
					// Time to harvest events.
					checkEdiServices();
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
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void checkEdiServices() {

		LOGGER.debug("Begin EDI harvest cycle.");

		// Loop through each facility to make sure that it's EDI service processes any queued EDI.
		for (Facility facility : mFacilityDao.getAll()) {
			for (IEdiService ediService : facility.getEdiServices()) {
				if (ediService.getServiceStateEnum().equals(EdiServiceStateEnum.LINKED)) {
					ediService.checkForOrderUpdates(mOrderImporter);
				}
			}
		}

		LOGGER.debug("End EDI harvest cycle.");
	}
}
