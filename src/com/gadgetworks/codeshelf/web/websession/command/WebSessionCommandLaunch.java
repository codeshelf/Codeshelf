/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCommandLaunch.java,v 1.2 2012/02/07 08:17:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.persist.User;

/**
 * @author jeffw
 *
 */
public class WebSessionCommandLaunch implements IWebSessionCommand {

	private static final String	LAUNCH_CODE	= "LAUNCH_CODE";

	private String				mLaunchCode;

	public WebSessionCommandLaunch(final JsonNode inDetailsAsJson) {
		JsonNode launchNode = inDetailsAsJson.get("launchCode");
		mLaunchCode = launchNode.getTextValue();
	}

	public final WebSessionCommandEnum getCommandEnum() {
		return WebSessionCommandEnum.LAUNCH_CODE;
	}

	public final String exec() {
		String result = IWebSessionCommand.FAIL;

		// Search for a user with the specified ID (that has no password).
		User user = User.DAO.findById(mLaunchCode);

		// CRITICAL SECURITY CONCEPT.
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).
		if ((user != null) && (user.getActive())) {
			if (user.getHashedPassword() != null) {
				result = IWebSessionCommand.NEED_LOGIN;
			} else {
				result = IWebSessionCommand.SUCCEED;
			}
		}

		return result;
	}

}
