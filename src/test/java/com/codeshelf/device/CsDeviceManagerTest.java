package com.codeshelf.device;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.codeshelf.flyweight.command.CommandControlDisplayMessage;
import com.codeshelf.flyweight.command.ICommand;
import com.codeshelf.flyweight.command.NetAddress;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.flyweight.controller.NetworkDeviceStateEnum;
import com.codeshelf.generators.FacilityGenerator;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.testframework.MockDaoTest;
import com.codeshelf.ws.client.CsClientEndpoint;

public class CsDeviceManagerTest extends MockDaoTest {
	
	@Test
	public void communicatesServerUnattachedToChe() throws DeploymentException, IOException {
		this.getTenantPersistenceService().beginTransaction();

		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(mockRadioController);		

		attachedDeviceManager.disconnected();
		
		attachedDeviceManager.unattached();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));

		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void communicatesServerDisconnectionToChe() throws DeploymentException, IOException {
		this.getTenantPersistenceService().beginTransaction();

		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(mockRadioController);

		attachedDeviceManager.disconnected();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));

		this.getTenantPersistenceService().commitTransaction();
	}

	private CsDeviceManager produceAttachedDeviceManager(IRadioController mockRadioController) throws DeploymentException, IOException {
		WebSocketContainer container = mock(WebSocketContainer.class);
		Session mockSession = mock(Session.class);
		when(mockSession.isOpen()).thenReturn(true);
		when(container.connectToServer(any(Endpoint.class), any(URI.class))).thenReturn(mockSession);
		
		CsClientEndpoint.setWebSocketContainer(container);
		CsClientEndpoint endpoint = new CsClientEndpoint();
		CsDeviceManager deviceManager = new CsDeviceManager(mockRadioController, endpoint);
		new SiteControllerMessageProcessor(deviceManager,endpoint);
		
		deviceManager.connected();
		
		FacilityGenerator facilityGenerator = new FacilityGenerator();
		Facility facility = facilityGenerator.generateValid();

		CodeshelfNetwork network=facility.getNetwork(CodeshelfNetwork.DEFAULT_NETWORK_NAME);
		deviceManager.attached(network);
		
		// DEV-459  additions. Need at least one associated CHE to see a CHE display message. Critical for above tests of disconnect or unattached.
		for(CheDeviceLogic theCheDevice : deviceManager.getCheControllers()) {
			theCheDevice.setDeviceStateEnum(NetworkDeviceStateEnum.STARTED); // Always call this with startDevice, as this says the device is associated.
			theCheDevice.startDevice(null); // not specifying reastart reason
		}

		return deviceManager;
	}
}
