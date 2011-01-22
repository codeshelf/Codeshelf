/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetworkId.java,v 1.4 2011/01/22 07:58:31 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

public final class NetworkId extends NetParameter {

	public static final int	NETWORK_ID_BYTES	= 2;

	public NetworkId(final String inNetworkId) {
		super(inNetworkId, NETWORK_ID_BYTES);
	}
	
	public NetworkId(final byte[] inNetworkId) {
		super(inNetworkId, NETWORK_ID_BYTES);
	}
}
