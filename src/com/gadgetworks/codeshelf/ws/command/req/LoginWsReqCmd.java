/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LoginWsReqCmd.java,v 1.2 2013/04/07 07:14:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.LoginWsRespCmd;
import com.gadgetworks.codeshelf.ws.command.resp.LoginWsRespCmdTest;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: LOGIN_REQ,
 * 	data {
 * 		organizationid: <ordid>
 * 		userid: <userid>
 * 		password: <password>
 * 	}
 * }
 * 
 * @author jeffw
 *
 */
public class LoginWsReqCmd extends WsReqCmdABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LoginWsReqCmd.class);

	private ITypedDao<Organization>	mOrganizationDao;

	public LoginWsReqCmd(final String inCommandId, final JsonNode inDataNodeAsJson, final ITypedDao<Organization> inOrganizationDao) {
		super(inCommandId, inDataNodeAsJson);
		mOrganizationDao = inOrganizationDao;
	}

	public final WsReqCmdEnum getCommandEnum() {
		return WsReqCmdEnum.LOGIN_REQ;
	}

	protected final IWsRespCmd doExec() {
		IWsRespCmd result = null;

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
					result = new LoginWsRespCmd(authenticateResult, organization, user);
				}
//				LOGGER.warn("Login " + authenticateResult + " for user: " + user.getDomainId());
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
		if (result == null) {
			result = new LoginWsRespCmd(authenticateResult, organization, null);
		}


		return result;
	}
}
