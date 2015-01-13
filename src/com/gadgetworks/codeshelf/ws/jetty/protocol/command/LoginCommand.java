package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.ContextLogging;
import com.gadgetworks.codeshelf.filter.NetworkChangeListener;
import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.service.PropertyService;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class LoginCommand extends CommandABC {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(LoginCommand.class);

	private LoginRequest			loginRequest;

	private ObjectChangeBroadcaster	objectChangeBroadcaster;

	public LoginCommand(UserSession session, LoginRequest loginRequest, ObjectChangeBroadcaster objectChangeBroadcaster) {
		super(session);
		this.loginRequest = loginRequest;
		this.objectChangeBroadcaster = objectChangeBroadcaster;
	}

	@Override
	public ResponseABC exec() {
		// CRITICAL SECURITY CONCEPT - (not implemented? comments preserved cic2014)
		// LaunchCodes are anonymous users that we create WITHOUT passwords or final userIDs.
		// If a user has a NULL hashed password then this is a launch code (promo) user.
		// A user with a launch code can elect to become a real user and change their userId (and created a password).

		LoginResponse response = new LoginResponse();
		String userId = loginRequest.getUserId();

		if (session != null) {
			User user = User.DAO.findByDomainId(null, userId);
			if (user != null) {
				String password = loginRequest.getPassword();
				if (user.isPasswordValid(password)) {
					session.authenticated(user);
					ContextLogging.setSession(session);
					try {
						Organization org = user.getParent();
						LOGGER.info("User " + userId + " of " + org.getDomainId() + " authenticated on session "
								+ session.getSessionId());

						// determine if site controller
						SiteController sitecon = SiteController.DAO.findByDomainId(null, userId);
						CodeshelfNetwork network = null;
						if (sitecon != null) {
							network = sitecon.getParent();
							response.setNetwork(network);

							// send all network updates to this session for this network 
							NetworkChangeListener.registerWithSession(this.objectChangeBroadcaster, session, network);
						} // else regular user session

						// update session counters
						SessionManager.getInstance().updateCounters();

						// generate login response
						response.setUser(user);
						response.setStatus(ResponseStatus.Success);
						response.setOrganization(org);
						response.setNetwork(network);

						// AUTOSHRT needed for sitecon, not UX clients, but go ahead and populate.
						if (network != null) {
							Facility facility = network.getParent();
							String valueStr = PropertyService.getPropertyFromConfig(facility, DomainObjectProperty.AUTOSHRT);
							response.setAutoShortValue(Boolean.parseBoolean(valueStr));
						} else {
							response.setAutoShortValue(false); // not read by client. No need to look it up.
						}

					} finally {
						ContextLogging.clearSession();
					}
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
