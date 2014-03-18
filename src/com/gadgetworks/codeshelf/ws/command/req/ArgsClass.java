/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ArgsClass.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import org.codehaus.jackson.JsonNode;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author jeffw
 *
 */
public class ArgsClass {
	@Getter
	@Setter
	@Accessors(prefix = "m")
	private String	mName		= "";

	@Getter
	@Setter
	@Accessors(prefix = "m")
	private JsonNode	mValue;

	@Getter
	@Setter
	@Accessors(prefix = "m")
	private String	mClassType	= "";

	public ArgsClass() {

	}

	public ArgsClass(final String inName, final JsonNode inValue, final String inClassType) {
		mName = inName;
		mValue = inValue;
		mClassType = inClassType;
	}
}
