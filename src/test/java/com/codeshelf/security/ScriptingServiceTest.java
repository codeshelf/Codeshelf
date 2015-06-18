package com.codeshelf.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;

import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.OutboundOrderCsvBean;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.service.ExtensionPointService;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.testframework.ServerTest;

public class ScriptingServiceTest extends ServerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingServiceTest.class);

	@Test
	public void evalScriptTest() {

		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderBean) { orderBean.description == 'abc' }";
		ExtensionPoint needsScanScript = new ExtensionPoint(facility, ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setScript(text);
		needsScanScript.setActive(true);
		ExtensionPoint.staticGetDao().store(needsScanScript);
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
	public void inactiveScriptsStopRunning() throws ScriptException {
		Facility facility = setUpSimpleNoSlotFacility();

		//test positive case when active
		beginTransaction();
		String text = "def OrderImportBeanTransformation(orderBean) { orderBean.description == 'abc' }";
		ExtensionPoint needsScanScript = new ExtensionPoint(facility, ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setScript(text);
		needsScanScript.setActive(true);
		ExtensionPoint.staticGetDao().store(needsScanScript);
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
		ExtensionPoint needsScanScript = new ExtensionPoint(facility, ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setActive(true);
		needsScanScript.setScript(text);
		ExtensionPoint.staticGetDao().store(needsScanScript);
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
	public void orderBeanExampleScriptTransformationTest() throws IOException {

		Facility facility = setUpSimpleNoSlotFacility();

		// define a rule to set needsscan for a specific customer
		beginTransaction();
		//Test the example script
		ExtensionPoint needsScanScript = new ExtensionPoint(facility, ExtensionPointType.OrderImportBeanTransformation);
		needsScanScript.setActive(true);
		ExtensionPoint.staticGetDao().store(needsScanScript);
		commitTransaction();

		String csvString = "orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence,needsScan"
				+ "\r\n1,USF314,SPECIALCUSTOMER,223,223,10700589,Napa Valley Bistro - Jalapeo Stuffed Olives,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,no"
				+ "\r\n1,USF314,SPECIALCUSTOMER,223,223,10706952,Italian Homemade Style Basil Pesto,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,yes"
				+ "\r\n1,USF314,SPECIALCUSTOMER,223,223,10706962,Authentic Pizza Sauces,1,case,2012-09-26 11:31:01,2012-09-26 11:31:03,0,"
				+ "\r\n1,USF314,COSTCO,224,224,10706972,Authentic Pizza Sauces,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0,";

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

}
