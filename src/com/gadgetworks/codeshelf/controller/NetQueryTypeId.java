/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetQueryTypeId.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class NetQueryTypeId extends NetParameter {

	private static final byte	QUERYID_BYTES	= 1;

	public NetQueryTypeId(final byte[] inNetAddress) {
		super(inNetAddress, QUERYID_BYTES);
	}
}
