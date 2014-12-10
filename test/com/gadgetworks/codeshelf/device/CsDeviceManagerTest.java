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
import java.util.UUID;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import lombok.Getter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.gadgetworks.codeshelf.application.Configuration;
import com.gadgetworks.codeshelf.generators.FacilityGenerator;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.util.MemoryConfiguration;
import com.gadgetworks.flyweight.command.CommandControlDisplayMessage;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.INetworkDevice;
import com.gadgetworks.flyweight.controller.IRadioController;
import com.gadgetworks.flyweight.controller.NetworkDeviceStateEnum;

public class CsDeviceManagerTest {
	@Getter
	PersistenceService persistenceService = PersistenceService.getInstance();

	@Before
	public final void setup() {
		Configuration.loadConfig("test");
	}	

	@Test
	public void communicatesServerUnattachedToChe() throws DeploymentException, IOException {
		this.getPersistenceService().beginTenantTransaction();

		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(null,mockRadioController);		

		attachedDeviceManager.disconnected();
		
		attachedDeviceManager.unattached();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));

		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void communicatesServerDisconnectionToChe() throws DeploymentException, IOException {
		this.getPersistenceService().beginTenantTransaction();

		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(null,mockRadioController);

		attachedDeviceManager.disconnected();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));

		this.getPersistenceService().commitTenantTransaction();
	}

	private CsDeviceManager produceAttachedDeviceManager(Organization organization, IRadioController mockRadioController) throws DeploymentException, IOException {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("websocket.uri", "wss://uri:800");
		WebSocketContainer container = mock(WebSocketContainer.class);
		Session mockSession = mock(Session.class);
		when(mockSession.isOpen()).thenReturn(true);
		when(container.connectToServer(any(Endpoint.class), any(URI.class))).thenReturn(mockSession);
		CsDeviceManager deviceManager = new CsDeviceManager(mockRadioController, new MemoryConfiguration(properties), container);
		
		deviceManager.start();
		
		deviceManager.connected();
		
		NetGuid cheGuid = new NetGuid("0x001");

		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();
		CodeshelfNetwork network = facility.createNetwork(organization,"DEFAULTTEST");
		Che che = new Che();
		che.setPersistentId(UUID.randomUUID());
		che.setDeviceNetGuid(cheGuid);
		network.addChe(che);

		deviceManager.attached(network);
		
		// DEV-459  additions. Need at least one associated CHE to see a CHE display message. Critical for above tests of disconnect or unattached.
		INetworkDevice theCheDevice = deviceManager.getDeviceByGuid(cheGuid);
		theCheDevice.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
		theCheDevice.startDevice();

		return deviceManager;
	}
}
