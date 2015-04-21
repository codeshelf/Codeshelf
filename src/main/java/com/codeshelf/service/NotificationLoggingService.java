package com.codeshelf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class NotificationLoggingService implements IApiService{
	public enum EventType {LOGIN, SKIP_UPC, BUTTON, WI, Short}
	
	private static final Logger			LOGGER				= LoggerFactory.getLogger(NotificationLoggingService.class);
	
	@Inject
	public NotificationLoggingService() {
	}

	public void notify(final String deviceId, final EventType eventType){
		LOGGER.info("Notification from {}: {}", deviceId, eventType);
	}
}
