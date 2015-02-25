package com.codeshelf.metrics;

import org.mockito.Mockito;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.util.concurrent.AbstractIdleService;

public class DummyMetricsService extends AbstractIdleService implements IMetricsService {

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
		return Mockito.mock(Counter.class);
	}

	@Override
	public Timer createTimer(MetricsGroup group, String metricName) {
		return Mockito.mock(Timer.class);
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

}
