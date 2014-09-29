package com.gadgetworks.codeshelf.device;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.gadgetworks.codeshelf.generators.FacilityGenerator;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.util.MemoryConfiguration;
import com.gadgetworks.flyweight.command.CommandControlDisplayMessage;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetAddress;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;

public class CsDeviceManagerTest {

	@Test
	public void communicatesServerUnattachedToChe() {
		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(mockRadioController);

		attachedDeviceManager.disconnected();
		
		attachedDeviceManager.unattached();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));
	}
	
	@Test
	public void communicatesServerDisconnectionToChe() {
		IRadioController mockRadioController = mock(IRadioController.class);
		CsDeviceManager attachedDeviceManager = produceAttachedDeviceManager(mockRadioController);

		attachedDeviceManager.disconnected();
		
		ArgumentCaptor<ICommand> commandCaptor = ArgumentCaptor.forClass(ICommand.class);
		verify(mockRadioController, atLeast(1)).sendCommand(commandCaptor.capture(), any(NetAddress.class), any(Boolean.class));
		
		Assert.assertTrue("Should be showing network unavailable", ((CommandControlDisplayMessage)commandCaptor.getValue()).getEntireMessageStr().contains("Unavailable"));
	}

	private CsDeviceManager produceAttachedDeviceManager(IRadioController mockRadioController) {
		Map<String, String> properties = new HashMap<String, String>();
		CsDeviceManager deviceManager = new CsDeviceManager(mockRadioController, new MemoryConfiguration(properties));
		
		deviceManager.start();
		
		deviceManager.connected();
		
		NetGuid cheGuid = new NetGuid("0x001");

		FacilityGenerator facilityGenerator = new FacilityGenerator();
		CodeshelfNetwork network = new CodeshelfNetwork(facilityGenerator.generateValid(), "0x00", "");
		Che che = new Che();
		che.setPersistentId(UUID.randomUUID());
		che.setDeviceNetGuid(cheGuid);
		network.addChe(che);

		deviceManager.attached(network);
		return deviceManager;
	}
}
