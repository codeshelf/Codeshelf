package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.generators.WorkInstructionGenerator;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.WiFactory;
import com.gadgetworks.codeshelf.model.WiSetSummary;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.service.ProductivityCheSummaryList;
import com.gadgetworks.codeshelf.service.ProductivitySummaryList;
import com.gadgetworks.codeshelf.service.ProductivitySummaryList.GroupSummary;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.flyweight.command.NetGuid;

public class ProductivityReportingTest extends DomainTestABC {
	@SuppressWarnings("unused")
	private final static Logger LOGGER=LoggerFactory.getLogger(ProductivityReportingTest.class);
	
	@Test
	public void testProductivitySummary() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createFacilityWithOutboundOrders();
		UUID facilityId = facility.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		ProductivitySummaryList productivitySummary = WorkService.getProductivitySummary(facilityId, true);
		Assert.assertNotNull(productivitySummary);
		HashMap<String, GroupSummary> groups = productivitySummary.getGroups();
		Assert.assertEquals(groups.size(), 3);
		Iterator<String> groupNames = groups.keySet().iterator();
		while (groupNames.hasNext()) {
			String groupName = groupNames.next();
			Assert.assertTrue(OrderGroup.UNDEFINED.equals(groupName) || "GROUP1".equals(groupName) || "GROUP2".equals(groupName));
		}
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testGetCheSummaryNoRuns() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createFacilityWithOutboundOrders();
		UUID facilityId = facility.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		//Get all summaries
		ProductivityCheSummaryList cheSummaries = WorkService.getCheByGroupSummary(facilityId);
		Assert.assertNotNull(cheSummaries);
		Assert.assertEquals(cheSummaries.getRunsByGroup().size(), 0);
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testGetCheSummaryAllWorkInstructionCombos() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		List<WorkInstruction> workInstructions = createFacilityWithOneRunAllWorkInstructionCombos();
		
		UUID facilityId = workInstructions.get(0).getParent().getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		//Get all summaries
		ProductivityCheSummaryList cheSummaries = WorkService.getCheByGroupSummary(facilityId);
		WiSetSummary summary = cheSummaries.getRunsByGroup().values().iterator().next().get(0);

		//filter housekeeping
		ArrayList<WorkInstruction> withoutHK = new ArrayList<>();
		for (WorkInstruction workInstruction : workInstructions) {
			if (!workInstruction.isHousekeeping()) {
				withoutHK.add(workInstruction);
			}
		}
		
		Assert.assertEquals(withoutHK.size(), summary.getTotal());
	}
	
	@Test
	public void testGetCheSummaryOneRun() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createFacilityWithOneRun("PRTEST2.O2");
		UUID facilityId = facility.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		//Get all summaries
		ProductivityCheSummaryList cheSummaries = WorkService.getCheByGroupSummary(facilityId);
		Assert.assertNotNull(cheSummaries);
		//Get summaries for the only group
		List<List<WiSetSummary>> groups = new ArrayList<>(cheSummaries.getRunsByGroup().values());
		Assert.assertEquals(groups.size(), 1);
		//Get runs for the group
		List<WiSetSummary> groupRuns = groups.get(0);
		Assert.assertEquals(groupRuns.size(), 1);
		WiSetSummary run = groupRuns.get(0);
		//Verify retrieved run
		testRunSummary(run, 0, 0, 2, 0, 2, 1);
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testGetCheSummaryTwoRuns() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createFacilityWithTwoRuns("PRTEST2.O3");
		UUID facilityId = facility.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		//Get all summaries
		ProductivityCheSummaryList cheSummaries = WorkService.getCheByGroupSummary(facilityId);
		Assert.assertNotNull(cheSummaries);
		//Get summaries for the only group
		List<List<WiSetSummary>> groups = new ArrayList<>(cheSummaries.getRunsByGroup().values());
		Assert.assertEquals(groups.size(), 1);
		//Get runs for the group
		List<WiSetSummary> groupRuns = groups.get(0);
		Assert.assertEquals(groupRuns.size(), 2);
		//Verify runs
		testRunSummary(groupRuns.get(0), 0, 2, 1, 0, 0, 0); //"2014-12-23 19:40:20.000+0000"
		testRunSummary(groupRuns.get(1), 1, 0, 0, 0, 1, 0);	//"2014-12-22 23:46:00.000+0000"
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testGetCheSummaryTwoGroups() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createFacilityWithTwoGroups("PRTEST2.O4");
		UUID facilityId = facility.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		//Get all summaries
		ProductivityCheSummaryList cheSummaries = WorkService.getCheByGroupSummary(facilityId);
		Assert.assertNotNull(cheSummaries);
		
		//Get groups
		HashMap<String, List<WiSetSummary>> groups = cheSummaries.getRunsByGroup();
		Assert.assertEquals(groups.size(), 2);
		Iterator<String> groupNames = groups.keySet().iterator();
		while (groupNames.hasNext()){
			String groupName = groupNames.next();
			List<WiSetSummary> groupRuns = groups.get(groupName);
			Assert.assertEquals(groupRuns.size(), 1);
			if (OrderGroup.UNDEFINED.equals(groupName)){
				testRunSummary(groupRuns.get(0), 1, 0, 0, 0, 1, 0);
			} else {
				testRunSummary(groupRuns.get(0), 0, 2, 1, 0, 0, 0);
			}
			
		}
		this.getPersistenceService().commitTenantTransaction();
	}

	private void testRunSummary(WiSetSummary s, int invalid, int New, int inprogress, int Short, int complete, int revert){
		Assert.assertNotNull(s);
		Assert.assertTrue(s.getInvalidCount() == invalid && s.getNewCount() == New && s.getInprogressCount() == inprogress && s.getShortCount() == Short && s.getCompleteCount() == complete && s.getRevertCount() == revert);
	}
	
	
	
	@SuppressWarnings("unused")
	private Facility createFacilityWithOneRun(String orgId){
		//12/22/14 6:46 PM = 1419291960000
		Facility facility = createFacility();
		Che che = Che.DAO.findByDomainId(null,"CHE1");

		UomMaster uomMaster = createUomMaster("EA", facility);
		ItemMaster itemMaster = createItemMaster("ITEM1", facility, uomMaster);
		Container container = createContainer("C1", facility);
		OrderHeader header = createOrderHeader("OH1", OrderTypeEnum.OUTBOUND, facility, null);
		OrderDetail detail = createOrderDetail(header, itemMaster);
		
		WorkInstruction wi = null;
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.COMPLETE, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419291960000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.COMPLETE, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419291960000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INPROGRESS, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419291960000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INPROGRESS, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419291960000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.REVERT, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419291960000l));
		
		return facility;
	}

	private List<WorkInstruction>  createFacilityWithOneRunAllWorkInstructionCombos(){
		//12/22/14 6:46 PM = 1419291960000
		Facility facility = getDefaultFacility();
		WorkInstructionGenerator generator = new WorkInstructionGenerator();
		List<WorkInstruction> generatedWIs = generator.generateCombinations(facility, new Timestamp(1419291960000l));
		return generatedWIs;
	}

	@SuppressWarnings("unused")
	private Facility createFacilityWithTwoRuns(String orgId){
		//12/22/14 6:46 PM = 1419291960000
		//12/23/14 7:40 PM = 1419363620000
		Facility facility = createFacility();
		Che che = Che.DAO.findByDomainId(null,"CHE1");

		UomMaster uomMaster = createUomMaster("EA", facility);
		ItemMaster itemMaster = createItemMaster("ITEM1", facility, uomMaster);
		Container container = createContainer("C1", facility);
		OrderHeader header = createOrderHeader("OH1", OrderTypeEnum.OUTBOUND, facility, null);
		OrderDetail detail = createOrderDetail(header, itemMaster);
		
		WorkInstruction wi = null;
		//Run 1
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.COMPLETE, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419291960000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INVALID, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419291960000l));
		//Run 2
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INPROGRESS, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419363620000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419363620000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW, WorkInstructionTypeEnum.ACTUAL, detail, container, che, facility, new Timestamp(1419363620000l));
		
		return facility;
	}	

	@SuppressWarnings("unused")
	private Facility createFacilityWithTwoGroups(String orgId){
		Facility facility = createFacility();
		CodeshelfNetwork network = facility.createNetwork("WITEST"); 
		Che che = network.createChe("WITEST", new NetGuid("0x00000001"));

		UomMaster uomMaster = createUomMaster("EA", facility);
		ItemMaster itemMaster = createItemMaster("ITEM1", facility, uomMaster);
		Container container = createContainer("C1", facility);
		WorkInstruction wi = null;
		
		//Group 1 (undefined)
		OrderHeader header1 = createOrderHeader("OH1", OrderTypeEnum.OUTBOUND, facility, null);
		OrderDetail detail1 = createOrderDetail(header1, itemMaster);
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.COMPLETE, WorkInstructionTypeEnum.ACTUAL, detail1, container, che, facility, new Timestamp(1419291960000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INVALID, WorkInstructionTypeEnum.ACTUAL, detail1, container, che, facility, new Timestamp(1419291960000l));
		
		//Group 2
		OrderGroup orderGroup1 = createOrderGroup("GROUP1", facility);
		OrderHeader header2 = createOrderHeader("OH2", OrderTypeEnum.OUTBOUND, facility, orderGroup1);
		OrderDetail detail2 = createOrderDetail(header2, itemMaster);
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.INPROGRESS, WorkInstructionTypeEnum.ACTUAL, detail2, container, che, facility, new Timestamp(1419363620000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW, WorkInstructionTypeEnum.ACTUAL, detail2, container, che, facility, new Timestamp(1419363620000l));
		wi = WiFactory.createWorkInstruction(WorkInstructionStatusEnum.NEW, WorkInstructionTypeEnum.ACTUAL, detail2, container, che, facility, new Timestamp(1419363620000l));

		return facility;
	}
}
