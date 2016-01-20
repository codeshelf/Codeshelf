package com.codeshelf.manager.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.service.ITenantManagerService;
import com.codeshelf.manager.service.TenantManagerService;
import com.codeshelf.util.FormUtility;

@Path("/tenants")
@RequiresPermissions("tenant")
public class TenantsResource {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(TenantsResource.class);
	private static final Set<String>	validCreateTenantFields	= new HashSet<String>();
	private static final Set<String>	validUpdateTenantFields	= new HashSet<String>();
	static {
		validCreateTenantFields.add("name");
		validCreateTenantFields.add("schemaname");
		validUpdateTenantFields.add("name");
		validUpdateTenantFields.add("active");
	}

	@GET
	@RequiresPermissions("tenant:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		try {
			List<Tenant> tenantList = TenantManagerService.getInstance().getTenants();
			return Response.ok(tenantList).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@POST
	@RequiresPermissions("tenant:create")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createTenant(MultivaluedMap<String,String> tenantParams) {
		try {
			Tenant newTenant = doCreateTenant(tenantParams);		
			if(newTenant == null)
				return Response.status(Status.BAD_REQUEST).build();
			//else
			return Response.ok(newTenant).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@Path("{id}")
	@GET
	@RequiresPermissions("tenant:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTenant(@PathParam("id") Integer id) {
		try {
			Tenant tenant = TenantManagerService.getInstance().getTenant(id); // logs if not found
			if(tenant == null) 
				return Response.status(Status.NOT_FOUND).build(); 
			//else
			return Response.ok(tenant).build();
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@Path("{id}")
	@POST
	@RequiresPermissions("tenant:edit")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateTenant(@PathParam("id") Integer id, MultivaluedMap<String,String> tenantParams) {
		try {
			Tenant tenant = TenantManagerService.getInstance().getTenant(id);
			if(tenant != null) {
				Tenant updatedTenant = updateTenantFromParams(tenant,tenantParams);
				if(updatedTenant != null) 
					return Response.ok(updatedTenant).build();
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
	@RequiresPermissions("tenant:edit")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response deleteTenant(@PathParam("id") Integer id) {
		try {
			Tenant tenant = TenantManagerService.getInstance().getTenant(id);
			if(tenant != null) {
				ITenantManagerService manager = TenantManagerService.getInstance();
				manager.deleteTenant(tenant);
				return Response.status(Status.OK).build(); 
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		} catch (Exception e) {
			LOGGER.error("Unexpected exception",e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/***** REST API methods above ^^^ private methods below ****/
		
	private Tenant updateTenantFromParams(Tenant tenant, MultivaluedMap<String, String> tenantParams) {
		// not in session - tenant is detached object 
		// iterate through all tenantParams and set fields accordingly.'
		// if there are no parameters, or any parameters are unrecognized or cannot be set, the operation fails and returns null.
		// if the tenant is changed, it will be saved and the persistent result returned.
		
		ITenantManagerService manager = TenantManagerService.getInstance();
		boolean updated = false;

		for(String key : tenantParams.keySet()) {
			if(validUpdateTenantFields.contains(key)) {
				List<String> values = tenantParams.get(key);
				if(values == null) {
					LOGGER.warn("update tenant {} - no value for key {}",tenant.getName(),key);
					updated = false;
					break; // key with no values
				} else if(values.size() != 1) {
					LOGGER.warn("update tenant {} - multiple values for key {}",tenant.getName(),key);
					updated = false;
					break; // multiple values specified				
				} else if(updateTenant(tenant,key,values.get(0))) {
					updated = true;
				} else {
					updated = false;
					break;
				}
			} else {
				LOGGER.warn("update tenant {} - unknown key {}",tenant.getName(),key);
				updated = false;
				break;
			}
		}

		if(updated) {
			return manager.updateTenant(tenant); // request updating user in database
		}
		//else
		return null;
	}

	private boolean updateTenant(Tenant tenant, String key, String value) {
		if(tenant == null || key == null || value == null)
			return false;
		
		// field name already validated from list (see top)
		boolean success = false;
		if(key.equals("name")) {
			String oldName = tenant.getName();
			if(oldName.equals(value)) {
				success = true;
			} if(TenantManagerService.getInstance().getTenantByName(value) == null) {
				tenant.setName(value);
				LOGGER.info("update tenant - change name from {} to {}",oldName,value);
				success = true;
			} else {
				LOGGER.warn("could not change name from {} to {} - name exists",oldName,value);
			}
		} else if (key.equals("active")) {
			Boolean active = Boolean.valueOf(value);
			if(tenant.isActive() != active) {
				LOGGER.info("update tenant {} - set active = {}", tenant.getName(),active);
				tenant.setActive(active);
			} // else ignore if no change
			success=true;
		} 
		// add other fields to support here
		return success;
	}

	private Tenant doCreateTenant(MultivaluedMap<String, String> tenantParams) {
		ITenantManagerService manager = TenantManagerService.getInstance();
		Tenant newTenant = null;
		
		Map<String,String> validFields = FormUtility.getValidFields(tenantParams, validCreateTenantFields);
		if(validFields != null) {
			String tenantName = validFields.get("name");
			String schemaName = validFields.get("schemaname");

			if(tenantName != null) {
				if(Tenant.isValidSchemaName(schemaName)) {
					if(manager.canCreateTenant(tenantName,schemaName)) {
						newTenant = manager.createTenant(tenantName, schemaName);
						if(newTenant == null) 
							LOGGER.warn("failed to create tenant {}",tenantName);
					} else {
						LOGGER.warn("cannot create tenant {} - name or schemaName duplicate",tenantName);
					}
				} else {
					LOGGER.warn("cannot create tenant schema {} - schemaname not accepted",schemaName);
				}
			} else {
				LOGGER.warn("cannot create tenant - incomplete data");
			}
		} // else validFieldsOnly will log reason
		return newTenant;
	}

}
