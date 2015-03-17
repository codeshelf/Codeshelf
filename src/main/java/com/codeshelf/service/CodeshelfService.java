package com.codeshelf.service;

import com.google.common.util.concurrent.Service;

public interface CodeshelfService extends Service {
	// service methods
	String serviceName();
	void awaitRunningOrThrow();
	void awaitTerminatedOrThrow();
	int getStartupTimeoutSeconds();
	int getShutdownTimeoutSeconds();

}
