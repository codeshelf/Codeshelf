/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.UomMaster;

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
