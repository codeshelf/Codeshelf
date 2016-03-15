package com.codeshelf.ws;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.SiteController.SiteControllerRole;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.validation.InputValidationException;

public class SiteControllerCreateEditTest extends ServerTest{
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SiteControllerCreateEditTest.class);
	
	private UiUpdateBehavior uiUpdate = null;
	
	@Before
	public void init(){
		uiUpdate = new UiUpdateBehavior(webSocketManagerService);
	}
	
	@Test
	public void testCreateSuccess(){
		beginTransaction();
		Facility facility = getFacility();
		CodeshelfNetwork network = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		uiUpdate.addSiteController(facility.getPersistentId().toString(), "5001", "Test Location", network.getDomainId());
		Map<String, SiteController> controllers = network.getSiteControllers();
		Assert.assertEquals(2, controllers.size());
		SiteController controller = controllers.get("5001");
		Assert.assertNotNull(controller);
		Assert.assertEquals("5001", controller.getDomainId());
		Assert.assertEquals(SiteControllerRole.STANDBY, controller.getRole());
		Assert.assertEquals("Test Location", controller.getLocation());
		Assert.assertEquals("Site Controller for Test Location", controller.getDescription());
		Assert.assertEquals(network, controller.getParent());
		commitTransaction();
	}
	
	@Test
	public void testCreateUpdateValidationFailure() throws Exception{
		beginTransaction();
		Facility facility = getFacility();
		CodeshelfNetwork network = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		//Missing id
		try {
			uiUpdate.addSiteController(facility.getPersistentId().toString(), "", "Test Location", network.getDomainId());
			throw new Exception("Validation should have failed");
		} catch (InputValidationException e) {
			Assert.assertEquals("Site Controller ID is required", getMessage(e));
		}
		//Missing location
		try {
			uiUpdate.addSiteController(facility.getPersistentId().toString(), "5001", "", network.getDomainId());
			throw new Exception("Validation should have failed");
		} catch (InputValidationException e) {
			Assert.assertEquals("Location is required", getMessage(e));
		}
		//Missing network
		try {
			uiUpdate.addSiteController(facility.getPersistentId().toString(), "5001", "Test Location", "");
			throw new Exception("Validation should have failed");
		} catch (InputValidationException e) {
			Assert.assertEquals("Network is required", getMessage(e));
		}
		//Bad network
		try {
			uiUpdate.addSiteController(facility.getPersistentId().toString(), "5001", "Test Location", "FakeNetwork");
			throw new Exception("Validation should have failed");
		} catch (InputValidationException e) {
			Assert.assertEquals("Network invalid. Server Error: Network FakeNetwork not found.", getMessage(e));
		}
		//Duplicate site controller
		try {
			uiUpdate.addSiteController(facility.getPersistentId().toString(), "5000", "Test Location", network.getDomainId());
			throw new Exception("Validation should have failed");
		} catch (InputValidationException e) {
			Assert.assertEquals("Site Controller ID invalid. Site controller 5000 already exists.", getMessage(e));
		}
		commitTransaction();
	}
	
	private String getMessage(InputValidationException e) {
		return e.getErrors().getAllErrors().get(0).getMessage();
	}
	
	@Test
	public void testUpdateCangeNetwork(){
		beginTransaction();
		Facility facility = getFacility();
		CodeshelfNetwork defaultNetwork = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		
		LOGGER.info("1. Create an additional network");
		CodeshelfNetwork network2 = facility.createNetwork("network2");
		
		LOGGER.info("2. Move existing site controller to that network");
		SiteController siteController = defaultNetwork.getSiteController("5000");
		Assert.assertEquals(SiteControllerRole.NETWORK_PRIMARY, siteController.getRole());
		uiUpdate.updateSiteController(siteController.getPersistentId().toString(), "5000", "New Location", network2.getDomainId());
		
		LOGGER.info("3. Verify that site controller was moved properly");
		siteController = defaultNetwork.getSiteController("5000");
		Assert.assertNull("Site Controller was not removed from default network", siteController);
		
		siteController = network2.getSiteController("5000");
		Assert.assertNotNull(siteController);
		Assert.assertEquals("New Location", siteController.getLocation());
		Assert.assertEquals("Site Controller for New Location", siteController.getDescription());
		Assert.assertEquals(SiteControllerRole.STANDBY, siteController.getRole());
		commitTransaction();
	}

	/**
	 * Changing domainId of a site controller causes its DB object to be deleted and a new one created
	 */
	@Test
	public void testUpdateCangeDomainId(){
		beginTransaction();
		Facility facility = getFacility();
		CodeshelfNetwork defaultNetwork = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
			
		LOGGER.info("1. Change ID of the site controller");
		SiteController siteController = defaultNetwork.getSiteController("5000");
		String siteControllerUUID = siteController.getPersistentId().toString();
		uiUpdate.updateSiteController(siteControllerUUID, "5001", "test", defaultNetwork.getDomainId());
		
		LOGGER.info("2. Verify that there is still only 1 site controller on the network");
		Assert.assertEquals(1, defaultNetwork.getSiteControllers().size());
		siteController = defaultNetwork.getSiteController("5001");
		Assert.assertNotNull(siteController);
		Assert.assertNotEquals(siteControllerUUID, siteController.getPersistentId().toString());
		commitTransaction();
		
		beginTransaction();
		LOGGER.info("3. Verify that a new DB object was created for the changed site controller, and that the old one no longer exists");
		Assert.assertNull(SiteController.staticGetDao().findByPersistentId(siteControllerUUID));
		commitTransaction();
	}

	@Test
	public void testMakePrimary(){
		beginTransaction();
		Facility facility = getFacility();
		CodeshelfNetwork network = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);

		LOGGER.info("1. Create an additional network");
		CodeshelfNetwork network2 = facility.createNetwork("network2");
		
		LOGGER.info("2. Create 2 additional site controllers for default network, and 1 for a new network");
		uiUpdate.addSiteController(facility.getPersistentId().toString(), "5001", "test", network.getDomainId());
		uiUpdate.addSiteController(facility.getPersistentId().toString(), "5002", "test", network.getDomainId());
		uiUpdate.addSiteController(facility.getPersistentId().toString(), "5003", "test", network2.getDomainId());
		SiteController sc5000 = network.getSiteController("5000");
		SiteController sc5001 = network.getSiteController("5001");
		SiteController sc5002 = network.getSiteController("5002");
		SiteController sc5003 = network2.getSiteController("5003");
		Assert.assertEquals(SiteControllerRole.NETWORK_PRIMARY, sc5000.getRole());
		Assert.assertEquals(SiteControllerRole.STANDBY, sc5001.getRole());
		Assert.assertEquals(SiteControllerRole.STANDBY, sc5002.getRole());
		Assert.assertEquals(SiteControllerRole.STANDBY, sc5003.getRole());
		
		LOGGER.info("3. Make the sole site controller on the new network primary");
		uiUpdate.makeSiteControllerPrimaryForNetwork(sc5003.getPersistentId().toString());
		Assert.assertEquals(SiteControllerRole.NETWORK_PRIMARY, sc5000.getRole());
		Assert.assertEquals(SiteControllerRole.STANDBY, sc5001.getRole());
		Assert.assertEquals(SiteControllerRole.STANDBY, sc5002.getRole());
		Assert.assertEquals(SiteControllerRole.NETWORK_PRIMARY, sc5003.getRole());

		LOGGER.info("4. Select a new primary site controller on the old network");
		uiUpdate.makeSiteControllerPrimaryForNetwork(sc5001.getPersistentId().toString());
		Assert.assertEquals(SiteControllerRole.STANDBY, sc5000.getRole());
		Assert.assertEquals(SiteControllerRole.NETWORK_PRIMARY, sc5001.getRole());
		Assert.assertEquals(SiteControllerRole.STANDBY, sc5002.getRole());
		Assert.assertEquals(SiteControllerRole.NETWORK_PRIMARY, sc5003.getRole());

		commitTransaction();
	}
	
	@Test
	public void testSecondarySiteControllerDeviceConnections(){
		LOGGER.info("1. Make SiteController STANDBY");
		beginTransaction();
		Facility facility = getFacility();
		CodeshelfNetwork network = facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		SiteController controller = network.getSiteController("5000");
		controller.setRole(SiteControllerRole.STANDBY);
		SiteController.staticGetDao().store(controller);
		commitTransaction();
		
		LOGGER.info("2. Ensure that SiteController doesn't have CHEs");
		startSiteController();
		try {
			createPickSim(cheGuid1);
			throw new Exception("Should not have found CHE in the Site Controller");
		} catch (Exception e) {
			Assert.assertEquals("No che found with guid: 0x00009991", e.getMessage());
		}
	}
}
