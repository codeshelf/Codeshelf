/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetworkId.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

public final class NetworkId extends NetParameter {

	public static final int	NETWORK_ID_BYTES	= 2;

	//private static final Log	LOGGER				= LogFactory.getLog(NetworkId.class);
	
	public NetworkId() {
		super("0x0000", NETWORK_ID_BYTES);
	}

	public NetworkId(final String inNetworkId) {
		super(inNetworkId, NETWORK_ID_BYTES);
	}

	public NetworkId(final byte[] inNetworkId) {
		super(inNetworkId, NETWORK_ID_BYTES);
	}
}
