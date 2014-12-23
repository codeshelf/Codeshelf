package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.WiFactory;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.service.ProductivityCheSummaryList;
import com.gadgetworks.codeshelf.service.ProductivitySummaryList;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.service.ProductivityCheSummaryList.RunSummary;
import com.gadgetworks.codeshelf.service.ProductivitySummaryList.GroupSummary;
import com.gadgetworks.flyweight.command.NetGuid;

public class ProductivityReportingTest extends DomainTestABC {
	@Test
	public void testProductivitySummary() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createFacilityWithOutboundOrders("PRTEST1.O1");
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
			Assert.assertTrue("undefined".equals(groupName) || "GROUP1".equals(groupName) || "GROUP2".equals(groupName));
		}
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testGetCheSummaryNoRuns() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createFacilityWithOutboundOrders("PRTEST2.O1");
		UUID facilityId = facility.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		//Get all summaries
		ProductivityCheSummaryList cheSummaries = WorkService.getCheByGroupSummary(facilityId);
		Assert.assertNotNull(cheSummaries);
		Assert.assertEquals(cheSummaries.getGroups().size(), 0);
		this.getPersistenceService().commitTenantTransaction();
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
		List<HashMap<UUID, HashMap<String, RunSummary>>> groups = new ArrayList<>(cheSummaries.getGroups().values());
		Assert.assertEquals(groups.size(), 1);
		//Get summaries for the only che
		HashMap<UUID, HashMap<String, RunSummary>> ches = groups.get(0);
		Assert.assertEquals(ches.size(), 1);
		//Get runs for the che
		HashMap<String, RunSummary> cheRuns = new ArrayList<>(ches.values()).get(0);
		Assert.assertEquals(cheRuns.size(), 1);
		RunSummary run = new ArrayList<>(cheRuns.values()).get(0);
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
		List<HashMap<UUID, HashMap<String, RunSummary>>> groups = new ArrayList<>(cheSummaries.getGroups().values());
		Assert.assertEquals(groups.size(), 1);
		//Get summaries for the only che
		HashMap<UUID, HashMap<String, RunSummary>> ches = groups.get(0);
		Assert.assertEquals(ches.size(), 1);
		//Get runs for the che
		HashMap<String, RunSummary> cheRuns = new ArrayList<>(ches.values()).get(0);
		Assert.assertEquals(cheRuns.size(), 2);
		//Verify runs
		RunSummary run1 = cheRuns.get("2014-12-22 15:46:00.0");
		RunSummary run2 = cheRuns.get("2014-12-23 11:40:20.0");
		testRunSummary(run1, 1, 0, 0, 0, 1, 0);
		testRunSummary(run2, 0, 2, 1, 0, 0, 0);
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
		HashMap<String, HashMap<UUID, HashMap<String, RunSummary>>> groups = cheSummaries.getGroups();
		Assert.assertEquals(groups.size(), 2);
		Iterator<String> groupNames = groups.keySet().iterator();
		while (groupNames.hasNext()){
			String groupName = groupNames.next();
			List<HashMap<String, RunSummary>> ches = new ArrayList<>(groups.get(groupName).values());
			Assert.assertEquals(ches.size(), 1);
			List<RunSummary> cheRuns = new ArrayList<>(ches.get(0).values());
			Assert.assertEquals(cheRuns.size(), 1);
			if ("undefined".equals(groupName)){
				testRunSummary(cheRuns.get(0), 1, 0, 0, 0, 1, 0);
			} else {
				testRunSummary(cheRuns.get(0), 0, 2, 1, 0, 0, 0);
			}
			
		}
		this.getPersistenceService().commitTenantTransaction();
	}

	private void testRunSummary(RunSummary s, int invalid, int New, int inprogress, int Short, int complete, int revert){
		Assert.assertNotNull(s);
		Assert.assertTrue(s.getInvalid() == invalid && s.getNew() == New && s.getInprogress() == inprogress && s.getShort() == Short && s.getComplete() == complete && s.getRevert() == revert);
	}
	
	private Facility createFacilityWithOneRun(String orgId){
		//12/22/14 6:46 PM = 1419291960000
		Facility facility = createDefaultFacility(orgId);
		CodeshelfNetwork network = facility.createNetwork("WITEST"); 
		Che che = network.createChe("WITEST", new NetGuid("0x00000001"));

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
	
	private Facility createFacilityWithTwoRuns(String orgId){
		//12/22/14 6:46 PM = 1419291960000
		//12/23/14 7:40 PM = 1419363620000
		Facility facility = createDefaultFacility(orgId);
		CodeshelfNetwork network = facility.createNetwork("WITEST"); 
		Che che = network.createChe("WITEST", new NetGuid("0x00000001"));

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

	private Facility createFacilityWithTwoGroups(String orgId){
		Facility facility = createDefaultFacility(orgId);
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
