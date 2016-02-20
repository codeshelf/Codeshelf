/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAddress.java,v 1.2 2013/07/22 04:30:18 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.bitfields.OutOfRangeException;

@SuppressWarnings("serial")
public final class NetAddress extends NBitInteger {

	/**
	 *  Create an n-bit integer that represents a network address. 
	 *  @throws OutOfRangeException
	 */
	public NetAddress() {
		super((byte) 0);
	}

	/** 
	 *  --------------------------------------------------------------------------
	 *  Create an n-bit integer that represents a network address.
	 *  @param inNewValue
	 *  @throws OutOfRangeException
	 */

	public NetAddress(final short inNewValue, final byte inValueBitLength) {
		super(inValueBitLength, inNewValue);
	}
	
	/** 
	 *  --------------------------------------------------------------------------
	 *  Create an n-bit integer that represents a network address.
	 *  @param inNewValue
	 *  @throws OutOfRangeException
	 */
	
	public NetAddress(final byte inNewValue, final byte inValueBitLength) {
		super(inValueBitLength, inNewValue);
	}

	//@Override
	public String toString() {
		return Integer.toHexString(getValue());
	}

}
