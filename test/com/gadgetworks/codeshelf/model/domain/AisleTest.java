package com.gadgetworks.codeshelf.model.domain;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

public class AisleTest extends DomainTestABC {

	@Test
	public final void updateControllerOnAisle() {
		LedController controller = getController(getNetwork(getFacility(getOrganization("Org1"))), "0xABCDEF");
		Aisle aisle = getAisle(getFacility(getOrganization("Org1")), "A1");

		Short testChannel = 8;
		aisle.setControllerChannel(controller.getPersistentId().toString(), testChannel.toString());
		
		Aisle storedAisle = mAisleDao.findByPersistentId(aisle.getPersistentId());
		assertEquals(controller.getDomainId(), storedAisle.getLedControllerId());
		assertEquals(testChannel, storedAisle.getLedChannel());
	}

	@Test
	public final void updateNonexistantController() {
		Short testChannel = 8;
		Aisle aisle = getAisle(getFacility(getOrganization("O1")), "A1");
		try {
			aisle.setControllerChannel(UUID.randomUUID().toString(), testChannel.toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		
	}
	
	@Test
	public final void updatePathSegment() {
		String aisleDomainId = "A1";
		
		Facility facility = getFacility(getOrganization("Org1"));
		
		PathSegment pathSegment  = getPathSegment(getPath(facility, "P1"), 1);
		Aisle aisle = getAisle(facility, aisleDomainId);

		String segPersistId = pathSegment.getPersistentId().toString();
		aisle.associatePathSegment(segPersistId);
		// Paul: please see facility.recomputeLocationPathDistances()
	
		Aisle storedAisle = (Aisle) facility.findLocationById(aisleDomainId);
		assertEquals(pathSegment.getPersistentId(), storedAisle.getAssociatedPathSegment().getPersistentId());
	}
	
	@Test
	public final void updateNonexistantPathSegment() {
		Aisle aisle = getAisle(getFacility(getOrganization("O1")), "A1");
		try {
			aisle.associatePathSegment(UUID.randomUUID().toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		
	}

	
}
