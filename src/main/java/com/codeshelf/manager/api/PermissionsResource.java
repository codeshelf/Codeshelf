package com.codeshelf.manager.api;

import java.util.List;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.TenantManagerService;
import com.codeshelf.manager.UserPermission;
import com.codeshelf.manager.UserPermission;

@Path("/permissions")
public class PermissionsResource {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(PermissionsResource.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		try {
			List<UserPermission> permissionList = TenantManagerService.getInstance().getPermissions();
			return Response.ok(permissionList).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createPermission(MultivaluedMap<String,String> permissionParams) {
		try {
			UserPermission newPermission = doCreatePermission(permissionParams);		
			if(newPermission == null)
				return Response.status(Status.BAD_REQUEST).build();
			//else
			return Response.ok(newPermission).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermission(@PathParam("id") Integer id) {
		try {
			UserPermission permission = TenantManagerService.getInstance().getPermission(id); // logs if not found
			if(permission == null) 
				return Response.status(Status.NOT_FOUND).build(); 
			//else
			return Response.ok(permission).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@Path("{id}")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updatePermission(@PathParam("id") Integer id, MultivaluedMap<String,String> permissionParams) {
		try {
			UserPermission permission = TenantManagerService.getInstance().getPermission(id);
			if(permission != null) {
				UserPermission updatedPermission = updatePermissionFromParams(permission,permissionParams);
				if(updatedPermission != null) 
					return Response.ok(updatedPermission).build();
				//else 
				return Response.status(Status.BAD_REQUEST).build();
			}
			return Response.status(Status.NOT_FOUND).build(); // must create first
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Path("{id}")
	@DELETE
	public Response deletePermission(@PathParam("id") Integer id) {
		try {
			UserPermission permission = TenantManagerService.getInstance().getPermission(id);
			if (permission != null) {
				TenantManagerService.getInstance().deletePermission(permission);
				return Response.ok().build();
			}
			return Response.status(Status.NOT_FOUND).build(); // must create first
		} catch (Exception e) {
			LOGGER.error("Unexpected exception", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	/***** REST API methods above ^^^ private methods below ****/

	private UserPermission doCreatePermission(MultivaluedMap<String, String> permissionParams) {
		String descriptor = permissionParams.getFirst("descriptor");
		UserPermission newPermission = null;
		if(UserPermission.descriptorIsValid(descriptor)) {
			if (TenantManagerService.getInstance().getPermissionByDescriptor(descriptor) == null) {
				newPermission = TenantManagerService.getInstance().createPermission(descriptor);
			} else {
				LOGGER.warn("failed to create duplicate permission {}",descriptor);
			}
		} else {
			LOGGER.warn("could not create permission, valid descriptor not specified");
		}
		return newPermission;
	}

	private UserPermission updatePermissionFromParams(UserPermission permission, MultivaluedMap<String, String> permissionParams) {
		String newDescriptor = permissionParams.getFirst("descriptor");
		UserPermission updatedPermission = null;
		
		if(UserPermission.descriptorIsValid(newDescriptor)) {
			if(TenantManagerService.getInstance().getPermissionByDescriptor(newDescriptor) == null) {
				permission.setDescriptor(newDescriptor);
				updatedPermission = TenantManagerService.getInstance().updatePermission(permission);
			} else {
				LOGGER.warn("failed to update, duplicate permission {}",newDescriptor);
			}
		} else {
			LOGGER.warn("could not update permission, valid descriptor not specified");
		}
		
		return updatedPermission;
	}

}
