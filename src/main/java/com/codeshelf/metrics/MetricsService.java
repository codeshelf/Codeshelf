package com.codeshelf.metrics;

import java.net.InetAddress;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codeshelf.service.AbstractCodeshelfIdleService;
import com.google.inject.Inject;

public class MetricsService extends AbstractCodeshelfIdleService implements IMetricsService {

	private static final Logger			LOGGER			= LoggerFactory.getLogger(MetricsService.class);

	@Inject
	private static IMetricsService theInstance;
	
	@Getter
	private String						hostName;

	@Getter
	private MetricRegistry				metricsRegistry;

	@Getter
	private HealthCheckRegistry			healthCheckRegistry;

	@Inject
	private MetricsService() {
		metricsRegistry	= new MetricRegistry();
		healthCheckRegistry	= new HealthCheckRegistry();
		try {
			this.hostName = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			LOGGER.error("Failed to determine host name: " + e.getMessage() + ".  Trying to fall back on host address.");
			try {
				this.hostName = InetAddress.getLocalHost().getHostAddress() + " ";
			} catch (Exception ex) {
				LOGGER.error("Failed to determine host address: " + e.getMessage());
				this.hostName = "unknown";
			}
		}
	}

	public final static IMetricsService getInstance() {
		//ServiceUtility.awaitRunningOrThrow(theInstance);
		return theInstance;
	}
	public final static void setInstance(IMetricsService instance) { 
		// for testing only!
		theInstance = instance;
	}
	public final static boolean exists() {
		return (theInstance != null);
	}
	
	private static String getFullName(MetricsGroup group, String metricName) {
		return group.getName() + "-" + metricName;
	}

	@Override
	public void registerMetric(MetricsGroup group, String metricName, Metric metric) {
		String fullName = MetricsService.getFullName(group, metricName);
		try {
			getMetricsRegistry().register(fullName, metric);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Failed to register " + metricName + "usually because already registered in same JVM");
		}
	}

	@Override
	public void registerHealthCheck(CodeshelfHealthCheck healthCheck) {
		getHealthCheckRegistry().register(healthCheck.getName(), healthCheck);
		LOGGER.info("Registered Healthcheck " + healthCheck);
	}
	
	@Override
	public Counter createCounter(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Counter counter = getMetricsRegistry().getCounters().get(fullName);
			if (counter != null) {
				// return existing metric
				//LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return counter;
			}
			// create and register new metric
			counter = getMetricsRegistry().counter(fullName);
			LOGGER.debug("Added counter " + fullName);
			return counter;
		} catch (Exception e) {
			LOGGER.error("Failed to add counter " + fullName, e);
		}
		return null;
	}

	@Override
	public Timer createTimer(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Timer timer = getMetricsRegistry().getTimers().get(fullName);
			if (timer != null) {
				// return existing metric
				// LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return timer;
			}
			// create and register new metric
			timer = getMetricsRegistry().timer(fullName);
			LOGGER.debug("Added timer " + fullName);
			return timer;
		} catch (Exception e) {
			LOGGER.error("Failed to add meter " + fullName, e);
		}
		return null;
	}
	
	@Override
	public Meter createMeter(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Meter meter = getMetricsRegistry().getMeters().get(fullName);
			if (meter != null) {
				// return existing metric
				//LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return meter;
			}
			// create and register new metric
			meter = getMetricsRegistry().meter(fullName);
			LOGGER.debug("Added meter " + fullName);
			return meter;
		} catch (Exception e) {
			LOGGER.error("Failed to add meter " + fullName, e);
		}
		return null;
	}

	// static convenience methods, may block until initialized
	
	/*
	public static MetricRegistry getRegistry() {
		return getInstance().getMetricsRegistry();
	}

	public static HealthCheckRegistry getHealthCheckRegistry() {
		return getInstance().getHealthRegistry();
	}

	public static Meter addMeter(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Meter meter = getRegistry().getMeters().get(fullName);
			if (meter != null) {
				// return existing metric
				// LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return meter;
			}
			// create and register new metric
			meter = getRegistry().meter(fullName);
			LOGGER.debug("Added meter " + fullName);
			return meter;
		} catch (Exception e) {
			LOGGER.error("Failed to add meter " + fullName, e);
		}
		return null;
	}

	*/
	
	@Override
	public Histogram createHistogram(MetricsGroup group, String metricName) {
		String fullName = getFullName(group, metricName);
		try {
			Histogram histogram = getMetricsRegistry().getHistograms().get(fullName);
			if (histogram != null) {
				// return existing metric
				// LOGGER.warn("Unable to add metric "+fullName+".  Metric already exists.");
				return histogram;
			}
			// create and register new metric
			histogram = getMetricsRegistry().histogram(fullName);
			LOGGER.debug("Added histogram " + fullName);
			return histogram;
		} catch (Exception e) {
			LOGGER.error("Failed to add histogram " + fullName, e);
		}
		return null;
	}

	@Override
	protected void startUp() throws Exception {
		// all in constructor
	}

	@Override
	protected void shutDown() throws Exception {
		//metricsRegistry.removeMatching(MetricFilter.ALL);
		// other cleanup?
	}
}
