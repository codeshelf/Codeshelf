/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IllegalBoundsException.java,v 1.2 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.bitfields;

public class IllegalBoundsException extends RuntimeException {
	
	static final long serialVersionUID = 0;

	public IllegalBoundsException(final String inMessage) {
		super(inMessage);
	}
}
