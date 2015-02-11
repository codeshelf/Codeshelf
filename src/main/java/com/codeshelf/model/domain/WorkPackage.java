package com.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class WorkPackage {
	public static class SingleWorkItem{
		@Getter
		@Setter
		private WorkInstruction instruction;
		
		@Getter
		@Setter
		private OrderDetail detail;
	}
	
	public static class WorkList{
		@Getter
		@Setter
		private List<WorkInstruction> instructions = new ArrayList<WorkInstruction>();
		
		@Getter
		@Setter
		private List<OrderDetail> details = new ArrayList<OrderDetail>();		
	}
}
