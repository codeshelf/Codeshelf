package com.codeshelf.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.ContextLogging;

/**
 * 
 * @author ilya
 *
 * This test class verifies that ThreadContext class we use to store logging tags is indeed isolated per thread.
 * So settings or clearing logging tags in one thread does not affect loggin from another thread
 * This is part of work for 
 * CD_0147 Logging context for tenant and facility  
 */
public class ThreadLoggingTest {
	private static final Logger	LOGGER		= LoggerFactory.getLogger(ThreadLoggingTest.class);
	
	@Test
	/**
	 * Launch a single thread off the main thread
	 */
	public void loggingTest1(){
		ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_NETGUID_KEY, "Main Thread");
		LOGGER.info("Main Thread Start");
		new LogThread("Thread 1").start();
		ThreadUtils.sleep(100);
		LOGGER.info("Main Thread Done");
		ThreadUtils.sleep(150);
	}
	
	@Test
	/**
	 * Sequentially run 2 threads off the main thread
	 */
	public void loggingTest2(){
		ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_NETGUID_KEY, "Main Thread");
		LOGGER.info("Main Thread Start");
		new LogThread("Thread 1").start();
		ThreadUtils.sleep(600);
		new LogThread("Thread 2").start();
		ThreadUtils.sleep(600);
		LOGGER.info("Main Thread Done");
	}

	@Test
	/**
	 * Launch a thread A off the main thread.
	 * Then launch thread B off thread A
	 * Let A finish before B finishes 
	 */
	public void loggingTest3(){
		ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_NETGUID_KEY, "Main Thread");
		LOGGER.info("Main Thread Start");
		new LogRecursiveThread().start();
		ThreadUtils.sleep(1000);
		LOGGER.info("Main Thread Done");

	}

	private class LogThread extends Thread{
		private String tagValue = null;
		
		public LogThread(String tagValue) {
			this.tagValue = tagValue;
		}
		
		@Override
		public synchronized void run() {
			ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_NETGUID_KEY, tagValue);
			LOGGER.info(tagValue + " start");
			ThreadUtils.sleep(200);
			LOGGER.info(tagValue + " done");
			ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_NETGUID_KEY, null);
		}
	}
		
	private class LogRecursiveThread extends Thread{
		@Override
		public synchronized void run() {
			String threadName = "Thread A";
			ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_NETGUID_KEY, threadName);
			LOGGER.info(threadName + " start");
			new LogThread("Thread B").start();
			ThreadUtils.sleep(200);
			LOGGER.info(threadName + " done");
			ContextLogging.setTag(ContextLogging.THREAD_CONTEXT_NETGUID_KEY, null);
		}
	}
}
