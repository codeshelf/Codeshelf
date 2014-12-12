package com.gadgetworks.codeshelf.ws.jetty.protocol.response;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class ComputeWorkResponse extends ResponseABC {

	@Getter @Setter
	String networkGuid;
	
	@Getter @Setter
	Integer totalWorkInstructionCount = null;

	@Getter
	Map<String, WorkInstructionCount>	containerToWorkInstructionCountMap	= new HashMap<String, WorkInstructionCount>();

	public void addWorkInstructionCount(String containerId, WorkInstructionCount workInstructionCount) {
		containerToWorkInstructionCountMap.put(containerId, workInstructionCount);
	}

	public static final class WorkInstructionCount {
		private final byte	goodCount;
		private final byte	badCount;

		public WorkInstructionCount(byte goodCount, byte badCount) {
			super();
			this.goodCount = goodCount;
			this.badCount = badCount;
		}

	}

}
