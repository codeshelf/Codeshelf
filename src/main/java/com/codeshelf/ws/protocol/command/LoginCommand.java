package com.codeshelf.ws.protocol.command;

import java.util.Set;

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
import com.codeshelf.persistence.AbstractPersistenceService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.TokenSession;
import com.codeshelf.security.TokenSession.Status;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.service.IPropertyService;
import com.codeshelf.service.PropertyService;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.response.LoginResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.base.Strings;

public class LoginCommand extends CommandABC {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(LoginCommand.class);

	private LoginRequest			loginRequest;

	private ObjectChangeBroadcaster	objectChangeBroadcaster;

	private WebSocketManagerService sessionManager;

	public LoginCommand(WebSocketConnection wsConnection, LoginRequest loginRequest, ObjectChangeBroadcaster objectChangeBroadcaster, WebSocketManagerService sessionManager) {
		super(wsConnection);
		this.loginRequest = loginRequest;
		this.objectChangeBroadcaster = objectChangeBroadcaster;
		this.sessionManager = sessionManager;
	}

	@Override
	public ResponseABC exec() {
		LoginResponse response = new LoginResponse();
		try {
			String cstoken = loginRequest.getCstoken();
			String username = loginRequest.getUserId();
			String password = loginRequest.getPassword();
			if (wsConnection != null) {
				TokenSession tokenSession = null;
				boolean authByToken = false;
				if (!Strings.isNullOrEmpty(cstoken)) {
					tokenSession = TokenSessionService.getInstance().checkToken(cstoken);
					authByToken = true;
				} else {
					tokenSession = TokenSessionService.getInstance().authenticate(username, password);
	            }
				if (tokenSession.getStatus().equals(Status.ACCEPTED)) {
					User authUser = tokenSession.getUser();
					// successfully authenticated user with password
					Tenant tenant = TenantManagerService.getInstance().getTenantByUser(authUser);				
					wsConnection.authenticated(authUser,tenant);
					CodeshelfSecurityManager.setContext(authUser,tenant);
					try {
						TenantPersistenceService.getInstance().beginTransaction();
						
						LOGGER.info("User " + authUser.getUsername() + " of " + tenant.getName() + " authenticated on session "
								+ wsConnection.getSessionId());
	
						// determine if site controller
						SiteController sitecon = SiteController.staticGetDao().findByDomainId(null, username);
						CodeshelfNetwork network = null;
						if (sitecon != null) {
							network = sitecon.getParent();
							
							// ensure all collections are loaded, because hibernate session 
							// will already be closed when response is serialized
							network.getChes().size();
							network.getLedControllers().size();
							network.getSiteControllers().size();
							network = AbstractPersistenceService.<CodeshelfNetwork>deproxify(network);
						
							// send all network updates to this session for this network 
							NetworkChangeListener.registerWithSession(this.objectChangeBroadcaster, wsConnection, network);
						} // else regular user session
	
						// update session counters
						this.sessionManager.updateCounters();
	
						// generate login response
						response.setStatus(ResponseStatus.Success);
						response.setUser(authUser);
						response.setOrganization(new Organization()); // just to avoid changing UI...
						Set<String> permSet = authUser.getPermissionStrings();
						String[] permArray = new String[permSet.size()];
						response.setPermissions(permSet.toArray(permArray));
						response.setNetwork(network);
	
						// AUTOSHRT needed for sitecon, not UX clients, but go ahead and populate.
						if (network != null) {
							IPropertyService properties = PropertyService.getInstance();
							
							Facility facility = network.getParent();
							String valueStr = properties.getPropertyFromConfig(facility, DomainObjectProperty.AUTOSHRT);
							response.setAutoShortValue(Boolean.parseBoolean(valueStr));
	
							String pickInfo = properties.getPropertyFromConfig(facility, DomainObjectProperty.PICKINFO);
							response.setPickInfoValue(pickInfo);
	
							String containerType = properties.getPropertyFromConfig(facility, DomainObjectProperty.CNTRTYPE);
							response.setContainerTypeValue(containerType);
							
							String scanType = properties.getPropertyFromConfig(facility, DomainObjectProperty.SCANPICK);
							response.setScanTypeValue(scanType);
							
							String sequenceKind = properties.getPropertyFromConfig(facility, DomainObjectProperty.WORKSEQR);
							response.setSequenceKind(sequenceKind);
							
							String pickMultValue = properties.getPropertyFromConfig(facility, DomainObjectProperty.PICKMULT);
							response.setPickMultValue(pickMultValue);
	
						} else {
							response.setAutoShortValue(false); // not read by client. No need to look it up.
						}
	
					} finally {
						TenantPersistenceService.getInstance().commitTransaction();
						CodeshelfSecurityManager.removeContext();
					}
				} else {
					LOGGER.info("Auth failed: {}",tokenSession.getStatus().toString());
					response.setStatus(ResponseStatus.Authentication_Failed);
				}
			} else {
				LOGGER.warn("Null session on login attempt for user " + username);
				response.setStatus(ResponseStatus.Authentication_Failed);
			}
		} catch(Exception e) {
			LOGGER.error("Unexpected exception processing login request",e);
			response.setStatus(ResponseStatus.Authentication_Failed);
		}

		return response;
	}

}
