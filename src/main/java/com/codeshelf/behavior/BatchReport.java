package com.codeshelf.behavior;

import lombok.Getter;
import lombok.Setter;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BatchReport {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(BatchReport.class);

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
			LOGGER.error("BatchReport setting to FAILED due to exception", e);
		}
	}

	public void setCount(int count) {
		if (total != 0 && total == count) {
			setStatus(Status.COMPLETE);
		}
		this.completeCount = count;
	}

	public void setComplete() {
		setStatus(Status.COMPLETE);
	}

	public String toString() { 
		return String.format("BatchReport: status: %s, completeCount: %d, total: %d", status, completeCount, total);
	}

	
}
