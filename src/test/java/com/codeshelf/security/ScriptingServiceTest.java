package com.codeshelf.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.OutboundOrderCsvBean;
import com.codeshelf.metrics.DataQuantityHealthCheckParameters;
import com.codeshelf.model.DataPurgeParameters;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.service.ExtensionPointService;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.service.PropertyService;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.FieldError;

public class ScriptingServiceTest extends ServerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingServiceTest.class);

	//-XX:PermSize=30M -XX:MaxPermSize=60M 
	@Test
	@Ignore
	public void permgentest() {
		Facility facility = setUpSimpleNoSlotFacility();
		for (int i = 0; i < 100000; i++) {
			evalScripts(facility);
			if (i % 100 == 0) {
				System.out.println("test " + i);
			}
		}
	}
	
		
	@Test
	public void evalScriptTest() {
		Facility facility = setUpSimpleNoSlotFacility();
		evalScripts(facility);
	}
	
	private void evalScripts(Facility facility) {
		
		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderBean) { orderBean.description == 'abc' }";
		createExtension(facility, ExtensionPointType.OrderImportBeanTransformation, text);
		commitTransaction();

		beginTransaction();
		try {
			// init service
			ExtensionPointService ss = ExtensionPointService.createInstance(facility);
			assertEquals(true,ss.hasExtensionPoint(ExtensionPointType.OrderImportBeanTransformation));
			// positive test
			OutboundOrderCsvBean bean1 = new OutboundOrderCsvBean();
			bean1.setDescription("abc");
			assertEval(ss, bean1, true);
			// negative test
			OutboundOrderCsvBean bean2 = new OutboundOrderCsvBean();
			bean2.setDescription("def");
			assertEval(ss, bean2, false);
		}
		catch (ScriptException e) {
			LOGGER.error("", e);
			fail(e.toString());
		}
		commitTransaction();

	}
	
	@Test
	public void parameterBeansTest() {

		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		String text = "def ParameterSetDataPurge(bean) { bean.orderBatch = '10'; return bean; }";
		createExtension(facility, ExtensionPointType.ParameterSetDataPurge, text);
		commitTransaction();

		beginTransaction();
		try {
			// init service
			ExtensionPointService ss = ExtensionPointService.createInstance(facility);
			
			// positive test. groovy exists
			assertEquals(true,ss.hasExtensionPoint(ExtensionPointType.ParameterSetDataPurge));
			DataPurgeParameters purgeParams = ss.getDataPurgeParameters();
			Assert.assertEquals(10, purgeParams.getOrderBatchValue());
			
			// Do the negative test. No groovy extension exists
			assertEquals(false,ss.hasExtensionPoint(ExtensionPointType.ParameterSetDataQuantityHealthCheck));
			// but we still get a good bean with useful defaults
			DataQuantityHealthCheckParameters dataQuanityParams = ss.getDataQuantityHealthCheckParameters();
			// Warning. This will fail when the default changes. Just change this test
			Assert.assertEquals(40000, dataQuanityParams.getMaxContainerUseValue()); 
		
		}
		catch (ScriptException e) {
			LOGGER.error("", e);
			fail(e.toString());
		}
		commitTransaction();
	}


	@Test
	public void inactiveScriptsStopRunning() throws ScriptException {
		Facility facility = setUpSimpleNoSlotFacility();

		//test positive case when active
		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderBean) { orderBean.description == 'abc' }";
		ExtensionPoint needsScanScript = createExtension(facility, ExtensionPointType.OrderImportBeanTransformation, text);
		UUID persistentId = needsScanScript.getPersistentId();
		commitTransaction();

		beginTransaction();
		ExtensionPointService ss = ExtensionPointService.createInstance(facility);
		OutboundOrderCsvBean bean1 = new OutboundOrderCsvBean();
		bean1.setDescription("abc");
		assertEval(ss, bean1, true);
		commitTransaction();

		//test case when INACTIVE
		beginTransaction();
		ExtensionPoint extpt = ExtensionPoint.staticGetDao().findByPersistentId(persistentId);
		extpt.setActive(false);
		ExtensionPoint.staticGetDao().store(extpt);
		commitTransaction();

		beginTransaction();
		facility.reload();
		ExtensionPointService newSS = ExtensionPointService.createInstance(facility);

		assertFalse(newSS.hasExtensionPoint(ExtensionPointType.OrderImportBeanTransformation));

		OutboundOrderCsvBean sameBean = new OutboundOrderCsvBean();
		sameBean.setDescription("abc");

		try {
			newSS.eval(ExtensionPointType.OrderImportBeanTransformation, new Object[]{sameBean});
			fail("Should have thrown ScriptException");
		} catch (ScriptException e) {

		}
		commitTransaction();
	}

	private void assertEval(ExtensionPointService ss, OutboundOrderCsvBean bean, boolean expected) throws ScriptException {
		Object[] data1 = {bean};
		Object result1 = ss.eval(ExtensionPointType.OrderImportBeanTransformation, data1);
		assertEquals(expected,result1);

	}

	@Test
	public void orderBeanTransformationTest() throws IOException {

		Facility facility = setUpSimpleNoSlotFacility();

		// define a rule to set needsscan for each picks, if not defined in import file
		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderBean) { orderBean.needsScan = true; orderBean }";
		createExtension(facility, ExtensionPointType.OrderImportBeanTransformation, text);
		commitTransaction();

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";
		beginTransaction();
		importOrdersData(facility, csvString);
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
	public void orderBeanAccuCustomerIdBasedNeedsScanTest() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		// define a rule to set needsscan for a specific customer

		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderLine) {" +
				"\r\n       customerNeedsScan = ['LUNERA']" +
				"\r\n       if (customerNeedsScan.contains(orderLine.customerId)) {" +
				"\r\n            orderLine.needsScan = true;" +
				"\r\n       } else  {" +
				"\r\n            orderLine.needsScan = false;" +
				"\r\n	    }" +
				"\r\n   	return orderLine;" +
				"\r\n  }";
		createExtension(facility, ExtensionPointType.OrderImportBeanTransformation, text);
		commitTransaction();

		
		beginTransaction();
		DomainObjectProperty scanPickProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.SCANPICK);
		Assert.assertEquals("Disabled", scanPickProperty.getValue());
		commitTransaction();

		String csvString = "orderId,orderDetailId, orderDate, dueDate,itemId,description,quantity,uom,preAssignedContainerId, gtin, customerId"
				+"\r\n\"268887\",268887.1,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00010\",\"HN-H-G24D-26W-4000-G2\",\"4\",\"EA\",268887,\"718421828746\",\"LUNERA\""
				+"\r\n\"268887\",268887.2,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00052\",\"HN-H-G24Q-26W-4000-G3\",\"4\",\"EA\",268887,\"871699969172\",\"LUNERA\""
				+"\r\n\"268887\",268887.3,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00056\",\"HN-V-G24Q-26W-4000-G3\",\"4\",\"EA\",268887,\"871699969554\",\"LUNERA\""
				+"\r\n\"268892\",268892.1,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00049\",\"HN-H-G24Q-26W-2700-G3\",\"23\",\"EA\",268892,\"871699968878\",\"WORLD\""
				+"\r\n\"268892\",268892.2,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00051\",\"HN-H-G24Q-26W-3500-G3\",\"62\",\"EA\",268892,\"871699969004\",\"WORLD\"";
		beginTransaction();
		importOrdersData(facility, csvString);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "268887");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "268892");
		Assert.assertNotNull(order2);
		Integer detailCount2 = order2.getOrderDetails().size();
		Assert.assertEquals((Integer) 2, detailCount2);

		// first three items for one customer should require scan
		OrderDetail detail1 = order.getOrderDetail("268887.1");
		Assert.assertNotNull(detail1);
		Assert.assertSame(true, detail1.getNeedsScan());

		OrderDetail detail2 = order2.getOrderDetail("268892.1");
		Assert.assertNotNull(detail2);
		Assert.assertSame(false, detail2.getNeedsScan());
		commitTransaction();
	}

	@Test
	public void orderBeanOverrideUPCtoFalse() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		// define a rule to set needsscan for a specific customer

		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderLine) {" +
				"\r\n       customerNeedsScan = ['LUNERA']" +
				"\r\n       if (customerNeedsScan.contains(orderLine.customerId)) {" +
				"\r\n            orderLine.needsScan = true;" +
				"\r\n       } else  {" +
				"\r\n            orderLine.needsScan = false;" +
				"\r\n	    }" +
				"\r\n   	return orderLine;" +
				"\r\n  }";
		createExtension(facility, ExtensionPointType.OrderImportBeanTransformation, text);
		
		PropertyService.getInstance().changePropertyValue(facility, DomainObjectProperty.SCANPICK, "UPC");
		commitTransaction();

		
		beginTransaction();
		
		DomainObjectProperty scanPickProperty = PropertyService.getInstance().getProperty(facility, DomainObjectProperty.SCANPICK);
		Assert.assertEquals("UPC", scanPickProperty.getValue());
		commitTransaction();

		String csvString = "orderId,orderDetailId, orderDate, dueDate,itemId,description,quantity,uom,preAssignedContainerId, gtin, customerId"
				+"\r\n\"268887\",268887.1,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00010\",\"HN-H-G24D-26W-4000-G2\",\"4\",\"EA\",268887,\"718421828746\",\"LUNERA\""
				+"\r\n\"268887\",268887.2,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00052\",\"HN-H-G24Q-26W-4000-G3\",\"4\",\"EA\",268887,\"871699969172\",\"LUNERA\""
				+"\r\n\"268887\",268887.3,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00056\",\"HN-V-G24Q-26W-4000-G3\",\"4\",\"EA\",268887,\"871699969554\",\"LUNERA\""
				+"\r\n\"268892\",268892.1,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00049\",\"HN-H-G24Q-26W-2700-G3\",\"23\",\"EA\",268892,\"871699968878\",\"WORLD\""
				+"\r\n\"268892\",268892.2,\"2015-08-10 12:00:00\",\"2015-08-10 12:00:00\",\"930-00051\",\"HN-H-G24Q-26W-3500-G3\",\"62\",\"EA\",268892,\"871699969004\",\"WORLD\"";
		beginTransaction();
		importOrdersData(facility, csvString);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "268887");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);

		OrderHeader order2 = OrderHeader.staticGetDao().findByDomainId(facility, "268892");
		Assert.assertNotNull(order2);
		Integer detailCount2 = order2.getOrderDetails().size();
		Assert.assertEquals((Integer) 2, detailCount2);

		// first three items for one customer should require scan
		OrderDetail detail1 = order.getOrderDetail("268887.1");
		Assert.assertNotNull(detail1);
		Assert.assertSame(true, detail1.getNeedsScan());

		OrderDetail detail2 = order2.getOrderDetail("268892.1");
		Assert.assertNotNull(detail2);
		Assert.assertSame(false, detail2.getNeedsScan());
		commitTransaction();
	}

	
	@Test
	public void orderBeanExampleScriptTransformationTest() throws IOException {

		Facility facility = setUpSimpleNoSlotFacility();

		// define a rule to set needsscan for a specific customer
		beginTransaction();
		//Test the example script
		ExtensionPoint needsScanScript = new ExtensionPoint(facility, ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setActive(true);
		ExtensionPoint.staticGetDao().store(needsScanScript);
		commitTransaction();

		String csvString = "orderGroupId,customerId,shipperId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,SPECIALCUSTOMER,223,223,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,SPECIALCUSTOMER,223,223,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,SPECIALCUSTOMER,223,223,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,UPS,224,224,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";

		beginTransaction();
		importOrdersData(facility, csvString);
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

		beginTransaction();
		ExtensionPoint extp = new ExtensionPoint(facility, ExtensionPointType.OrderImportHeaderTransformation);
		extp.setActive(true);
		ExtensionPoint.staticGetDao().store(extp);
		commitTransaction();

		String csvString = "orderGroupId^shipmentId^customerId^preAssignedContainerId^orderId^itemId^description^quantity^uom^orderDate^dueDate^workSequence^needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";

		beginTransaction();
		importOrdersData(facility, csvString);
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
	public void orderHeaderCreateTest() throws IOException {

		String desiredHeader = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan";

		String scriptText = "def OrderImportCreateHeader(orderHeader) { orderHeader= \"" + desiredHeader + "\" }";
		
/* an example
 * 		def OrderImportCreateHeader(orderHeader) { 
		     orderHeader= "asMission,orderId,preAssignedContainerId,locationId,quantity,itemId,orderDetailId,workSequence,buildingCode,uom"
		}
*/		
		
		Facility facility = setUpSimpleNoSlotFacility();

		
		beginTransaction();
		createExtension(facility, ExtensionPointType.OrderImportCreateHeader, scriptText);
		commitTransaction();
		
/*
 * This string works when extension is turned off above.
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";
*/
		String csvString = "1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";
		
		beginTransaction();
		importOrdersData(facility, csvString);
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

		// define a rule to set needsscan for each picks, if not defined in import file
		beginTransaction();
		ExtensionPoint extp = new ExtensionPoint(facility, ExtensionPointType.OrderImportLineTransformation);
		extp.setActive(true);
		ExtensionPoint.staticGetDao().store(extp);
		commitTransaction();

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1~USF314~COSTCO~123~123~10700589~Napa Valley Bistro - Jalapeo Stuffed Olives~1~each~2012-09-26 11:31:01~2012-09-26 11:31:03~0~yes"
				+ "\r\n1~USF314~COSTCO~123~123~10706952~Italian Homemade Style Basil Pesto~1~each~2012-09-26 11:31:01,2012-09-26 11:31:03~0~no";

		beginTransaction();
		importOrdersData(facility, csvString);
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

	@Test
	public void pfsWebExtensionsTest() throws IOException {

		// MESSAGETYPE field is not understood by Codeshelf.
		String desiredHeader = "MESSAGETYPE,preAssignedContainerId,orderId,locationId,quantity,itemId,orderDetailId,workSequence,customerId";
		String headerText = "def OrderImportCreateHeader(orderHeader) { orderHeader= \"" + desiredHeader + "\" }";
		
		String lineText = "def OrderImportLineTransformation(orderLine) {"
				+ "orderLine.replace('^', ',');"
				+ "}";
		
		String beanText = "def OrderImportBeanTransformation(bean) {" +
				"\r\n       bean.uom = 'EA';" +
				"\r\n   	return bean;" +
				"\r\n  }";
		
		Facility facility = setUpSimpleNoSlotFacility();

		
		beginTransaction();
		createExtension(facility, ExtensionPointType.OrderImportCreateHeader, headerText);
		createExtension(facility, ExtensionPointType.OrderImportLineTransformation, lineText);
		createExtension(facility, ExtensionPointType.OrderImportBeanTransformation, beanText);		
		commitTransaction();
		
		String csvString = "ADDMISSION^11243513^11243513^NM00806F^1^889789006171^11243513111112^99999^S7"
				+ "\r\nADDMISSION^11243513^11243513^NL01403D^1^889789003798^11243513111113^99999^S7"
				+ "\r\nADDMISSION^11243513^11243513^NM00102F^1^889789003743^11243513111114^99999^S7";
		
		beginTransaction();
		importOrdersData(facility, csvString);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		OrderHeader order = OrderHeader.staticGetDao().findByDomainId(facility, "11243513");
		Assert.assertNotNull(order);
		Integer detailCount = order.getOrderDetails().size();
		Assert.assertEquals((Integer) 3, detailCount);
		Assert.assertEquals("S7", order.getCustomerId());
		OrderDetail aDetail = order.getOrderDetails().get(0);
		Assert.assertEquals("EA", aDetail.getUomMasterId());

		commitTransaction();
	}
	
	/**
	 * This test proves an non-compilable groovy.
	 * Trying to import orders will deactivate the extension
	 */
	@Test
	public void badGroovyCompilationTest() throws IOException{
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		LOGGER.info("1: Add a non-compilable extension");
		String createLineTransformation = 
				"	def XXX OrderImportLineTransformation(orderLine) {\n" + 
				"    	orderLine.replace('^', ',');\n" + 
				"	}";
		ExtensionPoint extension = createExtension(facility, ExtensionPointType.OrderImportLineTransformation, createLineTransformation);
		
		LOGGER.info("2: Import orders");
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";		
		BatchResult<Object> result = importOrdersData(facility, csvString);
		
		LOGGER.info("3: Verify the violation that came from order importing");
		List<FieldError> violations = result.getViolations();
		Assert.assertEquals(1, violations.size());
		FieldError violation = violations.get(0);
		String message = violation.getMessage();
		Assert.assertTrue(message.contains("Extension failed to load and was deactivated: OrderImportLineTransformation"));
		Assert.assertTrue(message.contains("unable to resolve class XXX"));

		LOGGER.info("4: Verify that the extension was deactivaed");
		extension = ExtensionPoint.staticGetDao().reload(extension);
		Assert.assertFalse(extension.isActive());
		Assert.assertEquals(1, OrderHeader.staticGetDao().getAll().size());
		Assert.assertEquals(4, OrderDetail.staticGetDao().getAll().size());
		commitTransaction();
	}
	
	/**
	 * This test demonstrates error handling when OrderImportCreateHeader extension fails.
	 * This will terminate order importing at once
	 */
	@Test
	public void badGroovyErrorRuntimeCreateHeaderTest() throws IOException{
		badGroovyErrorRuntimeTestHelper(ExtensionPointType.OrderImportCreateHeader);
	}
	
	/**
	 * This test demonstrates error handling when OrderImportHeaderTransformation extension fails.
	 * This will terminate order importing at once
	 */
	@Test
	public void badGroovyErrorRuntimeHeaderTransformationTest() throws IOException{
		badGroovyErrorRuntimeTestHelper(ExtensionPointType.OrderImportHeaderTransformation);
	}
	
	/**
	 * This test demonstrates error handling when OrderImportLineTransformation extension fails.
	 * This will terminate order importing at once
	 */
	@Test
	public void badGroovyErrorRuntimeLineTransformationTest() throws IOException{
		badGroovyErrorRuntimeTestHelper(ExtensionPointType.OrderImportLineTransformation);
	}
	
	private void badGroovyErrorRuntimeTestHelper(ExtensionPointType type) throws IOException{
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		LOGGER.info("1: Add extension with a faulty call (it'll pass ExtensionService loading, but fail on runtime");
		String script = 
				"def " + type + "(param) { \n" + 
				"    fake() \n" + 
				"}";
		createExtension(facility, type, script);
		
		LOGGER.info("2: Import orders");
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";		
		BatchResult<Object> result = importOrdersData(facility, csvString);

		LOGGER.info("3: Verify violation");
		List<FieldError> violations = result.getViolations();
		Assert.assertEquals(1, violations.size());
		FieldError violation = violations.get(0);
		String message = violation.getMessage();
		Assert.assertTrue(message.contains("Script type " + type));
		Assert.assertTrue(message.contains("fake()"));

		commitTransaction();
	}
	
	/**
	 * This test demonstrates error handling when OrderImportBeanTransformation extension fails.
	 * Unlike the above "bad groovy" tests, this bad extension will generate a list of order file lines that it failed on
	 * With bad code, it will fail on every line. However, the failures are grouped by error messages
	 */
	@Test
	public void badGroovyErrorRuntimeBeanTransformationTest() throws IOException{
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		LOGGER.info("1: Add extension with a faulty call (it'll pass ExtensionService loading, but fail on runtime");
		String script = 
				"def OrderImportBeanTransformation(orderLine) { \n" + 
				"    fake() \n" + 
				"}";
		createExtension(facility, ExtensionPointType.OrderImportBeanTransformation, script);
		
		LOGGER.info("2: Import orders");
		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,COSTCO,123,123,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,COSTCO,123,123,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,COSTCO,123,123,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,123,123,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";		
		BatchResult<Object> result = importOrdersData(facility, csvString);

		LOGGER.info("3: Verify violation");
		List<FieldError> violations = result.getViolations();
		Assert.assertEquals(1, violations.size());
		FieldError violation = violations.get(0);
		String message = violation.getMessage();
		Assert.assertTrue(message.contains("Failed to evaluate OrderImportBeanTransformation extension point on line(s) 2, 3, 4, 5:"));
		Assert.assertTrue(message.contains("fake()"));

		commitTransaction();
		
	}

	
	private ExtensionPoint createExtension(Facility facility, ExtensionPointType type, String script){
		ExtensionPoint extension = new ExtensionPoint(facility, type);
		extension.setActive(true);
		extension.setScript(script);
		ExtensionPoint.staticGetDao().store(extension);
		return extension;
	}
}