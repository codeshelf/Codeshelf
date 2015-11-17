package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.WorkerHourlyMetricBehavior;
import com.codeshelf.testframework.HibernateTest;
import com.codeshelf.util.TimeUtils;

public class WorkerHourlyMetricTest extends HibernateTest{

	private static final Logger	LOGGER	= LoggerFactory.getLogger(WorkerHourlyMetricTest.class);
	
	@Test
	public void testDurationAccumulator(){
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Timestamp beginningOfHour = TimeUtils.truncateTimeToHour(now);
		Timestamp testTime = TimeUtils.addTime(beginningOfHour, 10 * TimeUtils.MILLISECOUNDS_IN_MINUTE);
		WorkerHourlyMetricBehavior behavior = new WorkerHourlyMetricBehavior();
		
		beginTransaction();
		Facility facility = getFacility();
		LOGGER.info("1: Create a worker that just logged it. Generate a metric for that worker and this hour.");
		Worker worker = createWorker(facility, "Worker1");
		UUID workerId = worker.getPersistentId();
		worker.setLastLogin(testTime);
		Worker.staticGetDao().store(worker);
		behavior.metricOpenSession(worker);
		
		
		List<WorkerHourlyMetric> metrics = behavior.findMetrics(worker);
		Assert.assertEquals(1, metrics.size());
		WorkerHourlyMetric metric = metrics.get(0);
		Assert.assertEquals(testTime, metric.getLastSessionStart());
		Assert.assertEquals(beginningOfHour, metric.getHourTimestamp());
		Assert.assertTrue(metric.isSessionActive());
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("2: Simulate worker logging out 5 minutes later, update metric.");
		worker = Worker.staticGetDao().findByPersistentId(workerId);
		testTime = TimeUtils.addTime(testTime, 5 * TimeUtils.MILLISECOUNDS_IN_MINUTE);
		worker.setLastLogout(testTime);
		Worker.staticGetDao().store(worker);
		behavior.metricCloseSession(worker);
		
		metric = behavior.findMetrics(worker).get(0);
		Assert.assertEquals(5, (int)metric.getLoggedInDurationMin());
		Assert.assertFalse(metric.isSessionActive());
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("3: Wait another 10 minues. Login again and update metric.");
		worker = Worker.staticGetDao().findByPersistentId(workerId);
		testTime = TimeUtils.addTime(testTime, 10 * TimeUtils.MILLISECOUNDS_IN_MINUTE);
		worker.setLastLogin(testTime);
		Worker.staticGetDao().store(worker);
		behavior.metricOpenSession(worker);
		metric = behavior.findMetrics(worker).get(0);
		Assert.assertEquals(5, (int)metric.getLoggedInDurationMin());
		Assert.assertTrue(metric.isSessionActive());
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("4: Wait 15 minues. Logout, and update metric.");
		worker = Worker.staticGetDao().findByPersistentId(workerId);
		testTime = TimeUtils.addTime(testTime, 15 * TimeUtils.MILLISECOUNDS_IN_MINUTE);
		worker.setLastLogout(testTime);
		behavior.metricCloseSession(worker);
		metric = behavior.findMetrics(worker).get(0);
		Assert.assertEquals(20, (int)metric.getLoggedInDurationMin());
		Assert.assertFalse(metric.isSessionActive());
		commitTransaction();
	}
	
	@Test
	public void testMetricBackfilling(){
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Timestamp beginningOfHourA = TimeUtils.truncateTimeToHour(now);
		Timestamp testTime = TimeUtils.addTime(beginningOfHourA, 10 * TimeUtils.MILLISECOUNDS_IN_MINUTE);
		WorkerHourlyMetricBehavior behavior = new WorkerHourlyMetricBehavior();
		
		beginTransaction();
		Facility facility = getFacility();
		LOGGER.info("1: Create a worker that just logged it. Generate a metric for that worker and this hour.");
		Worker worker = createWorker(facility, "Worker1");
		UUID workerId = worker.getPersistentId();
		worker.setLastLogin(testTime);
		Worker.staticGetDao().store(worker);
		behavior.metricOpenSession(worker);
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("2: Advance time by 3 hours and log out. This should result in 4 hourly metrics (for the covered duration)");
		worker = Worker.staticGetDao().findByPersistentId(workerId);
		testTime = TimeUtils.addTime(testTime, 3 * TimeUtils.MILLISECOUNDS_IN_HOUR);
		Timestamp beginningOfHourB = TimeUtils.truncateTimeToHour(testTime);
		worker.setLastLogout(testTime);
		behavior.metricCloseSession(worker);
		List<WorkerHourlyMetric> metrics = behavior.findMetrics(worker);
		Assert.assertEquals(4, metrics.size());
		for (WorkerHourlyMetric metric : metrics){
			Assert.assertFalse(metric.isSessionActive());
			if (metric.getHourTimestamp().equals(beginningOfHourA)){
				Assert.assertEquals(50, (int)metric.getLoggedInDurationMin());
			} else if (metric.getHourTimestamp().before(beginningOfHourB)){
				Assert.assertEquals(60, (int)metric.getLoggedInDurationMin());
			} else {
				Assert.assertEquals(10, (int)metric.getLoggedInDurationMin());
			}
		}
		commitTransaction();
	}

	
	private Worker createWorker(Facility facility, String badge) {
		Worker worker = new Worker();
		worker.setFacility(facility);
		worker.setActive(true);
		worker.setLastName(badge);
		worker.setBadgeId(badge);
		worker.generateDomainId();
		worker.setUpdated(new Timestamp(System.currentTimeMillis()));
		return worker;
		
	}

}
