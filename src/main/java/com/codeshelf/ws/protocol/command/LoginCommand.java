package com.codeshelf.ws.protocol.command;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.filter.NetworkChangeListener;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.model.FacilityPropertyType;
import com.codeshelf.model.PositionTypeEnum;
import com.codeshelf.model.dao.ObjectChangeBroadcaster;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Organization;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.persistence.AbstractPersistenceService;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.TokenSession;
import com.codeshelf.security.TokenSession.Status;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.ws.protocol.request.LoginRequest;
import com.codeshelf.ws.protocol.response.LoginResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.WebSocketConnection;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.base.Strings;

public class LoginCommand extends CommandABC<LoginRequest> {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(LoginCommand.class);

	private ObjectChangeBroadcaster	objectChangeBroadcaster;

	private WebSocketManagerService sessionManager;

	public LoginCommand(WebSocketConnection wsConnection, LoginRequest loginRequest, ObjectChangeBroadcaster objectChangeBroadcaster, WebSocketManagerService sessionManager) {
		super(wsConnection, loginRequest);
		this.objectChangeBroadcaster = objectChangeBroadcaster;
		this.sessionManager = sessionManager;
	}

	@Override
	public ResponseABC exec() {
		LoginResponse response = new LoginResponse();
		String tenantName = null;
		try {
			String cstoken = request.getCstoken();
			String username = request.getUserId();
			String password = request.getPassword();
			String version = request.getClientVersion();

			if (wsConnection != null) {
				TokenSession tokenSession = null;
				if (!Strings.isNullOrEmpty(cstoken)) {
					// client claims to be already authenticated via token
					tokenSession = TokenSessionService.getInstance().checkToken(cstoken);
				} else if(version != null) {
					// site controller / reported app version must be compatible
					tokenSession = TokenSessionService.getInstance().authenticate(username, password, version);
	            } else {
	            	// not a site controller, or a very old site controller - just allow login
	            	tokenSession = TokenSessionService.getInstance().authenticate(username, password);
	            }

				if (tokenSession.getStatus().equals(Status.ACTIVE_SESSION)) {
					User authUser = tokenSession.getUser();
					// successfully authenticated user with password
					Tenant tenant = TenantManagerService.getInstance().getTenantByUser(authUser);
					tenantName = tenant.getName();
					wsConnection.authenticated(authUser,tenant);
					CodeshelfSecurityManager.setContext(authUser,tenant);
					try {
						TenantPersistenceService.getInstance().beginTransaction();
					
						// determine if site controller
						SiteController sitecon = SiteController.staticGetDao().findByDomainId(null, username);
						
						LOGGER.info("User " + authUser.getUsername() + " of " + tenant.getName() + " authenticated on session "
								+ wsConnection.getSessionId());

						CodeshelfNetwork network = null;
						if (sitecon != null) {
							response.setSiteControllerRole(sitecon.getRole());
							network = sitecon.getParent();

							// ensure all collections are loaded, because hibernate session
							// will already be closed when response is serialized
							network.getChes().size();
							network.getLedControllers().size();
							network.getSiteControllers().size();
							network = AbstractPersistenceService.<CodeshelfNetwork>deproxify(network);

							// send all network updates to this session for this network
							NetworkChangeListener.registerWithSession(this.objectChangeBroadcaster, wsConnection, network);
						} else if (authUser.isSiteController()) {
							String msg = "Could not connect site controller, it is not associated with a facility";
							LOGGER.warn(msg);
							response.setStatusMessage(msg);
							response.setStatus(ResponseStatus.Fail);
							return response;
						} else {
							//regular ui client
							// First login from the client will make sure a facility is created only
							//  for the "default" tenant in Tracy, CA
							if (TenantManagerService.INITIAL_TENANT_NAME.equals(tenantName)) {
								if (Facility.staticGetDao().getAll().isEmpty()) {
									Facility.createFacility("F1", "First Facility",new Point(PositionTypeEnum.GPS, -122.2741133, 37.8004643, 0.0));
								}
							}
						}

						// update session counters
						this.sessionManager.updateCounters();

						// generate login response
						response.setStatus(ResponseStatus.Success);
						response.setUser(authUser);
						response.setTenantName(tenantName);
						response.setOrganization(new Organization()); // just to avoid changing UI...
						Set<String> permSet = authUser.getPermissionStrings();
						String[] permArray = new String[permSet.size()];
						response.setPermissions(permSet.toArray(permArray));
						response.setNetwork(network);

						// AUTOSHRT needed for sitecon, not UX clients, but go ahead and populate.
						if (network != null) {
							LOGGER.warn("Site Controller logging in: {}", authUser);

							Facility facility = network.getParent();
							response.setFacilityDomainId(facility.getDomainId());
							
							String valueStr = PropertyBehavior.getProperty(facility, FacilityPropertyType.AUTOSHRT);
							response.setAutoShortValue(Boolean.parseBoolean(valueStr));

							String pickInfo = PropertyBehavior.getProperty(facility, FacilityPropertyType.PICKINFO);
							response.setPickInfoValue(pickInfo);

							String containerType = PropertyBehavior.getProperty(facility, FacilityPropertyType.CNTRTYPE);
							response.setContainerTypeValue(containerType);

							String scanType = PropertyBehavior.getProperty(facility, FacilityPropertyType.SCANPICK);
							response.setScanTypeValue(scanType);

							String sequenceKind = PropertyBehavior.getProperty(facility, FacilityPropertyType.WORKSEQR);
							response.setSequenceKind(sequenceKind);

							String pickMultValue = PropertyBehavior.getProperty(facility, FacilityPropertyType.PICKMULT);
							response.setPickMultValue(pickMultValue);

							String responseValue = PropertyBehavior.getProperty(facility, FacilityPropertyType.PRODUCTION);
							response.setProductionValue(responseValue);
							
							String ordersubValue = PropertyBehavior.getProperty(facility, FacilityPropertyType.ORDERSUB);
							response.setOrdersubValue(ordersubValue);

							String protocol = PropertyBehavior.getProperty(facility, FacilityPropertyType.PROTOCOL);
							response.setProtocol(protocol);
						} else { //ui client user
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
