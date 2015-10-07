package com.codeshelf.behavior;

import lombok.Getter;
import lombok.Setter;

import org.joda.time.DateTime;

class BatchReport {

	public enum Status {
		INPROGRESS,
		CANCELLED,
		COMPLETE,
		FAILED
		
	}
	
	@Getter
	@Setter
	private DateTime started;

	@Getter
	@Setter
	private DateTime ended;
	
	@Getter
	@Setter
	private int total;
	
	@Getter
	private int completeCount;
	
	@Getter
	@Setter
	private Status status = Status.INPROGRESS;
	
	BatchReport(DateTime started) {
		this.started = started;
	}

	public void setCancelled() {
		setStatus(Status.CANCELLED);
	}
	
	public void setException(Exception e) {
		if (e != null) {
			setStatus(Status.FAILED);
		}
	}

	public void setCount(int count) {
		if (total != 0 && total == count) {
			setStatus(Status.COMPLETE);
		}
		this.completeCount = count;
	}
	
	public String toString() { 
		return String.format("BatchReport: status: %s, completeCount: %d, total: %d", status, completeCount, total);
	}
	
}
