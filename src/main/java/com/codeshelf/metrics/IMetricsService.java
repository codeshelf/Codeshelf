package com.codeshelf.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codeshelf.service.CodeshelfService;

public interface IMetricsService extends CodeshelfService {
	String getHostName();

	void registerHealthCheck(CodeshelfHealthCheck healthCheck);
	void registerMetric(MetricsGroup group, String metricName, Metric metric);
	
	Counter createCounter(MetricsGroup group, String metricName);
	Timer createTimer(MetricsGroup group, String metricName);

	MetricRegistry getMetricsRegistry();
	HealthCheckRegistry getHealthCheckRegistry();
}
