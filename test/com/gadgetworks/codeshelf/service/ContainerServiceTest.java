package com.gadgetworks.codeshelf.service;

import org.junit.Test;

import com.gadgetworks.codeshelf.edi.CrossBatchCsvBean;
import com.gadgetworks.codeshelf.edi.EdiTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;

public class ContainerServiceTest extends EdiTestABC {

	@Test
	public void testNoContainers() {
		Facility facility = createFacility(testName.getMethodName());
		//generateCrossbatches(10);
		
		ContainerService service  = new ContainerService();
		
		service.containersWithViolations(facility.getPersistentId().toString());
	}
	/*
	private Container generateCrossbatches(int number) {
		CrossBatchCsvBean bean = new CrossBatchCsvBean();
		bean.setContainerId(generateString());
		bean.setItemId(generateString());
		bean.setQuantity(String.valueOf(generateInt());
		bean.setUom(generateString());

	}*/
}
