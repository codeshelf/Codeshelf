/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSession.java,v 1.1 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.shiro.realm.Realm;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.command.req.IWsPersistentReqCmd;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmd;
import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmdFactory;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * @author jeffw
 *
 */
public class WebSession implements IWebSession, IDaoListener {

	private static final Logger					LOGGER						= LoggerFactory.getLogger(WebSession.class);

	private WebSessionStateEnum					mState;
	private Realm								mSecurityRealm;
	private WebSocket							mWebSocket;
	private IWsReqCmdFactory					mWebSessionReqCmdFactory;
	private Map<String, IWsPersistentReqCmd>	mPersistentCommands;
	private AtomicLong							mLastPongTime = new AtomicLong();
	private ConnectionActivityStatus			mConnectionActivityStatus	= ConnectionActivityStatus.ACTIVE;

	@Inject
	public WebSession(@Assisted final WebSocket inWebSocket,
		@Assisted final IWsReqCmdFactory inWebSessionReqCmdFactory,
		final Realm inRealm) {
		mWebSocket = inWebSocket;
		mWebSessionReqCmdFactory = inWebSessionReqCmdFactory;
		mSecurityRealm = inRealm;
		mState = WebSessionStateEnum.INVALID;
		mPersistentCommands = new ConcurrentHashMap<String, IWsPersistentReqCmd>();
	}

	public final IWsRespCmd processMessage(String inMessage) {

		IWsRespCmd result = null;

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(inMessage);
			IWsReqCmd command = mWebSessionReqCmdFactory.createWebSessionCommand(rootNode);
			LOGGER.debug(command.toString());

			result = command.exec();

			// Some commands persist, and we use them to respond to data changes.
			if (command instanceof IWsPersistentReqCmd) {
				IWsPersistentReqCmd persistCmd = (IWsPersistentReqCmd) command;
				// If the command is already in the map then remove it, otherwise add it.
				if (mPersistentCommands.get(command.getCommandId()) != null) {
					persistCmd.unregisterSessionWithDaos(this);
					mPersistentCommands.remove(command.getCommandId());
				} else {
					persistCmd.registerSessionWithDaos(this);
					mPersistentCommands.put(command.getCommandId(), persistCmd);
				}
			}

		} catch (JsonProcessingException e) {
			LOGGER.debug("", e);
		} catch (IOException e) {
			LOGGER.debug("", e);
		}

		return result;
	}

	public final void endSession() {
		for (IWsPersistentReqCmd command : mPersistentCommands.values()) {
			command.unregisterSessionWithDaos(this);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * The the response command back over the WebSession's WebSocket.
	 * @param inCommand
	 */
	public final void sendCommand(IWsRespCmd inCommand) {
		String message = inCommand.getResponseMsg();
		LOGGER.info("Sent Command: " + inCommand.getCommandId() + " Data: " + message);
		mWebSocket.send(message);
	}

	public final void objectAdded(IDomainObject inDomainObject) {
		for (IWsPersistentReqCmd command : mPersistentCommands.values()) {
			if (inDomainObject.getClass().equals(command.getPersistenceClass())) {
				IWsRespCmd respCommand = command.processObjectAdd(inDomainObject);
				if (respCommand != null) {
					respCommand.setCommandId(command.getCommandId());
					sendCommand(respCommand);
				}
			}
		}
	}

	public final void objectUpdated(IDomainObject inDomainObject, Set<String> inChangedProperties) {
		for (IWsPersistentReqCmd command : mPersistentCommands.values()) {
			if (inDomainObject.getClass().equals(command.getPersistenceClass())) {
				IWsRespCmd respCommand = command.processObjectUpdate(inDomainObject, inChangedProperties);
				if (respCommand != null) {
					respCommand.setCommandId(command.getCommandId());
					sendCommand(respCommand);
				}
			}
		}
	}

	public final void objectDeleted(IDomainObject inDomainObject) {
		for (IWsPersistentReqCmd command : mPersistentCommands.values()) {
			if (inDomainObject.getClass().equals(command.getPersistenceClass())) {
				IWsRespCmd respCommand = command.processObjectDelete(inDomainObject);
				if (respCommand != null) {
					respCommand.setCommandId(command.getCommandId());
					sendCommand(respCommand);
				}
			}
		}
	}

	@Override
	public final void resetPongTimer() {
		mLastPongTime.set(System.currentTimeMillis());
	}

	@Override
	public final long getPongTimerElapsedMillis() {
		long pong = mLastPongTime.get();
		if (pong == 0) {
			// first time called for this websocket
			resetPongTimer();
			return 0;
		} //else
		return System.currentTimeMillis() - pong;
	}

	@Override
	public final boolean setConnectionActivityStatus(ConnectionActivityStatus inNewStatus) {
		if (mConnectionActivityStatus != inNewStatus) {
			mConnectionActivityStatus = inNewStatus;
			return true;
		} //else
		mConnectionActivityStatus = inNewStatus;
		return false;
	}
}
