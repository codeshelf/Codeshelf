package com.codeshelf.model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.util.BlockingArrayQueue;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJob implements Job, InterruptableJob {

	public static final Object CANCELLED = new Object();
	
	private static final Logger LOGGER	= LoggerFactory.getLogger(TestJob.class);
	private static BlockingQueue<TestJob> instances = new BlockingArrayQueue<>();
	private Lock monitor;
	private Condition runningCondition;
	private CyclicBarrier runBarrier= new CyclicBarrier(2);
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private AtomicBoolean  isCancelled = new AtomicBoolean(false);
	
	public TestJob() {
		LOGGER.info("Constructing TestJob instance");
		monitor = new ReentrantLock();
		runningCondition = monitor.newCondition();
		instances.offer(this);
		
	}

	public void awaitRunning() throws InterruptedException, TimeoutException {
		try {
			monitor.lock();
			if (isRunning()) {
				return;
			} else {
				runningCondition.await(5, TimeUnit.SECONDS);
				if (!isRunning()) {
					throw new TimeoutException("timeout awaiting running state");
				}
			}
		} finally {
			monitor.unlock();
		}
	}
	
	public boolean isRunning() {
		return isRunning.get();
	}

	public void interrupt() {
		isCancelled.set(true);;
		LOGGER.info("Interrupting test job");
		runBarrier.reset();
	}

	public boolean isCancelled() {
		return isCancelled.get();
	}

	
	public void proceed() throws InterruptedException, BrokenBarrierException, TimeoutException {
		LOGGER.info("Parties waiting for proceed: {}", runBarrier.getNumberWaiting());
		runBarrier.await(5, TimeUnit.SECONDS); //failsafe for test completion
	}
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			monitor.lock();
			isRunning.set(true);
			runningCondition.signalAll();
			LOGGER.info("Executing test job, running: {}", isRunning());
		} finally {
			monitor.unlock();
		}
		
		try {
			if (isCancelled()) {
				LOGGER.info("Cancelled test job");
				throw new CancellationException("Cancelled test job");
			}
			LOGGER.info("Parties waiting for execution: {}", runBarrier.getNumberWaiting());
			runBarrier.await(5, TimeUnit.SECONDS); //failsafe for test completion
			isRunning.set(false);
			LOGGER.info("Completed test job");
			context.setResult(new Object());
		} catch (InterruptedException | TimeoutException e) {
			throw new JobExecutionException(e);
		} catch(BrokenBarrierException e) {
			LOGGER.info("Cancelled test job");
			isCancelled.set(true);;
			throw new CancellationException("Cancelled test job");
		}
	}
	
	public static TestJob pollInstance() throws InterruptedException {
		return instances.poll(5, TimeUnit.SECONDS);
	}

	
}
