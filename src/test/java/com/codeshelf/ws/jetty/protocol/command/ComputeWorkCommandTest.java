package com.codeshelf.ws.jetty.protocol.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.WorkList;

public class ComputeWorkCommandTest {

	@Test
	public void containerWorkCounts() {
		List<String> containers = new ArrayList<String>();
		containers.add("Container0");
		containers.add("Container1");
		containers.add("Container2");
		containers.add("Container3");
		containers.add("Container4");

		List<WorkInstruction> workInstructions = new ArrayList<WorkInstruction>();
		//Container 1 has 2 new wi, 1 in progress wi, 1  short
		WorkInstruction newWI = mock(WorkInstruction.class);
		when(newWI.getContainerId()).thenReturn("Container1");
		when(newWI.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(newWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		workInstructions.add(newWI);

		WorkInstruction newWI2 = mock(WorkInstruction.class);
		when(newWI2.getContainerId()).thenReturn("Container1");
		when(newWI2.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(newWI2.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		workInstructions.add(newWI2);

		WorkInstruction ipWI = mock(WorkInstruction.class);
		when(ipWI.getContainerId()).thenReturn("Container1");
		when(ipWI.getStatus()).thenReturn(WorkInstructionStatusEnum.INPROGRESS);
		when(ipWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		workInstructions.add(ipWI);

		WorkInstruction shortWI = mock(WorkInstruction.class);
		when(shortWI.getContainerId()).thenReturn("Container1");
		when(shortWI.getStatus()).thenReturn(WorkInstructionStatusEnum.SHORT);
		when(shortWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		workInstructions.add(shortWI);

		//Cotaniner 2 has 1 complete WI
		WorkInstruction completeWI = mock(WorkInstruction.class);
		when(completeWI.getContainerId()).thenReturn("Container2");
		when(completeWI.getStatus()).thenReturn(WorkInstructionStatusEnum.COMPLETE);
		when(completeWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);

		workInstructions.add(completeWI);

		//Container 3 has 1 good and one invalid WI
		WorkInstruction goodWI3 = mock(WorkInstruction.class);
		when(goodWI3.getContainerId()).thenReturn("Container3");
		when(goodWI3.getStatus()).thenReturn(WorkInstructionStatusEnum.INPROGRESS);
		when(goodWI3.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		workInstructions.add(goodWI3);

		WorkInstruction invalidWI3 = mock(WorkInstruction.class);
		when(invalidWI3.getContainerId()).thenReturn("Container3");
		when(invalidWI3.getStatus()).thenReturn(WorkInstructionStatusEnum.INVALID);
		when(invalidWI3.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		workInstructions.add(invalidWI3);

		//Container 4 has 1 good and 1 house keeping
		WorkInstruction goodWI4 = mock(WorkInstruction.class);
		when(goodWI4.getContainerId()).thenReturn("Container4");
		when(goodWI4.getStatus()).thenReturn(WorkInstructionStatusEnum.INPROGRESS);
		when(goodWI4.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		workInstructions.add(goodWI4);

		
		WorkInstruction hkWI4 = mock(WorkInstruction.class);
		when(hkWI4.getContainerId()).thenReturn("Container4");
		when(hkWI4.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(hkWI4.getType()).thenReturn(WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		workInstructions.add(hkWI4);


		WorkList workList = new WorkList();
		workList.setInstructions(workInstructions);
		Map<String, WorkInstructionCount> containerToWICountMap = ComputeWorkCommand.computeContainerWorkInstructionCounts(workList);

		//Make sure we have 4 entries with proper counts
		assertTrue(containerToWICountMap.size() == 4);

		//Check Container 0
		assertFalse(containerToWICountMap.containsKey("Container0"));

		//Check Container 1
		assertEquals(containerToWICountMap.get("Container1").getCompleteCount(), 0);
		assertEquals(containerToWICountMap.get("Container1").getGoodCount(), 3);
		assertEquals(containerToWICountMap.get("Container1").getShortCount(), 1);
		assertEquals(containerToWICountMap.get("Container1").getInvalidOrUnknownStatusCount(), 0);

		//Check Container 2
		assertEquals(containerToWICountMap.get("Container2").getCompleteCount(), 1);
		assertEquals(containerToWICountMap.get("Container2").getGoodCount(), 0);
		assertEquals(containerToWICountMap.get("Container2").getShortCount(), 0);
		assertEquals(containerToWICountMap.get("Container2").getInvalidOrUnknownStatusCount(), 0);


		//Check Container 3
		assertEquals(containerToWICountMap.get("Container3").getCompleteCount(), 0);
		assertEquals(containerToWICountMap.get("Container3").getGoodCount(), 1);
		assertEquals(containerToWICountMap.get("Container3").getShortCount(), 0);
		assertEquals(containerToWICountMap.get("Container3").getInvalidOrUnknownStatusCount(), 1);

		//Check Container 4
		assertEquals(containerToWICountMap.get("Container3").getCompleteCount(), 0);
		assertEquals(containerToWICountMap.get("Container3").getGoodCount(), 1);
		assertEquals(containerToWICountMap.get("Container3").getShortCount(), 0);
		assertEquals(containerToWICountMap.get("Container3").getInvalidOrUnknownStatusCount(), 1);
	}

}