/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CsDeviceManager;
import com.codeshelf.device.radio.RadioController;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.testframework.MinimalTest;

public final class RadioControllerTest extends MinimalTest {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(RadioControllerTest.class);

	private void addMockDevice(IRadioController radioController, String inGuid) {
		INetworkDevice device = new CheDeviceLogic(UUID.randomUUID(),
			new NetGuid(inGuid),
			mock(CsDeviceManager.class),
			radioController,
			null);

		radioController.addNetworkDevice(device);
	}

	/**
	 * Pretty kludge little thing, and dumb, which is good enough for testing as it forces rollover.
	 * Anyway, say start 1, and it gives you addresses 1-10. 
	 */
	private void add10MockDevices(IRadioController radioController, int startValue) {
		if (startValue % 10 != 1) {
			LOGGER.error("unsupported use of this function");
			return;
		}
		for (int count = 0; count < 10; count++) {
			Integer value = startValue + count;
				String guidStr = "0x" + value.toString();
				addMockDevice(radioController, guidStr);
		}
	}

	private void assertDeviceNetAddress(INetworkDevice testDevice, int expectedValue) {
		Short valueDevice1 = testDevice.getAddress().getValue();
		Assert.assertEquals(expectedValue, valueDevice1.intValue());
	}

	private void assertGuidsNetworkValue(IRadioController inRadioController, String guidStr, int expectedValue) {
		INetworkDevice testDevice1 = inRadioController.getNetworkDevice(new NetGuid(guidStr));
		Assert.assertNotNull(testDevice1);
		assertDeviceNetAddress(testDevice1, expectedValue);
	}

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

	@Test
	public void testRadioController2() {
		IRadioController radioController = new RadioController(Mockito.mock(IGatewayInterface.class));
		addMockDevice(radioController, "0x902");
		addMockDevice(radioController, "0x901");
		addMockDevice(radioController, "0x9FF");
		addMockDevice(radioController, "0xA01");
		addMockDevice(radioController, "0xAFE");
		addMockDevice(radioController, "0xBFE");
		INetworkDevice testDevice1 = radioController.getNetworkDevice(new NetGuid("0x902"));
		Assert.assertNotNull(testDevice1);
		INetworkDevice testDevice2 = radioController.getNetworkDevice(new NetGuid("0x901"));
		Assert.assertNotNull(testDevice2);
		INetworkDevice testDevice3 = radioController.getNetworkDevice(new NetGuid("0x9FF"));
		Assert.assertNotNull(testDevice3);
		INetworkDevice testDevice4 = radioController.getNetworkDevice(new NetGuid("0xA01"));
		Assert.assertNotNull(testDevice4);
		INetworkDevice testDevice5 = radioController.getNetworkDevice(new NetGuid("0xAFE"));
		Assert.assertNotNull(testDevice5);
		INetworkDevice testDevice6 = radioController.getNetworkDevice(new NetGuid("0xBFE"));
		Assert.assertNotNull(testDevice6);

		Assert.assertNotEquals(testDevice1.getAddress(), testDevice2.getAddress());
		Assert.assertNotEquals(testDevice1.getAddress(), testDevice3.getAddress());
		Assert.assertNotEquals(testDevice2.getAddress(), testDevice3.getAddress());

		// Very white box examination of how this works:
		// First added should be 2. 
		// Second added is 1.
		// Third is  FF, which must get another value. Roll to next value of 3.
		assertDeviceNetAddress(testDevice1, 2);
		assertDeviceNetAddress(testDevice2, 1);
		assertDeviceNetAddress(testDevice3, 3);

		// 4th device also tries for 1. 1,2,3 taken, so roll to 4
		assertDeviceNetAddress(testDevice4, 4);

		// 5th device tries for 254
		assertDeviceNetAddress(testDevice5, 254);

		// 6th device tries for 254. Rolls over, and must take 5
		assertDeviceNetAddress(testDevice6, 5);

	}

	@Test
	public void testRadioController3() {
		// Check our 00 cases.
		IRadioController radioController = new RadioController(Mockito.mock(IGatewayInterface.class));
		addMockDevice(radioController, "0x700");
		addMockDevice(radioController, "0x800");
		addMockDevice(radioController, "0x900");
		addMockDevice(radioController, "0x9FF");
		INetworkDevice testDevice1 = radioController.getNetworkDevice(new NetGuid("0x700"));
		Assert.assertNotNull(testDevice1);
		INetworkDevice testDevice2 = radioController.getNetworkDevice(new NetGuid("0x800"));
		Assert.assertNotNull(testDevice2);
		INetworkDevice testDevice3 = radioController.getNetworkDevice(new NetGuid("0x900"));
		Assert.assertNotNull(testDevice3);
		INetworkDevice testDevice4 = radioController.getNetworkDevice(new NetGuid("0x9FF"));
		Assert.assertNotNull(testDevice4);

		Assert.assertNotEquals(testDevice1.getAddress(), testDevice2.getAddress());
		Assert.assertNotEquals(testDevice1.getAddress(), testDevice3.getAddress());
		Assert.assertNotEquals(testDevice2.getAddress(), testDevice3.getAddress());

		// We are not willing give out address 0, which we use for the controller address
		assertDeviceNetAddress(testDevice1, 1); // Had a bug here.
		assertDeviceNetAddress(testDevice2, 2);
		assertDeviceNetAddress(testDevice3, 3);
		assertDeviceNetAddress(testDevice4, 4);

	}
	

	@Test
	public void testRadioController5() {
		// Check wrapping search
		IRadioController radioController = new RadioController(Mockito.mock(IGatewayInterface.class));
		addMockDevice(radioController, "0x701"); // gives out 1
		addMockDevice(radioController, "0x702");// gives out 2
		// skip 3
		addMockDevice(radioController, "0x704");// gives out 4
		addMockDevice(radioController, "0x7FE");// gives out 254
		addMockDevice(radioController, "0x8FD");// gives out 253
		addMockDevice(radioController, "0x9FD");// should wrap around, and find 3 as the first hole
	
		assertGuidsNetworkValue(radioController, "0x701", 1);
		assertGuidsNetworkValue(radioController, "0x702", 2);
		assertGuidsNetworkValue(radioController, "0x704", 4);
		assertGuidsNetworkValue(radioController, "0x7FE", 254);
		assertGuidsNetworkValue(radioController, "0x8FD", 253);
		assertGuidsNetworkValue(radioController, "0x9FD", 3);

	}


	@Test
	public void testRadioController4() {
		// Check the very full cases, including completely full cases
		IRadioController radioController = new RadioController(Mockito.mock(IGatewayInterface.class));

		// This is going to fill up 1-10,11.20, etc. but skip over 0a, ob, oc, etc.
		add10MockDevices(radioController, 101); // yields "0x101" though "0x110"
		// show that this skipped 104, but did the rest
		assertGuidsNetworkValue(radioController, "0x101", 1);
		assertGuidsNetworkValue(radioController, "0x102", 2);
		assertGuidsNetworkValue(radioController, "0x103", 3);
		
		add10MockDevices(radioController, 111);
		add10MockDevices(radioController, 121);
		add10MockDevices(radioController, 131);
		add10MockDevices(radioController, 141);
		add10MockDevices(radioController, 151);
		add10MockDevices(radioController, 161);
		add10MockDevices(radioController, 171);
		add10MockDevices(radioController, 181);
		add10MockDevices(radioController, 191); // this ends at"0x200"

		// 191 faced no 91 conflict yet, so it will have the x91 value (145)
		assertGuidsNetworkValue(radioController, "0x191", 145);

		// lots of searching for unused addresses
		add10MockDevices(radioController, 201);
		add10MockDevices(radioController, 211);
		add10MockDevices(radioController, 221);
		add10MockDevices(radioController, 231);
		add10MockDevices(radioController, 241);
		add10MockDevices(radioController, 251);
		add10MockDevices(radioController, 261);
		add10MockDevices(radioController, 271);
		add10MockDevices(radioController, 281);
		add10MockDevices(radioController, 291); // up to 200 filled slots now

		// fill to 250
		add10MockDevices(radioController, 301);
		add10MockDevices(radioController, 311);
		add10MockDevices(radioController, 321);
		add10MockDevices(radioController, 331);
		add10MockDevices(radioController, 341);

		// and beyond
		add10MockDevices(radioController, 901);

		// The last few will have all gotten net address 127
		assertGuidsNetworkValue(radioController, "0x901", 251);
		assertGuidsNetworkValue(radioController, "0x902", 252);
		assertGuidsNetworkValue(radioController, "0x903", 253);
		assertGuidsNetworkValue(radioController, "0x904", 254);
		assertGuidsNetworkValue(radioController, "0x905", 254);

		// We wanted to give out 127 or 254 here, not 255. Because 255 broadcast spoils the network for all CHE.
		assertGuidsNetworkValue(radioController, "0x906", 254);
		assertGuidsNetworkValue(radioController, "0x907", 254);
		assertGuidsNetworkValue(radioController, "0x908", 254);
		assertGuidsNetworkValue(radioController, "0x909", 254);
		assertGuidsNetworkValue(radioController, "0x910", 254); 

	}

}
