/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSession.java,v 1.2 2012/02/05 08:41:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCommand;

/**
 * @author jeffw
 *
 */
public class WebSession {

	private static final Log	LOGGER	= LogFactory.getLog(WebSessionManager.class);

	private WebSessionStateEnum	mState;
	
	public WebSession() {
		mState = WebSessionStateEnum.INVALID;
	}

	public final void processMessage(String inMessage) {
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode rootNode = mapper.readTree(inMessage);
			IWebSessionCommand command = WebSessionCommandFactory.createWebSessionCommand(rootNode);
			LOGGER.debug(command);
		} catch (JsonProcessingException e) {
			LOGGER.debug("", e);
		} catch (IOException e) {
			LOGGER.debug("", e);
		}
	}
}
