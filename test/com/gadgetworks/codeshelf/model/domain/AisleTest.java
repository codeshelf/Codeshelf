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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;

public class AisleTest extends DomainTestABC {
	private static final Logger	LOGGER			= LoggerFactory.getLogger(AisleTest.class);

	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		this.getTenantPersistenceService().beginTenantTransaction();
		
		Aisle aisle = getDefaultAisle(getDefaultFacility(), "A1");
		String locationId = aisle.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
		
		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void updateControllerOnAisle() {
		// Case 1: simple add
		this.getTenantPersistenceService().beginTenantTransaction();

		Facility facility = getDefaultFacility();
		CodeshelfNetwork network = facility.getNetworks().get(0);
		LedController controller1 = getDefaultController(network, "0xABCDEF");
		Aisle aisle = getDefaultAisle(getDefaultFacility(), "A1");

		Short testChannel = 8;
		aisle.setControllerChannel(controller1.getPersistentId().toString(), testChannel.toString());
		
		Aisle storedAisle = mAisleDao.findByPersistentId(aisle.getPersistentId());
		assertEquals(controller1.getDomainId(), storedAisle.getLedControllerId());
		assertEquals(testChannel, storedAisle.getLedChannel());

		this.getTenantPersistenceService().commitTenantTransaction();
		
		// Case 2: Cover the odd-ball case of aisle has a controller, but try to assign to a bad one.
		this.getTenantPersistenceService().beginTenantTransaction();
		try {
			aisle.setControllerChannel(UUID.randomUUID().toString(),"2");
			fail("Should have thrown an exception");
		}
		catch(Exception e) {			
		}
		// verify that no change happened.
		assertEquals(controller1.getDomainId(), storedAisle.getLedControllerId());
		this.getTenantPersistenceService().commitTenantTransaction();
		
		// Case 3: Make sure prior controller is removed
		this.getTenantPersistenceService().beginTenantTransaction();
		LedController controller2 = getDefaultController(network, "0x000FFF");
		aisle.setControllerChannel(controller2.getPersistentId().toString(),"3");
		// verify that the change happened.
		assertEquals(controller2.getDomainId(), storedAisle.getLedControllerId());
		LOGGER.info("controller1: "+ controller1.getPersistentId().toString());
		LOGGER.info("controller2: "+ controller2.getPersistentId().toString());
		this.getTenantPersistenceService().commitTenantTransaction();

	}

	@Test
	public final void updateNonexistantController() {
		this.getTenantPersistenceService().beginTenantTransaction();

		Short testChannel = 8;
		Aisle aisle = getDefaultAisle(getDefaultFacility(), "A1");
		try {
			aisle.setControllerChannel(UUID.randomUUID().toString(), testChannel.toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void associatePathSegment() {
		this.getTenantPersistenceService().beginTenantTransaction();
		IDaoListener listener = Mockito.mock(IDaoListener.class);
		this.getTenantPersistenceService().getObjectChangeBroadcaster().registerDAOListener(listener, Aisle.class);
		
		String aisleDomainId = "A1";
		
		Facility facility = getDefaultFacility();
		
		PathSegment pathSegment  = getDefaultPathSegment(getDefaultPath(facility, "P1"), 1);

		Aisle aisle = getDefaultAisle(facility, aisleDomainId);
		String segPersistId = pathSegment.getPersistentId().toString();
		this.getTenantPersistenceService().commitTenantTransaction();
		
		
		this.getTenantPersistenceService().beginTenantTransaction();
		aisle.associatePathSegment(segPersistId);
		// Paul: please see facility.recomputeLocationPathDistances()
		
		
		Aisle storedAisle = (Aisle) facility.findLocationById(aisleDomainId);
		assertEquals(pathSegment.getPersistentId(), storedAisle.getAssociatedPathSegment().getPersistentId());

		verify(listener, times(1)).objectAdded(eq(Aisle.class), eq(storedAisle.getPersistentId()));
		this.getTenantPersistenceService().commitTenantTransaction();
		
		// Cover the odd-ball case of aisle has a path segment, but try to assign to a bad one.
		this.getTenantPersistenceService().beginTenantTransaction();
		try {
			aisle.associatePathSegment(UUID.randomUUID().toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		// verify that no change happened.
		assertEquals(pathSegment.getPersistentId(), storedAisle.getAssociatedPathSegment().getPersistentId());
		this.getTenantPersistenceService().commitTenantTransaction();		
	}
	
	@Test
	public final void updateNonexistantPathSegment() {
		this.getTenantPersistenceService().beginTenantTransaction();

		Aisle aisle = getDefaultAisle(getDefaultFacility(), "A1");
		try {
			aisle.associatePathSegment(UUID.randomUUID().toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		this.getTenantPersistenceService().commitTenantTransaction();
		
	}

	
}
