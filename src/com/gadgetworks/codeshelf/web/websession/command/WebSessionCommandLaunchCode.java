/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandLaunchCode.java,v 1.1 2012/02/21 08:36:00 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.persist.User;

/**
 * @author jeffw
 *
 */
public class WebSessionCommandLaunchCode extends WebSessionCommandABC {

	private static final String	LAUNCH_CODE			= "LAUNCH_CODE";

	private static final String	SUCCEED				= "SUCCEED";
	private static final String	FAIL				= "FAIL";
	private static final String	NEED_LOGIN			= "NEED_LOGIN";

	private String				mLaunchCode;

	public WebSessionCommandLaunchCode(final String inCommandId, final JsonNode inDataNodeAsJson) {
		super(inCommandId, inDataNodeAsJson);

		JsonNode launchNode = inDataNodeAsJson.get("launchCode");
		mLaunchCode = launchNode.getTextValue();
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE;
	}

	protected final IWebSessionCommand doExec() {
		IWebSessionCommand result = null;

		String authenticateResult = FAIL;

		// Search for a user with the specified ID (that has no password).
		User user = User.DAO.findById(mLaunchCode);

		// CRITICAL SECURITY CONCEPT.
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).
		if ((user != null) && (user.getActive())) {
			if (user.getHashedPassword() != null) {
				authenticateResult = NEED_LOGIN;
			} else {
				authenticateResult = SUCCEED;
			}
		}

		result = new WebSessionCommandLaunchCodeResp(authenticateResult);

		return result;
	}

	protected void prepareDataNode(ObjectNode inOutDataNode) {
		// TODO Auto-generated method stub
	}
}
