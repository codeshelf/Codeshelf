/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSession.java,v 1.14 2012/03/24 18:28:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websocket.IWebSocket;

/**
 * @author jeffw
 *
 */
public class WebSession implements IWebSession, IDaoListener {

	private static final Log				LOGGER	= LogFactory.getLog(WebSession.class);

	private WebSessionStateEnum				mState;
	private IWebSocket						mWebSocket;
	private IWebSessionReqCmdFactory		mWebSessionReqCmdFactory;
	private Map<String, IWebSessionReqCmd>	mPersistentCommands;

	public WebSession(final IWebSocket inWebSocket, final IWebSessionReqCmdFactory inWebSessionReqCmdFactory) {
		mWebSocket = inWebSocket;
		mWebSessionReqCmdFactory = inWebSessionReqCmdFactory;
		mState = WebSessionStateEnum.INVALID;
		mPersistentCommands = new HashMap<String, IWebSessionReqCmd>();
	}

	public final void processMessage(String inMessage) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(inMessage);
			IWebSessionReqCmd command = mWebSessionReqCmdFactory.createWebSessionCommand(rootNode);
			LOGGER.debug(command);

			IWebSessionRespCmd respCommand = command.exec();

			// Some commands persist, and we use them to respond to data changes.
			if (command.doesPersist()) {
				// If the command is already in the map then remove it, otherwise add it.
				if (mPersistentCommands.get(command.getCommandId()) != null) {
					command.unregisterSessionWithDaos(this);
					mPersistentCommands.remove(command.getCommandId());
				} else {
					command.registerSessionWithDaos(this);
					mPersistentCommands.put(command.getCommandId(), command);
				}
			}

			if (respCommand != null) {
				try {
					String message = respCommand.getResponseMsg();
					LOGGER.info("Sent Command: " + respCommand.getCommandId() + " Data: " + message);
					mWebSocket.send(message);
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
	
	public final void endSession() {
		for (IWebSessionReqCmd command : mPersistentCommands.values()) {
			command.unregisterSessionWithDaos(this);
		}
	}

	public final void objectAdded(Object inObject) {

	}

	public final void objectUpdated(Object inObject) {
		for (IWebSessionReqCmd command : mPersistentCommands.values()) {
//			if (command.matches(inObject)) {
				IWebSessionRespCmd respCommand = command.getResponseCmd();
				if (respCommand != null) {
					respCommand.setCommandId(command.getCommandId());
					try {
						String message = respCommand.getResponseMsg();
						LOGGER.info("Sent Command: " + respCommand.getCommandId() + " Data: " + message);
						mWebSocket.send(message);
					} catch (InterruptedException e) {
						LOGGER.error("Can't send response", e);
					}
				}
//			}
		}
	}

	public final void objectDeleted(Object inObject) {

	}
}
