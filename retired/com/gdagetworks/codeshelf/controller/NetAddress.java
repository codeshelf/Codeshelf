/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAddress.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
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
