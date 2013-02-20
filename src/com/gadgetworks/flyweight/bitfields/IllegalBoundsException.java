/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: IllegalBoundsException.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.bitfields;

class IllegalBoundsException extends RuntimeException {
	
	static final long serialVersionUID = 0;

	public IllegalBoundsException(final String inMessage) {
		super(inMessage);
	}
}
