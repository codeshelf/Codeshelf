package com.codeshelf.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.CronExpression;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.TestJob;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.scheduler.ScheduledJobType;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.google.common.base.Optional;

public class FacilitySchedulerServiceTest {

	private FacilitySchedulerService subject;
	private Facility facility;
	
	@Before
	public void setUp() throws SchedulerException, ParseException {
		String schedulerName = "testname";
		SimpleThreadPool threadPool = new SimpleThreadPool(1, Thread.MIN_PRIORITY);
		DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();
		RAMJobStore store = new RAMJobStore();
		store.setMisfireThreshold(100); //basically if paused or delayed skip execution
		schedulerFactory.createScheduler(schedulerName, schedulerName, threadPool, store);
		Scheduler scheduler = schedulerFactory.getScheduler(schedulerName);
		
		
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		Tenant tenant = mock(Tenant.class);
		facility = mock(Facility.class);
		
		when(facility.getTimeZone()).thenReturn(TimeZone.getTimeZone(TimeZone.getAvailableIDs()[1]));
		subject = new FacilitySchedulerService(scheduler, systemUser, tenant, facility);
		subject.startAsync();
		subject.awaitRunningOrThrow();
		
	}

	@After
	public void teardown() {
		subject.stopAsync();
		subject.awaitTerminatedOrThrow();
	}

	@Test
	public void cronExpressionMatchFacilityTimeZone() throws SchedulerException, ParseException {
		ScheduledJobType type = ScheduledJobType.Test;
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		Assert.assertNotEquals(firstExp.getTimeZone(), facility.getTimeZone());
		
		subject.schedule(firstExp, type);
		CronExpression expression = subject.findSchedule(type);
		Assert.assertEquals(expression.getTimeZone(), facility.getTimeZone());
	}
	
	
	@Test
	public void testPauseResumeAreIdempotent() throws SchedulerException, ParseException {
		ScheduledJobType testType = ScheduledJobType.Test;	
		scheduleTestJob(testType);
		subject.pauseJob(testType);
		subject.pauseJob(testType);
		subject.resumeJob(testType);
		subject.resumeJob(testType);
	}
	
	@Test
	public void testRescheduleJobClass() throws ParseException, SchedulerException  {
		ScheduledJobType type = ScheduledJobType.Test;
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		subject.schedule(firstExp, type);

		CronExpression secondExp = new CronExpression("0 0 3 * * ?");
		CronExpression sameSecondExp = new CronExpression("0 0 3 * * ?");
		
		
		subject.schedule(secondExp, type);
		Map<ScheduledJobType, CronExpression> scheduledJobs = subject.getJobs();

		Assert.assertEquals(secondExp.getCronExpression(), sameSecondExp.getCronExpression());
		Assert.assertNotEquals(firstExp, secondExp);
		Assert.assertEquals(secondExp.getCronExpression(), scheduledJobs.get(type).getCronExpression());

	}
	
	/**
	 * Running jobs stop ASAP when shutdown is requested
	 */
	@Test
	public void runningJobCancelledOnShutdown() {
	}
	/**
	 * Test that the resuming of a paused job does not try to catch up
	 */
	@Test
	public void pausedJobDoesNotRun() throws SchedulerException, ParseException, InterruptedException, TimeoutException, BrokenBarrierException {
		try {
			TestJob.BlockingJobs = false;
			ScheduledJobType testType = ScheduledJobType.Test;	
			CronExpression firstExp = new CronExpression("* * * * * ?");
			int total = TestJob.runCount;
			subject.schedule(firstExp, testType);
			Thread.sleep(3000);
			subject.pauseJob(testType);
			Thread.sleep(3000);
			subject.resumeJob(testType);
			Thread.sleep(3000);
			int newTotal = TestJob.runCount;
			Assert.assertTrue("should have resumed", newTotal > total);
			Assert.assertTrue("should have run about 6 times but was " + (newTotal - total), newTotal - total < 7);
		}
		finally {
			TestJob.BlockingJobs = true;
			
		}

	}
	
	@Test
	public void manualJobIfPaused() throws ParseException, SchedulerException, InterruptedException, ExecutionException, TimeoutException, BrokenBarrierException {
		ScheduledJobType testType = ScheduledJobType.Test;	
		scheduleTestJob(testType);
		subject.pauseJob(testType);
		triggerWait(testType);
	}
	
	@Test
	public void manualJobTriggersIfNotRunning() throws ParseException, SchedulerException, InterruptedException, ExecutionException, TimeoutException, BrokenBarrierException {
		ScheduledJobType testType = ScheduledJobType.Test;	
		scheduleTestJob(testType);
		triggerWait(testType);
	}
	
	@Test
	public void noManualJobIfRunning() throws Exception {
		ScheduledJobType testType = ScheduledJobType.Test;	
		scheduleTestJob(testType);

		Future<ScheduledJobType> future1 = subject.trigger(testType);
		TestJob job1 = TestJob.pollInstance();
		job1.awaitRunning();
		try {
			subject.trigger(testType);
			Assert.fail("Should have prevented trigger while running");
		} catch(SchedulerException e) {
			
		}
		Assert.assertTrue(job1.isRunning());
		job1.proceed();
		future1.get(2, TimeUnit.SECONDS);

	}
	
	@Test
	public void scheduledJobRunsNextScheduleAfterManual() {
		
	}
	
	@Test
	public void inctivateAllFacilityJobs() {
		
	}

	/**
	 * Each job class belongs to a group allowing control by group
	 */
	@Test
	public void inactivateJobsByCategory() {
		
	}
	
	/**
	 * Running jobs can be cancelled
	 * @throws ParseException 
	 * @throws SchedulerException 
	 * @throws BrokenBarrierException 
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 */
	@Test
	public void manualCancelOfRunningJob() throws ParseException, SchedulerException, InterruptedException, BrokenBarrierException, ExecutionException, TimeoutException {
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		ScheduledJobType testType = ScheduledJobType.Test;	
		subject.schedule(firstExp, testType);
		Future<ScheduledJobType> future = subject.trigger(testType);
		
		Assert.assertTrue(!future.isDone() && !future.isCancelled());

		TestJob control = TestJob.pollInstance();
		control.awaitRunning();
		Assert.assertTrue("job was not able to be cancelled", subject.cancelJob(testType));
		try {
			future.get();
			Assert.fail("should have been cancelled");
		} catch(CancellationException e) {
			
		}
		Assert.assertTrue(future.isCancelled());
		Assert.assertTrue(control.isCancelled());
	}
	
	@Test
	public void missedJobsAreLogged() {
		
	}
	
	@Test
	public void viewWhichJobsAreRunning() throws ParseException, SchedulerException, InterruptedException, TimeoutException, BrokenBarrierException, ExecutionException {	
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		ScheduledJobType testType = ScheduledJobType.Test;	
		subject.schedule(firstExp, testType);

		
		List<JobExecutionContext> runningJobsBeforeStart = subject.getRunningJobs();
		Assert.assertEquals(0, runningJobsBeforeStart.size());
		Future<ScheduledJobType> future = subject.trigger(testType);
		TestJob control = TestJob.pollInstance();
		control.awaitRunning();
		
		List<JobExecutionContext> runningJobsAfterStart = subject.getRunningJobs();
		Assert.assertEquals(1, runningJobsAfterStart.size());
		Assert.assertEquals(testType.getKey(), runningJobsAfterStart.get(0).getJobDetail().getKey());

		control.proceed();
		future.get();
		Thread.sleep(100);// scheduler needs to register completion
		List<JobExecutionContext> runningJobsAfterFinished = subject.getRunningJobs();
		Assert.assertEquals(0, runningJobsAfterFinished.size());
	}

	@Test
	public void viewJobSchedules() throws ParseException, SchedulerException {
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		ScheduledJobType testType = ScheduledJobType.Test;	
		subject.schedule(firstExp, testType);

		CronExpression secondExp = new CronExpression("0 0 3 * * ?");
		ScheduledJobType otherType = ScheduledJobType.DatabasePurge;	//any will do 
		subject.schedule(secondExp, otherType);
		
		Map<ScheduledJobType, CronExpression> jobs = subject.getJobs();
		Assert.assertTrue(jobs.keySet().contains(testType));
		Assert.assertTrue(jobs.keySet().contains(otherType));
		Assert.assertEquals(firstExp.getCronExpression(), jobs.get(testType).getCronExpression());
		Assert.assertEquals(secondExp.getCronExpression(), jobs.get(otherType).getCronExpression());


	}


	@Test
	public void passesFacilityContextToJob() throws SchedulerException, InterruptedException, TimeoutException, BrokenBarrierException, ParseException, ExecutionException {
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		ScheduledJobType testType = ScheduledJobType.Test;	
		subject.schedule(firstExp, testType);
		Future<ScheduledJobType> future = subject.trigger(testType);
		TestJob job = TestJob.pollInstance();
		job.awaitRunning();
		job.proceed();
		future.get(5, TimeUnit.SECONDS);
		Assert.assertEquals(subject.getTenant(), job.getExecutionTenant());
		Assert.assertEquals(subject.getFacility(), job.getExecutionFacility());
	}
	

	private void scheduleTestJob(ScheduledJobType testType) throws SchedulerException, ParseException {
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		subject.schedule(firstExp, testType);
		Assert.assertFalse(subject.hasRunningJob(testType).isPresent());
		Optional<DateTime> neverTriggered = subject.getPreviousFireTime(testType);
		Assert.assertFalse(neverTriggered.isPresent());
		
	}
	
	private Optional<DateTime> triggerWait(ScheduledJobType testType) throws InterruptedException, SchedulerException, TimeoutException, BrokenBarrierException, ExecutionException {
		DateTime timeBeforeTrigger = DateTime.now();
		Thread.sleep(10);
		Future<ScheduledJobType> future = subject.trigger(testType);
		TestJob job = TestJob.pollInstance();
		job.awaitRunning();
		job.proceed();
		
		ScheduledJobType completedType = future.get(40, TimeUnit.SECONDS);
		Optional<DateTime> timeAfterTrigger = subject.getPreviousFireTime(testType);
		Assert.assertEquals("completed type was unexpected", testType, completedType);
		Assert.assertTrue(String.format("job does not appear to have been triggered before: %s, after %s", timeAfterTrigger, timeAfterTrigger), timeBeforeTrigger.isBefore(timeAfterTrigger.get()));
		Assert.assertFalse("job should not still be running", subject.hasRunningJob(testType).isPresent());
		return timeAfterTrigger;
	}


}
