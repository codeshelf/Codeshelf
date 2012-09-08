/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetQueryTypeId.java,v 1.4 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class NetQueryTypeId extends NetParameter {

	private static final byte	QUERYID_BYTES	= 1;

	public NetQueryTypeId(final String inNetAddress) {
		super(inNetAddress, QUERYID_BYTES);
	}
}
