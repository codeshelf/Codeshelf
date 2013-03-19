/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PickDocumentGeneratorTest.java,v 1.1 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.report;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.edi.IEdiProcessor;

/**
 * @author jeffw
 *
 */
public class PickDocumentGeneratorTest {

	@Test
	public void pickDocGeneratorThreadTest() {


		IPickDocumentGenerator pickDocumentGenerator = new PickDocumentGenerator();
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		pickDocumentGenerator.startProcessor(testBlockingQueue);

		Thread foundThread = null;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals(IPickDocumentGenerator.PICK_DOCUMENT_GENERATOR_THREAD_NAME)) {
				foundThread = thread;
			}
		}

		Assert.assertNotNull(foundThread);

		pickDocumentGenerator.stopProcessor();

		// That thread might be sleeping.
		if (foundThread != null) {
			foundThread.interrupt();
		}

		foundThread = null;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals(IPickDocumentGenerator.PICK_DOCUMENT_GENERATOR_THREAD_NAME)) {
				foundThread = thread;
			}
		}

		Assert.assertNotNull(foundThread);
	}

	@Test
	public void pickDocGenProcessorTest() {

		IPickDocumentGenerator pickDocumentGenerator = new PickDocumentGenerator();
		BlockingQueue<String> testBlockingQueue = new ArrayBlockingQueue<>(100);
		pickDocumentGenerator.startProcessor(testBlockingQueue);
		
		try {
			testBlockingQueue.put("TEST");
			// Causes a switch to the other thread.
			Thread.sleep(100);
		} catch (InterruptedException e) {

		}

		Assert.assertTrue(testBlockingQueue.size() == 0);
	}
}
