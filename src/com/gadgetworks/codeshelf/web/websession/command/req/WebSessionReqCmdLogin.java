/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: WebSessionReqCmdLogin.java,v 1.1 2012/11/09 08:53:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.codehaus.jackson.JsonNode;

import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.WebSessionRespCmdLaunchCode;

/**
 * 
 * The format of the command is:
 * 
 * command {
 * 	id: <cmd_id>,
 * 	type: LAUNCH_CODE_CHECK,
 * 	data {
 * 		launchCode: <launch_code>
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

		JsonNode organizationIdNode = getDataJsonNode().get("organization");
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
				JsonNode passwordNode = getDataJsonNode().get("hashedPw");
				String password = passwordNode.getTextValue();
				if (user.isPasswordValid(password)) {
					authenticateResult = SUCCEED;
					
				}
			}
		}

		result = new WebSessionRespCmdLaunchCode(authenticateResult, organization);

		return result;
	}
}
