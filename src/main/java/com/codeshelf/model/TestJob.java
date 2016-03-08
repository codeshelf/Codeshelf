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
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.scheduler.AbstractFacilityJob;

import lombok.Getter;

public class TestJob extends AbstractFacilityJob {

	public static final Object CANCELLED = new Object();
	
	public static boolean BlockingJobs = true;
	public static int runCount = 0;
	
	private static final Logger LOGGER	= LoggerFactory.getLogger(TestJob.class);
	private static BlockingQueue<TestJob> instances = new BlockingArrayQueue<>();

	private Condition runningCondition;
	private CyclicBarrier runBarrier;

	private Lock monitor;
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private AtomicBoolean  isCancelled = new AtomicBoolean(false);

	@Getter
	private Tenant executionTenant;
	@Getter
	private Facility executionFacility;
	
	public TestJob() {
		LOGGER.info("Constructing TestJob instance");
		monitor = new ReentrantLock();
		runningCondition = monitor.newCondition();
		if (BlockingJobs) {
			runBarrier= new CyclicBarrier(2);
			instances.offer(this);
		} 
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
	
	protected Object doFacilityExecute(Tenant tenant, Facility facility) throws JobExecutionException {
		executionFacility = facility;
		executionTenant = tenant;
		
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
			if (runBarrier != null) {
				LOGGER.info("Parties waiting for execution: {}", runBarrier.getNumberWaiting());
				runBarrier.await(5, TimeUnit.SECONDS); //failsafe for test completion
			}
			isRunning.set(false);
			LOGGER.info("Completed test job");
			runCount++;
			return new Object();
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
