package com.codeshelf.model;

import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import edu.emory.mathcs.backport.java.util.Arrays;

@Ignore
public class BayDistanceWorkSequencerTest {

	@Test
	public void testBothPreferredSequence() {
		
		
		WorkInstruction workInstruction1 = Mockito.mock(WorkInstruction.class);
		WorkInstruction workInstruction2 = Mockito.mock(WorkInstruction.class);
		Mockito.when(workInstruction1.getPreferredSequence()).thenReturn(1);
		Mockito.when(workInstruction2.getPreferredSequence()).thenReturn(2);

		BayDistanceWorkInstructionSequencer subject = new BayDistanceWorkInstructionSequencer();
		List<WorkInstruction> result = subject.sort(Mockito.mock(Facility.class), ImmutableList.of(workInstruction2, workInstruction1));
		Assert.assertEquals(ImmutableList.of(workInstruction1, workInstruction2), result);
	}

	@Test
	public void testPreferredSequenceNoPath() {
		
		
		WorkInstruction workInstruction1 = Mockito.mock(WorkInstruction.class);
		WorkInstruction workInstruction2 = Mockito.mock(WorkInstruction.class);
		Mockito.when(workInstruction1.getPreferredSequence()).thenReturn(1);
		Mockito.when(workInstruction2.getPreferredSequence()).thenReturn(2);

		//Setup locations no ordering because they have no path
		final Location location1 = Mockito.mock(Location.class);
		final Location location2 = Mockito.mock(Location.class);
		Mockito.when(workInstruction1.getLocation()).thenReturn(location1);
		Mockito.when(workInstruction2.getLocation()).thenReturn(location2);
		
		Comparator<?> locationComparator =  Ordering.allEqual();

		
		BayDistanceWorkInstructionSequencer subject = new BayDistanceWorkInstructionSequencer((Comparator<Location>)locationComparator);
		List<WorkInstruction> result = subject.sort(Mockito.mock(Facility.class), ImmutableList.of(workInstruction2, workInstruction1));
		Assert.assertEquals(ImmutableList.of(workInstruction1, workInstruction2), result);
	}

	
	@Test
	public void testSequenceByPath() {
		WorkInstruction workInstruction1 = Mockito.mock(WorkInstruction.class);
		WorkInstruction workInstruction2 = Mockito.mock(WorkInstruction.class);
		final Location location1 = Mockito.mock(Location.class);
		final Location location2 = Mockito.mock(Location.class);
		Mockito.when(workInstruction1.getLocation()).thenReturn(location1);
		Mockito.when(workInstruction2.getLocation()).thenReturn(location2);
		
		Comparator<Location> locationComparator = new StubLocationComparator(location1, location2);

		BayDistanceWorkInstructionSequencer subject = new BayDistanceWorkInstructionSequencer(locationComparator);
		List<WorkInstruction> result = subject.sort(Mockito.mock(Facility.class), ImmutableList.of(workInstruction2, workInstruction1));
		Assert.assertEquals(ImmutableList.of(workInstruction1, workInstruction2), result);
	}
	
	@Test
	public void testPreferredSequenceFirstThenByPath() {
		WorkInstruction workInstruction1 = Mockito.mock(WorkInstruction.class);
		WorkInstruction workInstruction2 = Mockito.mock(WorkInstruction.class);

		//this wi2 should sort first because it has an explicit preferred sequence
		Mockito.when(workInstruction1.getPreferredSequence()).thenReturn(null);
		Mockito.when(workInstruction2.getPreferredSequence()).thenReturn(2);

		//Setup locations in explicit order for wi
		final Location location1 = Mockito.mock(Location.class);
		final Location location2 = Mockito.mock(Location.class);
		Mockito.when(workInstruction1.getLocation()).thenReturn(location1);
		Mockito.when(workInstruction2.getLocation()).thenReturn(location2);
		
		Comparator<Location> locationComparator = new StubLocationComparator(location1, location2);
		
		BayDistanceWorkInstructionSequencer subject = new BayDistanceWorkInstructionSequencer(locationComparator);
		List<WorkInstruction> result = subject.sort(Mockito.mock(Facility.class), ImmutableList.of(workInstruction1, workInstruction2));
		//note that wi2 should sort first in this case
		Assert.assertEquals(ImmutableList.of(workInstruction2, workInstruction1), result);
	}
	
	@Test
	public void testSequenceFirstRemoveWhenNoLocation() {
		WorkInstruction workInstruction1 = Mockito.mock(WorkInstruction.class);
		WorkInstruction workInstruction2 = Mockito.mock(WorkInstruction.class);

		//this wi2 should sort first because it has an explicit preferred sequence
		Mockito.when(workInstruction1.getPreferredSequence()).thenReturn(null);
		Mockito.when(workInstruction2.getPreferredSequence()).thenReturn(2);

		//Setup locations in explicit order for wi
		Mockito.when(workInstruction1.getLocation()).thenReturn(null);
		Mockito.when(workInstruction2.getLocation()).thenReturn(null);
		
		BayDistanceWorkInstructionSequencer subject = new BayDistanceWorkInstructionSequencer();
		List<WorkInstruction> result = subject.sort(Mockito.mock(Facility.class), ImmutableList.of(workInstruction1, workInstruction2));
		//note that wi2 should sort wi should be removed
		Assert.assertEquals(ImmutableList.of(workInstruction2), result);
	}

	@Test
	public void testSequenceFirstRemoveWhenNoPath() {
		WorkInstruction workInstruction1 = Mockito.mock(WorkInstruction.class);
		WorkInstruction workInstruction2 = Mockito.mock(WorkInstruction.class);

		//this wi2 should sort first because it has an explicit preferred sequence
		Mockito.when(workInstruction1.getPreferredSequence()).thenReturn(null);
		Mockito.when(workInstruction2.getPreferredSequence()).thenReturn(2);

		//Setup locations in explicit order for wi
		final Location location1 = Mockito.mock(Location.class);
		final Location location2 = Mockito.mock(Location.class);
		Mockito.when(workInstruction1.getLocation()).thenReturn(location1);
		Mockito.when(workInstruction2.getLocation()).thenReturn(location2);
		
		Comparator locationComparator = Ordering.allEqual();
		
		BayDistanceWorkInstructionSequencer subject = new BayDistanceWorkInstructionSequencer(locationComparator);
		List<WorkInstruction> result = subject.sort(Mockito.mock(Facility.class), ImmutableList.of(workInstruction1, workInstruction2));
		//note that wi2 should sort first in this case
		Assert.assertEquals(ImmutableList.of(workInstruction2), result);
	}

	
	private static class StubLocationComparator implements Comparator<Location> {

		private Ordering<Location> ordering;
		
		public StubLocationComparator(Location... locations) {
			this.ordering = Ordering.explicit(Arrays.<Location>asList(locations));
		}
		
		@Override
		public int compare(Location o1, Location o2) {
			return this.ordering.compare(o1, o2);
		}
		
	}
}
