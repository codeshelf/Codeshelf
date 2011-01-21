/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: OutOfRangeException.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

public class OutOfRangeException extends RuntimeException {

	static final long serialVersionUID = 0;

	public OutOfRangeException(final String inMessage) {
		super(inMessage);
	}
}
