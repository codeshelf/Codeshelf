/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *******************************************************************************/

package com.codeshelf.flyweight.controller;

import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
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
	private IRadioController radioController = null;
	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(RadioControllerTest.class);

	@Before
	public void init(){
		radioController = new RadioController(Mockito.mock(IGatewayInterface.class));
		radioController.setProtocolVersion("1");
	}
	
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
		//if (startValue % 10 != 1) {
		//	LOGGER.error("unsupported use of this function");
		//	return;
		//}
		for (int count = 0; count < 10; count++) {
			Integer value = startValue + count;
				String guidStr = "0x" + value.toString();
				addMockDevice(radioController, guidStr);
		}
	}
	
	/**
	 * Pretty kludge little thing, and dumb, which is good enough for testing as it forces rollover.
	 * Anyway, say start 1, and it gives you addresses 1-10. 
	 */
	private void add301HexMockDevices(IRadioController radioController, int startValue) {

		for (int count = 0; count < 301; count++) {
			Integer value = startValue + count;
				String guidStr = Integer.toHexString(value);
				addMockDevice(radioController, guidStr);
		}
	}

	private void assertDeviceNetAddress(INetworkDevice testDevice, int expectedValue) {
		Integer valueDevice1 = testDevice.getAddress().getValue();
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
		addMockDevice(radioController, "0x90002");
		addMockDevice(radioController, "0x90001");
		addMockDevice(radioController, "0x9FFFF");
		addMockDevice(radioController, "0xA0001");
		addMockDevice(radioController, "0xAFFFE");
		addMockDevice(radioController, "0xBFFFE");
		INetworkDevice testDevice1 = radioController.getNetworkDevice(new NetGuid("0x90002"));
		Assert.assertNotNull(testDevice1);
		INetworkDevice testDevice2 = radioController.getNetworkDevice(new NetGuid("0x90001"));
		Assert.assertNotNull(testDevice2);
		INetworkDevice testDevice3 = radioController.getNetworkDevice(new NetGuid("0x9FFFF"));
		Assert.assertNotNull(testDevice3);
		INetworkDevice testDevice4 = radioController.getNetworkDevice(new NetGuid("0xA0001"));
		Assert.assertNotNull(testDevice4);
		INetworkDevice testDevice5 = radioController.getNetworkDevice(new NetGuid("0xAFFFE"));
		Assert.assertNotNull(testDevice5);
		INetworkDevice testDevice6 = radioController.getNetworkDevice(new NetGuid("0xBFFFE"));
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
		assertDeviceNetAddress(testDevice5, 65534);

		// 6th device tries for 254. Rolls over, and must take 5
		assertDeviceNetAddress(testDevice6, 5);

	}

	@Test
	public void testRadioController3() {
		// Check our 00 cases.
		addMockDevice(radioController, "0x70000");
		addMockDevice(radioController, "0x80000");
		addMockDevice(radioController, "0x90000");
		addMockDevice(radioController, "0x9FFFF");
		INetworkDevice testDevice1 = radioController.getNetworkDevice(new NetGuid("0x70000"));
		Assert.assertNotNull(testDevice1);
		INetworkDevice testDevice2 = radioController.getNetworkDevice(new NetGuid("0x80000"));
		Assert.assertNotNull(testDevice2);
		INetworkDevice testDevice3 = radioController.getNetworkDevice(new NetGuid("0x90000"));
		Assert.assertNotNull(testDevice3);
		INetworkDevice testDevice4 = radioController.getNetworkDevice(new NetGuid("0x9FFFF"));
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
		addMockDevice(radioController, "0x70001"); // gives out 1
		addMockDevice(radioController, "0x70002");// gives out 2
		// skip 3
		addMockDevice(radioController, "0x70004");// gives out 4
		addMockDevice(radioController, "0x7FFFE");// gives out 254
		addMockDevice(radioController, "0x8FFFD");// gives out 253
		addMockDevice(radioController, "0x9FFFD");// should wrap around, and find 3 as the first hole
	
		assertGuidsNetworkValue(radioController, "0x70001", 1);
		assertGuidsNetworkValue(radioController, "0x70002", 2);
		assertGuidsNetworkValue(radioController, "0x70004", 4);
		assertGuidsNetworkValue(radioController, "0x7FFFE", 65534);
		assertGuidsNetworkValue(radioController, "0x8FFFD", 65533);
		assertGuidsNetworkValue(radioController, "0x9FFFD", 3);

	}


	@Test
	public void testRadioController4() {
		
		// Check the very full cases
		// Fill up bottom 300 and top 300 addresses

		// This is going to fill up 1-10,11.20, etc. but skip over 0a, ob, oc, etc.
		add10MockDevices(radioController, 10001); // yields "0x101" though "0x110"
		// show that this skipped 104, but did the rest
		assertGuidsNetworkValue(radioController, "0x10001", 1);
		assertGuidsNetworkValue(radioController, "0x10002", 2);
		assertGuidsNetworkValue(radioController, "0x10003", 3);
		
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
		assertGuidsNetworkValue(radioController, "0x191", 401);

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

		// fill to 300
		add10MockDevices(radioController, 301);
		add10MockDevices(radioController, 311);
		add10MockDevices(radioController, 321);
		add10MockDevices(radioController, 331);
		add10MockDevices(radioController, 341);
		add10MockDevices(radioController, 351);
		add10MockDevices(radioController, 361);
		add10MockDevices(radioController, 371);
		add10MockDevices(radioController, 381);
		add10MockDevices(radioController, 391);

		// Fill up top 301
		add301HexMockDevices(radioController, 0xFED3);

		
		// The last few will have all gotten net address 127
		assertGuidsNetworkValue(radioController, "0xFED4", 65236);
		assertGuidsNetworkValue(radioController, "0xFFFB", 65531);
		assertGuidsNetworkValue(radioController, "0xFFFC", 65532);
		assertGuidsNetworkValue(radioController, "0xFFFD", 65533);
		assertGuidsNetworkValue(radioController, "0xFFFE", 65534);
		
		// We wanted to give out 127 or 254 here, not 255. Because 255 broadcast spoils the network for all CHE.
		// We wanted to give out 655534. Because 65535 broadcast spoils the network for all CHEs
		assertGuidsNetworkValue(radioController, "0xFFFF", 10);
	}

}
