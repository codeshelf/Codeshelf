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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.platform.multitenancy.ITenantManagerService;
import com.codeshelf.platform.multitenancy.Tenant;
import com.codeshelf.platform.multitenancy.TenantManagerService;

@Path("/tenants")
public class TenantsResource {
	private static final Logger	LOGGER				= LoggerFactory.getLogger(TenantsResource.class);
	private static final Set<String>	validCreateTenantFields	= new HashSet<String>();
	static {
		validCreateTenantFields.add("name");
		validCreateTenantFields.add("schemaname");
	}

	@GET
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
	

	/***** REST API methods above ^^^ private methods below ****/
		
	private Tenant updateTenantFromParams(Tenant tenant, MultivaluedMap<String, String> tenantParams) {
		// not in session - tenant is detached object 
		// iterate through all tenantParams and set fields accordingly.'
		// if there are no parameters, or any parameters are unrecognized or cannot be set, the operation fails and returns null.
		// if the tenant is changed, it will be saved and the persistent result returned.
		
		ITenantManagerService manager = TenantManagerService.getInstance();
		boolean updated = false;

		for(String key : tenantParams.keySet()) {
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
				tenant.setActive(active);
			} // else ignore if no change
			success=true;
		} else {
			// TODO: support updating other user fields here  
			LOGGER.warn("update tenant {} - unknown key {}",tenant.getName(),key);
		}
		return success;
	}

	private Tenant doCreateTenant(MultivaluedMap<String, String> tenantParams) {
		ITenantManagerService manager = TenantManagerService.getInstance();
		Tenant newTenant = null;
		
		Map<String,String> validFields = RootResource.validFieldsOnly(tenantParams, validCreateTenantFields);
		if(validFields != null) {
			String tenantName = validFields.get("name");
			String schemaName = validFields.get("schemaname");

			if(tenantName != null) {
				if(Tenant.isValidSchemaName(schemaName)) {
					if(manager.canCreateTenant(tenantName,schemaName)) {
						newTenant = manager.createTenant(tenantName, 
							manager.getDefaultShard().getName(), 
							schemaName);
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
		}
		return newTenant;
	}

}
