package com.codeshelf.manager.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import com.codeshelf.manager.ITenantManagerService;
import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.UserPermission;
import com.codeshelf.manager.UserRole;

@Path("/roles")
@RequiresPermissions("role")
public class RolesResource {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(RolesResource.class);
	
	private static final Set<String>	validUpdateRoleFields	= new HashSet<String>();
	static {
		validUpdateRoleFields.add("name");
		validUpdateRoleFields.add("permissions");
	}

	
	@GET
	@RequiresPermissions("role:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		try {
			List<UserRole> roleList = TenantManagerService.getInstance().getRoles();
			return Response.ok(roleList).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@RequiresPermissions("role:create")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createRole(MultivaluedMap<String, String> roleParams) {
		try {
			UserRole newRole = doCreateRole(roleParams);
			if (newRole == null)
				return Response.status(Status.BAD_REQUEST).build();
			//else
			return Response.ok(newRole).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Path("{id}")
	@GET
	@RequiresPermissions("role:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRole(@PathParam("id") Integer id) {
		try {
			UserRole role = TenantManagerService.getInstance().getRole(id); // logs if not found
			if (role == null)
				return Response.status(Status.NOT_FOUND).build();
			//else
			return Response.ok(role).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Path("{id}")
	@POST
	@RequiresPermissions("role:edit")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateRole(@PathParam("id") Integer id, MultivaluedMap<String, String> roleParams) {
		try {
			UserRole role = TenantManagerService.getInstance().getRole(id);
			if (role != null) {
				UserRole updatedRole = updateRoleFromParams(role, roleParams);
				if (updatedRole != null)
					return Response.ok(updatedRole).build();
				//else 
				return Response.status(Status.BAD_REQUEST).build();
			}
			return Response.status(Status.NOT_FOUND).build(); // must create first
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Path("{id}")
	@DELETE
	@RequiresPermissions("role:delete")
	public Response deleteRole(@PathParam("id") Integer id) {
		try {
			UserRole role = TenantManagerService.getInstance().getRole(id);
			if (role != null) {
				TenantManagerService.getInstance().deleteRole(role);
				return Response.ok().build();
			}
			return Response.status(Status.NOT_FOUND).build(); // must create first
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/***** REST API methods above ^^^ private methods below ****/

	private UserRole doCreateRole(MultivaluedMap<String, String> roleParams) {
		String name = roleParams.getFirst("name");
		UserRole newRole = null;
		if (UserRole.nameIsValid(name)) {
			if (TenantManagerService.getInstance().getRoleByName(name) == null) {
				newRole = TenantManagerService.getInstance().createRole(name);
			} else {
				LOGGER.warn("failed to create duplicate role named {}", name);
			}
		} else {
			LOGGER.warn("could not create role, valid name not specified");
		}
		return newRole;
	}

	private UserRole updateRoleFromParams(UserRole role, MultivaluedMap<String, String> roleParams) {
		ITenantManagerService manager = TenantManagerService.getInstance();
		boolean updated = false;

		for (String key : roleParams.keySet()) {
			if(validUpdateRoleFields.contains(key)) {
				List<String> values = roleParams.get(key);
				if (values == null) {
					LOGGER.warn("update role {} - no value for key {}", role.getName(), key);
					updated = false;
					break; // key with no values
				} else if (values.size() != 1) {
					LOGGER.warn("update role {} - multiple values for key {}", role.getName(), key);
					updated = false;
					break; // multiple values specified				
				} else if (updateRole(role, key, values.get(0))) {
					updated = true;
				} else {
					updated = false;
					break;
				}
			} else {
				LOGGER.warn("update role {} - unknown key {}", role.getName(), key);
				updated = false;
				break;
			}
		}

		if (updated) {
			return manager.updateRole(role); // request updating user in database
		}
		//else
		return null;
	}

	private boolean updateRole(UserRole role, String key, String value) {
		if (role == null || key == null || value == null)
			return false;

		boolean success = false;
		if (key.equals("name")) {
			if (UserRole.nameIsValid(value)) {
				if(TenantManagerService.getInstance().getRoleByName(value) == null) {
					LOGGER.info("update role {} - change name to {}", role.getName(), value);
					role.setName(value);
					success = true;
				} else {
					LOGGER.warn("failed to update, duplicate role named {}",value);
				}
			} else {
				LOGGER.warn("update role {} - invalid name specified", role.getName());
			}
		} else if (key.equals("permissions")) {
			Set<UserPermission> permissions = userPermissions(value);			
			if(permissions != null) {
				if(!role.getPermissions().equals(permissions)) {
					LOGGER.info("update role {} - set permissions = {}", role.getName(),value);
					role.setPermissions(permissions);
					success = true;
				} // else ignore
			} else {
				LOGGER.warn("invalid permissions specified: {}",value);
			}
		}
		return success;
	}

	private Set<UserPermission> userPermissions(String value) {
		Set<UserPermission> result = new HashSet<UserPermission>();
		if(value == null || value.isEmpty())
			return result;
		
		ITenantManagerService manager = TenantManagerService.getInstance();
		String[] permissionDescriptors = value.split(UserPermission.TOKEN_SEPARATOR);
		for(int i=0;i<permissionDescriptors.length;i++) {
			UserPermission permission = manager.getPermissionByDescriptor(permissionDescriptors[i]);
			if(permission != null) {
				result.add(permission);
			} else {
				LOGGER.warn("invalid permission name in list: {}",value);
				return null;
			}
		}
		return result;
	}

}
