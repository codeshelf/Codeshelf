package com.codeshelf.api.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.FacilityResource;
import com.codeshelf.api.responses.FacilityShort;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

@Path("/facilities")
public class FacilitiesResource {
	@Context
	private ResourceContext resourceContext;
	private final WebSocketManagerService webSocketManagerService;
	
	@Inject
	public FacilitiesResource(WebSocketManagerService webSocketManagerService){
		this.webSocketManagerService = webSocketManagerService;
	}
	
	@Path("{id}")
	@RequiresPermissions("companion:view")
	public FacilityResource getManufacturer(@PathParam("id") UUIDParam uuidParam) throws Exception {
		Facility facility = Facility.staticGetDao().findByPersistentId(uuidParam.getValue());
		if (facility == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		FacilityResource r = resourceContext.getResource(FacilityResource.class);
	    r.setFacility(facility);
	    return r;
	}
	
	@GET
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllFacilities() {
		List<Facility> facilities = Facility.staticGetDao().getAll();
		List<FacilityShort> facilitiesShort = FacilityShort.generateList(facilities);
		return BaseResponse.buildResponse(facilitiesShort);
	}
	
	@POST
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addFacility(@FormParam("domainId") String domainId, @FormParam("description") String description) {
		Facility facility = Facility.createFacility(domainId, description, Point.getZeroPoint());
		return BaseResponse.buildResponse(facility);
	}
	
	@POST
	@Path("/recreate/{domainId}")
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response recreateFacility(@PathParam("domainId") String domainId) {
		Facility facility = Facility.staticGetDao().findByDomainId(null, domainId);
		String description = facility.getDescription();
		facility.delete(webSocketManagerService);
		Facility recreatedFacility = Facility.createFacility(domainId, description, Point.getZeroPoint());
		return BaseResponse.buildResponse(recreatedFacility);
	}
	
	
	@DELETE
	@RequiresPermissions("facility:edit")
	public Response deleteFacilities(){
		try {
			Facility.deleteAll(webSocketManagerService);
			return BaseResponse.buildResponse("Facilities Deleted");
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
}
