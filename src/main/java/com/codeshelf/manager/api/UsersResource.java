package com.codeshelf.manager.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.manager.UserRole;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.SecurityEmails;
import com.codeshelf.security.TokenSessionService;
import com.codeshelf.util.FormUtility;

// note:
// it is intentional that the reasons for errors are only logged and not returned to the client
@Path("/users")
@RequiresPermissions("user")
public class UsersResource {
	TokenSessionService tokenSessionService;
	
	private static final Logger			LOGGER					= LoggerFactory.getLogger(UsersResource.class);
	private static final Set<String>	validCreateUserFields	= new HashSet<String>();
	private static final Set<String>	validUpdateUserFields	= new HashSet<String>();
	private static final Set<String>	multiValueFields        = new HashSet<String>();

	static {
		validCreateUserFields.add("tenantid");
		validCreateUserFields.add("username");
		validCreateUserFields.add("password");
		validCreateUserFields.add("roles");
		
		validUpdateUserFields.add("password");
		validUpdateUserFields.add("active");
		validUpdateUserFields.add("roles");

		multiValueFields.add("roles");
	}
	
	public UsersResource() {
		this.tokenSessionService = TokenSessionService.getInstance();
	}
	
	@GET
	@RequiresPermissions("user:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("username") String username, 
				@QueryParam("tenantid") Integer tenantId,
				@QueryParam("sitecon") Boolean sitecon,
				@QueryParam("needsupgrade") Boolean needsUpgrade) {
		if (username == null)
			return getUsers(tenantId,sitecon,needsUpgrade);
		//else
		return getUser(username,tenantId); // htpasswd ignored for username search
	}
	
	@GET
	@Path("htpasswd")
	@RequiresPermissions("user:htpasswd")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getHtpasswd() {
		return Response.ok(new String(TenantManagerService.getInstance().getHtpasswd())).build();
	}

	@POST
	@RequiresPermissions("user:create")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createUser(MultivaluedMap<String, String> userParams) {
		try {
			User newUser = doCreateUser(userParams);
			if (newUser == null)
				return Response.status(Status.BAD_REQUEST).build();
			//else
			return Response.ok(newUser).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Path("{id}")
	@GET
	@RequiresPermissions("user:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@PathParam("id") Integer id) {
		try {
			User user = TenantManagerService.getInstance().getUser(id); // logs if not found
			if (user == null)
				return Response.status(Status.NOT_FOUND).build();
			//else
			return Response.ok(user).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Path("{id}")
	@POST
	@RequiresPermissions("user:edit")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateUser(@PathParam("id") Integer id, MultivaluedMap<String, String> userParams) {
		try {
			User user = TenantManagerService.getInstance().getUser(id);
			if (user != null) {
				User updatedUser = updateUserFromParams(user, userParams);
				if (updatedUser != null)
					return Response.ok(updatedUser).build();
				//else 
				return Response.status(Status.BAD_REQUEST).build();
			}
			return Response.status(Status.NOT_FOUND).build(); // must create first
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/***** REST API methods above ^^^ private methods below ****/

	private User updateUserFromParams(User user, MultivaluedMap<String, String> userParams) {
		// not in session - user is detached object 
		// iterate through all userParams and set fields of user accordingly.'
		// if there are no parameters, or any parameters are unrecognized or cannot be set, the operation fails and returns null.
		// if the user is changed, it will be saved and the persistent result returned.

		ITenantManagerService manager = TenantManagerService.getInstance();
		boolean updated = false;

		for (String key : userParams.keySet()) {
			if(validUpdateUserFields.contains(key)) {
				List<String> values = userParams.get(key);
				if (values == null) {
					LOGGER.warn("update user {} - no value for key {}", user.getUsername(), key);
					updated = false;
					break; // key with no values
				} else if (values.size() != 1) {
					LOGGER.warn("update user {} - multiple values for key {}", user.getUsername(), key);
					updated = false;
					break; // multiple values specified				
				} else if (updateUser(user, key, values.get(0))) {
					updated = true;
				} else {
					updated = false;
					break;
				}
			} else {
				LOGGER.warn("update user {} - unknown key {}", user.getUsername(), key);
				updated = false;
				break;
			}
		}

		if (updated) {
			return manager.updateUser(user); // request updating user in database
		}
		//else
		return null;
	}

	private boolean updateUser(User user, String key, String value) {
		if (user == null || key == null || value == null)
			return false;

		boolean success = false;
		if (key.equals("password")) {
			if (tokenSessionService.passwordMeetsRequirements(value)) {
				LOGGER.info("update user {} - change password requested", user.getUsername());
				user.setHashedPassword(tokenSessionService.hashPassword(value));
				SecurityEmails.sendPasswordChangedByAdmin(user);
				success = true;
			} else {
				LOGGER.warn("update user {} - invalid password specified", user.getUsername());
			}
		} else if (key.equals("active")) {
			Boolean active = Boolean.valueOf(value);
			if (user.isActive() != active) {
				LOGGER.info("update user {} - set active = {}", user.getUsername(),active);
				user.setActive(active);
			} // else ignore if no change
			success = true;
		} else if (key.equals("roles")) {
			Set<UserRole> roles = TenantManagerService.getInstance().getUserRoles(value,true);
			if(roles != null) {
				if(!user.getRoles().equals(roles)) {
					LOGGER.info("update user {} - set roles = {}", user.getUsername(),value);
					user.setRoles(roles);
				} // else ignore
				success=true;
			} else {
				LOGGER.warn("invalid roles specified: {}",value);
			}
		}
		return success;
	}

	private User doCreateUser(MultivaluedMap<String, String> userParams) {
		ITenantManagerService manager = TenantManagerService.getInstance();
		User newUser = null;

		Map<String, String> cleanInput = FormUtility.getValidFields(userParams, validCreateUserFields, multiValueFields);
		if (cleanInput != null) {
			String username = cleanInput.get("username");
			String tenantIdString = cleanInput.get("tenantid");
			String password = cleanInput.get("password");
			Integer tenantId = null;
			try {
				tenantId = Integer.valueOf(tenantIdString);
			} catch(NumberFormatException e) {
				LOGGER.warn("count not convert value to tenantId: {}", tenantIdString);
			}
			Set<UserRole> roles = manager.getUserRoles(cleanInput.get("roles"),true);

			if (username != null && password != null && roles != null) {
				if (manager.canCreateUser(username)) {
					if (tokenSessionService.passwordMeetsRequirements(password)) {
						Tenant tenant;
						if(tenantId == null)
							tenant = manager.getInitialTenant();
						else 
							tenant = manager.getTenant(tenantId);
						
						if(tenant != null) {
							newUser = manager.createUser(tenant, username, password, roles);
							if (newUser == null)
								LOGGER.warn("failed to create user {}", username);
							// else success
						} else {
							LOGGER.warn("could not create user because tenant {} not found", tenantId);
						}
					} else {
						LOGGER.warn("cannot create user {} - invalid password", username);
					}
				} else {
					LOGGER.warn("cannot create user {} - username not accepted", username);
				}
			} else {
				LOGGER.warn("cannot create user - incomplete form data");
			}
		} // else validFieldsOnly will log reason
		return newUser;
	}

	private Response getUsers(Integer tenantId, Boolean sitecon, Boolean needsUpgrade) {
		try {
			Tenant tenant = null;
			if (tenantId != null) 
				tenant = TenantManagerService.getInstance().getTenant(tenantId);
			
			List<User> userList;
			
			if(tenant == null && sitecon != null && sitecon) {
				userList = TenantManagerService.getInstance().getSiteControllerUsers(needsUpgrade != null && needsUpgrade);
			} else {
				userList = TenantManagerService.getInstance().getUsers(tenant);
			}
			
			return Response.ok(userList).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private Response getUser(String username, Integer tenantId) {
		try {
			User user = TenantManagerService.getInstance().getUser(username);
			if(tenantId == null || tenantId.equals(user.getTenant().getId()) )
				return Response.ok(user).build();
			//else 
			return Response.status(Status.NOT_FOUND).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
