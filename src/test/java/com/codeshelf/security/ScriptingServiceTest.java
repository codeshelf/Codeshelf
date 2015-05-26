package com.codeshelf.security;

import static org.junit.Assert.*;

import javax.script.ScriptException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.OutboundOrderCsvBean;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Script;
import com.codeshelf.service.ExtensionPoint;
import com.codeshelf.service.ScriptingService;
import com.codeshelf.testframework.ServerTest;

public class ScriptingServiceTest extends ServerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingServiceTest.class);

	@Test
	public void simpleScriptingEngineTest() {
		
		Facility facility = setUpSimpleNoSlotFacility();

		beginTransaction();
		String text = "def OrderImportNeedsScan(orderBean) { orderBean.description == 'abc' }";
		Script needsScanScript = new Script();
		needsScanScript.setParent(facility);
		needsScanScript.setExtension(ExtensionPoint.OrderImportNeedsScan);
		needsScanScript.setBody(text);
		needsScanScript.setDomainId(ExtensionPoint.OrderImportNeedsScan.toString());
		Script.staticGetDao().store(needsScanScript);
		commitTransaction();
		
		beginTransaction();
		try {
			// init service
			ScriptingService ss = ScriptingService.createInstance();
			assertEquals(false,ss.hasExtentionPoint(ExtensionPoint.OrderImportNeedsScan));
			ss.loadScripts(facility);
			assertEquals(true,ss.hasExtentionPoint(ExtensionPoint.OrderImportNeedsScan));
			// positive test
			OutboundOrderCsvBean bean1 = new OutboundOrderCsvBean();
			bean1.setDescription("abc");
			Object[] data1 = {bean1};
			Object result1 = ss.eval(facility, ExtensionPoint.OrderImportNeedsScan, data1);
			assertEquals(true,result1);
			// negative test
			OutboundOrderCsvBean bean2 = new OutboundOrderCsvBean();
			bean2.setDescription("def");
			Object[] data2 = {bean2};
			Object result2 = ss.eval(facility, ExtensionPoint.OrderImportNeedsScan, data2);
			assertEquals(false,result2);
		} 
		catch (ScriptException e) {
			LOGGER.error("", e);
			fail(e.toString());
		}
		commitTransaction();
	}
}
