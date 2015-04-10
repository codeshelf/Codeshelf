package com.codeshelf.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.PropertyService;
import com.codeshelf.testframework.ServerTest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import edu.emory.mathcs.backport.java.util.Arrays;

public class BayDistanceWorkSequencerTest extends ServerTest {
	@Test
	public void testEdiSequenceByPath() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		//Combined Path order: [A3.B3, A3.B2, A3.B1, A1.B3, A2.B3, A1.B2, A2.B2, A1.B1, A2.B1]
		//Default Item pick order: I3(D303, [2])->I2(D302, [1])->I1(D301, [0])->I4(D401, [3])
		Facility facility = setUpSimpleNoSlotFacility();
		String prefferences[] = {"","","",""};
		List<WorkInstruction> instructions = common(facility, prefferences);

		WorkInstructionSequencerABC sequencer = getWorkSequenceSequencer(facility);
		List<WorkInstruction> sorted = sequencer.sort(facility, instructions);

		Assert.assertEquals(ImmutableList.of(instructions.get(2), instructions.get(1), instructions.get(0), instructions.get(3)), sorted);
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void testEdiBothWorkSequence() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		//Combined Path order: [A3.B3, A3.B2, A3.B1, A1.B3, A2.B3, A1.B2, A2.B2, A1.B1, A2.B1]
		//Default Item pick order: I3(D303, [2])->I2(D302, [1])->I1(D301, [0])->I4(D401, [3])
		Facility facility = setUpSimpleNoSlotFacility();
		String prefferences[] = {"1","2","3","4"};
		List<WorkInstruction> instructions = common(facility, prefferences);
		WorkInstructionSequencerABC sequencer = getWorkSequenceSequencer(facility);
		List<WorkInstruction> sorted = sequencer.sort(facility, instructions);

		Assert.assertEquals(ImmutableList.of(instructions.get(0), instructions.get(1), instructions.get(2), instructions.get(3)), sorted);
		this.getTenantPersistenceService().commitTransaction();
	}

	private WorkInstructionSequencerABC getWorkSequenceSequencer(Facility facility) {
		PropertyService.getInstance().changePropertyValue(facility, DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.WorkSequence.toString());
		WorkInstructionSequencerABC sequencer = WorkInstructionSequencerFactory.createSequencer(facility);
		return sequencer;
	}
	
	@Test
	public void testEdiSequenceMixed() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		//Combined Path order: [A3.B3, A3.B2, A3.B1, A1.B3, A2.B3, A1.B2, A2.B2, A1.B1, A2.B1]
		//Default Item pick order: I3(D303, [2])->I2(D302, [1])->I1(D301, [0])->I4(D401, [3])
		Facility facility = setUpSimpleNoSlotFacility();
		String prefferences[] = {"","","","2"};
		List<WorkInstruction> instructions = common(facility, prefferences);

		WorkInstructionSequencerABC sequencer = getWorkSequenceSequencer(facility);
		List<WorkInstruction> sorted = sequencer.sort(facility, instructions);

		Assert.assertEquals(ImmutableList.of(instructions.get(3), instructions.get(2), instructions.get(1), instructions.get(0)), sorted);
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testEdiSequenceNoLocationsOlnyPrefferences() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		//Combined Path order: [A3.B3, A3.B2, A3.B1, A1.B3, A2.B3, A1.B2, A2.B2, A1.B1, A2.B1]
		//Default Item pick order: I3(D303, [2])->I2(D302, [1])->I1(D301, [0])->I4(D401, [3])
		Facility facility = setUpSimpleNoSlotFacility();
		String prefferences[] = {"1","2","3","4"};
		List<WorkInstruction> instructions = common(facility, prefferences);
		
		instructions.get(0).setLocation(null);
		instructions.get(1).setLocation(null);
		instructions.get(2).setLocation(null);
		instructions.get(3).setLocation(null);

		WorkInstructionSequencerABC sequencer = getWorkSequenceSequencer(facility);
		List<WorkInstruction> sorted = sequencer.sort(facility, instructions);

		Assert.assertEquals(ImmutableList.of(instructions.get(0), instructions.get(1), instructions.get(2), instructions.get(3)), sorted);
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testEdiSequenceFirstRemoveWhenNoLocation() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		//Combined Path order: [A3.B3, A3.B2, A3.B1, A1.B3, A2.B3, A1.B2, A2.B2, A1.B1, A2.B1]
		//Default Item pick order: I3(D303, [2])->I2(D302, [1])->I1(D301, [0])->I4(D401, [3])
		Facility facility = setUpSimpleNoSlotFacility();
		String prefferences[] = {"","2","","4"};
		List<WorkInstruction> instructions = common(facility, prefferences);
		
		instructions.get(0).setLocation(null);
		instructions.get(1).setLocation(null);
		instructions.get(2).setLocation(null);
		instructions.get(3).setLocation(null);

		WorkInstructionSequencerABC sequencer = getWorkSequenceSequencer(facility);
		List<WorkInstruction> sorted = sequencer.sort(facility, instructions);

		Assert.assertEquals(ImmutableList.of(instructions.get(1), instructions.get(3)), sorted);
		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void testEdiSequenceFirstRemoveWhenNoPath() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		//Combined Path order: [A3.B3, A3.B2, A3.B1, A1.B3, A2.B3, A1.B2, A2.B2, A1.B1, A2.B1]
		//Default Item pick order: I3(D303, [2])->I2(D302, [1])->I1(D301, [0])->I4(D401, [3])
		Facility facility = setUpSimpleNoSlotFacility();
		String prefferences[] = {"1","","3",""};
		List<WorkInstruction> instructions = common(facility, prefferences);

		List<Path> paths = facility.getPaths();
		for (Path path : paths) {
			facility.removePath(path.getDomainId());
			path.deleteThisPath();
		}
		this.getTenantPersistenceService().commitTransaction();
		this.getTenantPersistenceService().beginTransaction();
		Facility.staticGetDao().reload(facility);
		WorkInstructionSequencerABC sequencer = getWorkSequenceSequencer(facility);
		List<WorkInstruction> sorted = sequencer.sort(facility, instructions);
	
		Assert.assertEquals(ImmutableList.of(instructions.get(0), instructions.get(2)), sorted);
		
		//Nicely put the Paths back, as Facilities don't like haviing them riped out
		for (Path path : paths) {
			facility.addPath(path);
		}
		this.getTenantPersistenceService().commitTransaction();
	}

	private List<WorkInstruction> common(Facility facility, String[] preffernces) throws IOException{
		//Combined Path order: [A3.B3, A3.B2, A3.B1, A1.B3, A2.B3, A1.B2, A2.B2, A1.B1, A2.B1]
		//Default Item pick order: I3(D303)->I2(D302)->I1(D301)->I4(D401)
		
		String csvString = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "I1,D301,Test Item 1,6,EA,6/25/14 12:00,135\r\n" //
				+ "I2,D302,Test Item 2,6,EA,6/25/14 12:00,8\r\n" //
				+ "I3,D303,Test Item 3,6,EA,6/25/14 12:00,55\r\n" //
				+ "I4,D401,Test Item 4,6,EA,6/25/14 12:00,66\r\n";
		importInventoryData(facility, csvString);

		String csvString2 = "orderId,orderGroupId,preAssignedContainerId,orderDetailId,itemId,description,quantity,uom,orderDate,dueDate,workSequence\r\n"
				+ "1,G1,1,101,I1,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,"+preffernces[0]+"\r\n"
				+ "1,G1,1,102,I2,Test Item 2,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,"+preffernces[1]+"\r\n"
				+ "1,G1,1,103,I3,Test Item 3,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,"+preffernces[2]+"\r\n"
				+ "1,G1,1,104,I4,Test Item 4,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,"+preffernces[3]+"\r\n";
		importOrdersData(facility, csvString2);

		OrderHeader header = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		// facility.getOrderHeader("1");
		
		OrderDetail detail1 = header.getOrderDetail("101");
		WorkInstruction workInstruction1 = new WorkInstruction();
		workInstruction1.setOrderDetail(detail1);
		workInstruction1.setLocation(LocationAlias.staticGetDao().findByDomainId(facility, "D301").getMappedLocation());
		
		OrderDetail detail2 = header.getOrderDetail("102");
		WorkInstruction workInstruction2 = new WorkInstruction();
		workInstruction2.setOrderDetail(detail2);
		workInstruction2.setLocation(LocationAlias.staticGetDao().findByDomainId(facility, "D302").getMappedLocation());

		OrderDetail detail3 = header.getOrderDetail("103");
		WorkInstruction workInstruction3 = new WorkInstruction();
		workInstruction3.setOrderDetail(detail3);
		workInstruction3.setLocation(LocationAlias.staticGetDao().findByDomainId(facility, "D303").getMappedLocation());
		
		OrderDetail detail4 = header.getOrderDetail("104");
		WorkInstruction workInstruction4 = new WorkInstruction();
		workInstruction4.setOrderDetail(detail4);
		workInstruction4.setLocation(LocationAlias.staticGetDao().findByDomainId(facility, "D401").getMappedLocation());

		List<WorkInstruction> instructions = new ArrayList<WorkInstruction>();
		instructions.add(workInstruction1);
		instructions.add(workInstruction2);
		instructions.add(workInstruction3);
		instructions.add(workInstruction4);
		return instructions;
	}
	
	@SuppressWarnings("unused")
	private static class StubLocationComparator implements Comparator<Location> {

		private Ordering<Location> ordering;
		
		@SuppressWarnings({ "unchecked" })
		public StubLocationComparator(Location... locations) {
			this.ordering = Ordering.explicit(Arrays.<Location>asList(locations));
		}
		
		@Override
		public int compare(Location o1, Location o2) {
			return this.ordering.compare(o1, o2);
		}
		
	}
}
