package com.codeshelf.security;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.OutboundOrderCsvBean;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.service.ScriptingService;
import com.codeshelf.testframework.ServerTest;

public class ScriptingServiceTest extends ServerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingServiceTest.class);
	
	@Test
	public void simpleScriptingEngineTest() {
		
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		String text = "def OrderImportNeedsScan(orderBean) { orderBean.description == 'abc' }";
		ExtensionPoint needsScanScript = new ExtensionPoint();
		needsScanScript.setParent(facility);
		needsScanScript.setExtension(ExtensionPointType.OrderImportNeedsScan);
		needsScanScript.setScript(text);
		needsScanScript.setDomainId(ExtensionPointType.OrderImportNeedsScan.toString());
		ExtensionPoint.staticGetDao().store(needsScanScript);
		commitTransaction();
		
		beginTransaction();
		try {
			// init service
			ScriptingService ss = ScriptingService.createInstance();
			assertEquals(false,ss.hasExtentionPoint(ExtensionPointType.OrderImportNeedsScan));
			ss.loadScripts(facility);
			assertEquals(true,ss.hasExtentionPoint(ExtensionPointType.OrderImportNeedsScan));
			// positive test
			OutboundOrderCsvBean bean1 = new OutboundOrderCsvBean();
			bean1.setDescription("abc");
			Object[] data1 = {bean1};
			Object result1 = ss.eval(facility, ExtensionPointType.OrderImportNeedsScan, data1);
			assertEquals(true,result1);
			// negative test
			OutboundOrderCsvBean bean2 = new OutboundOrderCsvBean();
			bean2.setDescription("def");
			Object[] data2 = {bean2};
			Object result2 = ss.eval(facility, ExtensionPointType.OrderImportNeedsScan, data2);
			assertEquals(false,result2);
		} 
		catch (ScriptException e) {
			LOGGER.error("", e);
			fail(e.toString());
		}
		commitTransaction();
	}


	@Test
	public void needsScanTest() throws IOException {
		
		Facility facility = setUpSimpleNoSlotFacility();
		
		ICsvOrderImporter importer = createOrderImporter();

		// define a rule to set needsscan for each picks, if not defined in import file
		beginTransaction();
		String text = "def OrderImportNeedsScan(orderBean) { orderBean.uom == 'each' }";
		ExtensionPoint needsScanScript = new ExtensionPoint();
		needsScanScript.setParent(facility);
		needsScanScript.setExtension(ExtensionPointType.OrderImportNeedsScan);
		needsScanScript.setScript(text);
		needsScanScript.setDomainId(ExtensionPointType.OrderImportNeedsScan.toString());
		ExtensionPoint.staticGetDao().store(needsScanScript);
		commitTransaction();
		
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";

		InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(csvString.getBytes()));

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		beginTransaction();
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		commitTransaction();
		
		beginTransaction();
		facility = facility.reload();

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "123");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 4, detailCount);

		// ensure "yes" is interpreted as true
		OrderDetail detail1 = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(detail1); 
		Assert.assertSame(true, detail1.getNeedsScan());

		// ensure "no" is interpreted as false
		OrderDetail detail2 = order.getOrderDetail("10706952-each");
		Assert.assertNotNull(detail2); 
		Assert.assertSame(false, detail2.getNeedsScan());

		// ensure undefined field is interpreted as true for each pick
		OrderDetail detail3 = order.getOrderDetail("10706972-each");
		Assert.assertNotNull(detail3); 
		Assert.assertSame(true, detail3.getNeedsScan());

		// ensure undefined field is interpreted as false for case pick
		OrderDetail detail4 = order.getOrderDetail("10706962-case");
		Assert.assertNotNull(detail4); 
		Assert.assertSame(false, detail4.getNeedsScan());

		commitTransaction();
	}


}
