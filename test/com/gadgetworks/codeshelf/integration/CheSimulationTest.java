package com.gadgetworks.codeshelf.integration;

import java.io.IOException;

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

public class CheSimulationTest extends EndToEndIntegrationTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(CheSimulationTest.class);

	@Test
	public final void testChe() throws IOException {
		// get basic data from database
		Organization organization = mOrganizationDao.findByDomainId(null, organizationId);
		Assert.assertNotNull(organization);
		Facility fac = mFacilityDao.findByDomainId(organization, facilityId);
		Assert.assertNotNull(fac);
		CodeshelfNetwork network = fac.getNetwork(networkId);
		Assert.assertNotNull(network);
		Che che = network.getChe(cheId);
		Assert.assertNotNull(che);
		
		// verify that che is in site controller's device list
		CheDeviceLogic cheDeviceLogic = (CheDeviceLogic) this.siteController.getDeviceManager().getDeviceByGuid(cheGuid);
		Assert.assertNotNull(cheDeviceLogic);
		
		// cycle through empty WI list scenario
		cheDeviceLogic.scanCommandReceived("U%PICKER1");
		waitForCheState(cheDeviceLogic,CheStateEnum.CONTAINER_SELECT,1000);

		cheDeviceLogic.scanCommandReceived("C%1");
		waitForCheState(cheDeviceLogic,CheStateEnum.CONTAINER_POSITION,1000);

		cheDeviceLogic.scanCommandReceived("P%1");
		waitForCheState(cheDeviceLogic,CheStateEnum.CONTAINER_SELECT,1000);

		cheDeviceLogic.scanCommandReceived("X%START");
		waitForCheState(cheDeviceLogic,CheStateEnum.PICK_COMPLETE,5000);
		
		cheDeviceLogic.scanCommandReceived("X%LOGOUT");		
		waitForCheState(cheDeviceLogic,CheStateEnum.IDLE,1000);
	}
}
