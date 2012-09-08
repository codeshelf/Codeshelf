/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAddress.java,v 1.4 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;


public final class NetAddress extends NetParameter {

	public static final int	NET_ADDRESS_BYTES	= 3;

	public NetAddress(final String inNetAddress) {
		super(inNetAddress, NET_ADDRESS_BYTES);
	}
	
	public NetAddress(final byte[] inNetAddress) {
		super(inNetAddress, NET_ADDRESS_BYTES);
	}
}
