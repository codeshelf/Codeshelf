/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OutOfRangeException.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.codeshelf.controller;

public class OutOfRangeException extends RuntimeException {

	static final long serialVersionUID = 0;

	public OutOfRangeException(final String inMessage) {
		super(inMessage);
	}
}
