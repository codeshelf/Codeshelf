package com.codeshelf.ws.protocol.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.codeshelf.model.WorkInstructionCount;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.testframework.MinimalTest;
import com.google.common.collect.Lists;

public class ComputeWorkCommandTest extends MinimalTest {

	@Test
	public void containerWorkCounts() {
		List<WorkInstruction> workInstructions = new ArrayList<WorkInstruction>();
		//Container 1 has 2 new wi, 1 in progress wi, 1  short
		WorkInstruction newWI = mock(WorkInstruction.class);
		when(newWI.getContainerId()).thenReturn("Container1");
		when(newWI.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(newWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(newWI.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec1"));
		workInstructions.add(newWI);

		WorkInstruction newWI2 = mock(WorkInstruction.class);
		when(newWI2.getContainerId()).thenReturn("Container1");
		when(newWI2.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(newWI2.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(newWI2.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec2"));
		workInstructions.add(newWI2);

		WorkInstruction ipWI = mock(WorkInstruction.class);
		when(ipWI.getContainerId()).thenReturn("Container1");
		when(ipWI.getStatus()).thenReturn(WorkInstructionStatusEnum.INPROGRESS);
		when(ipWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(ipWI.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec3"));
		workInstructions.add(ipWI);

		WorkInstruction shortWI = mock(WorkInstruction.class);
		when(shortWI.getContainerId()).thenReturn("Container1");
		when(shortWI.getStatus()).thenReturn(WorkInstructionStatusEnum.SHORT);
		when(shortWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(shortWI.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec4"));
		workInstructions.add(shortWI);

		//Cotaniner 2 has 1 complete WI
		WorkInstruction completeWI = mock(WorkInstruction.class);
		when(completeWI.getContainerId()).thenReturn("Container2");
		when(completeWI.getStatus()).thenReturn(WorkInstructionStatusEnum.COMPLETE);
		when(completeWI.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(completeWI.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec5"));
		workInstructions.add(completeWI);

		//Container 3 has 1 good and one invalid WI
		WorkInstruction goodWI3 = mock(WorkInstruction.class);
		when(goodWI3.getContainerId()).thenReturn("Container3");
		when(goodWI3.getStatus()).thenReturn(WorkInstructionStatusEnum.INPROGRESS);
		when(goodWI3.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(goodWI3.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec6"));
		workInstructions.add(goodWI3);

		WorkInstruction invalidWI3 = mock(WorkInstruction.class);
		when(invalidWI3.getContainerId()).thenReturn("Container3");
		when(invalidWI3.getStatus()).thenReturn(WorkInstructionStatusEnum.INVALID);
		when(invalidWI3.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(invalidWI3.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec7"));
		workInstructions.add(invalidWI3);

		//Container 4 has 1 good and 1 house keeping
		WorkInstruction goodWI4 = mock(WorkInstruction.class);
		when(goodWI4.getContainerId()).thenReturn("Container4");
		when(goodWI4.getStatus()).thenReturn(WorkInstructionStatusEnum.INPROGRESS);
		when(goodWI4.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(goodWI4.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec8"));
		workInstructions.add(goodWI4);

		
		WorkInstruction hkWI4 = mock(WorkInstruction.class);
		when(hkWI4.getContainerId()).thenReturn("Container4");
		when(hkWI4.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(hkWI4.getType()).thenReturn(WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		when(hkWI4.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec9"));
		workInstructions.add(hkWI4);

		WorkList allWork = new WorkList();
		allWork.setInstructions(workInstructions);
		Map<String, WorkInstructionCount> containerToWICountMap = ComputeWorkCommand.computeContainerWorkInstructionCounts(allWork, workInstructions);

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

	@Test
	public void containerWorkMultiPath() {
		ArrayList<WorkInstruction> allInstructions = Lists.newArrayList();
		ArrayList<WorkInstruction> instructionsOnPath = Lists.newArrayList();
		
		WorkInstruction wi1 = mock(WorkInstruction.class);
		when(wi1.getContainerId()).thenReturn("Container1");
		when(wi1.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(wi1.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(wi1.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec1"));
		allInstructions.add(wi1);
		instructionsOnPath.add(wi1);

		WorkInstruction wi2 = mock(WorkInstruction.class);
		when(wi2.getContainerId()).thenReturn("Container1");
		when(wi2.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(wi2.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(wi2.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec2"));
		allInstructions.add(wi2);

		WorkInstruction wi3 = mock(WorkInstruction.class);
		when(wi3.getContainerId()).thenReturn("Container1");
		when(wi3.getStatus()).thenReturn(WorkInstructionStatusEnum.SHORT);
		when(wi3.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(wi3.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec3"));
		allInstructions.add(wi3);
		instructionsOnPath.add(wi3);

		WorkInstruction wi4 = mock(WorkInstruction.class);
		when(wi4.getContainerId()).thenReturn("Container1");
		when(wi4.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(wi4.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(wi4.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec4"));
		allInstructions.add(wi4);
		
		WorkInstruction wi5 = mock(WorkInstruction.class);
		when(wi5.getContainerId()).thenReturn("Container1");
		when(wi5.getStatus()).thenReturn(WorkInstructionStatusEnum.NEW);
		when(wi5.getType()).thenReturn(WorkInstructionTypeEnum.ACTUAL);
		when(wi5.getPersistentId()).thenReturn(UUID.fromString("43d066b2-dd73-11e4-b9d6-1681e6b88ec5"));
		allInstructions.add(wi5);
		instructionsOnPath.add(wi5);

		WorkList allWork = new WorkList();
		allWork.setInstructions(allInstructions);
		Map<String, WorkInstructionCount> containerToWICountMap = ComputeWorkCommand.computeContainerWorkInstructionCounts(allWork, instructionsOnPath);
		
		assertEquals(2, containerToWICountMap.get("Container1").getGoodCount());
		assertEquals(1, containerToWICountMap.get("Container1").getShortCount());
		assertEquals(2, containerToWICountMap.get("Container1").getUncompletedInstructionsOnOtherPaths());
	}
}
