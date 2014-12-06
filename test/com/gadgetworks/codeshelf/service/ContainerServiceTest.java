package com.gadgetworks.codeshelf.service;

import org.junit.Test;

import com.gadgetworks.codeshelf.edi.EdiTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;

public class ContainerServiceTest extends EdiTestABC {

	@Test
	public void testNoContainers() {
		this.getPersistenceService().beginTenantTransaction();
		
		Facility facility = createDefaultFacility(testName.getMethodName());
		//generateCrossbatches(10);
		
		ContainerService service  = new ContainerService();
		
		service.containersWithViolations(facility);
		
		this.getPersistenceService().commitTenantTransaction();
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
