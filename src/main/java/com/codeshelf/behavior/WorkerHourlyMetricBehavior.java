package com.codeshelf.behavior;

import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerHourlyMetric;
import com.codeshelf.util.TimeUtils;

public class WorkerHourlyMetricBehavior implements IApiBehavior{
	private static final Logger	LOGGER					= LoggerFactory.getLogger(WorkerHourlyMetricBehavior.class);
	
	protected void metricOpenSession(Worker worker) {
		Timestamp loginTime = worker.getLastLogin();
		//Try to find a metric for this hour
		WorkerHourlyMetric metric = worker.getHourlyMetric(loginTime);
		if (metric == null) {
			//If a metric doesn't exist, create it.
			metric = new WorkerHourlyMetric(worker, loginTime);
		} else if (!metric.isSessionActive()){
			//If multiple workers use the same badge, a metric with an active session may already exist. Do nothing in that case.
			//If an metric with an inactive session exists, update it an open session.
			metric.setLastSessionStart(loginTime);
			metric.setSessionActive(true);
		}
	}
	
	protected void metricCloseSession(Worker worker){
		Timestamp logoutTime = worker.getLastLogout();
		WorkerHourlyMetric metric = worker.getHourlyMetric(logoutTime);
		if (metric != null) {
			//If an ongoing metric was found, close it
			updateMetricDuration(metric, worker.getLastLogin(), logoutTime);
		} else {
			//If an ongoing metric was not found, this likely indicates that a worker logged in during the last hour, and made no picks during this one.
			//Create a new metric and update it with the time worker was logged in during the current hour.
			Timestamp beginningOfHour = TimeUtils.truncateTimeToHour(logoutTime);
			//Decide when to start the session for this hour metric: if a worker last logged in after the start of the hour, set it then, if not - set it to the beginning of the hour
			Timestamp sessionStart = beginningOfHour.after(worker.getLastLogin()) ? beginningOfHour : worker.getLastLogin();
			metric = new WorkerHourlyMetric(worker, sessionStart);
			updateMetricDuration(metric, sessionStart, logoutTime);
			
			//Try to find an ongoing metric from the last hour. Extend it to the end of its hour and close it
			WorkerHourlyMetric previousMetric = worker.getHourlyMetric(new Timestamp(logoutTime.getTime() - TimeUtils.MILLISECOUNDS_IN_HOUR));
			if (previousMetric != null){
				extendMetricDurationTillEndOfHour(previousMetric);
			}
		}
	}
	
	private void extendMetricDurationTillEndOfHour(WorkerHourlyMetric metric) {
		Timestamp hourEnd = new Timestamp(metric.getHourTimestamp().getTime() + TimeUtils.MILLISECOUNDS_IN_HOUR);
		updateMetricDuration(metric, metric.getLastSessionStart(), hourEnd);
	}
	
	private void updateMetricDuration(WorkerHourlyMetric metric, Timestamp sessionStart, Timestamp sessionEnd) {
		if (!metric.isSessionActive()){
			LOGGER.warn("Trying to close inactive metric {}", metric.getDomainId());
			return;
		}
		int sessionDurationMin = (int)((sessionEnd.getTime() - sessionStart.getTime()) / TimeUtils.MILLISECOUNDS_IN_MINUTE);
		metric.setLoggedInDurationMin(metric.getLoggedInDurationMin() + sessionDurationMin);
		metric.setSessionActive(false);
		WorkerHourlyMetric.staticGetDao().store(metric);
	}
}
