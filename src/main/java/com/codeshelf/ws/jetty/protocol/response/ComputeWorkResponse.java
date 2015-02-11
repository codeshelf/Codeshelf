package com.codeshelf.ws.jetty.protocol.response;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.model.WorkInstructionCount;

public class ComputeWorkResponse extends ResponseABC {

	@Getter @Setter
	String networkGuid;
	
	@Getter @Setter
	Integer	totalGoodWorkInstructions = null;

	@Getter
	@Setter
	Map<String, WorkInstructionCount>	containerToWorkInstructionCountMap	= new HashMap<String, WorkInstructionCount>();

	public void addWorkInstructionCount(String containerId, WorkInstructionCount workInstructionCount) {
		containerToWorkInstructionCountMap.put(containerId, workInstructionCount);
	}


}