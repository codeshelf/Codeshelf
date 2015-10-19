package com.codeshelf.application;

import static org.mockito.Mockito.mock;

import java.text.ParseException;
import java.util.Map;
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
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.ScheduledJobType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.UserContext;
import com.google.common.base.Optional;

public class FacilitySchedulerServiceTest {

	private FacilitySchedulerService subject;
	
	@Before
	public void setUp() throws SchedulerException, ParseException {
		DirectSchedulerFactory.getInstance().createVolatileScheduler(1);
		Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler();
		
		UserContext systemUser = CodeshelfSecurityManager.getUserContextSYSTEM();
		Tenant tenant = mock(Tenant.class);
		Facility facility = mock(Facility.class);
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
	public void testRescheduleJobClass() throws ParseException, SchedulerException  {
		
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		subject.schedule(firstExp, ScheduledJobType.DatabasePurge);

		CronExpression secondExp = new CronExpression("0 0 3 * * ?");
		CronExpression sameSecondExp = new CronExpression("0 0 3 * * ?");
		
		
		subject.schedule(secondExp, ScheduledJobType.DatabasePurge);
		Map<String, ?> scheduledJobs = subject.getJobs();

		Assert.assertEquals(secondExp.getCronExpression(), sameSecondExp.getCronExpression());
		Assert.assertNotEquals(firstExp, secondExp);
		Assert.assertEquals(1, scheduledJobs.size());
		Assert.assertTrue(scheduledJobs.containsKey(secondExp.getCronExpression()));

	}
	
	/**
	 * Running jobs stop ASAP when shutdown is requested
	 */
	@Test
	public void runningJobCancelledOnShutdown() {
		
	}

	@Test
	public void manualJobTriggersIfNotRunning() throws ParseException, SchedulerException, InterruptedException, ExecutionException, TimeoutException {
		CronExpression firstExp = new CronExpression("0 0 2 * * ?");
		ScheduledJobType testType = ScheduledJobType.Test;
		subject.schedule(firstExp, testType);
		Assert.assertFalse(subject.isJobRunning(testType));
		Optional<DateTime> neverTriggered = subject.getPreviousFireTime(testType);
		Assert.assertFalse(neverTriggered.isPresent());
		
		DateTime timeBeforeTrigger = DateTime.now();
		Future<ScheduledJobType> future = subject.trigger(testType);
		ScheduledJobType completedType = future.get(40, TimeUnit.SECONDS);
		Optional<DateTime> timeAfterTrigger = subject.getPreviousFireTime(testType);
		
		Assert.assertEquals(testType, completedType);
		Assert.assertTrue(timeBeforeTrigger.isBefore(timeAfterTrigger.get()));
		Assert.assertFalse(subject.isJobRunning(testType));
	}
	
	@Test
	public void noManualJobIfRunning() {
		
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
	 * Running jobs stop ASAP when shutdown is requested
	 */
	@Test
	public void manualCancelOfRunningJob() {
		
	}
	
	@Test
	public void missedJobsAreLogged() {
		
	}
	
	@Test
	public void viewWhichJobsAreRunning() {
		
	}

	@Test
	public void viewJobSchedules() {
		
	}

	
}
