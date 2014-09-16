/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAddress.java,v 1.2 2013/07/22 04:30:18 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import com.gadgetworks.flyweight.bitfields.NBitInteger;
import com.gadgetworks.flyweight.bitfields.OutOfRangeException;


@SuppressWarnings("serial")
public final class NetAddress extends NBitInteger {

	/**
	 *  Create an n-bit integer that represents a network address. 
	 *  @throws OutOfRangeException
	 */
	public NetAddress() {
		super(IPacket.ADDRESS_BITS);
	}
	
	/** 
	 *  --------------------------------------------------------------------------
	 *  Create an n-bit integer that represents a network address.
	 *  @param inNewValue
	 *  @throws OutOfRangeException
	 */

	public NetAddress(final byte inNewValue) {
		super(IPacket.ADDRESS_BITS, inNewValue);
	}
	
	public String toString() {
		return Integer.toHexString(getValue());
	}
}
