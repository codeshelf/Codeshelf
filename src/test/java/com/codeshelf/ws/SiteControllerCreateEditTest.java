package com.codeshelf.ws;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.testframework.HibernateTest;

public class SiteControllerCreateEditTest extends HibernateTest{
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
		Assert.assertEquals("Test Location", controller.getLocation());
		Assert.assertEquals("Site Controller for Test Location", controller.getDescription());
		Assert.assertEquals(network, controller.getParent());
		commitTransaction();
	}
}
