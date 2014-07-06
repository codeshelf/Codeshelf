package com.gadgetworks.codeshelf.ws.jetty.command;

import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;

public class LoginCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(LoginCommand.class);
	
	private LoginRequest loginRequest;
	
	@Setter
	private OrganizationDao	organizationDao;
	
	public LoginCommand(CsSession session, LoginRequest loginRequest) {
		super(session);
		this.loginRequest = loginRequest;
	}

	@Override
	public ResponseABC exec() {
		LOGGER.info("Executing "+this);
		// Search for a user with the specified ID (that has no password).
		String organizationId = loginRequest.getOrganizationId();
		Organization organization = organizationDao.findByDomainId(null, organizationId);

		// CRITICAL SECURITY CONCEPT.
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).
		if (organization != null) {
			// Find the user
			String userId = loginRequest.getUserId();
			User user = organization.getUser(userId);
			if (user != null) {
				String password = loginRequest.getPassword();
				if (user.isPasswordValid(password)) {
					// generate a 
					LoginResponse response = new LoginResponse();
					response.setOrganization(organization);
					response.setStatus(ResponseStatus.Success);
					session.setAuthenticated(true);
					LOGGER.info("User "+userId+" authenticated on session "+session.getSessionId());
					return response;
				}
				// LOGGER.warn("Login " + authenticateResult + " for user: " + user.getDomainId());
			}
		}
		LoginResponse response = new LoginResponse();
		response.setStatus(ResponseStatus.Authentication_Failed);
		return response;
	}
}
