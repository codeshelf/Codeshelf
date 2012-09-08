/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: EdiProcessor.java,v 1.1 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.WebAuthSession;
import com.gadgetworks.codeshelf.model.EdiProviderEnum;
import com.gadgetworks.codeshelf.model.EdiServiceStateEnum;
import com.gadgetworks.codeshelf.model.IEdiService;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.domain.DropboxService;
import com.gadgetworks.codeshelf.model.domain.EdiServiceABC;
import com.gadgetworks.codeshelf.model.domain.Facility;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class EdiProcessor {

	public static final long				PROCESS_INTERVAL_MILLIS		= 1 * 60 * 1000;

	private static final Log				LOGGER						= LogFactory.getLog(EdiProcessor.class);

	private static final String				EDIPROCESSOR_THREAD_NAME	= "EDI Processor";

	private static volatile EdiProcessor	mEdiProcessor;

	private long							mLastProcessMillis;
	private boolean							mShouldRun;
	private Thread							mProcessorThread;
	private ThreadGroup						mProcessThreadGroup;

	private Facility						mFacility;

	public EdiProcessor(final Facility inFacility) {
		mFacility = inFacility;
		mShouldRun = false;
		mLastProcessMillis = 0;
		mProcessThreadGroup = new ThreadGroup(EDIPROCESSOR_THREAD_NAME);
	}

	public static ThreadGroup getProcessingThreadGroup() {
		ThreadGroup result = null;
		if (mEdiProcessor != null) {
			result = mEdiProcessor.doGetProcessingThreadGroup();
		}
		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void startProcessor(final Facility inFacility) {
		if (mEdiProcessor == null) {
			mEdiProcessor = new EdiProcessor(inFacility);
		}
		mEdiProcessor.doStartProcessor();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void restartProcessor() {
		if (mEdiProcessor != null) {
			mEdiProcessor.doRestartProcessor();
		} else {
			LOGGER.error("restartEventHarvester(): EdiProcessor was never started!");
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public static void stopProcessor() {
		if (mEdiProcessor != null) {
			mEdiProcessor.doStopProcessor();
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inHooBee
	 */
	public static void processForDropbox() {
		mEdiProcessor.checkEdiServices();
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
	private void doStartProcessor() {
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
	private void doRestartProcessor() {
		mLastProcessMillis = 0;

		doStopProcessor();
		// Loop until the existing threads stop.
		while (mProcessorThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				LOGGER.error("", e);
			}
		}
		doStartProcessor();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void doStopProcessor() {
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

//		try {
//			AppKeyPair appKeyPair = new AppKeyPair("feh3ontnajdmmin", "4jm05vbugwnq9pe");
//			WebAuthSession was = new WebAuthSession(appKeyPair, Session.AccessType.APP_FOLDER);
//
//			// Make the user log in and authorize us.
//			WebAuthSession.WebAuthInfo info = was.getAuthInfo();
//			LOGGER.info(info.url);
//
//			was.retrieveWebAccessToken(info.requestTokenPair);
//			AccessTokenPair accessToken = was.getAccessTokenPair();
//			LOGGER.info(accessToken);
//
//			ObjectMapper mapper = new ObjectMapper();
//			ObjectNode credentialsNode = mapper.createObjectNode();
//			ObjectNode appNode = credentialsNode.putObject("appToken");
//			appNode.put("key", "feh3ontnajdmmin");
//			appNode.put("secret", "4jm05vbugwnq9pe");
//			ObjectNode accessNode = credentialsNode.putObject("accessToken");
//			accessNode.put("key", accessToken.key);
//			accessNode.put("secret", accessToken.secret);
//			String credentials = credentialsNode.asText();
//
//			DropboxService dropbox = new DropboxService();
//			dropbox.setDomainId("dropbox");
//			dropbox.setParentFacility(mFacility);
//			dropbox.setProviderEnum(EdiProviderEnum.DROPBOX);
//			dropbox.setServiceStateEnum(EdiServiceStateEnum.REGISTERED);
//			dropbox.setProviderCredentials(credentials);
//			try {
//				dropbox.getDao().store(dropbox);
//			} catch (DaoException e) {
//				LOGGER.error("", e);
//			}
//
//		} catch (DropboxException e) {
//			LOGGER.error("", e);
//		}

		Collection<EdiServiceABC> ediServices = EdiServiceABC.DAO.getAll();
		for (IEdiService ediService : ediServices) {
			if (ediService.getServiceStateEnum().equals(EdiServiceStateEnum.REGISTERED)) {
				ediService.updateDocuments();
			}
		}

		LOGGER.debug("End EDI harvest cycle.");
	}
}
