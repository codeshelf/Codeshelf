/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Jeffrey B. Williams, All rights reserved
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.domain.DomainTestABC;

/**
 * @author ranstrom
 * 
 */
public class AisleImporterTest extends DomainTestABC {

	@Test
	public final void testAisleImporter() {
/*
		String csvString = "type,name,length,slotsintier,ctlrconfig,cltrlocaction,anchorx,anchory,tubelight\r\n" //
				+ ",Aisle,A1,,Tier,Right,12.85,43.45,\r\n" //
				+ ",Bay,B1,8,,,,,,\r\n" //
				+ ",Tier,T1,,8,,,,,\r\n" //
				+ ",Tier,T2,,9,,,,,\r\n" //
				+ ",Tier,T3,,5,,,,,\r\n" //
				+ ",Bay,B2,8,,,,,,\r\n" //
				+ ",Tier,T1,,5,,,,,\r\n" //
				+ ",Tier,T2,,6,,,,,\r\n" //
				+ ",Tier,T3,,4,,,,,\r\n"; //
	
		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS1", "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility("F-CROSS1");

*/

		// 
		Assert.assertTrue(true);

	}

	

}
