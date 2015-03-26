package com.codeshelf.service;

import com.google.common.util.concurrent.AbstractScheduledService;

public abstract class AbstractCodeshelfScheduledService extends AbstractScheduledService implements CodeshelfService {

	private boolean started = false;

	@Override
	protected abstract void startUp() throws Exception;

	@Override
	protected abstract void shutDown() throws Exception;
	
	@Override
	protected abstract void runOneIteration() throws Exception;

	@Override
	public String serviceName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void awaitRunningOrThrow() {
		ServiceUtility.awaitRunningOrThrow(this);
	}
	
	@Override
	public void awaitTerminatedOrThrow() {
		ServiceUtility.awaitTerminatedOrThrow(this);
	}
	
	protected void started() {
		if(this.started) return;
		awaitRunningOrThrow();
		this.started = true;
	}

	@Override
	public int getStartupTimeoutSeconds() {
		return ServiceUtility.DEFAULT_STARTUP_WAIT_SECONDS;
	}

	@Override
	public int getShutdownTimeoutSeconds() {
		return ServiceUtility.DEFAULT_SHUTDOWN_WAIT_SECONDS;		
	}
	
}
