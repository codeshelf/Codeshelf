/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAddress.java,v 1.3 2011/01/22 07:58:31 jeffw Exp $
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
