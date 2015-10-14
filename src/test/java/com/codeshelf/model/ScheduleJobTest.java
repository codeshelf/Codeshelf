package com.codeshelf.model;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.testframework.MockDaoTest;

public class ScheduleJobTest extends MockDaoTest {

	/**
	 * This is initially a trivial test of an enum. Should grow to do a few more elaborate things
	 */
	@Test
	public void testScheduledJobType() {
		Assert.assertEquals("AccumulateDailyMetrics", ScheduledJobType.AccumulateDailyMetricsJob.getJobName());
	}


}
