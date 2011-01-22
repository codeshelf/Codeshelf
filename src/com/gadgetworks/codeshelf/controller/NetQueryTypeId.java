/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: NetQueryTypeId.java,v 1.3 2011/01/22 07:58:31 jeffw Exp $
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
