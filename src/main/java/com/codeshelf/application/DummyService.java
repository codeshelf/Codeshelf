package com.codeshelf.application;

import com.google.common.util.concurrent.AbstractIdleService;
/**
 * in some test situations, the app service manager won't own any services. 
 * @author default
 *
 */
public class DummyService extends AbstractIdleService {
	@Override
	protected void startUp() throws Exception {
	}

	@Override
	protected void shutDown() throws Exception {
	}
}
