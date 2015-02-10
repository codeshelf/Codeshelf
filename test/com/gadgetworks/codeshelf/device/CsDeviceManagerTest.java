package com.gadgetworks.codeshelf.device;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.gadgetworks.codeshelf.generators.FacilityGenerator;
import com.gadgetworks.codeshelf.model.dao.DAOTestABC;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
import com.gadgetworks.codeshelf.util.MemoryConfiguration;
import com.gadgetworks.flyweight.command.CommandControlDisplayMessage;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;

public class CsDeviceManagerTest extends DAOTestABC {
	
	@Test
	public void communicatesServerUnattachedToChe() throws DeploymentException, IOException {
		this.getTenantPersistenceService().beginTenantTransaction();

		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(TenantManagerService.getInstance().getDefaultTenant(),mockRadioController);		

		attachedDeviceManager.disconnected();
		
		attachedDeviceManager.unattached();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));

		this.getTenantPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void communicatesServerDisconnectionToChe() throws DeploymentException, IOException {
		this.getTenantPersistenceService().beginTenantTransaction();

		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(TenantManagerService.getInstance().getDefaultTenant(),mockRadioController);

		attachedDeviceManager.disconnected();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));

		this.getTenantPersistenceService().commitTenantTransaction();
	}

	private CsDeviceManager produceAttachedDeviceManager(Tenant tenant,IRadioController mockRadioController) throws DeploymentException, IOException {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("websocket.uri", "ws://127.0.0.1:8181/ws/"); // this URL doesn't need to be accurate, just parseable
		WebSocketContainer container = mock(WebSocketContainer.class);
		Session mockSession = mock(Session.class);
		when(mockSession.isOpen()).thenReturn(true);
		when(container.connectToServer(any(Endpoint.class), any(URI.class))).thenReturn(mockSession);
		CsDeviceManager deviceManager = new CsDeviceManager(mockRadioController, new MemoryConfiguration(properties), container);
		
		deviceManager.start();
		
		deviceManager.connected();
		
		FacilityGenerator facilityGenerator = new FacilityGenerator(tenant);
		Facility facility = facilityGenerator.generateValid();

		CodeshelfNetwork network=facility.getNetworks().get(0);
		deviceManager.attached(network);
		
		// DEV-459  additions. Need at least one associated CHE to see a CHE display message. Critical for above tests of disconnect or unattached.
		for(CheDeviceLogic theCheDevice : deviceManager.getCheControllers()) {
			theCheDevice.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
			theCheDevice.startDevice();
		}

		return deviceManager;
	}
}
