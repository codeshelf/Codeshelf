/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdLogin.java,v 1.4 2012/11/24 04:23:54 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdLogin;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: LOGIN_REQ,
 * 	data {
 * 		userid: <userid>
 * 		password: <password>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class WebSessionReqCmdLogin extends WebSessionReqCmdABC {

	private ITypedDao<Organization>	mOrganizationDao;

	public WebSessionReqCmdLogin(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Organization> inOrganizationDao) {
		super(inCommandId, inDataNodeAsJson);
		mOrganizationDao = inOrganizationDao;
	}

	public final WebSessionReqCmdEnum getCommandEnum() {
		return WebSessionReqCmdEnum.LOGIN_REQ;
	}

	protected final IWebSessionRespCmd doExec() {
		IWebSessionRespCmd result = null;

		String authenticateResult = FAIL;

		// Search for a user with the specified ID (that has no password).

		JsonNode organizationIdNode = getDataJsonNode().get("organizationId");
		String organizationId = organizationIdNode.getTextValue();
		Organization organization = mOrganizationDao.findByDomainId(null, organizationId);

		// CRITICAL SECURITY CONCEPT.
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).
		if (organization != null) {
			// Find the user.

			JsonNode userIdNode = getDataJsonNode().get("userId");
			String userId = userIdNode.getTextValue();
			User user = organization.getUser(userId);
			if (user != null) {
				JsonNode passwordNode = getDataJsonNode().get("password");
				String password = passwordNode.getTextValue();
				if (user.isPasswordValid(password)) {
					authenticateResult = SUCCEED;
				}
			}
		}

		// Sleep for two seconds to slow down brute-force attacks.
		if (!authenticateResult.equals(SUCCEED)) {
			organization = null;
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}

		result = new WebSessionRespCmdLogin(authenticateResult, organization);

		return result;
	}
}
