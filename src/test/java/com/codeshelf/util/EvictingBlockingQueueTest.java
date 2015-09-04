package com.codeshelf.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;


public class EvictingBlockingQueueTest {

	@Test
	public void testEvictsOldest() throws InterruptedException {
		EvictingBlockingQueue<String> subject = new EvictingBlockingQueue<String>(1);
		subject.offer("a");
		Assert.assertEquals("a", subject.take());
		
		Assert.assertTrue(subject.isEmpty());
		
		subject.offer("b");
		subject.offer("c");
		Assert.assertEquals("c", subject.take());
		
	}

	@Test
	public void testBlocks() throws InterruptedException, ExecutionException, TimeoutException {
		final EvictingBlockingQueue<String> subject = new EvictingBlockingQueue<String>(1);
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> result = executor.submit(new Callable<String>(){

			@Override
			public String call() throws Exception {
				// TODO Auto-generated method stub
				return subject.take();
			}});
		Assert.assertTrue(!result.isDone());
		
		subject.offer("test");
		
		Assert.assertEquals("test", result.get(500, TimeUnit.MILLISECONDS));
		
		
	}

	
}
