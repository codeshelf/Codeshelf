package com.gadgetworks.codeshelf.platform;

import lombok.Getter;
import lombok.Setter;

public abstract class Service {
	
	@Getter @Setter
	boolean isInitialized=false;
	
	public abstract boolean stop();
	public abstract boolean start();
}
