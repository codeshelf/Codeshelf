package com.codeshelf.security;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import javax.script.ScriptException;

import org.junit.Assert;
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
		String text = "def OrderImportBeanTransformation(orderBean) { orderBean.description == 'abc' }";
		ExtensionPoint needsScanScript = new ExtensionPoint();
		needsScanScript.setParent(facility);
		needsScanScript.setExtension(ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setScript(text);
		needsScanScript.setDomainId(ExtensionPointType.OrderImportBeanTransformation.toString());
		ExtensionPoint.staticGetDao().store(needsScanScript);
		commitTransaction();
		
		beginTransaction();
		try {
			// init service
			ScriptingService ss = ScriptingService.createInstance();
			assertEquals(false,ss.hasExtentionPoint(ExtensionPointType.OrderImportBeanTransformation));
			ss.loadScripts(facility);
			assertEquals(true,ss.hasExtentionPoint(ExtensionPointType.OrderImportBeanTransformation));
			// positive test
			OutboundOrderCsvBean bean1 = new OutboundOrderCsvBean();
			bean1.setDescription("abc");
			Object[] data1 = {bean1};
			Object result1 = ss.eval(facility, ExtensionPointType.OrderImportBeanTransformation, data1);
			assertEquals(true,result1);
			// negative test
			OutboundOrderCsvBean bean2 = new OutboundOrderCsvBean();
			bean2.setDescription("def");
			Object[] data2 = {bean2};
			Object result2 = ss.eval(facility, ExtensionPointType.OrderImportBeanTransformation, data2);
			assertEquals(false,result2);
		} 
		catch (ScriptException e) {
			LOGGER.error("", e);
			fail(e.toString());
		}
		commitTransaction();
	}

	@Test
	public void orderBeanTransformationTest() throws IOException {
		
		Facility facility = setUpSimpleNoSlotFacility();
		
		ICsvOrderImporter importer = createOrderImporter();

		// define a rule to set needsscan for each picks, if not defined in import file
		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderBean) { orderBean.needsScan = true; orderBean }";
		ExtensionPoint needsScanScript = new ExtensionPoint();
		needsScanScript.setParent(facility);
		needsScanScript.setExtension(ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setScript(text);
		needsScanScript.setDomainId(ExtensionPointType.OrderImportBeanTransformation.toString());
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
		Assert.assertSame(true, detail2.getNeedsScan());

		// ensure undefined field is interpreted as true for each pick
		OrderDetail detail3 = order.getOrderDetail("10706972-each");
		Assert.assertNotNull(detail3); 
		Assert.assertSame(true, detail3.getNeedsScan());

		// ensure undefined field is interpreted as false for case pick
		OrderDetail detail4 = order.getOrderDetail("10706962-case");
		Assert.assertNotNull(detail4); 
		Assert.assertSame(true, detail4.getNeedsScan());

		commitTransaction();
	}	
		
	@Test
	public void orderBeanTransformationTest2() throws IOException {
		
		Facility facility = setUpSimpleNoSlotFacility();
		
		ICsvOrderImporter importer = createOrderImporter();

		// define a rule to set needsscan for a specific customer
		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderBean) { if (orderBean.customerId=='FOOBAR') orderBean.needsScan = true; orderBean }";
		ExtensionPoint needsScanScript = new ExtensionPoint();
		needsScanScript.setParent(facility);
		needsScanScript.setExtension(ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setScript(text);
		needsScanScript.setDomainId(ExtensionPointType.OrderImportBeanTransformation.toString());
		ExtensionPoint.staticGetDao().store(needsScanScript);
		commitTransaction();

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,FOOBAR,223,223,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,FOOBAR,223,223,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,FOOBAR,223,223,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,224,224,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";

		InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(csvString.getBytes()));

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		beginTransaction();
		importer.importOrdersFromCsvStream(reader, facility, ediProcessTime);
		commitTransaction();
		
		beginTransaction();
		facility = facility.reload();

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "223");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "224");
		Assert.assertNotNull(order2);
		Integer detailCount2 = order2.getOrderDetails().size();
		Assert.assertEquals((Integer) 1, detailCount2);

		// first three items for one customer should require scan
		OrderDetail detail1 = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(detail1); 
		Assert.assertSame(true, detail1.getNeedsScan());

		OrderDetail detail2 = order.getOrderDetail("10706952-each");
		Assert.assertNotNull(detail2); 
		Assert.assertSame(true, detail2.getNeedsScan());

		OrderDetail detail4 = order.getOrderDetail("10706962-case");
		Assert.assertNotNull(detail4); 
		Assert.assertSame(true, detail4.getNeedsScan());

		// fourth item is for a different customer and should not require a scan
		OrderDetail detail3 = order2.getOrderDetail("10706972-each");
		Assert.assertNotNull(detail3); 
		Assert.assertSame(false, detail3.getNeedsScan());

		commitTransaction();
	}
	
	@Test
	public void orderHeaderTransformationTest() throws IOException {
		
		Facility facility = setUpSimpleNoSlotFacility();
		
		ICsvOrderImporter importer = createOrderImporter();

		// define a rule to set needsscan for each picks, if not defined in import file
		beginTransaction();
		String text = "def OrderImportHeaderTransformation(orderHeader) { orderHeader.replaceAll('\\\\^', ',') }";
		ExtensionPoint extp = new ExtensionPoint();
		extp.setParent(facility);
		extp.setExtension(ExtensionPointType.OrderImportHeaderTransformation);
		extp.setScript(text);
		extp.setDomainId(ExtensionPointType.OrderImportHeaderTransformation.toString());
		ExtensionPoint.staticGetDao().store(extp);
		commitTransaction();

		String csvString = "orderGroupId^shipmentId^customerId^preAssignedContainerId^orderId^itemId^description^quantity^uom^orderDate^dueDate^workSequence^needsScan"
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
		Assert.assertSame(false, detail3.getNeedsScan());

		// ensure undefined field is interpreted as false for case pick
		OrderDetail detail4 = order.getOrderDetail("10706962-case");
		Assert.assertNotNull(detail4); 
		Assert.assertSame(false, detail4.getNeedsScan());

		commitTransaction();
	}
	
	@Test
	public void orderLineTransformationTest() throws IOException {
		
		Facility facility = setUpSimpleNoSlotFacility();
		
		ICsvOrderImporter importer = createOrderImporter();

		// define a rule to set needsscan for each picks, if not defined in import file
		beginTransaction();
		String text = "def OrderImportLineTransformation(orderLine) { orderLine.replaceAll('~', ',') }";
		ExtensionPoint extp = new ExtensionPoint();
		extp.setParent(facility);
		extp.setExtension(ExtensionPointType.OrderImportLineTransformation);
		extp.setScript(text);
		extp.setDomainId(ExtensionPointType.OrderImportLineTransformation.toString());
		ExtensionPoint.staticGetDao().store(extp);
		commitTransaction();

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1~USF314~COSTCO~123~123~10700589~Napa Valley Bistro - Jalapeo Stuffed Olives~1~each~2012-09-26 11:31:01~2012-09-26 11:31:03~0~yes"
				+ "\r\n1~USF314~COSTCO~123~123~10706952~Italian Homemade Style Basil Pesto~1~each~2012-09-26 11:31:01,2012-09-26 11:31:03~0~no";

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
		Assert.assertEquals((Integer) 2, detailCount);

		// ensure "yes" is interpreted as true
		OrderDetail detail1 = order.getOrderDetail("10700589-each");
		Assert.assertNotNull(detail1); 
		Assert.assertSame(true, detail1.getNeedsScan());

		// ensure "no" is interpreted as false
		OrderDetail detail2 = order.getOrderDetail("10706952-each");
		Assert.assertNotNull(detail2); 
		Assert.assertSame(false, detail2.getNeedsScan());

		commitTransaction();
	}	

}
