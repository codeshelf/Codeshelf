package com.codeshelf.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import com.codeshelf.application.Configuration;

public class ServiceStatusHealthCheck extends CodeshelfHealthCheck {

	public ServiceStatusHealthCheck() {
		super("Service Status");
	}

	@Override
	protected Result check() throws Exception {
		RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
		long jvmUpTimeMillis = runtimeMX.getUptime();
		String jvmVersion = runtimeMX.getSpecVersion()+"-"+runtimeMX.getVmVersion();
		
		return Result.healthy("App v" + Configuration.getVersionStringShort() 
			+ " / JVM " + jvmVersion
			+ " / uptime " + jvmUpTimeMillis/1000 + "s");
	}
}
