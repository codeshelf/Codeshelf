/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSession.java,v 1.9 2012/03/16 15:59:07 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.gadgetworks.codeshelf.model.dao.IDAOListener;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionReqCmd;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.IWebSocket;
import com.gadgetworks.codeshelf.web.websocket.WebSocket;

/**
 * @author jeffw
 *
 */
public class WebSession implements IDAOListener {

	private static final Log	LOGGER	= LogFactory.getLog(WebSession.class);

	private WebSessionStateEnum	mState;
	private IWebSocket			mWebSocket;

	public WebSession(final IWebSocket inWebSocket) {
		mWebSocket = inWebSocket;
		mState = WebSessionStateEnum.INVALID;
	}

	public final void processMessage(String inMessage) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(inMessage);
			IWebSessionReqCmd command = WebSessionReqCmdFactory.createWebSessionCommand(rootNode);
			LOGGER.debug(command);

			IWebSessionRespCmd respCommand = command.exec();

			if (respCommand != null) {
				try {
					mWebSocket.send(respCommand.getResponseMsg());
				} catch (InterruptedException e) {
					LOGGER.error("Can't send response", e);
				}
			}

		} catch (JsonProcessingException e) {
			LOGGER.debug("", e);
		} catch (IOException e) {
			LOGGER.debug("", e);
		}
	}

	@Override
	public void objectAdded(Object inObject) {
		
	}

	@Override
	public void objectUpdated(Object inObject) {
		
	}

	@Override
	public void objectDeleted(Object inObject) {
		
	}
}
