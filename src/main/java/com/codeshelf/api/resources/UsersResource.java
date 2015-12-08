package com.codeshelf.api.resources;

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
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.security.SecurityEmails;
import com.codeshelf.util.FormUtility;

@Path("/users")
@RequiresPermissions("user")
public class UsersResource {
	private static final Logger			LOGGER					= LoggerFactory.getLogger(UsersResource.class);
	private static final Set<String>	validCreateUserFields	= new HashSet<String>();
	private static final Set<String>	validUpdateUserFields	= new HashSet<String>();

	static {
		validCreateUserFields.add("username");
		validCreateUserFields.add("roles");

		validUpdateUserFields.add("active");
		validUpdateUserFields.add("roles");
	}

	@GET
	@RequiresPermissions("user:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsers() {
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		List<User> users = TenantManagerService.getInstance().getUsers(tenant);		
		return Response.ok(users).build();
	}
	
	@Path("roles")
	@GET
	@RequiresPermissions("user:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRoles() {
		List<UserRole> roles = TenantManagerService.getInstance().getRoles(false);
		return Response.ok(roles).build();
	}
	
	@Path("{id}")
	@GET
	@RequiresPermissions("user:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@PathParam("id") Integer id) {
		User user = TenantManagerService.getInstance().getUser(id);
		if(user != null && user.getTenant().equals(CodeshelfSecurityManager.getCurrentTenant())) {
			return Response.ok(user).build();
		} //else
		return Response.status(Status.NOT_FOUND).build();
	}

	@POST
	@RequiresPermissions("user:create")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createUser(MultivaluedMap<String, String> userParams) {
		Map<String, String> cleanInput = FormUtility.getValidFields(userParams, validCreateUserFields);
		if(cleanInput != null) {
			String roleList = cleanInput.get("roles");
			Set<UserRole> roles = null;
			if(roleList != null) {
				boolean allowRestricted = currentCodeshelfUser();
				roles = TenantManagerService.getInstance().getUserRoles(roleList, allowRestricted);
			} 
			if(roles != null) {
				// must have a role
				String username = cleanInput.get("username");
				if(username != null && TenantManagerService.getInstance().canCreateUser(username)) {
					User newUser = TenantManagerService.getInstance().createUser(CodeshelfSecurityManager.getCurrentTenant(), username, null, roles);
					LOGGER.info("Created new user {}",username);
					SecurityEmails.sendAccountCreation(newUser);
					return Response.ok(newUser).build();
				} else {
					LOGGER.warn("Invalid username specified trying to create user");
				}
			} else {
				LOGGER.warn("No valid roles specified trying to create user");
			}
		} else {
			LOGGER.warn("Bad parameters trying to create user");
		}
		return Response.status(Status.BAD_REQUEST).build();
	}

	@POST
	@Path("{id}/resend")
	@RequiresPermissions("user:create")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resendNewUserEmail(@PathParam("id") Integer id) {
		User user = TenantManagerService.getInstance().getUser(id);
		if(user != null && user.getTenant().equals(CodeshelfSecurityManager.getCurrentTenant())) {
			if(user.getLastAuthenticated() == null
				&& user.getHashedPassword() == null) {
				SecurityEmails.sendAccountCreation(user);
				return Response.ok(user).build();
			} //else
			return Response.status(Status.BAD_REQUEST).build();
		} //else
		return Response.status(Status.NOT_FOUND).build();
	}

	@Path("{id}")
	@POST
	@RequiresPermissions("user:edit")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateUser(@PathParam("id") Integer id, MultivaluedMap<String, String> userParams) {
		Map<String, String> cleanInput = FormUtility.getValidFields(userParams, validUpdateUserFields);
		if(cleanInput != null) {
			User user = TenantManagerService.getInstance().getUser(id);
			if(user != null 
					&& user.getTenant().equals(CodeshelfSecurityManager.getCurrentTenant())
					&& !user.getId().equals(CodeshelfSecurityManager.getCurrentUserContext().getId())) {
				String username = user.getUsername();
				boolean valueSet=false;
				// user exists, within this tenant, is not current user = ok to update
				String roleList = cleanInput.get("roles");
				Set<UserRole> roles = null; 
				if(roleList != null) {
					boolean allowRestricted = currentCodeshelfUser();
					roles = TenantManagerService.getInstance().getUserRoles(roleList, allowRestricted);
					if(roles != null) {
						LOGGER.info("Edited roles for user {}: {}",username,roleList);
						user.setRoles(roles);
						valueSet = true;
					} else {
						LOGGER.warn("Invalid role(s) specified trying to update user");
					}
				}
				String activeString = cleanInput.get("active");
				if(activeString != null) {
					valueSet = true;
					boolean setActive = Boolean.valueOf(activeString);
					if(user.isActive() != setActive) {
						user.setActive(setActive);
						LOGGER.info("Set user {} ACTIVE={}",username,setActive);
					}
				} 
				if(valueSet) {
					user = TenantManagerService.getInstance().updateUser(user);
					if(user != null) {
						return Response.ok(user).build();
					} else {
						LOGGER.warn("Failed to update user");
					}
				} else {
					LOGGER.warn("Update user request was sent without any fields to update");
				}
			} else {
				LOGGER.warn("Not allowed to update user id {}",id);
			}
		} else {
			LOGGER.warn("Bad input updating user");
		}
		return Response.status(Status.BAD_REQUEST).build();
	}


	@Path("{id}/reset")
	@POST
	@RequiresPermissions("user:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resetRecovery(@PathParam("id") Integer id) {
		User user = TenantManagerService.getInstance().getUser(id);
		if(user != null 
				&& user.getTenant().equals(CodeshelfSecurityManager.getCurrentTenant())
				&& !user.getId().equals(CodeshelfSecurityManager.getCurrentUserContext().getId())) {
			// allow the user to use recovery
			user.setRecoveryEmailsRemain(User.DEFAULT_RECOVERY_EMAILS);
			user.setRecoveryTriesRemain(User.DEFAULT_RECOVERY_TRIES);
			user.setLastRecoveryEmail(null);
			user.setHashedPassword(null);
			SecurityEmails.sendAccountReset(user);
			LOGGER.info("Resetting recovery tries for user {}",user.getUsername());
			user = TenantManagerService.getInstance().updateUser(user);
			return Response.ok(user).build();
		} // else
		LOGGER.info("Invalid request to reset recovery tries");
		return Response.status(Status.BAD_REQUEST).build();
	}
	
	private boolean currentCodeshelfUser() {
		String username = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
		return username.endsWith("@codeshelf.com");
	}

}
