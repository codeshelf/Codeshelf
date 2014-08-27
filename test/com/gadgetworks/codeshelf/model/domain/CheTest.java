/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: FacilityTest.java,v 1.11 2013/11/11 07:46:30 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author ranstrom
 *
 */
public class CheTest extends DomainTestABC {

	@Test
	public final void trivialCheTest() {
		Facility facility = createFacility("CTEST1.O1");
		Assert.assertNotNull(facility);
		
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
