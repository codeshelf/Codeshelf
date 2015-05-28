package com.codeshelf.ws.protocol.message;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import org.joda.time.DateTime;

import com.codeshelf.service.NotificationService.EventType;

public class NotificationMessage extends DeviceMessageABC{
	@Getter
	private Class<?> deviceClass;

	@Getter
	private UUID devicePersistentId;
		
	@Getter
	private String workerId;

	@Getter
	private EventType eventType;
	
	@Getter @Setter
	private UUID workInstructionId;

	@Getter
	private long	timestamp;
	
	public NotificationMessage() {}
	
	public NotificationMessage(final Class<?> deviceClass, final UUID devicePersistentId, final String deviceGuid, final String userId, final EventType eventType) {
		this(new DateTime(), deviceClass, devicePersistentId, deviceGuid, userId, eventType);
	}
	
	public NotificationMessage(final DateTime timestamp, final Class<?> deviceClass, final UUID devicePersistentId, final String deviceGuid, final String userId, final EventType eventType) {
		this.deviceClass = deviceClass;
		this.devicePersistentId = devicePersistentId;
		this.workerId = userId;
		this.eventType = eventType;
		this.timestamp = timestamp.getMillis();
		setNetGuidStr(deviceGuid);
	}
}
