/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandControlTest.java,v 1.3 2013/09/05 03:26:03 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.radio.RadioController;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.testframework.MinimalTest;

public final class RadioControllerTest extends MinimalTest {

	@Test
	public void testRadioController() {

		IRadioController radioController = new RadioController(Mockito.mock(IGatewayInterface.class));

		INetworkDevice device1 = new CheDeviceLogic(UUID.randomUUID(),
			new NetGuid("0x901"),
			mock(CsDeviceManager.class),
			radioController,
			null);
		
		radioController.addNetworkDevice(device1);

		INetworkDevice device2 = new CheDeviceLogic(UUID.randomUUID(),
			new NetGuid("0x902"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		radioController.addNetworkDevice(device2);

		INetworkDevice device3 = new CheDeviceLogic(UUID.randomUUID(),
			new NetGuid("0x9FF"),
			mock(CsDeviceManager.class),
			radioController,
			null);

		radioController.addNetworkDevice(device3);

		INetworkDevice device4 = new CheDeviceLogic(UUID.randomUUID(),
			new NetGuid("0xA01"),
			mock(CsDeviceManager.class),
			radioController,
			null);
		
		radioController.addNetworkDevice(device4);
		
		// Make sure all the devices got added.	
		INetworkDevice testDevice1 = radioController.getNetworkDevice(device1.getGuid());
		Assert.assertNotNull(testDevice1);
		INetworkDevice testDevice2 = radioController.getNetworkDevice(device2.getGuid());
		Assert.assertNotNull(testDevice2);
		INetworkDevice testDevice3 = radioController.getNetworkDevice(device3.getGuid());
		Assert.assertNotNull(testDevice3);
		INetworkDevice testDevice4 = radioController.getNetworkDevice(device4.getGuid());
		Assert.assertNotNull(testDevice4);
		
		// Make sure none of them have the same net address.	
		Assert.assertNotEquals(testDevice1.getAddress(), testDevice2.getAddress());
		Assert.assertNotEquals(testDevice1.getAddress(), testDevice3.getAddress());
		Assert.assertNotEquals(testDevice1.getAddress(), testDevice4.getAddress());
		Assert.assertNotEquals(testDevice2.getAddress(), testDevice3.getAddress());
		Assert.assertNotEquals(testDevice2.getAddress(), testDevice4.getAddress());
		Assert.assertNotEquals(testDevice3.getAddress(), testDevice4.getAddress());
		
	}

}
