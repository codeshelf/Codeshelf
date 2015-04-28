package com.codeshelf.api.responses;

import java.sql.Timestamp;

import lombok.Getter;

public class PickRate {
	@Getter
	private String workerId;

	@Getter
	private Timestamp hour;
	
	@Getter
	private String hourUTC;
	
	@Getter
	private Integer picks;
	
	@Getter
	private Integer quantity;

	public PickRate(String workerId, Timestamp hour, String hourUTC, Integer picks, Integer quantity) {
		this.workerId = workerId;
		this.hour = hour;
		this.hourUTC = hourUTC;
		this.picks = picks;
		this.quantity = quantity;
	}
}
