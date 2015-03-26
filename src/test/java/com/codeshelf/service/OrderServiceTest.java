package com.codeshelf.service;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.service.OrderService.OrderDetailView;
import com.codeshelf.testframework.ServerTest;

public class OrderServiceTest extends ServerTest{
	
	@Test
	public void orderdetailNoLocation() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = setUpSimpleNoSlotFacility();
		Location exists = new LinkedList<Location>(facility.getSubLocationsInWorkingOrder()).getLast();
		String csvOrders = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,locationId"
				+ "\r\n1,USF314,COSTCO,11111,11111,NOLOCITEMID,Test Item 1,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0," + exists.getAliases().get(0).getAlias()
				+ "\r\n1,USF314,COSTCO,22222,22222,2,Test Item 2,3,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,44444,44444,5,Test Item 5,5,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,55555,55555,2,Test Item 2,7,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";
		importOrdersData(facility, csvOrders);
		this.getTenantPersistenceService().commitTransaction();
		
		this.getTenantPersistenceService().beginTransaction();
		OrderService orderService = new OrderService();
		Session session = this.getTenantPersistenceService().getSession();
		Collection<OrderDetailView> orderDetails = orderService.orderDetailsNoLocation(getTenantPersistenceService().getDefaultSchema(), session, facility.getPersistentId());
		Assert.assertEquals(3, orderDetails.size());
		int totalQuantity = 0;
		for (OrderDetailView orderDetailView : orderDetails) {
			totalQuantity += orderDetailView.getPlanQuantity();
			Assert.assertNotEquals("NOLOCITEMID", orderDetailView.getSku());
			
		}
		Assert.assertEquals(15, totalQuantity);
		this.getTenantPersistenceService().commitTransaction();
	}
}
