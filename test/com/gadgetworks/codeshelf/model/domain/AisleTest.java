package com.gadgetworks.codeshelf.model.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;

public class AisleTest extends DomainTestABC {
	Organization org=new Organization();

	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		this.getPersistenceService().beginTenantTransaction();
		
		Aisle aisle = getDefaultAisle(getDefaultFacility(), "A1");
		String locationId = aisle.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
		
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void updateControllerOnAisle() {
		this.getPersistenceService().beginTenantTransaction();

		LedController controller = getDefaultController(getDefaultNetwork(org,getDefaultFacility()), "0xABCDEF");
		Aisle aisle = getDefaultAisle(getDefaultFacility(getDefaultOrganization("Org1")), "A1");

		Short testChannel = 8;
		aisle.setControllerChannel(controller.getPersistentId().toString(), testChannel.toString());
		
		Aisle storedAisle = mAisleDao.findByPersistentId(aisle.getPersistentId());
		assertEquals(controller.getDomainId(), storedAisle.getLedControllerId());
		assertEquals(testChannel, storedAisle.getLedChannel());

		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public final void updateNonexistantController() {
		this.getPersistenceService().beginTenantTransaction();

		Short testChannel = 8;
		Aisle aisle = getDefaultAisle(getDefaultFacility(), "A1");
		try {
			aisle.setControllerChannel(UUID.randomUUID().toString(), testChannel.toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void associatePathSegment() {
		this.getPersistenceService().beginTenantTransaction();
		IDaoListener listener = Mockito.mock(IDaoListener.class);
		this.getPersistenceService().getObjectChangeBroadcaster().registerDAOListener(listener, Aisle.class);
		
		String aisleDomainId = "A1";
		
		Facility facility = getDefaultFacility();
		
		PathSegment pathSegment  = getDefaultPathSegment(getDefaultPath(facility, "P1"), 1);

		Aisle aisle = getDefaultAisle(facility, aisleDomainId);
		String segPersistId = pathSegment.getPersistentId().toString();
		this.getPersistenceService().commitTenantTransaction();
		
		
		this.getPersistenceService().beginTenantTransaction();
		aisle.associatePathSegment(segPersistId);
		// Paul: please see facility.recomputeLocationPathDistances()
		
		
		Aisle storedAisle = (Aisle) facility.findLocationById(aisleDomainId);
		assertEquals(pathSegment.getPersistentId(), storedAisle.getAssociatedPathSegment().getPersistentId());

		verify(listener, times(1)).objectAdded(eq(Aisle.class), eq(storedAisle.getPersistentId()));
		this.getPersistenceService().commitTenantTransaction();
		
	}
	
	@Test
	public final void updateNonexistantPathSegment() {
		this.getPersistenceService().beginTenantTransaction();

		Aisle aisle = getDefaultAisle(getDefaultFacility(), "A1");
		try {
			aisle.associatePathSegment(UUID.randomUUID().toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		this.getPersistenceService().commitTenantTransaction();
		
	}

	
}
