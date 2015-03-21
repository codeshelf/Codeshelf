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

import org.apache.shiro.authz.annotation.RequiresRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.ITenantManagerService;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.manager.UserRole;
import com.codeshelf.model.domain.UserType;
import com.codeshelf.security.AuthProviderService;
import com.codeshelf.security.HmacAuthService;

// note:
// it is intentional that the reasons for errors are only logged and not returned to the client
@Path("/users")
public class UsersResource {
	AuthProviderService authProviderService;
	
	private static final Logger			LOGGER					= LoggerFactory.getLogger(UsersResource.class);
	private static final Set<String>	validCreateUserFields	= new HashSet<String>();
	private static final Set<String>	validUpdateUserFields	= new HashSet<String>();

	static {
		validCreateUserFields.add("tenantid");
		validCreateUserFields.add("username");
		validCreateUserFields.add("password");
		validCreateUserFields.add("type");
		validCreateUserFields.add("roles");
		validUpdateUserFields.add("password");
		validUpdateUserFields.add("active");
		validUpdateUserFields.add("type");
		validUpdateUserFields.add("roles");
	}
	
	public UsersResource() {
		this.authProviderService = HmacAuthService.getInstance();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("username") String username, 
				@QueryParam("tenantid") Integer tenantId) {
		if (username == null)
			return getUsers(tenantId);
		//else
		return getUser(username,tenantId); // htpasswd ignored for username search
	}
	
	@GET
	@Path("htpasswd")
	@Produces(MediaType.TEXT_PLAIN)
	@RequiresRoles("SUPER")
	public Response getHtpasswd() {
		return Response.ok(new String(TenantManagerService.getInstance().getHtpasswd())).build();
	}

	@POST
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
			if (authProviderService.passwordMeetsRequirements(value)) {
				LOGGER.info("update user {} - change password requested", user.getUsername());
				user.setHashedPassword(authProviderService.hashPassword(value));
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
		} else if (key.equals("type")) {
			UserType type = UserType.valueOf(value);
			if (!user.getType().equals(type)) {
				LOGGER.info("update user {} - set type = {}", user.getUsername(),type);
				user.setType(type);
			} // else ignore if no change
			success = true;
		} else if (key.equals("roles")) {
			Set<UserRole> roles = userRoles(value);
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

	private Set<UserRole> userRoles(String value) {
		Set<UserRole> result = new HashSet<UserRole>();
		if(value == null || value.isEmpty())
			return result;
		
		ITenantManagerService manager = TenantManagerService.getInstance();
		String[] roleNames = value.split(UserRole.TOKEN_SEPARATOR);
		for(int i=0;i<roleNames.length;i++) {
			UserRole role = manager.getRoleByName(roleNames[i]);
			if(role != null) {
				result.add(role);
			} else {
				LOGGER.warn("invalid role name in list: {}",value);
				return null;
			}
		}
		return result;
	}

	private User doCreateUser(MultivaluedMap<String, String> userParams) {
		ITenantManagerService manager = TenantManagerService.getInstance();
		User newUser = null;

		Map<String, String> cleanInput = RootResource.validFieldsOnly(userParams, validCreateUserFields);
		if (cleanInput != null) {
			String username = cleanInput.get("username");
			String tenantIdString = cleanInput.get("tenantid");
			String password = cleanInput.get("password");
			String typeString = cleanInput.get("type");
			UserType type = null;
			try {
				type = UserType.valueOf(typeString);
			} catch (Exception e) {
				LOGGER.warn("could not convert value to UserType: {}", typeString);
			}
			Integer tenantId = null;
			try {
				tenantId = Integer.valueOf(tenantIdString);
			} catch(NumberFormatException e) {
				LOGGER.warn("count not convert value to tenantId: {}", tenantIdString);
			}
			Set<UserRole> roles = userRoles(cleanInput.get("roles"));

			if (username != null && password != null && type != null && roles != null) {
				if (manager.canCreateUser(username)) {
					if (authProviderService.passwordMeetsRequirements(password)) {
						Tenant tenant = null;
						if(tenantId != null)
							tenant = manager.getTenant(tenantId);
						
						if(tenant != null) {
							newUser = manager.createUser(tenant, username, password, type, roles);
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

	private Response getUsers(Integer tenantId) {
		try {
			Tenant tenant = null;
			if (tenantId != null) 
				tenant = TenantManagerService.getInstance().getTenant(tenantId);
			
			List<User> userList = TenantManagerService.getInstance().getUsers(tenant);
			
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
