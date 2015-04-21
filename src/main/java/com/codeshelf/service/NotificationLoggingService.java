package com.codeshelf.service;

import com.google.inject.Inject;

public class NotificationLoggingService implements IApiService{
	public enum EventType {LOGIN, SKIP_UPC, BUTTON, WI, Short}
	
	@Inject
	public NotificationLoggingService() {
	}

	public void log(final String deviceId, final EventType eventType){
		
	}
}
