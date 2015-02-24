package com.codeshelf.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.util.concurrent.Service;

public interface IMetricsService extends Service {
	String getHostName();

	void registerHealthCheck(CodeshelfHealthCheck healthCheck);
	void registerMetric(MetricsGroup group, String metricName, Metric metric);
	
	Counter createCounter(MetricsGroup group, String metricName);
	Timer createTimer(MetricsGroup group, String metricName);

	MetricRegistry getMetricsRegistry();
	HealthCheckRegistry getHealthCheckRegistry();
}
