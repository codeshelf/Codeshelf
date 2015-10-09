package com.codeshelf.api.resources;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.api.resources.subresources.ImportResource;
import com.codeshelf.behavior.OrderBehavior;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.event.EventProducer;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.ServerTest;
import com.sun.jersey.core.header.FormDataContentDisposition;

public class OrdersResourceTest extends ServerTest {

	@Test
	public void testDeleteAllOrders() throws UnsupportedEncodingException {
		beginTransaction();
		Facility facility = getFacility();
		commitTransaction();
		
		beginTransaction();
		String orders  = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
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
						+ "\r\n1,USF314,COSTCO,120,931,10706962,Sun Ripened Dried Tomato Pesto 24oz,1,each,2012-09-26 11:31:01,2012-09-26 11:31:02,0";;
		ImportResource resource = new ImportResource(null, null, null, new OutboundOrderPrefetchCsvImporter(Mockito.mock(EventProducer.class)) , null, null);
		resource.setFacility(facility);
		resource.uploadOrders(new ByteArrayInputStream(orders.getBytes("UTF-8")), Mockito.mock(FormDataContentDisposition.class));
		commitTransaction();
		
		beginTransaction();
		OrdersResource ordersResource = new OrdersResource(new OrderBehavior());
		ordersResource.setFacility(facility);
		Response response = ordersResource.deleteOrders();
		commitTransaction();
		Assert.assertEquals(200, response.getStatus());
	}
	
}
