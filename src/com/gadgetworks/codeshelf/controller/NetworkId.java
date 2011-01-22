/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetworkId.java,v 1.3 2011/01/22 02:06:13 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

public final class NetworkId extends NetParameter {

	public static final int	NETWORK_ID_BYTES	= 2;

	public NetworkId(final byte[] inNetworkId) {
		super(inNetworkId, NETWORK_ID_BYTES);
	}

	public NetworkId(String inNetworkIdAsShortString) {
		super(null, NETWORK_ID_BYTES);
		
		byte[] tempArray = new byte[NetworkId.NETWORK_ID_BYTES];
		short networkId = Short.parseShort(inNetworkIdAsShortString);
		tempArray[0] = (byte) (networkId >>> 8);
		tempArray[1] = (byte) (networkId);
		setParamValue(tempArray);
	}
}
