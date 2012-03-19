/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionManager.java,v 1.6 2012/03/19 04:05:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.web.websession.command.req.WebSessionReqCmdFactory;
import com.gadgetworks.codeshelf.web.websocket.WebSocket;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author jeffw
 *
 */
@Singleton
public class WebSessionManager implements IWebSessionManager {

	private static final Log			LOGGER	= LogFactory.getLog(WebSessionManager.class);

	private Map<WebSocket, WebSession>	mWebSessions;
	private WebSessionReqCmdFactory		mWebSessionReqCmdFactory;

	@Inject
	public WebSessionManager(final WebSessionReqCmdFactory inWebSessionReqCmdFactory) {
		mWebSessions = new HashMap<WebSocket, WebSession>();
		mWebSessionReqCmdFactory = inWebSessionReqCmdFactory;
	}

	public final void handleSessionOpen(WebSocket inWebSocket) {
		// First check to see if we already have this web socket in our session map.
		if (mWebSessions.containsKey(inWebSocket)) {
			LOGGER.error("Opening new web socket for session that exists!");
			// Don't remove it, because it could be a security risk (someone else may be trying to masquerade).
		} else {
			WebSession webSession = new WebSession(inWebSocket, mWebSessionReqCmdFactory);
			mWebSessions.put(inWebSocket, webSession);
		}
	}

	public final void handleSessionClose(WebSocket inWebSocket) {
		// First check to see if we already have this web socket in our session map.
		if (!mWebSessions.containsKey(inWebSocket)) {
			LOGGER.error("Closing web socket for session that doesn't exist!");
		} else {
			mWebSessions.remove(inWebSocket);
		}
	}

	public final void handleSessionMessage(WebSocket inWebSocket, String inMessage) {
		WebSession webSession = mWebSessions.get(inWebSocket);
		if (webSession != null) {
			webSession.processMessage(inMessage);
		}
	}
}
