/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionCmdLaunchCodeCheck.java,v 1.1 2012/02/24 07:41:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.gadgetworks.codeshelf.model.persist.Organization;
import com.gadgetworks.codeshelf.model.persist.User;

/**
 * @author jeffw
 *
 */
public class WebSessionCmdLaunchCodeCheck extends WebSessionCmdABC {

	private static final String	LAUNCH_CODE			= "LAUNCH_CODE";

	private static final String	SUCCEED				= "SUCCEED";
	private static final String	FAIL				= "FAIL";
	private static final String	NEED_LOGIN			= "NEED_LOGIN";

	private String				mLaunchCode;

	public WebSessionCmdLaunchCodeCheck(final String inCommandId, final JsonNode inDataNodeAsJson) {
		super(inCommandId, inDataNodeAsJson);

		JsonNode launchNode = inDataNodeAsJson.get("launchCode");
		mLaunchCode = launchNode.getTextValue();
	}

	public final WebSessionCmdEnum getCommandEnum() {
		return WebSessionCmdEnum.LAUNCH_CODE_CHECK;
	}

	protected final IWebSessionCmd doExec() {
		IWebSessionCmd result = null;

		String authenticateResult = FAIL;

		// Search for a user with the specified ID (that has no password).
		User user = User.DAO.findById(mLaunchCode);
		Organization organization = null;

		// CRITICAL SECURITY CONCEPT.
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).
		if ((user != null) && (user.getActive())) {
			if (user.getHashedPassword() != null) {
				authenticateResult = NEED_LOGIN;
			} else {
				authenticateResult = SUCCEED;
				organization = user.getParentOrganization();
			}
		}

		result = new WebSessionCmdLaunchCodeResp(authenticateResult, organization);

		return result;
	}

	protected void prepareDataNode(ObjectNode inOutDataNode) {
		// TODO Auto-generated method stub
	}
}
