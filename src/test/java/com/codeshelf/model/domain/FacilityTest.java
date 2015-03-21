/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.testframework.ServerTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author jeffw
 *
 */
public class FacilityTest extends ServerTest { // TODO: mock property service so that testSerializationOfExtraFields will work in MocKDao environ
	
	@Test
	public final void testGetParentAtLevelWithInvalidSublevel() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Facility facility = createFacility();
		Tier nullParent = facility.getParentAtLevel(Tier.class);
		Assert.assertNull(nullParent);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
	
	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Facility facility = createFacility();
		String locationId = facility.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
	
	/**
	 * Tests that fields that are not ebean properties are properly serialized (has annotations)
	 */
	@Test
	public void testSerializationOfExtraFields() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Facility facility = createFacility();

		// serializing requires a user context because it depends on tenant specific property configuration
		this.createMockWsConnection();
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode= mapper.valueToTree(facility);
		
		Assert.assertNotNull(objectNode.findValue("hasMeaningfulOrderGroups"));
		Assert.assertNotNull(objectNode.findValue("hasCrossBatchOrders"));
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
	}
	
	@Test
	public void testVerticesDeletion() {
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		Facility facility = createFacility();
		facility.setDomainId("Vertex Test Facility");
		UUID id = facility.getPersistentId();
		//Default anchor = (-120.0, 30.0, 0.0);
		createAndSaveVertex(facility, "V01", 0, facility.getAnchorPosX(), facility.getAnchorPosY());
		createAndSaveVertex(facility, "V02", 1, -120d, 30d);
		createAndSaveVertex(facility, "V03", 2, -119.999d, 29.999d);
		createAndSaveVertex(facility, "V04", 2, -120d, 29.999d);
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		facility = Facility.staticGetDao().findByPersistentId(getDefaultTenant(),id);
		facility.removeAllVertices(getDefaultTenant());
		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());
		
		this.getTenantPersistenceService().beginTransaction(getDefaultTenant());
		facility = Facility.staticGetDao().findByPersistentId(getDefaultTenant(),id);

		this.getTenantPersistenceService().commitTransaction(getDefaultTenant());

	}

	private void createAndSaveVertex(Facility facility, String name, int drawOrder, final Double inX, final Double inY){
		Vertex v = new Vertex();
		v.setDomainId(name);
		v.setDrawOrder(drawOrder);
		v.setPoint(new Point(PositionTypeEnum.GPS, inX, inY, 0d));
		facility.addVertex(v);
		Vertex.staticGetDao().store(getDefaultTenant(),v);		
	}
}
