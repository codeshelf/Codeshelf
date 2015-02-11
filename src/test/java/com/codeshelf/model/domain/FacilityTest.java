/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.codeshelf.model.PositionTypeEnum;

/**
 * @author jeffw
 *
 */
public class FacilityTest extends DomainTestABC {
	
	@Test
	public final void testGetParentAtLevelWithInvalidSublevel() {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility = createFacility();
		Tier nullParent = facility.getParentAtLevel(Tier.class);
		Assert.assertNull(nullParent);
		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility = createFacility();
		String locationId = facility.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	/**
	 * Tests that fields that are not ebean properties are properly serialized (has annotations)
	 */
	@Test
	public void testSerializationOfExtraFields() {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility = createFacility();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode= mapper.valueToTree(facility);
		Assert.assertNotNull(objectNode.findValue("hasMeaningfulOrderGroups"));
		Assert.assertNotNull(objectNode.findValue("hasCrossBatchOrders"));
		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testVerticesDeletion() {
		this.getTenantPersistenceService().beginTenantTransaction();
		Facility facility = createFacility();
		facility.setDomainId("Vertex Test Facility");
		UUID id = facility.getPersistentId();
		//Default anchor = (-120.0, 30.0, 0.0);
		createAndSaveVertex(facility, "V01", 0, facility.getAnchorPosX(), facility.getAnchorPosY());
		createAndSaveVertex(facility, "V02", 1, -120d, 30d);
		createAndSaveVertex(facility, "V03", 2, -119.999d, 29.999d);
		createAndSaveVertex(facility, "V04", 2, -120d, 29.999d);
		this.getTenantPersistenceService().commitTenantTransaction();
		
		this.getTenantPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(id);
		facility.removeAllVertices();
		this.getTenantPersistenceService().commitTenantTransaction();
		
		this.getTenantPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(id);

		this.getTenantPersistenceService().commitTenantTransaction();

	}

	private void createAndSaveVertex(Facility facility, String name, int drawOrder, final Double inX, final Double inY){
		Vertex v = new Vertex();
		v.setDomainId(name);
		v.setDrawOrder(drawOrder);
		v.setPoint(new Point(PositionTypeEnum.GPS, inX, inY, 0d));
		facility.addVertex(v);
		Vertex.DAO.store(v);		
	}
}
