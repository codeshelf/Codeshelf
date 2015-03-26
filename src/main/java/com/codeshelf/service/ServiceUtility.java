package com.codeshelf.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServiceUtility {

	public static final int DEFAULT_STARTUP_WAIT_SECONDS = 45;
	public static final int DEFAULT_SHUTDOWN_WAIT_SECONDS = 45;

	public static void awaitRunningOrThrow(CodeshelfService service) {
		try {
			service.awaitRunning(DEFAULT_STARTUP_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout initializing "+service.serviceName(),e);
		}
	}

	public static void awaitTerminatedOrThrow(CodeshelfService service) {
		try {
			service.awaitRunning(DEFAULT_SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new IllegalStateException("timeout initializing "+service.serviceName(),e);
		}
	}

}
