/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetEndpoint.java,v 1.2 2013/02/20 20:39:00 jeffw Exp $
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

	public static final byte		MGMT_ENDPOINT_NUM		= 0x00;
	public static final byte		PRIMARY_ENDPOINT_NUM	= 0x01;
	public static final byte		BROADCAST_ENDPOINT_NUM	= 0x0F;

	public static final NetEndpoint	MGMT_ENDPOINT			= new NetEndpoint(MGMT_ENDPOINT_NUM);
	public static final NetEndpoint	PRIMARY_ENDPOINT		= new NetEndpoint(PRIMARY_ENDPOINT_NUM);
	public static final NetEndpoint	BROADCAST_ENDPOINT		= new NetEndpoint(BROADCAST_ENDPOINT_NUM);

	public static final byte		ENDPOINT_BITS			= 4;
	public static final byte		ENDPOINT_PAD_BITS		= 4;

	private static final long		serialVersionUID		= 5160779892712753154L;

	public NetEndpoint(final byte inNewValue) { // throws OutOfRangeException {
		super(ENDPOINT_BITS, inNewValue);
	}

}
