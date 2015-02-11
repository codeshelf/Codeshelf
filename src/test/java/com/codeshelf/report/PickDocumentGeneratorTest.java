/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: PickDocumentGeneratorTest.java,v 1.1 2013/03/19 01:19:59 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.report;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Assert;
import org.junit.Test;

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

		Thread foundThread = findPickDocumentGeneratorThread();
		Assert.assertFalse(foundThread == null); // running

		pickDocumentGenerator.stopProcessor();
		// That thread might be sleeping, interrupt if so
		if (foundThread != null) {
			foundThread.interrupt();
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		} // give it a sec to stop

		Assert.assertNull(findPickDocumentGeneratorThread()); // no longer running
	}
	
	Thread findPickDocumentGeneratorThread() {
		Thread found = null;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals(IPickDocumentGenerator.PICK_DOCUMENT_GENERATOR_THREAD_NAME)) {
				found = thread;
			}
		}
		return found;
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
