/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetCommandId.java,v 1.2 2013/07/22 04:30:18 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import com.gadgetworks.flyweight.bitfields.NBitInteger;

/** 
 *  --------------------------------------------------------------------------
 *  Create an 8-bit integer that represents a command ID within a command group.
 *  @param inNewValue
 */

public class NetCommandId extends NBitInteger {

	public static final byte	COMMANDID_BITS	= 8;

	public NetCommandId() {
		super(COMMANDID_BITS);
	}

	public NetCommandId(final byte inNewValue) {
		super(COMMANDID_BITS, inNewValue);
	}

}
