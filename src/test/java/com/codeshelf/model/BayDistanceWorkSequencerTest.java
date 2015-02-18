package com.codeshelf.model;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableList;

public class BayDistanceWorkSequencerTest {

	//@Test
	public void testSequenceByOrderDetail() {
		OrderDetail orderDetail1 = Mockito.mock(OrderDetail.class);
		OrderDetail orderDetail2 = Mockito.mock(OrderDetail.class);
		
		Mockito.when(orderDetail1.getPreferredSequence()).thenReturn(1);
		Mockito.when(orderDetail2.getPreferredSequence()).thenReturn(2);
		
		WorkInstruction workInstruction1 = Mockito.mock(WorkInstruction.class);
		WorkInstruction workInstruction2 = Mockito.mock(WorkInstruction.class);
		
		Mockito.when(workInstruction1.getOrderDetail()).thenReturn(orderDetail1);
		Mockito.when(workInstruction2.getOrderDetail()).thenReturn(orderDetail2);
		
		BayDistanceWorkInstructionSequencer subject = new BayDistanceWorkInstructionSequencer();
		List<WorkInstruction> result = subject.sort(Mockito.mock(Facility.class), ImmutableList.of(workInstruction2, workInstruction1));
		Assert.assertEquals(ImmutableList.of(workInstruction1, workInstruction2), result);
	}
	
}
