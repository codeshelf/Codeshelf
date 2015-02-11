/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ArgsClass.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.ws.jetty.protocol.command;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jeffw
 *
 */
public class ArgsClass {
	@Getter
	@Setter
	private String		name		= "";

	@Getter
	@Setter
	private Object	value;

	@Getter
	@Setter
	private String		classType	= "";

	public ArgsClass() {
	}

	public ArgsClass(final String inName, final Object inValue, final String inClassType) {
		name = inName;
		value = inValue;
		classType = inClassType;
	}
}
