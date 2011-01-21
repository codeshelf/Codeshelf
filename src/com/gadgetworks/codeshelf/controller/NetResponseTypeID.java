/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: NetResponseTypeID.java,v 1.1 2011/01/21 01:08:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class NetResponseTypeID extends NetParameter {

	private static final int	RESPONSEID_BYTES	= 1;

	public NetResponseTypeID(final byte[] inNetAddress) {
		super(inNetAddress, RESPONSEID_BYTES);
	}
}
