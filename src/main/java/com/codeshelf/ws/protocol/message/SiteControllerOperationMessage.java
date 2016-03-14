package com.codeshelf.ws.protocol.message;

import lombok.Getter;

public class SiteControllerOperationMessage extends MessageABC{
	//Note that while the SHUTDOWN task simply shuts down a site controller that receives it
	//we use it as a restart command, since we have automated processes that will start downed site controllers
	//Such processes are not part of the local dev environment, so devs need to start site controllers manually
	//after they've been shut down
	public enum SiteControllerTask {DISCONNECT_DEVICES, SHUTDOWN};
	
	@Getter
	private SiteControllerTask task;
	
	public SiteControllerOperationMessage() {}
	
	public SiteControllerOperationMessage(SiteControllerTask task) {
		this.task = task;
	}
	
	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
