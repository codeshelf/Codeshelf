package com.codeshelf.perf;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.application.JvmProperties;
import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;

public class ComputeWorkPerformanceTest extends ServerTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(ComputeWorkPerformanceTest.class);
	
	static {
		JvmProperties.load("server");
	}
	
	/**
	 * This test should detect any deterioration in compute-work time.
	 * For example, if someone adds a multi-table SQL call for every detail, this should fail.
	 */
	@Test
	public void testComputeWorkTime() throws IOException{
		LOGGER.info("1. Setup facility and import 50 orders.");		
		beginTransaction();
		Facility facility = getFacility();
		PropertyBehavior.setProperty(facility, FacilityPropertyType.WORKSEQR, "WorkSequence");
		commitTransaction();
		
		beginTransaction();
		facility = facility.reload();
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("perftest/orders-4.csv");
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer);
		String orders = writer.toString();
		importOrdersData(facility, orders);
		commitTransaction();
		
		LOGGER.info("2. Load orders on the CHE.");
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);
		picker.loginAndSetup("Worker1");
		int position = 1;
		int orderIds[] = {26721526,26721525,26721527,26715534,26715535,26715536,26715537,26712892,26712893,26711617,26711616,26711619,26711618,26711622,26711621,26711620,26711615,26711623,26709059,26709073,26709068,26709074,26709069,26709075,26709066,26709058,26709067,26709064,26709065,26709062,26709063,26709061,26709060,26709070,26709072,26709071,26715677,26715676,26715675,26715674,26718668,26737725,26749379,26749378,26749380,26749384,26749383,26749382,26749381,26707499};
		for (int orderId : orderIds){
			picker.setupContainer(orderId + "", (position++) + "");
		}
		
		LOGGER.info("3. Assure that work is computed withing reasonable time.");
		//Note that a few seconds are added for the framework operations. The actual ComputeWork command should execute in 1-2s
		int threshold = 5000;
		picker.start(CheStateEnum.SETUP_SUMMARY, threshold);
	}
}
