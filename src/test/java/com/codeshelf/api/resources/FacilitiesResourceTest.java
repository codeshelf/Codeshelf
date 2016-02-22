package com.codeshelf.api.resources;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.ServerTest;

public class FacilitiesResourceTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(FacilitiesResourceTest.class);

	@Test
	public void testRecreateStandardTestFacility() {
		beginTransaction();
		Facility facility = createFacility();
		String domainId = facility.getDomainId();
		commitTransaction();	
		beginTransaction();
		FacilitiesResource resource = new FacilitiesResource(this.webSocketManagerService, this.applicationSchedulerService);
		Response response = resource.recreateFacility(domainId);
		Facility newFacility = (Facility) response.getEntity();
		commitTransaction();	
		Assert.assertNotEquals(facility.getPersistentId(), newFacility.getPersistentId());
		Assert.assertEquals(facility.getDomainId(), newFacility.getDomainId());
		Assert.assertTrue(!this.applicationSchedulerService.findService(facility).isPresent());
		Assert.assertTrue(this.applicationSchedulerService.findService(newFacility).orNull().isRunning());
	}
	
	@Test
	public void testCreateDuplicateFacilities() {
		String domainId1 = "TestF1";
		String domainId2 = "TestF2";
		
		beginTransaction();
		LOGGER.info("1. Verify that there are no facilities in this tenant");
		Assert.assertFalse(Facility.facilityExists(domainId1));
		Assert.assertFalse(Facility.facilityExists(domainId2));
		Assert.assertEquals(0, Facility.staticGetDao().getAll().size());
		
		LOGGER.info("2. Successfully create and verify a facility");
		FacilitiesResource resource = new FacilitiesResource(this.webSocketManagerService, this.applicationSchedulerService);
		Response response = resource.addFacility(domainId1, "Test Facility");
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Assert.assertTrue(Facility.facilityExists(domainId1));
		Assert.assertEquals(1, Facility.staticGetDao().getAll().size());
		
		LOGGER.info("3. Try and fail to create a facility with the same domain id");
		response = resource.addFacility(domainId1, "Test Facility");
		Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		Assert.assertEquals(1, Facility.staticGetDao().getAll().size());

		LOGGER.info("2. Successfully create and verify a facility with a different dimain id");
		response = resource.addFacility(domainId2, "Test Facility");
		Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
		Assert.assertTrue(Facility.facilityExists(domainId2));
		Assert.assertEquals(2, Facility.staticGetDao().getAll().size());
		commitTransaction();
	}
}
