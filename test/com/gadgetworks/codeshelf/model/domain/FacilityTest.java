/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.UUID;

import javax.websocket.EncodeException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ServiceMethodResponse;

/**
 * @author jeffw
 *
 */
public class FacilityTest extends DomainTestABC {
	
	@Test
	public final void testGetParentAtLevelWithInvalidSublevel() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility("ORG-testGetParentAtLevelWhenSublevel");
		Tier nullParent = facility.getParentAtLevel(Tier.class);
		Assert.assertNull(nullParent);
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility("ORG-testGetParentAtLevelWhenSublevel");
		String locationId = facility.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testHasCrossbatchOrders() {
		this.getPersistenceService().beginTenantTransaction();

		Facility facility = createDefaultFacility("FTEST3.O1");
		OrderHeader crossbatchOrder = new OrderHeader();
		crossbatchOrder.setDomainId("ORDER1");
		crossbatchOrder.setUpdated(new Timestamp(0));
		crossbatchOrder.setOrderType(OrderTypeEnum.CROSS);
		crossbatchOrder.setActive(true);

		facility.addOrderHeader(crossbatchOrder);

		mOrderHeaderDao.store(crossbatchOrder);

		
		boolean hasCrossBatchOrders = facility.hasCrossBatchOrders();
		Assert.assertTrue(hasCrossBatchOrders);
		
		Facility retrievedFacility=mFacilityDao.findByPersistentId(facility.getPersistentId());
		hasCrossBatchOrders = retrievedFacility.hasCrossBatchOrders();
		
		Assert.assertTrue(hasCrossBatchOrders);
		this.getPersistenceService().commitTenantTransaction();
	}
	
	/**
	 * Tests that fields that are not ebean properties are properly serialized (has annotations)
	 */
	@Test
	public void testSerializationOfExtraFields() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility("FTEST2.O1");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode= mapper.valueToTree(facility);
		Assert.assertNotNull(objectNode.findValue("hasMeaningfulOrderGroups"));
		Assert.assertNotNull(objectNode.findValue("hasCrossBatchOrders"));
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testVerticesDeletion() {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = createDefaultFacility("FTEST4.O1");
		facility.setDomainId("Vertex Test Facility");
		UUID id = facility.getPersistentId();
		//Default anchor = (-120.0, 30.0, 0.0);
		createAndSaveVertex(facility, "V01", 0, facility.getAnchorPosX(), facility.getAnchorPosY());
		createAndSaveVertex(facility, "V02", 1, -120d, 30d);
		createAndSaveVertex(facility, "V03", 2, -119.999d, 29.999d);
		createAndSaveVertex(facility, "V04", 2, -120d, 29.999d);
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(id);
		facility.removeAllVertices();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		facility = Facility.DAO.findByPersistentId(id);

		this.getPersistenceService().commitTenantTransaction();

	}
	
	private void createAndSaveVertex(Facility facility, String name, int drawOrder, final Double inX, final Double inY){
		Vertex v = new Vertex();
		v.setDomainId(name);
		v.setDrawOrder(drawOrder);
		v.setPoint(new Point(PositionTypeEnum.GPS, inX, inY, 0d));
		facility.addVertex(v);
		Vertex.DAO.store(v);		
	}
	
	@Test
	public void testProductivitySummary() throws EncodeException {
		//This method doesn't assert anything at the moment, as we are just building out productivity reporting
		this.getPersistenceService().beginTenantTransaction();
		WorkService ws = new WorkService();
		Facility facility = createFacilityWithOutboundOrders("FTEST5.O1");
		UUID facilityId = facility.getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
		
		this.getPersistenceService().beginTenantTransaction();
		ProductivitySummary productivitySummary = ws.getProductivitySummary(facilityId);
		JsonEncoder encoder = new JsonEncoder();
		ServiceMethodResponse response = new ServiceMethodResponse();
		response.setResults(productivitySummary);
		String encoded = encoder.encode(response);
		this.getPersistenceService().commitTenantTransaction();
	}
}
