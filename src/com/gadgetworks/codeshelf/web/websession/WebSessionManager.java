/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionManager.java,v 1.2 2012/02/07 08:17:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.persist.User;
import com.gadgetworks.codeshelf.web.websocket.WebSocket;

/**
 * @author jeffw
 *
 */
public class WebSessionManager {

	private static final Log			LOGGER	= LogFactory.getLog(WebSessionManager.class);

	private Map<WebSocket, WebSession>	mWebSessions;

	public WebSessionManager() {
		mWebSessions = new HashMap<WebSocket, WebSession>();

		// Create two dummy users for testing.
		User user1 = User.DAO.findById("1234");
		if (user1 == null) {
			user1 = new User();
			user1.setActive(true);
			user1.setId("1234");
			user1.setHashedPassword("TEST");
			try {
				User.DAO.store(user1);
			} catch (DAOException e) {
				e.printStackTrace();
			}
		}

		User user2 = User.DAO.findById("12345");
		if (user2 == null) {
			user2 = new User();
			user2.setActive(true);
			user2.setId("12345");
			try {
				User.DAO.store(user2);
			} catch (DAOException e) {
				e.printStackTrace();
			}
		}
	}

	public final void handleSessionOpen(WebSocket inWebSocket) {
		// First check to see if we already have this web socket in our session map.
		if (mWebSessions.containsKey(inWebSocket)) {
			LOGGER.error("Opening new web socket for session that exists!");
			// Don't remove it, because it could be a security risk (someone else may be trying to masquerade).
		} else {
			WebSession webSession = new WebSession(inWebSocket);
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
