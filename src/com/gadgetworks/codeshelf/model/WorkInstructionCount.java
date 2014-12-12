package com.gadgetworks.codeshelf.model;

import lombok.Getter;

/**
 * 
 * @author saba
 * A class used in the CheDeviceLogic and ComputeWorkResponse to hold counts for various types of work instruction. (Good/Bad for now)
 */
public class WorkInstructionCount {

	@Getter
	private final byte	goodCount;

	@Getter
	private final byte	badCount;

	public WorkInstructionCount(byte goodCount, byte badCount) {
		super();
		this.goodCount = goodCount;
		this.badCount = badCount;
	}

}