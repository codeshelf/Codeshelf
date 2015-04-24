package com.codeshelf.api.responses;

import java.sql.Timestamp;
import java.util.UUID;

import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.service.NotificationService.EventType;

import lombok.Getter;

public class EventDisplay {
	//Event Fields
	@Getter
	private UUID eventId;
	
	@Getter
	private EventType type;
	
	@Getter
	private Timestamp createdAt;
	
	@Getter
	private String description;

	
	//Resolution Fields
	@Getter
	private Boolean resolved = false;
	
	@Getter
	private Timestamp resolvedAt;
	
	@Getter
	private String resolvedBy;

	public EventDisplay(WorkerEvent event) {
		eventId = event.getPersistentId();
		type = event.getEventType();
		createdAt = event.getCreated();
		description = event.getDomainId();
		Resolution resolution = event.getResolution();
		if (resolution != null){
			resolved = true;
			resolvedAt = resolution.getTimestamp();
			resolvedBy = resolution.getResolvedBy();					
		}
	}
}