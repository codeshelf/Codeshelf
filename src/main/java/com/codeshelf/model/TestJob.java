package com.codeshelf.model;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJob implements Job {

	private static final Logger LOGGER	= LoggerFactory.getLogger(TestJob.class);

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOGGER.info("Executed test job");
		context.setResult(new Object());
	}

}
