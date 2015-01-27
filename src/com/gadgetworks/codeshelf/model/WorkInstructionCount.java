package com.gadgetworks.codeshelf.model;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * @author saba
 * A class used in the CheDeviceLogic and ComputeWorkResponse to hold counts for various types of work instruction.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkInstructionCount {

	@Getter
	@Setter
	private int	goodCount	= 0;

	@Getter
	@Setter
	private int	shortCount	= 0;

	@Getter
	@Setter
	private int	completeCount		= 0;

	@Getter
	@Setter
	private int	invalidOrUnknownStatusCount	= 0;

	public WorkInstructionCount(int goodCount,
		int unknownOrderIdCount,
		int immediateShortCount,
		int completeCount,
		int invalidOrUnknownStatusCount) {
		super();
		this.goodCount = goodCount;
		this.shortCount = immediateShortCount;
		this.completeCount = completeCount;
		this.invalidOrUnknownStatusCount = invalidOrUnknownStatusCount;
	}

	public WorkInstructionCount() {
		super();
	}

	public void decrementGoodCountAndIncrementShortCount() {
		goodCount--;
		shortCount++;
	}

	public void decrementGoodCountAndIncrementCompleteCount() {
		goodCount--;
		completeCount++;
	}

	/**
	 * @return immediateShortCount > 0 || invalidOrUnknownStatusCount > 0
	 */
	public boolean hasBadCounts() {
		return shortCount > 0 || invalidOrUnknownStatusCount > 0;
	}

	public void incrementGoodCount() {
		goodCount++;
	}

	public void incrementImmediateShortCount() {
		shortCount++;
	}

	public void incrementCompleteCount() {
		completeCount++;
	}

	public void incrementInvalidOrUnknownStatusCount() {
		invalidOrUnknownStatusCount++;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + completeCount;
		result = prime * result + goodCount;
		result = prime * result + shortCount;
		result = prime * result + invalidOrUnknownStatusCount;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass().equals(obj.getClass()))
			return false;
		WorkInstructionCount other = (WorkInstructionCount) obj;
		if (completeCount != other.completeCount)
			return false;
		if (goodCount != other.goodCount)
			return false;
		if (shortCount != other.shortCount)
			return false;
		if (invalidOrUnknownStatusCount != other.invalidOrUnknownStatusCount)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WorkInstructionCount [goodCount=" + goodCount + ", immediateShortCount=" + shortCount + ", completeCount="
				+ completeCount + ", invalidOrUnknownStatusCount=" + invalidOrUnknownStatusCount + "]";
	}



}
