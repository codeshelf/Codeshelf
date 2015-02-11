/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetworkId.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.bitfields.OutOfRangeException;

@SuppressWarnings("serial")
public final class NetworkId extends NBitInteger {

	/** 
	 *  --------------------------------------------------------------------------
	 *  Create an n-bit integer that represents a network address.
	 *  @param inNewValue
	 *  @throws OutOfRangeException
	 */

	public NetworkId(final byte inNewValue) { // OutOfRangeException {
		super(IPacket.NETWORK_NUMBER_BITS, inNewValue);
	}

}
