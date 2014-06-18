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
	
}
