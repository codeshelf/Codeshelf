/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.5 2012/11/03 03:24:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import junit.framework.Assert;

import org.junit.Test;

import com.gadgetworks.codeshelf.model.dao.MockDao;

/**
 * @author jeffw
 *
 */
public class FacilityTest {

	@Test
	public final void createAisleTest() {

		Organization.DAO = new MockDao<Organization>();
		Facility.DAO = new MockDao<Facility>();
		Aisle.DAO = new MockDao<Aisle>();
		Bay.DAO = new MockDao<Bay>();
		Vertex.DAO = new MockDao<Vertex>();
		Path.DAO = new MockDao<Path>();
		PathSegment.DAO = new MockDao<PathSegment>();

		Organization organization = new Organization();
		organization.setOrganizationId("O1");
		Organization.DAO.store(organization);

		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setFacilityId("F1");
		facility.createAisle("TEST", 1.0, 1.0, 2.0, 2.0, 2.0, 2, 5, true, true);
		Facility.DAO.store(facility);

		Facility foundFacility = Facility.DAO.findByDomainId(organization, "F1");

		Assert.assertNotNull(foundFacility);
	}

}
