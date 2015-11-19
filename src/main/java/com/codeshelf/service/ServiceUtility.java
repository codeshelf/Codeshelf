package com.codeshelf.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.codeshelf.application.FacilitySchedulerService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

public class ServiceUtility {

	public static final int DEFAULT_STARTUP_WAIT_SECONDS = 45;
	public static final int DEFAULT_SHUTDOWN_WAIT_SECONDS = 45;
	public static void awaitRunningOrThrow(ServiceManager serviceManager) {
		try {
			serviceManager.awaitHealthy(DEFAULT_STARTUP_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout initializing "+ serviceManager.servicesByState(), e);
		}
	}

	public static void awaitTerminatedOrThrow(ServiceManager serviceManager) {
		try {
			serviceManager.awaitStopped(DEFAULT_SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout terminating "+ serviceManager.servicesByState(), e);
		}
	}

	public static void awaitRunningOrThrow(List<? extends Service> services) {
		for (Service service : services) {
			try {
				service.awaitRunning(DEFAULT_STARTUP_WAIT_SECONDS, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new IllegalStateException("timeout initializing " + service, e);
			}
		}
	}

	public static void awaitTerminatedOrThrow(List<? extends Service> services) {
		for (Service service : services) {
			try {
				service.awaitTerminated(DEFAULT_STARTUP_WAIT_SECONDS, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new IllegalStateException("timeout terminating " + service, e);
			}
		}
	}
	
	
	
	public static void awaitRunningOrThrow(CodeshelfService service) {
		try {
			service.awaitRunning(DEFAULT_STARTUP_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout initializing " + service.serviceName(), e);
		}
	}

	public static void awaitTerminatedOrThrow(CodeshelfService service) {
		try {
			service.awaitTerminated(DEFAULT_SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout terminating " + service.serviceName(), e);
		}
	}

	public static void stopAsync(List<FacilitySchedulerService> services) {
		for (Service service : services) {
			service.stopAsync();
		}
	}

}
