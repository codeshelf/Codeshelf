/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetChannelValue.java,v 1.2 2013/07/22 04:30:18 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import com.gadgetworks.flyweight.bitfields.NBitInteger;

/** 
 *  --------------------------------------------------------------------------
 *  Create an 8-bit integer that represents an unsigned channel energy or link quality value.
 *  @param inNewValue
 */

public class NetChannelValue extends NBitInteger {

	public static final byte	CHANNEL_ENERGY_BITS	= 8;

	public NetChannelValue() {
		super(CHANNEL_ENERGY_BITS);
	}

	public NetChannelValue(final byte inNewValue) {
		super(CHANNEL_ENERGY_BITS, inNewValue);
	}

}
