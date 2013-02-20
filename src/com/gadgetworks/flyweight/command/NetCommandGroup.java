/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetCommandGroup.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

import com.gadgetworks.flyweight.bitfields.NBitInteger;

/** 
 *  --------------------------------------------------------------------------
 *  Create an n-bit integer that represents a network address.
 *  @param inNewValue
 *  @throws IllegalBoundsException
 *  @throws OutOfRangeException
 */

public final class NetCommandGroup extends NBitInteger {

	private static final byte	BITS_CMD_GROUP	= 4;

	public NetCommandGroup(final CommandGroupEnum inCommandEnum) {
		super(BITS_CMD_GROUP, inCommandEnum.getValue());
	}

	public CommandGroupEnum getCommandEnum() {
		return CommandGroupEnum.getCommandGroupEnum(getValue());
	}
}
