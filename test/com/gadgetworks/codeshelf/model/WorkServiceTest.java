package com.gadgetworks.codeshelf.model;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.service.WorkService;
import com.google.common.collect.ImmutableList;

public class WorkServiceTest<E> {

	
	
	@Test
	public void summariesAreSorted() {
		ITypedDao<WorkInstruction> workInstructionDao = mock(ITypedDao.class);
		WorkInstruction.DAO = workInstructionDao;
		
		ArrayList<WorkInstruction> inputs = new ArrayList<WorkInstruction>();
		for (int i = 0; i < 4; i++) {
			inputs.add(generateValidWorkInstruction(nextUniqueTimestamp()));
		}
		when(workInstructionDao.findByFilter(anyString(), anyMap())).thenReturn(inputs);
		
		WorkService workService = new WorkService();
		List<WiSetSummary> workSummaries  = workService.workSummary("testCheId", "testFacilityId");
		
		//since each timestamp is unique they will each get summarized into one summary object
		Assert.assertEquals(inputs.size(), workSummaries.size());
		Timestamp lastTimestamp = new Timestamp(Long.MAX_VALUE);
		for (WiSetSummary wiSetSummary : workSummaries) {
			Timestamp thisTime = wiSetSummary.getAssignedTime();
			Assert.assertTrue(thisTime.toString() + "should have been before" + lastTimestamp, wiSetSummary.getAssignedTime().before(lastTimestamp));
			lastTimestamp = wiSetSummary.getAssignedTime();
		}
	}

	private WorkInstruction generateValidWorkInstruction(Timestamp timestamp) {
		WorkInstruction wi = new WorkInstruction();
		wi.setTypeEnum(WorkInstructionTypeEnum.ACTUAL);
		wi.setStatusEnum(WorkInstructionStatusEnum.COMPLETE);
		wi.setAssigned(timestamp);
		return wi;
	}
	
	private Timestamp nextUniqueTimestamp() {
		return new Timestamp(System.currentTimeMillis() - Math.abs(RandomUtils.nextLong()));
	}
	
}
