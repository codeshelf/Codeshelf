package com.codeshelf.ws.protocol.message;

import com.codeshelf.service.NotificationLoggingService.EventType;

import lombok.Getter;

public class NotificationMessage extends MessageABC{
	@Getter
	private String deviceId;

	@Getter
	private EventType eventType;
	
	public NotificationMessage() {}
	
	public NotificationMessage(final String deviceId, final EventType eventType) {
		this.deviceId = deviceId;
		this.eventType = eventType;
	}
}
