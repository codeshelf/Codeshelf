package com.gadgetworks.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.application.ContextLogging;
import com.gadgetworks.codeshelf.filter.NetworkChangeListener;
import com.gadgetworks.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.DomainObjectProperty;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.platform.multitenancy.Tenant;
import com.gadgetworks.codeshelf.platform.multitenancy.TenantManagerService;
import com.gadgetworks.codeshelf.platform.multitenancy.User;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
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
		LoginResponse response = new LoginResponse();
		String username = loginRequest.getUserId();
		String password = loginRequest.getPassword();
		if (session != null) {
			User authUser = TenantManagerService.getInstance().authenticate(username, password);
			if (authUser != null) {
				Tenant tenant = authUser.getTenant();				
				session.authenticated(authUser);
				ContextLogging.setSession(session);
				try {
					LOGGER.info("User " + username + " of " + tenant.getName() + " authenticated on session "
							+ session.getSessionId());

					// determine if site controller
					SiteController sitecon = SiteController.DAO.findByDomainId(null, username);
					CodeshelfNetwork network = null;
					if (sitecon != null) {
						network = PersistenceService.<CodeshelfNetwork>deproxify(sitecon.getParent());
						response.setNetwork(network);

						// send all network updates to this session for this network 
						NetworkChangeListener.registerWithSession(this.objectChangeBroadcaster, session, network);
					} // else regular user session

					// update session counters
					SessionManager.getInstance().updateCounters();

					// generate login response
					response.setUser(authUser);
					response.setStatus(ResponseStatus.Success);
					response.setNetwork(network);

					// AUTOSHRT needed for sitecon, not UX clients, but go ahead and populate.
					if (network != null) {
						Facility facility = network.getParent();
						String valueStr = PropertyService.getPropertyFromConfig(facility, DomainObjectProperty.AUTOSHRT);
						response.setAutoShortValue(Boolean.parseBoolean(valueStr));

						String pickInfo = PropertyService.getPropertyFromConfig(facility, DomainObjectProperty.PICKINFO);
						response.setPickInfoValue(pickInfo);

						String containerType = PropertyService.getPropertyFromConfig(facility, DomainObjectProperty.CNTRTYPE);
						response.setContainerTypeValue(containerType);
					} else {
						response.setAutoShortValue(false); // not read by client. No need to look it up.
					}

				} finally {
					ContextLogging.clearSession();
				}
			} else {
				LOGGER.warn("Authentication failed: " + username);
				response.setStatus(ResponseStatus.Authentication_Failed);
			}
		} else {
			LOGGER.warn("Null session on login attempt for user " + username);
			response.setStatus(ResponseStatus.Authentication_Failed);
		}

		return response;
	}

}
