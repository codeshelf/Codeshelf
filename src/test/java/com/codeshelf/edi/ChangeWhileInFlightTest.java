package com.codeshelf.edi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;
import com.google.common.base.Strings;

public class ChangeWhileInFlightTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ChangeWhileInFlightTest.class);

	@Test
	public final void importRunReimport() throws IOException {
		beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		commitTransaction();

		this.startSiteController();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We are going to put cases in A3 and each in A2. Also showing variation in EA/each, etc.
		// 402 and 403 are in A2, the each aisle. 502 and 503 are in A3, the case aisle, on a separate path.
		String csvInventory = "itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n" //
				+ "1123,D402,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n" //
				+ "1493,D502,PARK RANGER Doll,2,each,6/25/14 12:00,66\r\n" //
				+ "1522,D503,SJJ BPP,1,each,6/25/14 12:00,3\r\n" //
				+ "1522,D403,SJJ BPP,10,each,6/25/14 12:00,3\r\n";//
		importInventoryData(facility, csvInventory);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		Location locationD402 = facility.findSubLocationById("D402");

		Item item1123Loc402EA = locationD402.getStoredItemFromMasterIdAndUom("1123", "EA");
		Assert.assertNotNull(item1123Loc402EA);

		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		// Outbound order. No group. Using 5 digit order number and preassigned container number.
		// Item 1123 exists in case and each.
		// Item 1493 exists in case only. Order for each should short.
		// Item 1522 exists in case and each.

		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
//				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		importOrdersData(facility, csvOrders);
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		// We should have one order with 2 details.
		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		//OrderHeader order = facility.getOrderHeader("12345");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 2, detailCount);

		List<String> itemLocations = new ArrayList<String>();
		for (OrderDetail detail : order.getOrderDetails()) {
			String itemLocationString = detail.getItemLocations();
			if (!Strings.isNullOrEmpty(itemLocationString)) {
				itemLocations.add(itemLocationString);
			} else {
				LOGGER.debug(detail.getItemMasterId() + " " + detail.getUomMasterId() + " has no location");
			}
		}
		Assert.assertEquals(2, itemLocations.size());
		// Turn off housekeeping work instructions for next test so as to not confuse the counts
		commitTransaction();

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.turnOffHK(facility);
		commitTransaction();

		// Set up a cart for order 12345, which will generate work instructions
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker #1");
		picker.setupContainer("12345", "1");
		picker.startAndSkipReview("D403", 8000, 5000);

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);
		propertyService.restoreHKDefaults(facility);
		commitTransaction();

		Assert.assertEquals(picker.getLastSentPositionControllerDisplayValue((byte) 1).byteValue(), 1);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayDutyCycle((byte) 1), PosControllerInstr.BRIGHT_DUTYCYCLE);
		Assert.assertEquals(picker.getLastSentPositionControllerDisplayFreq((byte) 1), PosControllerInstr.SOLID_FREQ);

		Assert.assertEquals(1, picker.countActiveJobs());
		WorkInstruction currentWI = picker.nextActiveWi();
		Assert.assertEquals("SJJ BPP", currentWI.getDescription());
		Assert.assertEquals("1522", currentWI.getItemId());

		// pick first item
		picker.pick(1, 1);
		Assert.assertEquals(1, picker.countActiveJobs());
		currentWI = picker.nextActiveWi();
		Assert.assertEquals("1123", currentWI.getItemId());
		
		// re-import order with different order lines
		beginTransaction();
		csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,12345,12345,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
 				+ "\r\n1,USF314,COSTCO,12345,12345,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
//				+ "\r\n1,USF314,COSTCO,12345,12345,1522,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0";
		
		facility = Facility.staticGetDao().reload(facility);
		importOrdersData(facility, csvOrders);
		commitTransaction();

		// pick second item
		picker.pick(1, 1);
		Assert.assertEquals(0, picker.countActiveJobs());

		picker.waitForCheState(picker.getCompleteState(), 1000);
		picker.logout();
		
		// check order state
		beginTransaction();
		ThreadUtils.sleep(2000); // wait for changes to settle
		facility = Facility.staticGetDao().reload(facility);
		order = OrderHeader.staticGetDao().findByDomainId(facility, "12345");
		OrderStatusEnum status = order.getStatus();
		Assert.assertEquals(OrderStatusEnum.INPROGRESS, status);
		commitTransaction();		
	}

}
