/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetCommandGroup.java,v 1.2 2013/07/22 04:30:18 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.command;

import com.codeshelf.flyweight.bitfields.IllegalBoundsException;
import com.codeshelf.flyweight.bitfields.NBitInteger;
import com.codeshelf.flyweight.bitfields.OutOfRangeException;

/** 
 *  --------------------------------------------------------------------------
 *  Create an n-bit integer that represents a network address.
 *  @param inNewValue
 *  @throws IllegalBoundsException
 *  @throws OutOfRangeException
 */

@SuppressWarnings({ "serial", "unused" })
public final class NetCommandGroup extends NBitInteger {

	private static final byte	BITS_CMD_GROUP	= 4;

	public NetCommandGroup() {
		super(BITS_CMD_GROUP);
	}

	public NetCommandGroup(final CommandGroupEnum inCommandEnum) {
		super(BITS_CMD_GROUP, inCommandEnum.getValue());
	}

	public CommandGroupEnum getCommandEnum() {
		return CommandGroupEnum.getCommandGroupEnum(getValue());
	}
}
