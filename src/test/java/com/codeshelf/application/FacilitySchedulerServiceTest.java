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
		DirectSchedulerFactory.getInstance().createVolatileScheduler(1);
		Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler();
		
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

	@Test
	public void manualJobTriggersIfNotRunning() throws ParseException, SchedulerException, InterruptedException, ExecutionException, TimeoutException, BrokenBarrierException {
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		ScheduledJobType testType = ScheduledJobType.Test;	
		subject.schedule(firstExp, testType);
		Assert.assertFalse(subject.hasRunningJob(testType).isPresent());
		Optional<DateTime> neverTriggered = subject.getPreviousFireTime(testType);
		Assert.assertFalse(neverTriggered.isPresent());
		
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
	}
	
	@Test
	public void noManualJobIfRunning() throws Exception {
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		ScheduledJobType testType = ScheduledJobType.Test;	
		
		subject.schedule(firstExp, testType);
		Assert.assertFalse(subject.hasRunningJob(testType).isPresent());
		
		Optional<DateTime> neverTriggered = subject.getPreviousFireTime(testType);
		Assert.assertFalse(neverTriggered.isPresent());
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
}
