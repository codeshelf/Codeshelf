package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionType;

public class LoginCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(LoginCommand.class);
	
	private LoginRequest loginRequest;
	
	public LoginCommand(CsSession session, LoginRequest loginRequest) {
		super(session);
		this.loginRequest = loginRequest;
	}

	@Override
	public ResponseABC exec() {
		// CRITICAL SECURITY CONCEPT - (not implemented? comments preserved cic2014)
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).

		LoginResponse response = new LoginResponse();
		String userId = loginRequest.getUserId();

		if(session != null) {
			User user = User.DAO.findByDomainId(null, userId);
			if (user != null) {
				String password = loginRequest.getPassword();
				if (user.isPasswordValid(password)) {
					Organization org = user.getParent();

					if(user.getSiteController() != null) {
						session.setType(SessionType.SiteController);
					} else {
						session.setType(SessionType.UserApp);						
					}
					session.setOrganizationName(org.getDomainId());
					session.setAuthenticated(true);

					// generate login response
					LOGGER.info("User "+userId+" of "+org.getDomainId()+" authenticated on session "+session.getSessionId());
					response.setOrganization(org);

					response.setStatus(ResponseStatus.Success);
					response.setUser(user);
				} else {
					LOGGER.warn("Invalid password for user: " + user.getDomainId());
					response.setStatus(ResponseStatus.Authentication_Failed);
				}
			} else {
				LOGGER.warn("Invalid username: " + userId);
				response.setStatus(ResponseStatus.Authentication_Failed);
			}
		} else {
			LOGGER.warn("Null session on login attempt for user " + userId);
			response.setStatus(ResponseStatus.Authentication_Failed);
		}
						
		return response;
	}
}
