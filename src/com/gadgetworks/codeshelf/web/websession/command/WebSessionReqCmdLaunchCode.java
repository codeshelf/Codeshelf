/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdLaunchCode.java,v 1.1 2012/03/16 15:59:07 jeffw Exp $
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
public class WebSessionReqCmdLaunchCode extends WebSessionReqCmdABC {

	private static final String	LAUNCH_CODE			= "LAUNCH_CODE";

	private static final String	SUCCEED				= "SUCCEED";
	private static final String	FAIL				= "FAIL";
	private static final String	NEED_LOGIN			= "NEED_LOGIN";

	public WebSessionReqCmdLaunchCode(final String inCommandId, final JsonNode inDataNodeAsJson) {
		super(inCommandId, inDataNodeAsJson);
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.LAUNCH_CODE_CHECK;
	}

	protected final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		String authenticateResult = FAIL;

		// Search for a user with the specified ID (that has no password).

		JsonNode launchNode = getDataJsonNode().get("launchCode");
		String launchCode = launchNode.getTextValue();
		User user = User.DAO.findById(launchCode);
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

		result = new WebSessionRespCmdLaunchCode(authenticateResult, organization);

		return result;
	}
}
