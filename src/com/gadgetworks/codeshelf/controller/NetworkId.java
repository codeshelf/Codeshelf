/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetworkId.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

public final class NetworkId extends NetParameter {

	private static final int	NETWORK_ID_BYTES	= 2;

	public NetworkId(final byte[] inNetworkId) {
		super(inNetworkId, NETWORK_ID_BYTES);
	}
}
