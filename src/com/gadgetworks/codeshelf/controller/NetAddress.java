/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetAddress.java,v 1.2 2011/01/21 01:12:12 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;


public final class NetAddress extends NetParameter {

	public static final int	NET_ADDRESS_BYTES	= 3;

	public NetAddress(final byte[] inNetAddress) {
		super(inNetAddress, NET_ADDRESS_BYTES);
	}
}
