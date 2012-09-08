/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ArgsClass.java,v 1.2 2012/09/08 03:03:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jeffw
 *
 */
public class ArgsClass {
	@Getter
	@Setter
	private String name = "";
	@Getter
	@Setter
	private Object value = null;
	@Getter
	@Setter
	private String classType = "";
	
	public ArgsClass() {
		
	}
	
	public ArgsClass(String inName, Object inValue, String inClassType) {
		name = inName;
		value = inValue;
		classType = inClassType;
	}
}
