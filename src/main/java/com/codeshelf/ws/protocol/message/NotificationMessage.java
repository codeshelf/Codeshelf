package com.codeshelf.ws.protocol.message;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.service.NotificationService.EventType;

public class NotificationMessage extends MessageABC{
	@Getter
	private Class<?> deviceClass;

	@Getter
	private UUID devicePersistentId;
	
	@Getter
	private String deviceGuid;
	
	@Getter
	private String workerId;

	@Getter
	private EventType eventType;
	
	@Getter @Setter
	private UUID workInstructionId;
	
	public NotificationMessage() {}
	
	public NotificationMessage(final Class<?> deviceClass, final UUID devicePersistentId, final String deviceGuid, final String userId, final EventType eventType) {
		this.deviceClass = deviceClass;
		this.devicePersistentId = devicePersistentId;
		this.deviceGuid = deviceGuid;
		this.workerId = userId;
		this.eventType = eventType;
	}

	@Override
	public String getDeviceIdentifier() {
		return getDeviceGuid();
	}
}
