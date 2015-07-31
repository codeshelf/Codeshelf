package com.codeshelf.api.resources;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.ServerTest;

public class FacilitiesResourceTest extends ServerTest {

	@Test
	public void testRecreateStandardTestFacility() {
		beginTransaction();
		Facility facility = createFacility();
		String domainId = facility.getDomainId();
		commitTransaction();	
		beginTransaction();
		FacilitiesResource resource = new FacilitiesResource(this.webSocketManagerService);
		Response response = resource.recreateFacility(domainId);
		Facility newFacility = (Facility) response.getEntity();
		commitTransaction();	
		Assert.assertNotEquals(facility.getPersistentId(), newFacility.getPersistentId());
		Assert.assertEquals(facility.getDomainId(), newFacility.getDomainId());
	}
}
