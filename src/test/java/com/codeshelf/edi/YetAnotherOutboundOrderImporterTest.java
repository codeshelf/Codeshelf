/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2015, All rights reserved
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Point;
import com.codeshelf.testframework.ServerTest;

/**
 * Time to start another test, since the other order import test has almost 2000 lines now...
 */
public class YetAnotherOutboundOrderImporterTest extends ServerTest {
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(YetAnotherOutboundOrderImporterTest.class);

	// the full set of fields known to the bean  (in the order of the bean, just for easier verification) is
	// orderGroupId,orderId,orderDetailID,itemId,description,quantity,minQuantity,maxQuantity,uom,orderDate,dueDate,destinationId,pickStrategy,preAssignedContainerId,shipmentId,customerId,workSequence
	// of these: orderId,itemId,description,quantity,uom are not nullable

	private ICsvOrderImporter	importer;
	private UUID				facilityId;

	@Before
	public void doBefore() {
		super.doBefore();

		this.getTenantPersistenceService().beginTransaction();

		importer = createOrderImporter();
		facilityId = getTestFacility("O-" + getTestName(), "F-" + getTestName()).getPersistentId();

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public final void testNeedsScanField() throws IOException {
		beginTransaction();
		Facility facility = Facility.staticGetDao().findByPersistentId(this.facilityId);

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalape������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������������o Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";

		InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(csvString.getBytes()));

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		commitTransaction();
		
		beginTransaction();
		facility = facility.reload();

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		OrderDetail detail1 = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(detail1); 
		Assert.assertSame(true, detail1.getNeedsScan());

		OrderDetail detail2 = order.getOrderDetail("10706952-each");
		Assert.assertNotNull(detail2); 
		Assert.assertSame(false, detail2.getNeedsScan());

		OrderDetail detail3 = order.getOrderDetail("10706962-each");
		Assert.assertNotNull(detail3); 
		Assert.assertSame(false, detail3.getNeedsScan());

		commitTransaction();
	}

	//******************** private helpers ***********************

	private Facility getTestFacility(String orgId, String facilityId) {
		Facility facility = Facility.createFacility(facilityId, "TEST", Point.getZeroPoint());
		return facility;
	}
}
