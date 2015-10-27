package com.codeshelf.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

import com.codeshelf.application.JvmProperties;

public class ServiceStatusHealthCheck extends CodeshelfHealthCheck {

	public ServiceStatusHealthCheck() {
		super("Service Status");
	}

	@Override
	protected Result check() throws Exception {
		RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
		long jvmUpTimeMillis = runtimeMX.getUptime();
		String jvmVersion = runtimeMX.getSpecVersion() + "-" + runtimeMX.getVmVersion();

		long millis = jvmUpTimeMillis;

		// Compute a pleasing string
		long days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days);
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes);

		StringBuilder sb = new StringBuilder(64);
		sb.append(days);
		sb.append(" days; ");
		sb.append(hours);
		sb.append(" hours; ");
		sb.append(minutes);
		sb.append(" minutes ");

		return Result.healthy("App v" + JvmProperties.getVersionStringShort() + " / JVM " + jvmVersion + " / Uptime "
				+ sb);
	}
}
