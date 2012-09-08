/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OutOfRangeException.java,v 1.3 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

public class OutOfRangeException extends RuntimeException {

	static final long serialVersionUID = 0;

	public OutOfRangeException(final String inMessage) {
		super(inMessage);
	}
}
