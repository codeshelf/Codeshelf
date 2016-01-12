package com.codeshelf.api.resources;

import java.util.List;
import java.util.UUID;

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
import org.hibernate.criterion.Property;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.resources.subresources.FacilityResource;
import com.codeshelf.api.responses.FacilityShort;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Point;
import com.codeshelf.scheduler.ApplicationSchedulerService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

@Path("/facilities")
public class FacilitiesResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(FacilitiesResource.class);

	@Context
	private ResourceContext resourceContext;
	private final WebSocketManagerService webSocketManagerService;
	private final ApplicationSchedulerService applicationSchedulerService;
	
	@Inject
	public FacilitiesResource(WebSocketManagerService webSocketManagerService, ApplicationSchedulerService applicationSchedulerService){
		this.webSocketManagerService = webSocketManagerService;
		this.applicationSchedulerService = applicationSchedulerService;
	}
	
	@Path("/{id}")
	@RequiresPermissions("companion:view")
	public FacilityResource getFacility(@PathParam("id") String idParam) throws Exception {
		Facility facility = null;
		try {
			UUID uuid = UUID.fromString(idParam);
			facility = Facility.staticGetDao().findByPersistentId(uuid);
		} catch(Exception e) {
			List<Facility> facilities = Facility.staticGetDao().findByFilter(
				ImmutableList.of(Property.forName("domainId").eq(idParam)));
			if (facilities.size() == 1) {
				facility = facilities.get(0);
			} else if (facilities.size() != 1) {
				throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
			}
		}
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
		if (facility != null) {
			String description = facility.getDescription();
			applicationSchedulerService.stopFacility(facility);
			facility.delete(webSocketManagerService);

			Facility recreatedFacility = Facility.createFacility(domainId, description, Point.getZeroPoint());
			try {
				applicationSchedulerService.startFacility(CodeshelfSecurityManager.getCurrentTenant(), recreatedFacility);
			} catch (SchedulerException e) {
				LOGGER.error("Unable to start the scheduler for the newly recreated facility {}", recreatedFacility, e);
			}
			return BaseResponse.buildResponse(recreatedFacility);
		} else {
			ErrorResponse response = new ErrorResponse();
			response.addBadParameter("domainId", domainId);
			return response.buildResponse();
		}
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
