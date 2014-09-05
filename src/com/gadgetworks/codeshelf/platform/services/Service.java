package com.gadgetworks.codeshelf.platform.services;

import lombok.Getter;

public abstract class Service {
	
	@Getter
	boolean isInitialized=false;
	
	public abstract boolean stop();
	public abstract boolean start();
}
