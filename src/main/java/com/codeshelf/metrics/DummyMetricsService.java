package com.codeshelf.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codeshelf.service.AbstractCodeshelfIdleService;

public class DummyMetricsService extends AbstractCodeshelfIdleService implements IMetricsService {

	@Override
	protected void startUp() throws Exception {}

	@Override
	protected void shutDown() throws Exception {}

	@Override
	public void registerMetric(MetricsGroup group, String metricName, Metric metric) {}

	@Override
	public void registerHealthCheck(CodeshelfHealthCheck healthCheck) {}

	@Override
	public Counter createCounter(MetricsGroup group, String metricName) {
		return new Counter();
	}

	@Override
	public Timer createTimer(MetricsGroup group, String metricName) {
		return new Timer();
	}

	@Override
	public String getHostName() {
		return "testHost";
	}

	@Override
	public MetricRegistry getMetricsRegistry() {
		return null;
	}

	@Override
	public HealthCheckRegistry getHealthCheckRegistry() {
		return null;
	}

	@Override
	public Meter createMeter(MetricsGroup group, String metricName) {
		return null;
	}
	
	@Override
	public Histogram createHistogram(MetricsGroup group, String metricName) {
		return null;
	}
}
