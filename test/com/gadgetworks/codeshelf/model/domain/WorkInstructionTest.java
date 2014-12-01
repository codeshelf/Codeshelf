/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderGroupTest.java,v 1.4 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.generators.WorkInstructionGenerator;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author jeffw
 *
 */
public class WorkInstructionTest extends DomainTestABC {

	@Test
	public final void cookedDescriptionTest() {
		// The goal of cooking descriptions is to make the remaining description safe to be transmitted to the cart controller.
		// Our general test mechanism is to measure the string length before and after.
		
		// This was real order data from GoodEggs. The problem was non-ASCI characters
		// String inputStr = "Napa Valley Bistro - Jalape������������������������������������o Stuffed Olives";
		// But Git seems to give us different numbers of non-ASCII frequently. So, construct our own string.
		// This was not a hibernate failure, just the next git change in the reference string.  Fix once and for all.
		String startStr = "Napa Valley Bistro - Jalape";
		String endStr = "o Stuffed Olives";
		// Now add our enye on
		Character enye = '\ufffd';
		
		String inputStr = startStr + enye + endStr;
		Integer lengthBefore = inputStr.length();
		Assert.assertEquals((Integer) 44, lengthBefore);

		String referenceStr = "Napa Valley Bistro - Jalapeo Stuffed Olives";
		Integer lengthOfReference = referenceStr.length();
		Assert.assertEquals((Integer) 43, lengthOfReference);

		String cookedString = WorkInstruction.cookDescription(inputStr);
		Integer lengthAfterCooking = cookedString.length();
		Assert.assertEquals((Integer) 43, lengthAfterCooking);
		
		// This is real data from Accu-Logistics. The problem is the internal quote for inches. And perhaps trailing double quote
		// use \" to place a quote inside a java string
		inputStr = "22\" LLAMA LLAMA Doll\"\"";
		lengthBefore = inputStr.length();
		Assert.assertEquals((Integer) 22, lengthBefore);

		referenceStr = "22 LLAMA LLAMA Doll";
		lengthOfReference = referenceStr.length();
		Assert.assertEquals((Integer) 19, lengthOfReference);
		
		cookedString = WorkInstruction.cookDescription(inputStr);
		lengthAfterCooking = cookedString.length();
		Assert.assertEquals((Integer) 19, lengthAfterCooking);
		
		// What shall we keep? See WorkInstruction.cookDescription  
		// replaceAll("[^\\p{L}\\p{Z}\\-]",""); means keep numbers, letters, whitespace, and minus. We can modify to add or remove characters like plus, period, etc.
		// Below show that we lose single and double quotes, but keep +, -, comma, and period. In fact we are only keep these four extra chars
		inputStr = "22\" Llama-donkey + 8.5' tail, 50% assembled!";
		lengthBefore = inputStr.length();
		Assert.assertEquals((Integer) 44, lengthBefore);

		referenceStr = "22 Llama-donkey + 8.5 tail, 50 assembled";
		lengthOfReference = referenceStr.length();
		Assert.assertEquals((Integer) 40, lengthOfReference);
		
		cookedString = WorkInstruction.cookDescription(inputStr);
		lengthAfterCooking = cookedString.length();
		Assert.assertEquals((Integer) 40, lengthAfterCooking);

	}

	@SuppressWarnings("unused")
	@Test
	public final void addRemoveOrderGroupTest() {
		this.getPersistenceService().beginTenantTransaction();

		Organization organization = new Organization();
		organization.setOrganizationId("OWI.1");
		mOrganizationDao.store(organization);

		organization.createFacility("F1", "test", Point.getZeroPoint());
		Facility facility = organization.getFacility("F1");

		Aisle aisle1 = facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		mAisleDao.store(aisle1);

		Bay baya1b1 = aisle1.createBay( "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya1b1);
		Bay baya1b2 = aisle1.createBay( "B2", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya1b2);

		Aisle aisle2 = facility.createAisle("A2", Point.getZeroPoint(), Point.getZeroPoint());
		mAisleDao.store(aisle2);

		Bay baya2b1 = aisle2.createBay( "B1", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya2b1);
		Bay baya2b2 = aisle2.createBay("B2", Point.getZeroPoint(), Point.getZeroPoint());
		mBayDao.store(baya2b2);

		Container container = new Container();
		container.setDomainId("C1");
		facility.addContainer(container);
		container.setKind(facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND));
		container.setActive(true);
		container.setUpdated(new Timestamp(System.currentTimeMillis()));
		mContainerDao.store(container);

		UomMaster uomMaster = new UomMaster();
		uomMaster.setUomMasterId("EA");
		uomMaster.setParent(facility);
		mUomMasterDao.store(uomMaster);
		facility.addUomMaster(uomMaster);

		ItemMaster itemMaster = new ItemMaster();
		itemMaster.setItemId("AAA");
		itemMaster.setParent(facility);
		itemMaster.setStandardUom(uomMaster);
		itemMaster.setActive(true);
		itemMaster.setUpdated(new Timestamp(System.currentTimeMillis()));
		mItemMasterDao.store(itemMaster);

		OrderHeader order1 = new OrderHeader();
		order1.setParent(facility);
		order1.setOrderId("1");
		order1.setOrderType(OrderTypeEnum.OUTBOUND);
		order1.setOrderDate(new Timestamp(System.currentTimeMillis()));
		order1.setDueDate(new Timestamp(System.currentTimeMillis()));
		order1.setActive(true);
		order1.setUpdated(new Timestamp(System.currentTimeMillis()));
		mOrderHeaderDao.store(order1);

		OrderDetail orderDetail = new OrderDetail();
		orderDetail.setDomainId(itemMaster.getItemId());
		orderDetail.setItemMaster(itemMaster);
		orderDetail.setQuantities(5);
		orderDetail.setUomMaster(uomMaster);
		orderDetail.setStatus(OrderStatusEnum.CREATED);
		orderDetail.setActive(true);
		orderDetail.setUpdated(new Timestamp(System.currentTimeMillis()));
		order1.addOrderDetail(orderDetail);
		mOrderDetailDao.store(orderDetail);

		WorkInstruction wi = new WorkInstruction();
		facility.addWorkInstruction(wi);
		wi.setOrderDetail(orderDetail);
		wi.setCreated(new Timestamp(System.currentTimeMillis()));

		// Update the WI
		wi.setDomainId(Long.toString(System.currentTimeMillis()));
		wi.setType(WorkInstructionTypeEnum.PLAN);
		wi.setStatus(WorkInstructionStatusEnum.NEW);

		wi.setLocation(baya1b2);
		wi.setLocationId(baya1b2.getFullDomainId());
		wi.setItemMaster(itemMaster);
		wi.setDescription(itemMaster.getDescription());
		wi.setPickInstruction("pick instruction");
		wi.setDescription("description");

		wi.setPosAlongPath(0.0);
		wi.setContainer(container);
		wi.setPlanQuantity(5);
		wi.setPlanMinQuantity(5);
		wi.setPlanMaxQuantity(5);
		wi.setActualQuantity(0);
		wi.setAssigned(new Timestamp(System.currentTimeMillis()));
		mWorkInstructionDao.store(wi);

		// Check if the work instruction is contained by the facility, aisle and bay
		Assert.assertTrue(wi.isContainedByLocation(facility));
		Assert.assertTrue(wi.isContainedByLocation(aisle1));
		Assert.assertTrue(wi.isContainedByLocation(baya1b2));

		Assert.assertFalse(wi.isContainedByLocation(aisle2));
		Assert.assertFalse(wi.isContainedByLocation(null));
		
		// Test some of our toString() calls. Just looking for trouble with Lamboc annotations.
		// Recently changed CHE, container, and containerUse
		String theString = wi.toString();
		theString = orderDetail.toString();
		theString = order1.toString();
		theString = itemMaster.toString();
		theString = container.toString();
		theString = aisle1.toString();
		theString = baya1b1.toString();
		theString = facility.toString();
		// no containerUse in this test case
	
		this.getPersistenceService().endTenantTransaction();
	}

	@SuppressWarnings("unused")
	// @Test
	// edi/CrossBatchRunTest.java is a much better test.  This is failing now (after WI schema change for v5) for not finding the facility persistentId in the locations table in H2.
	public final void crossBatchOrderGroupTest() {
		Facility facility = createFacilityWithOutboundOrders("WITEST2");

		List<String> containerIdList = new ArrayList<String>();
		containerIdList.add("C4");
		containerIdList.add("C5");
		containerIdList.add("C6");
		containerIdList.add("C7");

		CodeshelfNetwork network = facility.getNetwork("WITEST");
		Che che = network.getChe("WITEST");

		Integer wiCount = facility.computeWorkInstructions(che, containerIdList);

		OrderHeader out1Group1 = facility.getOrderHeader("OUT1GROUP1");
		Assert.assertNotNull(out1Group1);
		Assert.assertTrue(wiExistsForOrder(out1Group1));
		
		OrderHeader out2Group1 = facility.getOrderHeader("OUT2GROUP1");
		Assert.assertNotNull(out2Group1);
		Assert.assertTrue(wiExistsForOrder(out2Group1));

		OrderHeader out3Group2 = facility.getOrderHeader("OUT3GROUP2");
		Assert.assertNotNull(out3Group2);
		Assert.assertFalse(wiExistsForOrder(out3Group2));

		OrderHeader out4NoGroup = facility.getOrderHeader("OUT4NOGROUP");
		Assert.assertNotNull(out4NoGroup);
		Assert.assertTrue(wiExistsForOrder(out4NoGroup));

	}
	
	@Test
	public void filterTest() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility(getTestName());
		WorkInstructionGenerator generator = new WorkInstructionGenerator();
		
		WorkInstruction wi = generator.generateWithNewStatus(facility);
		wi.getLocation().getDao().store(wi.getLocation());
		wi.getAssignedChe().getParent().getDao().store(wi.getAssignedChe().getParent());
		wi.getAssignedChe().getDao().store(wi.getAssignedChe());
		wi.getContainer().getDao().store(wi.getContainer());
		
		wi.getItemMaster().getStandardUom().getDao().store(wi.getItemMaster().getStandardUom());
		wi.getItemMaster().getDao().store(wi.getItemMaster());
		
		wi.getOrderDetail().getParent().getOrderGroup().getDao().store(wi.getOrderDetail().getParent().getOrderGroup());
		wi.getOrderDetail().getParent().getDao().store(wi.getOrderDetail().getParent());
		wi.getOrderDetail().getDao().store(wi.getOrderDetail());
		
		WorkInstruction.DAO.store(wi);
		this.getPersistenceService().endTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		Map<String, Object> params = ImmutableMap.<String, Object>of(
			"che_persistentId", wi.getAssignedChe().getPersistentId().toString(),
			"assigned", wi.getAssigned());
		List<WorkInstruction> foundInstructions = WorkInstruction.DAO.findByFilterAndClass("workInstructionByCheAndAssignedTime", params, WorkInstruction.class);
		Assert.assertEquals(ImmutableList.of(wi), foundInstructions);
		this.getPersistenceService().endTenantTransaction();
	}
	
	private final boolean wiExistsForOrder(final OrderHeader inOrderHeader) {
		boolean result = false;
		for (WorkInstruction wi : WorkInstruction.DAO.getAll()) {
			if (wi.getOrderId().equals(inOrderHeader.getOrderId())) {
				result = true;
			}
		}
		return result;
	}

}
