package com.gadgetworks.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.device.CheStateEnum;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class CheSimulationTest extends EndToEndIntegrationTest {

	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheSimulationTest.class);

	@Test
	public final void testNoWorkToDo() throws IOException {
		// get basic data from database
		Organization organization = mOrganizationDao.findByDomainId(null, organizationId);
		Assert.assertNotNull(organization);
		Facility fac = mFacilityDao.findByDomainId(organization, facilityId);
		Assert.assertNotNull(fac);
		CodeshelfNetwork network = fac.getNetwork(networkId);
		Assert.assertNotNull(network);
		Che che = network.getChe(cheId1);
		Assert.assertNotNull(che);
		
		PickSimulator picker = new PickSimulator(this,cheGuid1);
		picker.login("Picker #1");
		picker.setupContainer("1", "1");
		picker.start(null);
		picker.waitForCheState(CheStateEnum.NO_WORK,1000);
		picker.logout();
	}
}
