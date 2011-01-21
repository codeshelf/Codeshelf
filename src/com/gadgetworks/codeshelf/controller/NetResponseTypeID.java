/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetResponseTypeID.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
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
