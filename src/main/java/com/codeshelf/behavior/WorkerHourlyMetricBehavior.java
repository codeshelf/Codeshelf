package com.codeshelf.behavior;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.model.domain.WorkerHourlyMetric;
import com.codeshelf.util.TimeUtils;
import com.google.common.collect.ImmutableList;

public class WorkerHourlyMetricBehavior implements IApiBehavior{
	private static final Logger	LOGGER					= LoggerFactory.getLogger(WorkerHourlyMetricBehavior.class);
	
	public void metricOpenSession(Worker worker) {
		/*
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
		*/
	}
	
	public void metricCloseSession(Worker worker){
		/*
		Timestamp logoutTime = worker.getLastLogout();
		WorkerHourlyMetric metric = produceMetric(worker, logoutTime);
		updateMetricDuration(metric, logoutTime);
		*/
	}

	public void recordEvent(Facility facility, String pickerId, EventType type) {
		/*
		Worker worker = Worker.findWorker(facility, pickerId);
		if (worker == null) {
			LOGGER.warn("Trying to update metrics for non-existent worker {}", pickerId);
			return;
		}
		if (!worker.getActive()){
			LOGGER.warn("Trying to update metrics for inactive worker {}", pickerId);
		}
		boolean completeEvent = type == EventType.COMPLETE, shortEvent = type == EventType.SHORT;
		if (!completeEvent && !shortEvent){
			LOGGER.warn("Trying to update metrics for an unexpected event type {}", type);
			return;
		}
		WorkerHourlyMetric metric = produceMetric(worker, new Timestamp(System.currentTimeMillis()));
		metric.setPicks(metric.getPicks() + 1);
		if (completeEvent) {
			metric.setCompletes(metric.getCompletes() + 1);
		} else if (shortEvent){
			metric.setShorts(metric.getShorts() + 1);
		}
		WorkerHourlyMetric.staticGetDao().store(metric);
		*/
	}

	
	
	/**
	 * Thus function retrieves or generates a Mertic for the requested hour
	 * It will also fill in metrics between the last metric with an active session, and the requested time 
	 */
	private WorkerHourlyMetric produceMetric(Worker worker, Timestamp time) {
		Timestamp loginTime = worker.getLastLogin();
		WorkerHourlyMetric metric = worker.getHourlyMetric(time);
		if (metric == null){
			Timestamp beginningOfCurrentHour = TimeUtils.truncateTimeToHour(time);
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
		}
		return metric;
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

	public List<WorkerHourlyMetric> findMetrics(Worker worker) {
		return WorkerHourlyMetric.staticGetDao().findByFilter(ImmutableList.of(Restrictions.eq("parent", worker)));
	}
	
}
