/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiHarvester.java,v 1.1 2012/09/06 22:59:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class EdiHarvester {

	public static final long				HARVEST_INTERVAL_MILLIS	= 10 * 60 * 1000;

	private static final Log				LOGGER					= LogFactory.getLog(EdiHarvester.class);

	private static final String				HARVEST_THREAD_NAME		= "EDI Harvester";

	private static volatile EdiHarvester	mEventHarvester;

	private long							mLastHarvestMillis;
	private boolean							mShouldRun;
	private Thread							mHarvestThread;
	private ThreadGroup						mProcessThreadGroup;

	public EdiHarvester() {
		mShouldRun = false;
		mLastHarvestMillis = 0;
		mProcessThreadGroup = new ThreadGroup(HARVEST_THREAD_NAME);
	}

	public static ThreadGroup getProcessingThreadGroup() {
		ThreadGroup result = null;
		if (mEventHarvester != null) {
			result = mEventHarvester.doGetProcessingThreadGroup();
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void startEventHarvester() {
		if (mEventHarvester == null) {
			mEventHarvester = new EdiHarvester();
		}
		mEventHarvester.doStartEventHarvester();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void restartEventHarvester() {
		if (mEventHarvester != null) {
			mEventHarvester.doRestartEventHarvester();
		} else {
			LOGGER.error("restartEventHarvester(): EdiHarvester was never started!");
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void stopEventHarvester() {
		if (mEventHarvester != null) {
			mEventHarvester.doStopEventHarvester();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inHooBee
	 */
	public static void harvestEventsForDropbox() {
		mEventHarvester.doHarvestEventsForHooBee();
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public ThreadGroup doGetProcessingThreadGroup() {
		return mProcessThreadGroup;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void doStartEventHarvester() {
		mShouldRun = true;
		mHarvestThread = new Thread(new Runnable() {
			public void run() {
				harvestEvents();
			}
		}, HARVEST_THREAD_NAME);
		mHarvestThread.setDaemon(true);
		mHarvestThread.setPriority(Thread.MIN_PRIORITY);
		mHarvestThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void doRestartEventHarvester() {
		mLastHarvestMillis = 0;

		doStopEventHarvester();
		// Loop until the existing threads stop.
		while (mHarvestThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
		doStartEventHarvester();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void doStopEventHarvester() {
		mShouldRun = false;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void harvestEvents() {
		while (mShouldRun) {
			if (System.currentTimeMillis() > (mLastHarvestMillis + HARVEST_INTERVAL_MILLIS)) {

				// Time to harvest events.

				mLastHarvestMillis = System.currentTimeMillis();
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void cleanupRemoteDataCaches() {
		// Cleanup the remote data cache items (that are old).
		// (Add to a delete list, so that we don't get a concurrent mod exception on the iterator.)
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inHooBee
	 */
	private void doHarvestEventsForHooBee() {

		LOGGER.debug("Begin EDI harvest cycle.");

		LOGGER.debug("End event harvest cycle.");
	}

}
