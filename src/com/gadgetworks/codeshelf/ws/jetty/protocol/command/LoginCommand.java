package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.filter.NetworkChangeListener;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.model.domain.UserType;
import com.gadgetworks.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.gadgetworks.codeshelf.ws.jetty.server.UserSession;

public class LoginCommand extends CommandABC {

	private static final Logger	LOGGER = LoggerFactory.getLogger(LoginCommand.class);
	
	private LoginRequest loginRequest;
	
	private IDaoProvider daoProvider;
	
	public LoginCommand(UserSession session, LoginRequest loginRequest, IDaoProvider daoProvider) {
		super(session);
		this.loginRequest = loginRequest;
		this.daoProvider = daoProvider;
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
					boolean loginOkay = true;
					session.setUser(user);
					// determine if site controller
					if(user.getType().equals(UserType.SITECON)) {
						SiteController sitecon = SiteController.DAO.findByDomainId(null, userId);
						if(sitecon  != null) {
							CodeshelfNetwork network = sitecon.getParent();
							network.getDomainId(); // restore entity
							response.setNetwork(network); 
							// send all network updates to this session for this network 
							NetworkChangeListener.registerWithSession(session, network, daoProvider);
						} else {
							LOGGER.warn("Sitecon user "+userId+" - assoc site controller not found, cannot log in");
							response.setStatus(ResponseStatus.Authentication_Failed);
							loginOkay = false;
						}
					}
					if(loginOkay) {
						Organization org = user.getParent();
						session.setUser(user);
						LOGGER.info("User "+userId+" of "+org.getDomainId()+" authenticated on session "+session.getSessionId());
						response.setUser(user);
						response.setStatus(ResponseStatus.Success);
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
