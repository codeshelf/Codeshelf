/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandLaunch.java,v 1.1 2012/02/05 08:41:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;


/**
 * @author jeffw
 *
 */
public class WebSessionCommandLaunch implements IWebSessionCommand {
	
	public WebSessionCommandLaunch(JsonNode inDetailsAsJson) {
		
	}

	@Override
	public WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE;
	}

}
