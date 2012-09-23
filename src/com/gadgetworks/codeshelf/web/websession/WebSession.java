/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSession.java,v 1.21 2012/09/23 03:05:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionPersistentReqCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmd;
import com.gadgetworks.codeshelf.web.websession.command.req.IWebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websocket.IWebSocket;

/**
 * @author jeffw
 *
 */
public class WebSession implements IWebSession, IDaoListener {

	private static final Log							LOGGER	= LogFactory.getLog(WebSession.class);

	private WebSessionStateEnum							mState;
	private IWebSocket									mWebSocket;
	private IWebSessionReqCmdFactory					mWebSessionReqCmdFactory;
	private Map<String, IWebSessionPersistentReqCmd>	mPersistentCommands;

	public WebSession(final IWebSocket inWebSocket, final IWebSessionReqCmdFactory inWebSessionReqCmdFactory) {
		mWebSocket = inWebSocket;
		mWebSessionReqCmdFactory = inWebSessionReqCmdFactory;
		mState = WebSessionStateEnum.INVALID;
		mPersistentCommands = new HashMap<String, IWebSessionPersistentReqCmd>();
	}

	public final void processMessage(String inMessage) {

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(inMessage);
			IWebSessionReqCmd command = mWebSessionReqCmdFactory.createWebSessionCommand(rootNode);
			LOGGER.debug(command);

			IWebSessionRespCmd respCommand = command.exec();

			// Some commands persist, and we use them to respond to data changes.
			if (command instanceof IWebSessionPersistentReqCmd) {
				IWebSessionPersistentReqCmd persistCmd = (IWebSessionPersistentReqCmd) command;
				// If the command is already in the map then remove it, otherwise add it.
				if (mPersistentCommands.get(command.getCommandId()) != null) {
					persistCmd.unregisterSessionWithDaos(this);
					mPersistentCommands.remove(command.getCommandId());
				} else {
					persistCmd.registerSessionWithDaos(this);
					mPersistentCommands.put(command.getCommandId(), persistCmd);
				}
			}

			if (respCommand != null) {
				sendCommand(respCommand);
			}

		} catch (JsonProcessingException e) {
			LOGGER.debug("", e);
		} catch (IOException e) {
			LOGGER.debug("", e);
		}
	}

	public final void endSession() {
		for (IWebSessionPersistentReqCmd command : mPersistentCommands.values()) {
			command.unregisterSessionWithDaos(this);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The the response command back over the WebSession's WebSocket.
	 * @param inCommand
	 */
	private void sendCommand(IWebSessionRespCmd inCommand) {
		try {
			String message = inCommand.getResponseMsg();
			LOGGER.info("Sent Command: " + inCommand.getCommandId() + " Data: " + message);
			mWebSocket.send(message);
		} catch (InterruptedException e) {
			LOGGER.error("Can't send response", e);
		}
	}

	public final void objectAdded(IDomainObject inDomainObject) {
		for (IWebSessionPersistentReqCmd command : mPersistentCommands.values()) {
			if (inDomainObject.getClass().equals(command.getPersistenceClass())) {
				IWebSessionRespCmd respCommand = command.processObjectAdd(inDomainObject);
				if (respCommand != null) {
					respCommand.setCommandId(command.getCommandId());
					sendCommand(respCommand);
				}
			}
		}
	}

	public final void objectUpdated(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		for (IWebSessionPersistentReqCmd command : mPersistentCommands.values()) {
			if (inDomainObject.getClass().equals(command.getPersistenceClass())) {
				IWebSessionRespCmd respCommand = command.processObjectUpdate(inDomainObject, inChangedProperties);
				if (respCommand != null) {
					respCommand.setCommandId(command.getCommandId());
					sendCommand(respCommand);
				}
			}
		}
	}

	public final void objectDeleted(IDomainObject inDomainObject) {
		for (IWebSessionPersistentReqCmd command : mPersistentCommands.values()) {
			if (inDomainObject.getClass().equals(command.getPersistenceClass())) {
				IWebSessionRespCmd respCommand = command.processObjectDelete(inDomainObject);
				if (respCommand != null) {
					respCommand.setCommandId(command.getCommandId());
					sendCommand(respCommand);
				}
			}
		}
	}
}
