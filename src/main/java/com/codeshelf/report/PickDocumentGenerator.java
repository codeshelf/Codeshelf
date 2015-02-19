/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: PickDocumentGenerator.java,v 1.1 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.report;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * @author jeffw
 *
 */
public class PickDocumentGenerator implements IPickDocumentGenerator {

	private static final Logger		LOGGER		= LoggerFactory.getLogger(PickDocumentGenerator.class);

	private static final String		SHUTDOWN	= "$$$SHTUDOWN$$$";

	private boolean					mShouldRun;
	private Thread					mProcessorThread;
	private BlockingQueue<String>	mSignalQueue;

	@Inject
	public PickDocumentGenerator() {

	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void startProcessor(final BlockingQueue<String> inEdiSignalQueue) {
		mShouldRun = true;
		try {
			Thread.sleep(Integer.getInteger("service.pickdocgen.init.delay"));
		} catch (Exception e) {
		}
		mSignalQueue = inEdiSignalQueue;
		mProcessorThread = new Thread(new Runnable() {
			public void run() {
				process();
			}
		}, PICK_DOCUMENT_GENERATOR_THREAD_NAME);
		mProcessorThread.setDaemon(true);
		mProcessorThread.setPriority(Thread.MIN_PRIORITY);
		mProcessorThread.start();
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public final void stopProcessor() {
		mShouldRun = false;
		try {
			// Send a shutdown signal to the queue so that it loops and exits its thread.
			mSignalQueue.put(SHUTDOWN);
		} catch (InterruptedException e) {
			LOGGER.error("", e);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	private void process() {
		while (mShouldRun) {
			try {
				String signalName = mSignalQueue.take();

				if (signalName.equals(SHUTDOWN)) {
					mShouldRun = false;
				} else {
					LOGGER.debug("Pick doc generator received signal from: " + signalName);
					//createDocuments();
				} 

			} catch (Exception e) {
				// We don't want the thread to exit on some weird, uncaught errors in the processor.
				LOGGER.error("", e);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	private void createDocuments() {
		try {
			URL defaultDocUrl = ClassLoader.getSystemClassLoader().getResource("conf/DefaultPickDoc.jasper");
			if (defaultDocUrl != null) {
				//JasperDesign jasperDesign = JRXmlLoader.load(defaultDocUrl.openStream());
				//JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
				JasperReport jasperReport = (JasperReport) JRLoader.loadObject(defaultDocUrl);

				Map<String,Object> parameters = new HashMap<String,Object>();

				String xml = "<codeshelf></codeshelf>";
				InputStream xmlStream = new ByteArrayInputStream(xml.getBytes());
			    JRXmlDataSource xmlDataSource = new JRXmlDataSource(xmlStream, "/*");
				JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, xmlDataSource);

				JasperExportManager.exportReportToPdfFile(jasperPrint, "conf/DefaultPickDoc.pdf");
			}
		} catch (SecurityException | JRException e) {
			LOGGER.error("", e);
		}

	}
	 */
}
