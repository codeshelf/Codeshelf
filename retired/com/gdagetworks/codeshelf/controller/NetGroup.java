/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: NetGroup.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;


public final class NetGroup extends NetParameter {

	public static final int	NET_GROUP_BYTES	= 2;

	public NetGroup(final String inNetGroup) {
		super(inNetGroup, NET_GROUP_BYTES);
	}
	
	public NetGroup(final byte[] inNetGroup) {
		super(inNetGroup, NET_GROUP_BYTES);
	}
}
