package com.codeshelf.api.resources;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.resources.subresources.ImportResource;
import com.codeshelf.behavior.OrderBehavior;
import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.event.EventProducer;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.sun.jersey.core.header.FormDataContentDisposition;

public class OrdersResourceTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OrdersResourceTest.class);

	@Test
	public void testDeleteAllOrders() throws UnsupportedEncodingException {
		beginTransaction();
		Facility facility = getFacility();
		commitTransaction();

		beginTransaction();
		String orders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,120,931,10706962,Sun Ripened Dried Tomato Pesto 24oz,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		ImportResource resource = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource.setFacility(facility);
		resource.uploadOrders(new ByteArrayInputStream(orders.getBytes("UTF-8")), Mockito.mock(FormDataContentDisposition.class), false);
		commitTransaction();

		beginTransaction();
		OrdersResource ordersResource = new OrdersResource(new OrderBehavior(), null);
		ordersResource.setFacility(facility);
		Response response = ordersResource.deleteOrders();
		commitTransaction();
		Assert.assertEquals(200, response.getStatus());
	}

	@Test
	public void testDoubleImportOrders() throws UnsupportedEncodingException {
		String orders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		beginTransaction();
		Facility facility = getFacility();
		commitTransaction();

		LOGGER.info("1: import the orders");
		beginTransaction();
		ImportResource resource = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource.setFacility(facility);
		Response response = resource.uploadOrders(new ByteArrayInputStream(orders.getBytes("UTF-8")),
			Mockito.mock(FormDataContentDisposition.class),
			false);
		commitTransaction();
		Assert.assertEquals(200, response.getStatus());

		LOGGER.info("1b: check order status");
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh1 = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		OrderStatusEnum status1 = oh1.getStatus();
		Assert.assertEquals(OrderStatusEnum.RELEASED, status1);
		commitTransaction();

		LOGGER.info("2: Import again. This looks like different file with exact same content. Should act the same as if the same file");
		// Notice that it does not overwrite the completed order.
		beginTransaction();
		ImportResource resource2 = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource2.setFacility(facility);
		Response response2 = resource2.uploadOrders(new ByteArrayInputStream(orders.getBytes("UTF-8")),
			Mockito.mock(FormDataContentDisposition.class),
			false);
		commitTransaction();
		Assert.assertEquals(200, response2.getStatus());

		LOGGER.info("2b: check order status");
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh2 = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		OrderStatusEnum status2 = oh2.getStatus();
		Assert.assertEquals(OrderStatusEnum.RELEASED, status2);
		commitTransaction();

	}

	@Test
	public void testDoubleImportOrdersAfterChange() throws UnsupportedEncodingException {
		String orders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,locationId,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocA,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocB,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocC,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocD,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,LocE,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,LocF,0";

		beginTransaction();
		Facility facility = getFacility();
		PropertyBehavior.turnOffHK(facility);
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, "WorkSequence");
		commitTransaction();

		LOGGER.info("1: import the orders");
		beginTransaction();
		ImportResource resource = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource.setFacility(facility);
		Response response = resource.uploadOrders(new ByteArrayInputStream(orders.getBytes("UTF-8")),
			Mockito.mock(FormDataContentDisposition.class),
			false);
		commitTransaction();
		Assert.assertEquals(200, response.getStatus());

		LOGGER.info("2: Complete order");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker1");
		picker.setupContainer("123", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.logCheDisplay();
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		
		waitForOrderStatus(facility, "123", OrderStatusEnum.COMPLETE, true, 2000);

		LOGGER.info("3: Import again. This looks like different file with exact same content. Should act the same as if the same file");
		// Notice that it does not overwrite the completed order.
		beginTransaction();
		ImportResource resource2 = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource2.setFacility(facility);
		Response response2 = resource2.uploadOrders(new ByteArrayInputStream(orders.getBytes("UTF-8")),
			Mockito.mock(FormDataContentDisposition.class),
			false);
		commitTransaction();
		Assert.assertEquals(200, response2.getStatus());

		beginTransaction();
		facility = facility.reload();
		OrderHeader oh3 = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		OrderStatusEnum status3 = oh3.getStatus();
		LOGGER.info("same bug here! changed to INPROGRESS. Why? Should still be complete");
		Assert.assertEquals(OrderStatusEnum.COMPLETE, status3);
		commitTransaction();

	}

	@Test
	public void testImportAPIAfterChange() throws UnsupportedEncodingException {
		// This imports once, then a change, then imports with the new API that deletes and imports.
		// We start with another import with other order just to prove the new API only deletes orders represented in the file.
		String orders1 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				+ "\r\n1,USF314,COSTCO,789,789,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,789,789,10706961,Sun Ripened Dried Tomato Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0"
				+ "\r\n1,USF314,COSTCO,120,931,10706962,Sun Ripened Dried Tomato Pesto 24oz,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";

		String orders2 = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,locationId,workSequence"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocA,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocB,0"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocC,0"
				+ "\r\n1,USF314,COSTCO,123,123,10100250,Organic Fire-Roasted Red Bell Peppers,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,LocD,0"
				+ "\r\n1,USF314,COSTCO,456,456,10711111,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,LocE,0"
				+ "\r\n1,USF314,COSTCO,456,456,10722222,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,LocF,0";

		beginTransaction();
		Facility facility = getFacility();
		PropertyBehavior.turnOffHK(facility);
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, "WorkSequence");
		commitTransaction();

		LOGGER.info("1: import the first orders file");
		beginTransaction();
		ImportResource resource = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource.setFacility(facility);
		Response response = resource.uploadOrders(new ByteArrayInputStream(orders1.getBytes("UTF-8")),
			Mockito.mock(FormDataContentDisposition.class),
			false);
		commitTransaction();
		Assert.assertEquals(200, response.getStatus());

		LOGGER.info("1b: see that order 789 is there");
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh789 = OrderHeader.staticGetDao().findByDomainId(facility, "789");
		OrderStatusEnum status789 = oh789.getStatus();
		Assert.assertEquals(OrderStatusEnum.RELEASED, status789);
		commitTransaction();

		LOGGER.info("2: import the second orders file. This does not have 789 in it");
		beginTransaction();
		facility = facility.reload();
		ImportResource resource2 = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource2.setFacility(facility);
		Response response2 = resource2.uploadOrders(new ByteArrayInputStream(orders2.getBytes("UTF-8")),
			Mockito.mock(FormDataContentDisposition.class),
			false);
		commitTransaction();
		Assert.assertEquals(200, response2.getStatus());

		LOGGER.info("2b: see that order 789 is there and unchanged. See that 123 imported");
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh789b = OrderHeader.staticGetDao().findByDomainId(facility, "789");
		OrderHeader oh123 = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		OrderStatusEnum status789b = oh789b.getStatus();
		OrderStatusEnum status123 = oh123.getStatus();
		Assert.assertEquals(OrderStatusEnum.RELEASED, status789b);
		Assert.assertEquals(OrderStatusEnum.RELEASED, status123);
		commitTransaction();

		LOGGER.info("3: Complete order 123");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Picker1");
		picker.setupContainer("123", "1");
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		picker.logCheDisplay();
		picker.scanCommand("START");
		picker.waitForCheState(CheStateEnum.DO_PICK, 4000);
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		picker.pickItemAuto();
		picker.waitInSameState(CheStateEnum.DO_PICK, 500);
		picker.pickItemAuto();
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 4000);
		
		waitForOrderStatus(facility, "123", OrderStatusEnum.COMPLETE, true, 2000);

		LOGGER.info("4: Import using the other API");
		// TODO change 
		beginTransaction();
		ImportResource resource3 = new ImportResource(null,
			null,
			null,
			new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)),
			null,
			null);
		resource3.setFacility(facility);
		Response response3 = resource3.uploadOrders(new ByteArrayInputStream(orders2.getBytes("UTF-8")),
			Mockito.mock(FormDataContentDisposition.class),
			false);
		commitTransaction();
		Assert.assertEquals(200, response3.getStatus());

		LOGGER.info("4b: see that order 789 is there and unchanged. See that 123 imported");
		beginTransaction();
		facility = facility.reload();
		OrderHeader oh789d = OrderHeader.staticGetDao().findByDomainId(facility, "789");
		OrderHeader oh123d = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		OrderStatusEnum status789d = oh789d.getStatus();
		OrderStatusEnum status123d = oh123d.getStatus();
		Assert.assertEquals(OrderStatusEnum.RELEASED, status789d);
		Assert.assertEquals(OrderStatusEnum.COMPLETE, status123d);
		commitTransaction();

	}

}
