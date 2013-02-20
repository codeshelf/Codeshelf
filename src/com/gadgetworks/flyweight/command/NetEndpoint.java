/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetEndpoint.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import com.gadgetworks.flyweight.bitfields.NBitInteger;

// --------------------------------------------------------------------------
/** 
 *  --------------------------------------------------------------------------
 *  Create an n-bit integer that represents a network endpoint.
 *  @param inNewValue
 *  @author jeffw
 */

public class NetEndpoint extends NBitInteger {

	public static final byte		MGMT_ENDPOINT_NUM			= 0x00;
	public static final byte		AUDIO_ENDPOINT_NUM			= 0x01;
	public static final byte		MOTOR1_CONTROL_ENDPOINT_NUM	= 0x02;
	public static final byte		MOTOR2_CONTROL_ENDPOINT_NUM	= 0x03;
	
	// On a HooBee Orb
	public static final byte		MOOD_LED_ENDPOINT_NUM		= 0x04;
	
	// On a HooBee PhotoJack
	public static final byte		SDCARD_ENDPOINT_NUM			= 0x01;
	
	public static final byte		BROADCAST_ENDPOINT_NUM		= 0x0F;

	public static final NetEndpoint	MGMT_ENDPOINT				= new NetEndpoint(MGMT_ENDPOINT_NUM);
	public static final NetEndpoint	AUDIO_ENDPOINT				= new NetEndpoint(AUDIO_ENDPOINT_NUM);
	public static final NetEndpoint	MOTOR1_CONTROL_ENDPOINT		= new NetEndpoint(MOTOR1_CONTROL_ENDPOINT_NUM);
	public static final NetEndpoint	MOTOR2_CONTROL_ENDPOINT		= new NetEndpoint(MOTOR2_CONTROL_ENDPOINT_NUM);
	public static final NetEndpoint	MOOD_LED_ENDPOINT			= new NetEndpoint(MOOD_LED_ENDPOINT_NUM);
	public static final NetEndpoint	SDCARD_ENDPOINT				= new NetEndpoint(SDCARD_ENDPOINT_NUM);
	public static final NetEndpoint	BROADCAST_ENDPOINT			= new NetEndpoint(BROADCAST_ENDPOINT_NUM);

	public static final byte		ENDPOINT_BITS				= 4;
	public static final byte		ENDPOINT_PAD_BITS			= 4;

	private static final long	serialVersionUID	= 5160779892712753154L;
	
	public NetEndpoint(final byte inNewValue) { // throws OutOfRangeException {
		super(ENDPOINT_BITS, inNewValue);
	}

}
