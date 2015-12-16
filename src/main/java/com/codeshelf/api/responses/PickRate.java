package com.codeshelf.api.responses;

import com.codeshelf.model.domain.WorkerEvent.EventType;

import lombok.Getter;
import lombok.Setter;

public class PickRate {
	@Getter @Setter
	private String workerId;

	@Getter @Setter
	private EventType eventType;

	@Getter @Setter
	private String purpose;
	
	@Getter @Setter
	private Integer hour;
	
	@Getter @Setter
	private Long picks;
	
	@Getter @Setter
	private Long quantity;

	public PickRate() {}
	
	public PickRate(String workerId, Integer hour, Long picks, Long quantity) {
		this.workerId = workerId;
		this.hour = hour;
		this.picks = picks;
		this.quantity = quantity;
	}
}
