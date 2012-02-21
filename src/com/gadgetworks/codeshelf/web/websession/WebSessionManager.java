/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionManager.java,v 1.3 2012/02/21 02:45:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.persist.Organization;
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
		createUser("1234", "passowrd");
		createUser("12345", null);
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
	
	private void createUser(String userID, String password) {
		Organization organization = Organization.DAO.findById(userID);
		if (organization == null) {
			organization = new Organization();
			organization.setId(userID);
			try {
				Organization.DAO.store(organization);
			} catch (DAOException e) {
				e.printStackTrace();
			}
		}

		User user = User.DAO.findById(userID);
		if (user == null) {
			user = new User();
			user.setActive(true);
			user.setId(userID);
			if (password != null) {
				user.setHashedPassword(password);
			}
			user.setparentOrganization(organization);
			try {
				User.DAO.store(user);
			} catch (DAOException e) {
				e.printStackTrace();
			}
		}
	}
}
