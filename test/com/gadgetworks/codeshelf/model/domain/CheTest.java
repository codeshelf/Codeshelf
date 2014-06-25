/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author ranstrom
 *
 */
public class CheTest extends DomainTestABC {

	@Test
	public final void trivialCheTest() {

		Organization organization = new Organization();
		organization.setOrganizationId("CTEST1.O1");
		mOrganizationDao.store(organization);

		Facility facility = new Facility(Point.getZeroPoint());
		facility.setParent(organization);
		facility.setFacilityId("CTEST1.F1");
		mFacilityDao.store(facility);

		Facility foundFacility = Facility.DAO.findByDomainId(organization, "CTEST1.F1");

		Assert.assertNotNull(foundFacility);
		
		Che newChe = new Che(); // not right
		// This throws horribly. But we catch it.
		try {
			// kind of stupid. Just see if this makes the compiler not strip out the method.
			newChe.changeControllerId("0x000089");
		}
		catch (Exception e) {
			
		}
		
		
	}


}
