/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetGuid.java,v 1.1 2013/02/27 22:06:27 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.command;

public final class NetGuid extends NetParameter {

	public static final int	NET_GUID_BYTES		= 4;
	public static final int	NET_GUID_HEX_CHARS	= 8;

	public NetGuid(final String inGuidStr) {
		super(inGuidStr, NET_GUID_BYTES);
	}

	public NetGuid(final byte[] inGuidBytes) {
		super(inGuidBytes, NET_GUID_BYTES);
	}
}
