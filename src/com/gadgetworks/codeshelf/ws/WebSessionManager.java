/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionManager.java,v 1.1 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws;

import java.util.HashMap;
import java.util.Map;

import org.java_websocket.IWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.command.req.IWsReqCmdFactory;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class WebSessionManager implements IWebSessionManager {

	private static final Logger				LOGGER	= LoggerFactory.getLogger(WebSessionManager.class);

	private Map<IWebSocket, IWebSession>	mWebSessions;
	private IWsReqCmdFactory				mWebSessionReqCmdFactory;
	private IWebSessionFactory				mWebSessionFactory;

	@Inject
	public WebSessionManager(final IWsReqCmdFactory inWebSessionReqCmdFactory, final IWebSessionFactory inWebSessionFactory) {
		mWebSessions = new HashMap<IWebSocket, IWebSession>();
		mWebSessionReqCmdFactory = inWebSessionReqCmdFactory;
		mWebSessionFactory = inWebSessionFactory;
	}

	public final void handleSessionOpen(IWebSocket inWebSocket) {
		// First check to see if we already have this web socket in our session map.
		if (mWebSessions.containsKey(inWebSocket)) {
			LOGGER.error("Opening new web socket for session that exists!");
			// Don't remove it, because it could be a security risk (someone else may be trying to masquerade).
		} else {
			IWebSession webSession = mWebSessionFactory.create(inWebSocket, mWebSessionReqCmdFactory);
			mWebSessions.put(inWebSocket, webSession);
		}
	}

	public final void handleSessionClose(org.java_websocket.IWebSocket inWebSocket) {
		// First check to see if we already have this web socket in our session map.
		if (!mWebSessions.containsKey(inWebSocket)) {
			LOGGER.error("Closing web socket for session that doesn't exist!");
		} else {
			IWebSession session = mWebSessions.get(inWebSocket);
			session.endSession();
			mWebSessions.remove(inWebSocket);
		}
	}

	public final void handleSessionMessage(org.java_websocket.IWebSocket inWebSocket, String inMessage) {
		IWebSession webSession = mWebSessions.get(inWebSocket);
		if (webSession != null) {
			IWsRespCmd respCommand = webSession.processMessage(inMessage);
			if (respCommand != null) {
				webSession.sendCommand(respCommand);
			}
		}
	}
}
