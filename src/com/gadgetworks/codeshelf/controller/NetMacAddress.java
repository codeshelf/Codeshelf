/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetMacAddress.java,v 1.2 2011/01/25 02:10:59 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;


public final class NetMacAddress extends NetParameter {

	public static final int	NET_MACADDR_BYTES	= 8;

	public NetMacAddress(final String inNetMacAddress) {
		super(inNetMacAddress, NET_MACADDR_BYTES);
	}
	
	public NetMacAddress(final byte[] inNetMacAddress) {
		super(inNetMacAddress, NET_MACADDR_BYTES);
	}
}
