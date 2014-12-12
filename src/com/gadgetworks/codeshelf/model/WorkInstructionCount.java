package com.gadgetworks.codeshelf.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author saba
 * A class used in the CheDeviceLogic and ComputeWorkResponse to hold counts for various types of work instruction. (Good/Bad for now)
 */
public class WorkInstructionCount {

	@Getter
	@Setter
	private byte	goodCount;

	@Getter
	@Setter
	private byte	badCount;

	public WorkInstructionCount(byte goodCount, byte badCount) {
		super();
		this.goodCount = goodCount;
		this.badCount = badCount;
	}

	/*
	 * No args-constructor for jackson
	 */
	public WorkInstructionCount() {
		super();
	}

	/**
	 * Equals is used for unit test
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WorkInstructionCount other = (WorkInstructionCount) obj;
		if (badCount != other.badCount)
			return false;
		if (goodCount != other.goodCount)
			return false;
		return true;
	}

}