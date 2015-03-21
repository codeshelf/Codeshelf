package com.codeshelf.ws.jetty.protocol.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.filter.NetworkChangeListener;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Organization;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.security.AuthResponse;
import com.codeshelf.security.AuthResponse.Status;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.HmacAuthService;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.PropertyService;
import com.codeshelf.ws.jetty.protocol.request.LoginRequest;
import com.codeshelf.ws.jetty.protocol.response.LoginResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.WebSocketConnection;
import com.codeshelf.ws.jetty.server.WebSocketManagerService;

public class LoginCommand extends CommandABC {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(LoginCommand.class);

	private LoginRequest			loginRequest;

	private WebSocketManagerService sessionManager;

	public LoginCommand(WebSocketConnection wsConnection, LoginRequest loginRequest, WebSocketManagerService sessionManager) {
		super(wsConnection);
		this.loginRequest = loginRequest;
		this.sessionManager = sessionManager;
	}

	@Override
	public ResponseABC exec() {
		LoginResponse response = new LoginResponse();
		String username = loginRequest.getUserId();
		String password = loginRequest.getPassword();
		if (wsConnection != null) {
			AuthResponse authResponse = HmacAuthService.getInstance().authenticate(username, password);
			if (authResponse.getStatus().equals(Status.ACCEPTED)) {
				User authUser = authResponse.getUser();
				// successfully authenticated user with password
				Tenant tenant = TenantManagerService.getInstance().getTenantByUser(authUser);				
				wsConnection.authenticated(authUser);
				CodeshelfSecurityManager.setCurrentUser(authUser);
				try {
					LOGGER.info("User " + username + " of " + tenant.getName() + " authenticated on session "
							+ wsConnection.getSessionId());

					// determine if site controller
					// LoginCommand does NOT have a transaction opened by endpoint
					
					SiteController sitecon = null;
					CodeshelfNetwork network = null;
					TenantPersistenceService.getInstance().beginTransaction(tenant);
					try {
						sitecon = SiteController.staticGetDao().findByDomainId(tenant, null, username);
						network = null;
						if (sitecon != null) {
							network = sitecon.getParent();
							
							// our session is going to be closed by the time the response is serialized
							// so this is to avoid lazy init errors
							// TODO: handle this better
							for(String cheId : network.getChes().keySet()) { network.getChes().get(cheId); }
							for(String ledId : network.getLedControllers().keySet()) { network.getLedControllers().get(ledId); }
							for(String scId : network.getSiteControllers().keySet()) { network.getSiteControllers().get(scId); }
							
							network = TenantPersistenceService.<CodeshelfNetwork>deproxify(network);
						
							// send all network updates to this session for this network 
							ObjectChangeBroadcaster ocb = TenantPersistenceService.getInstance().getEventListenerIntegrator(tenant).getChangeBroadcaster();
							NetworkChangeListener.registerWithSession(tenant,ocb, wsConnection, network);
						} // else regular user session

						// update session counters
						this.sessionManager.updateCounters();
	
						// generate login response
						response.setUser(authUser);
						response.setStatus(ResponseStatus.Success);
						response.setOrganization(new Organization()); // just to avoid changing UI...
						response.setNetwork(network);
	
						// AUTOSHRT needed for sitecon, not UX clients, but go ahead and populate.
						if (network != null) {
							IPropertyService properties = PropertyService.getInstance();
							
							Facility facility = network.getParent();
							String valueStr = properties.getPropertyFromConfig(getTenant(),facility, DomainObjectProperty.AUTOSHRT);
							response.setAutoShortValue(Boolean.parseBoolean(valueStr));
	
							String pickInfo = properties.getPropertyFromConfig(getTenant(),facility, DomainObjectProperty.PICKINFO);
							response.setPickInfoValue(pickInfo);
	
							String containerType = properties.getPropertyFromConfig(getTenant(),facility, DomainObjectProperty.CNTRTYPE);
							response.setContainerTypeValue(containerType);
							
							String scanType = properties.getPropertyFromConfig(getTenant(),facility, DomainObjectProperty.SCANPICK);
							response.setScanTypeValue(scanType);
							
							String sequenceKind = properties.getPropertyFromConfig(getTenant(),facility, DomainObjectProperty.WORKSEQR);
							response.setSequenceKind(sequenceKind);
							
							String pickMultValue = properties.getPropertyFromConfig(getTenant(),facility, DomainObjectProperty.PICKMULT);
							response.setPickMultValue(pickMultValue);
	
						} else {
							response.setAutoShortValue(false); // not read by client. No need to look it up.
						}
	
					} finally {
						TenantPersistenceService.getInstance().commitTransaction(tenant);
					}
				} finally {
					CodeshelfSecurityManager.removeCurrentUser();
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
