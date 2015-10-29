package com.codeshelf.application;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import lombok.Getter;

import org.joda.time.DateTime;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.ScheduledJobType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractFuture;

/*
 * This service class schedules jobs that are executed periodically while
 * the application server is running.
 */
public class FacilitySchedulerService extends AbstractCodeshelfIdleService {
	
	public static class NotImplementedJob  implements Job {
		private static final Logger LOGGER	= LoggerFactory.getLogger(NotImplementedJob.class);

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			LOGGER.warn("Scheduled a job that has no implementation");
		}
		
	}
	
	private class JobFuture<T> extends AbstractFuture<T> {
		
		private ScheduledJobType type;
		
		public JobFuture(ScheduledJobType type) {
			this.type = type;
		}
		
		public boolean set(T o) {
			return super.set(o);
		}
		public boolean setException(Throwable t) {
			return super.setException(t);
		}

		@Override
		protected void interruptTask() {
			super.interruptTask();
			try {
				FacilitySchedulerService.this.scheduler.interrupt(this.type.getKey());
			} catch (UnableToInterruptJobException e) {
				this.setException(e);
			}
		}
		
		
	}
	
	private static class FutureResolver extends JobListenerSupport {
		private static final String FUTURE_PROPERTY = "future";
		@Override
		public String getName() {
			return "futureResolver";
		}

		@Override
		public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
			super.jobWasExecuted(context, jobException);
			Object value = context.getMergedJobDataMap().get(FUTURE_PROPERTY);
			if (value != null && value instanceof JobFuture) {
				@SuppressWarnings("unchecked")
				JobFuture<ScheduledJobType> future = (JobFuture<ScheduledJobType>) value;
				if (jobException != null) {
					future.setException(jobException);
				} else {
					ScheduledJobType jobType = (ScheduledJobType) context.getJobDetail().getJobDataMap().get(TYPE_PROPERTY);
					future.set(jobType);
				}
			}
		}
	}
	
	private static final Logger LOGGER	= LoggerFactory.getLogger(FacilitySchedulerService.class);
	
	private static final String TYPE_PROPERTY = "type";
	
	private Scheduler scheduler;

	private UserContext	systemUserContext;

	private Tenant	tenant;

	@Getter
	private Facility	facility;
	
	private Map<ScheduledJobType, DateTime> lastFiredTimes;
	
	public FacilitySchedulerService(Scheduler scheduler, UserContext systemUserContext, Tenant tenant, Facility facility) throws SchedulerException {
		this.systemUserContext = systemUserContext;
		this.tenant = tenant;
		this.facility = facility;
		this.scheduler = scheduler;
		this.scheduler.getListenerManager().addJobListener(new FutureResolver());
		lastFiredTimes = new HashMap<>();
	}

	@Override
	protected void startUp() throws Exception {
		scheduler.start();
	}

	@Override
	protected void shutDown() throws Exception {
		scheduler.shutdown(true);
	}

	public Map<String, Class<? extends Job>> getJobs() throws SchedulerException  {
		HashMap<String, Class<? extends Job>> jobs = new HashMap<>();
		Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup()); //throw this
		for (JobKey jobKey : jobKeys) {
			JobDetail jobDetail;
			try {
				jobDetail = scheduler.getJobDetail(jobKey);
				List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
				for (Trigger trigger : triggers) {
					if (trigger instanceof CronTrigger) {
						CronTrigger cronTrigger = (CronTrigger) trigger;
						jobs.put(cronTrigger.getCronExpression(), jobDetail.getJobClass());
					}
				}
			} catch (SchedulerException e1) {
				LOGGER.error("", e1);
				//todo indicate list is partial
			}
		}
		return jobs;
	}

	public void schedule(CronExpression exp1, ScheduledJobType jobType) throws SchedulerException {
		// create and schedule  job
		JobDataMap map = new JobDataMap();
		map.put("facility", facility);
		map.put("tenant", tenant);
		map.put("userContext", systemUserContext);
		map.put(TYPE_PROPERTY, jobType);
		JobDetail archivingJob = JobBuilder.newJob(jobType.getJobClass())
                .withIdentity(jobType.getKey())
                .usingJobData(map).build();

		Trigger trigger = TriggerBuilder
	            .newTrigger()
	            .withIdentity(exp1.getExpressionSummary())
	            .withSchedule(CronScheduleBuilder.cronSchedule(exp1))
	            .build();
		scheduler.deleteJob(archivingJob.getKey());
		scheduler.scheduleJob(archivingJob, trigger);
	}

	public boolean isJobRunning(ScheduledJobType jobType) throws SchedulerException {
		for (JobExecutionContext context : scheduler.getCurrentlyExecutingJobs()) {
			ScheduledJobType runningType = (ScheduledJobType) context.getMergedJobDataMap().get(TYPE_PROPERTY);
			if (jobType != null && runningType.equals(jobType)) {
				return true;
			}
		};
		return false;
	}

	public Future<ScheduledJobType> trigger(ScheduledJobType type) throws SchedulerException {
		Future<ScheduledJobType> future = new JobFuture<ScheduledJobType>(type);		
		lastFiredTimes.put(type, DateTime.now());
		scheduler.triggerJob(type.getKey(), new JobDataMap(ImmutableMap.of(FutureResolver.FUTURE_PROPERTY, future)));
		return future;
	}

	public Optional<DateTime> getPreviousFireTime(ScheduledJobType type) throws SchedulerException {
		DateTime lastTime = lastFiredTimes.get(type);
		List<? extends Trigger> triggers = scheduler.getTriggersOfJob(type.getKey());
		for (Trigger trigger : triggers) {
			Date time = trigger.getPreviousFireTime();
			if (time != null) {
				DateTime dt = new DateTime(time);
				if (lastTime == null) {
					lastTime = dt;
				} else {
					lastTime.isBefore(dt);
					lastTime = dt;
				}
			}
		}
		return Optional.fromNullable(lastTime);
	}

	public List<JobExecutionContext> getRunningJobs() throws SchedulerException {
		List<JobExecutionContext> runningJobs = scheduler.getCurrentlyExecutingJobs();
		return runningJobs;
	}

}
