package com.gadgetworks.codeshelf.metrics;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class MetricsService {
	
	private static final Logger	LOGGER = LoggerFactory.getLogger(MetricsService.class);

	@Getter
	final private static MetricsService instance = new MetricsService();
	
	@Getter
	private String hostName="";
	
	@Getter
	private MetricRegistry metricsRegistry = new MetricRegistry();
	
	private MetricsService() {
		try {
			this.hostName=InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			LOGGER.error("Failed to determine hostname", e);
		}
	}
	
	public static <T> void registerMetric(MetricsGroup group, String metricName, Metric metric) {
		String fullName = getFullName(group,metricName);
		getRegistry().register(fullName, metric);
	}

	private static String getFullName(MetricsGroup group, String metricName) {
		return getInstance().getHostName()+"-"+group.getName()+"-"+metricName;
	}

	public static MetricRegistry getRegistry() {
		return instance.getMetricsRegistry();
	}

	// <T extends Metric> T
	public static Counter addCounter(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Counter counter = getRegistry().getCounters().get(fullName);
			if (counter!=null) {
				// return existing metric
				LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return counter;
			}
			// create and register new metric
			counter = getRegistry().counter(fullName);
			LOGGER.debug("Added counter "+fullName);
			return counter;
		}
		catch (Exception e) {
			LOGGER.error("Failed to add counter "+fullName,e);
		}
		return null;
	}

	public static Meter addMeter(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Meter meter = getRegistry().getMeters().get(fullName);
			if (meter!=null) {
				// return existing metric
				LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return meter;
			}
			// create and register new metric
			meter = getRegistry().meter(fullName);
			LOGGER.debug("Added meter "+fullName);

			return meter;
		}
		catch (Exception e) {
			LOGGER.error("Failed to add meter "+fullName,e);
		}
		return null;
	}

	public static Timer addTimer(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Timer timer = getRegistry().getTimers().get(fullName);
			if (timer!=null) {
				// return existing metric
				LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return timer;
			}
			// create and register new metric
			timer = getRegistry().timer(fullName);
			LOGGER.info("Added timer "+fullName);
			return timer;
		}
		catch (Exception e) {
			LOGGER.error("Failed to add meter "+fullName,e);
		}
		return null;
	}
}
