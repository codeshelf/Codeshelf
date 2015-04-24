package com.codeshelf.api.resources;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.EventResource;
import com.codeshelf.model.domain.WorkerEvent;
import com.sun.jersey.api.core.ResourceContext;

@Path("/events")
public class EventsResource {
	@Context
	private ResourceContext resourceContext;
	
	@Path("{id}")
	@RequiresPermissions("event:view")
	public EventResource getEvent(@PathParam("id") UUIDParam uuidParam) throws Exception {
		WorkerEvent event = WorkerEvent.staticGetDao().findByPersistentId(uuidParam.getValue());
		if (event == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		EventResource r = resourceContext.getResource(EventResource.class);
	    r.setEvent(event);
	    return r;
	}
}
