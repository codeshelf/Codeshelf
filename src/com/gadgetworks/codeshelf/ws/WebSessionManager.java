/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionManager.java,v 1.1 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws;

import java.util.HashMap;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.ws.IWebSession.ConnectionActivityStatus;
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

	private static final Logger			LOGGER	= LoggerFactory.getLogger(WebSessionManager.class);

	private Map<WebSocket, IWebSession>	mWebSessions;
	private IWsReqCmdFactory			mWebSessionReqCmdFactory;
	private IWebSessionFactory			mWebSessionFactory;

	@Inject
	public WebSessionManager(final IWsReqCmdFactory inWebSessionReqCmdFactory, final IWebSessionFactory inWebSessionFactory) {
		mWebSessions = new HashMap<WebSocket, IWebSession>();
		mWebSessionReqCmdFactory = inWebSessionReqCmdFactory;
		mWebSessionFactory = inWebSessionFactory;
	}

	public final void handleSessionOpen(WebSocket inWebSocket) {
		// First check to see if we already have this web socket in our session map.
		if (mWebSessions.containsKey(inWebSocket)) {
			LOGGER.error("Opening new web socket for session that exists!");
			// Don't remove it, because it could be a security risk (someone else may be trying to masquerade).
		} else {
			IWebSession webSession = mWebSessionFactory.create(inWebSocket, mWebSessionReqCmdFactory);
			mWebSessions.put(inWebSocket, webSession);
		}
	}

	public final void handleSessionClose(org.java_websocket.WebSocket inWebSocket) {
		// First check to see if we already have this web socket in our session map.
		if (!mWebSessions.containsKey(inWebSocket)) {
			LOGGER.error("Closing web socket for session that doesn't exist!");
		} else {
			IWebSession session = mWebSessions.get(inWebSocket);
			session.endSession();
			mWebSessions.remove(inWebSocket);
		}
	}

	public final void handleSessionMessage(org.java_websocket.WebSocket inWebSocket, String inMessage) {
		IWebSession webSession = mWebSessions.get(inWebSocket);
		if (webSession != null) {
			IWsRespCmd respCommand = webSession.processMessage(inMessage);
			if (respCommand != null) {
				webSession.sendCommand(respCommand);
			}
		}
	}

	@Override
	public final void handlePong(WebSocket inWebSocket) {
		IWebSession webSession = mWebSessions.get(inWebSocket);
		if (webSession == null) {
			LOGGER.warn("Got PONG on web socket for session that doesn't exist!");
		} else {
			webSession.resetPongTimer();
		}
	}

	@Override
	public final boolean checkLastPongTime(WebSocket inWebSocket, long inWarningMillis, long inErrorMillis) {
		IWebSession webSession = mWebSessions.get(inWebSocket);
		if (webSession == null) {
			LOGGER.warn("checkLastPongTime called for session that doesn't exist!");
			return false;
		} // else
		long elapsed = webSession.getPongTimerElapsedMillis();
		boolean elapsedOk = true;

		if (elapsed > inErrorMillis) {
			if (webSession.setConnectionActivityStatus(ConnectionActivityStatus.DEAD)) {
				LOGGER.warn("Connection DEAD - elapsed time since PONG " + elapsed + " ms " + inWebSocket.getRemoteSocketAddress());
			}
			elapsedOk = false;
		} else if (elapsed > inWarningMillis) {
			if (webSession.setConnectionActivityStatus(ConnectionActivityStatus.IDLE)) {
				LOGGER.info("Connection WARNING - elapsed time since PONG " + elapsed + " ms "
						+ inWebSocket.getRemoteSocketAddress());
			}
		} else {
			if (webSession.setConnectionActivityStatus(ConnectionActivityStatus.ACTIVE)) {
				LOGGER.info("Connection RECOVERED - elapsed time since PONG " + elapsed + " ms "
						+ inWebSocket.getRemoteSocketAddress());
			}
		}

		return elapsedOk;
	}
}
