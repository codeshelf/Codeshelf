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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.ITenantManagerService;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.User;
import com.codeshelf.model.domain.UserType;

// note:
// it is intentional that the reasons for errors are only logged and not returned to the client
@Path("/users")
public class UsersResource {
	private static final Logger			LOGGER					= LoggerFactory.getLogger(UsersResource.class);
	private static final Set<String>	validCreateUserFields	= new HashSet<String>();
	static {
		validCreateUserFields.add("username");
		validCreateUserFields.add("password");
		validCreateUserFields.add("type");
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("username") String username) {
		if (username == null) {
			return getUsers();
		} else {
			return getUser(username);
		}
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
			if (User.isValidPassword(value)) {
				LOGGER.info("update user {} - change password requested", user.getUsername());
				user.setPassword(value);
				success = true;
			} else {
				LOGGER.warn("update user {} - invalid password specified", user.getUsername(), key);
			}
		} else if (key.equals("active")) {
			Boolean active = Boolean.valueOf(value);
			if (user.isActive() != active) {
				user.setActive(active);
			} // else ignore if no change
			success = true;
		} else {
			// TODO: support updating other user fields here  
			LOGGER.warn("update user {} - unknown key {}", user.getUsername(), key);
		}
		return success;
	}

	private User doCreateUser(MultivaluedMap<String, String> userParams) {
		ITenantManagerService manager = TenantManagerService.getInstance();
		User newUser = null;

		Map<String, String> cleanInput = RootResource.validFieldsOnly(userParams, validCreateUserFields);
		if (cleanInput != null) {
			String username = cleanInput.get("username");
			String password = cleanInput.get("password");
			String typeString = cleanInput.get("type");
			UserType type = null;
			try {
				type = UserType.valueOf(typeString);
			} catch (Exception e) {
				LOGGER.warn("could not convert value to UserType: {}", typeString);
			}

			if (username != null && password != null && type != null) {
				if (manager.canCreateUser(username)) {
					if (User.isValidPassword(password)) {
						Tenant defaultTenant = manager.getDefaultTenant();
						newUser = manager.createUser(defaultTenant, username, password, type);
						if (newUser == null)
							LOGGER.warn("failed to create user {}", username);
					} else {
						LOGGER.warn("cannot create user {} - invalid password", username);
					}
				} else {
					LOGGER.warn("cannot create user {} - username not accepted", username);
				}
			} else {
				LOGGER.warn("cannot create user - incomplete data");
			}
		}
		return newUser;
	}

	private Response getUsers() {
		try {
			List<User> userList = TenantManagerService.getInstance().getUsers();
			return Response.ok(userList).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	private Response getUser(String username) {
		try {
			User user = TenantManagerService.getInstance().getUser(username);
			return Response.ok(user).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
