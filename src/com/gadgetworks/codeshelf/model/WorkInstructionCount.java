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
	private int	unknownOrderIdCount	= 0;

	@Getter
	@Setter
	private int	immediateShortCount	= 0;


	public WorkInstructionCount(int goodCount, int unknownOrderIdCount, int immediateShortCount) {
		super();
		this.goodCount = goodCount;
		this.unknownOrderIdCount = unknownOrderIdCount;
		this.immediateShortCount = immediateShortCount;
	}

	/**
	 * No-arg constructor for jackson
	 */
	public WorkInstructionCount() {
		super();
	}

	/**
	 * A work instruction is already done if there are no good work instructions. No bad works instructions (immediate short, etc)
	 * And the work was valid (it did not have an unknown orderId)
	 */
	public boolean isAlreadyDone() {
		return unknownOrderIdCount == 0 && goodCount == 0 && immediateShortCount == 0;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + goodCount;
		result = prime * result + immediateShortCount;
		result = prime * result + unknownOrderIdCount;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorkInstructionCount other = (WorkInstructionCount) obj;
		if (goodCount != other.goodCount)
			return false;
		if (immediateShortCount != other.immediateShortCount)
			return false;
		if (unknownOrderIdCount != other.unknownOrderIdCount)
			return false;
		return true;
	}


}