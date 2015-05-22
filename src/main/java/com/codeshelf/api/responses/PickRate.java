package com.codeshelf.api.responses;

import lombok.Getter;
import lombok.Setter;

public class PickRate {
	@Getter @Setter
	private String workerId;

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
