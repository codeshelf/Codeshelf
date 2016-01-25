package com.codeshelf.api.resources.subresources;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.CSVParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.resources.EventsResource;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

import lombok.Setter;

public class CheResource {
	@Context
	private ResourceContext								resourceContext;

	@Setter
	private Che che;
	
	private WorkBehavior workBehavior;
	
	@Inject
	public CheResource(WorkBehavior workService) {
		this.workBehavior = workService;
	}
	
	@GET
	@RequiresPermissions("che:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		return BaseResponse.buildResponse(che);
	}
	
	@Path("/events")
	public EventsResource getEvents() {
		EventsResource r = resourceContext.getResource(EventsResource.class);
		r.setChe(che);
		return r;
	}

	
	@POST
	@Path("/workinstructions/compute")
	@RequiresPermissions("che:simulate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response computeWorkInstructions(@FormParam("containers") CSVParam containers) {
		ErrorResponse errors = new ErrorResponse();

		try {
			WorkList workList = workBehavior.setUpCheContainerFromString(che, containers.getRawValue());
			return BaseResponse.buildResponse(workList);
		} catch (Exception e) {
			return errors.processException(e);
		} 
	}
}
