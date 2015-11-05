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
		Timestamp loginTime = worker.getLastLogin();
		Timestamp logoutTime = worker.getLastLogout();
		WorkerHourlyMetric metric = worker.getHourlyMetric(logoutTime);
		if (metric != null) {
			//If an ongoing metric was found, close it
			updateMetricDuration(metric, logoutTime);
		} else {
			Timestamp beginningOfCurrentHour = TimeUtils.truncateTimeToHour(logoutTime);
			Timestamp currentMetricSessionStart = beginningOfCurrentHour;
			//Try to find a metric with an active session in the past
			WorkerHourlyMetric latestMetric = worker.getLatestActiveHourlyMetricBeforeTime(beginningOfCurrentHour);
			if (latestMetric == null){
				if (loginTime.after(beginningOfCurrentHour)) {
					//If no past metrics found, and the login occurred after the beginning of the current hour,
					currentMetricSessionStart = loginTime;
				}
			} else {
				//If a past metric with an active session was found, extend it to the end of its hour
				//Additionally, generate metrics for all full hours between then and now 
				extendMetricDurationTillEndOfHour(latestMetric);
				Timestamp historicalFillerTimestamp = TimeUtils.addTime(latestMetric.getHourTimestamp(), TimeUtils.MILLISECOUNDS_IN_HOUR);
				while (historicalFillerTimestamp.before(beginningOfCurrentHour)){
					WorkerHourlyMetric historicalFillerMetric = new WorkerHourlyMetric(worker, historicalFillerTimestamp);
					extendMetricDurationTillEndOfHour(historicalFillerMetric);
					historicalFillerTimestamp = TimeUtils.addTime(historicalFillerTimestamp, TimeUtils.MILLISECOUNDS_IN_HOUR);
				}
			}
			
			//Finally, create a metric for the current hour
			metric = new WorkerHourlyMetric(worker, currentMetricSessionStart);
			updateMetricDuration(metric, logoutTime);
		}
	}
	
	private void extendMetricDurationTillEndOfHour(WorkerHourlyMetric metric) {
		Timestamp hourEnd = TimeUtils.addTime(metric.getHourTimestamp(), TimeUtils.MILLISECOUNDS_IN_HOUR);
		updateMetricDuration(metric, hourEnd);
	}
	
	private void updateMetricDuration(WorkerHourlyMetric metric, Timestamp sessionEnd) {
		if (!metric.isSessionActive()){
			LOGGER.warn("Trying to close inactive metric {}", metric.getDomainId());
			return;
		}
		int sessionDurationMin = (int)((sessionEnd.getTime() - metric.getLastSessionStart().getTime()) / TimeUtils.MILLISECOUNDS_IN_MINUTE);
		int metricDurationMin = metric.getLoggedInDurationMin() + sessionDurationMin;
		if (metricDurationMin > 60) {
			LOGGER.warn("Trying to set duration of metric {} to {} minutes. Reducing time to 60 min. Please investigate.", metric.getDomainId(), metricDurationMin);
			metricDurationMin = 60;
		}
		metric.setLoggedInDurationMin(metricDurationMin);
		metric.setSessionActive(false);
		WorkerHourlyMetric.staticGetDao().store(metric);
	}
}
