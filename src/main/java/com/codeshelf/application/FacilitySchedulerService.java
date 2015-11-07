package com.codeshelf.application;

import java.text.ParseException;
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
import com.codeshelf.model.domain.Facility;
import com.codeshelf.scheduler.ScheduledJobType;
import com.codeshelf.security.UserContext;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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
			LOGGER.warn("Scheduled a job that has no implementation {}", context);
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

		public static JobFuture<ScheduledJobType> getFuture(JobExecutionContext context) {
			Object value = context.getMergedJobDataMap().get(FUTURE_PROPERTY);
			if (value != null && value instanceof JobFuture) {
				@SuppressWarnings("unchecked")
				JobFuture<ScheduledJobType> future = (JobFuture<ScheduledJobType>) value;
				return future;
			} else {
				LOGGER.error("should have had a future for job instance {}", context);
				return null;
			}
		}
		
		@Override
		public String getName() {
			return "futureResolver";
		}

		@Override
		public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
			super.jobWasExecuted(context, jobException);
			JobFuture<ScheduledJobType> future = getFuture(context);
			if (future != null) {
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

	@Getter
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

	public CronExpression findSchedule(ScheduledJobType type) throws SchedulerException {
		return getJobs().get(type);
		
	}
	
	public Map<ScheduledJobType, CronExpression>  getJobs() throws SchedulerException  {
		HashMap<ScheduledJobType, CronExpression> jobs = new HashMap<>();
		Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup()); //throw this
		for (JobKey jobKey : jobKeys) {
			Optional<ScheduledJobType> type = ScheduledJobType.findByKey(jobKey);
			if (type.isPresent()) {
				try {
					List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
					for (Trigger trigger : triggers) {
						if (trigger instanceof CronTrigger) {
							CronTrigger cronTrigger = (CronTrigger) trigger;
							String expression = cronTrigger.getCronExpression();
							try {
								jobs.put(type.get(), new CronExpression(expression));
							} catch (ParseException e) {
								LOGGER.warn("Unable to parse cron expression {} for jobKey {}", expression, jobKey, e);
							}
						}
					}
				} catch (SchedulerException e) {
					LOGGER.warn("Unable to return schedules for jobKey {}", jobKey, e);
				} 
			} else {
				LOGGER.error("Unable to find type matching key {}", jobKey);
			}
		}
		return jobs;
	}

	public void schedule(CronExpression cronExpression, ScheduledJobType jobType) throws SchedulerException {
		// create and schedule  job
		JobDataMap map = new JobDataMap();
		map.put("facility", facility);
		map.put("tenant", tenant);
		map.put("userContext", systemUserContext);
		map.put(TYPE_PROPERTY, jobType);
		JobDetail jobDetail = JobBuilder.newJob(jobType.getJobClass())
                .withIdentity(jobType.getKey())
                .usingJobData(map).build();

		Trigger trigger = TriggerBuilder
	            .newTrigger()
	            .withIdentity(jobType.getKey().toString())
	            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
	            .build();
		scheduler.unscheduleJob(trigger.getKey());
		scheduler.deleteJob(jobType.getKey());
		scheduler.scheduleJob(jobDetail, trigger);
		LOGGER.info("Scheduled {} for {}", jobType, cronExpression);
	}

	public Optional<JobFuture<ScheduledJobType>> hasRunningJob(ScheduledJobType jobType) throws SchedulerException {
		for (JobExecutionContext context : scheduler.getCurrentlyExecutingJobs()) {
			ScheduledJobType runningType = (ScheduledJobType) context.getMergedJobDataMap().get(TYPE_PROPERTY);
			if (jobType != null && runningType.equals(jobType)) {
				return Optional.of(FutureResolver.getFuture(context));
			}
		};
		return Optional.absent();
	}

	public Future<ScheduledJobType> trigger(ScheduledJobType type) throws SchedulerException {
		if (!hasRunningJob(type).isPresent()) {
			Future<ScheduledJobType> future = new JobFuture<ScheduledJobType>(type);		
			lastFiredTimes.put(type, DateTime.now());
			scheduler.triggerJob(type.getKey(), new JobDataMap(ImmutableMap.of(FutureResolver.FUTURE_PROPERTY, future)));
			return future;
		} else {
			throw new SchedulerException(String.format("type %s is already running for facility %s", type, this.facility));
		}
	}

	public boolean cancelJob(ScheduledJobType type) throws SchedulerException {
		Optional<JobFuture<ScheduledJobType>> hasRunningJob = hasRunningJob(type);
		if(hasRunningJob.isPresent()) {
			return hasRunningJob.get().cancel(true);
		} else {
			return false;
		}
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

	public boolean hasFacility(Facility facility) {
		Preconditions.checkNotNull(facility, "facility cannot be null");
		return (this.facility.getPersistentId().equals(facility.getPersistentId()));
	}

}
