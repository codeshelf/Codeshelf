package com.codeshelf.application;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.DataArchivingJob;
import com.codeshelf.service.AbstractCodeshelfIdleService;

/*
 * This service class schedules jobs that are executed periodically while
 * the application server is running.
 */
public class SchedulingService extends AbstractCodeshelfIdleService {

	private static final Logger LOGGER	= LoggerFactory.getLogger(SchedulingService.class);

	SchedulerFactory schedFact;
	Scheduler scheduler;
	
	public SchedulingService() {
		try {
			this.schedFact = new org.quartz.impl.StdSchedulerFactory("quartz.properties");
			this.scheduler = schedFact.getScheduler();
		}
		catch (Exception e) {
			LOGGER.error("Failed to create scheduler",e);
		}
	}
	
	@Override
	protected void startUp() throws Exception {
		Trigger daily2AMTrigger = TriggerBuilder
	            .newTrigger()
	            .withIdentity("Daily 2AM","default")
	            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?")).build();

		Trigger everyMinuteTrigger = TriggerBuilder
	            .newTrigger()
	            .withIdentity("Every Minute","default")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?")).build();

		// create and schedule archive job
		JobDetail archivingJob = JobBuilder.newJob(DataArchivingJob.class)
                .withIdentity("Archiving", "default").build();
		scheduler.scheduleJob(archivingJob, daily2AMTrigger);
		//scheduler.scheduleJob(archivingJob, everyMinuteTrigger);
		
		scheduler.start();
	}

	@Override
	protected void shutDown() throws Exception {
		scheduler.shutdown(true);
	}

}
