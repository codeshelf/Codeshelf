package com.codeshelf.api.resources.subresources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.resources.EventsResource;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

import lombok.Setter;

public class CheResource {
	@Context
	private ResourceContext								resourceContext;

	@Setter
	private Che che;
	
	private WorkBehavior workService;
	
	@Inject
	public CheResource(WorkBehavior workService) {
		this.workService = workService;
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

	
	@GET
	@Path("/computeinstructions")
	@RequiresPermissions("wi:compute")
	@Produces(MediaType.APPLICATION_JSON)
	public Response computeWorkInstructions(@QueryParam("containers") List<String> containers) {
		ErrorResponse errors = new ErrorResponse();

		try {
			Facility facility = che.getFacility();
			Map<String, String> validContainers = new HashMap<String,String>();
			int position = 1;
			for (String containerId : containers) {
				Container container = Container.staticGetDao().findByDomainId(facility, containerId);
				if (container != null) {
					validContainers.put(Integer.toString(position),containerId);
					position++;
				}
			}
			WorkList workList = workService.computeWorkInstructions(che, validContainers);
			return BaseResponse.buildResponse(workList);
		} catch (Exception e) {
			return errors.processException(e);
		} 
	}
}
