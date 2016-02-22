package com.codeshelf.api.resources;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.codeshelf.api.BaseResponse.IntervalParam;
import com.codeshelf.api.BaseResponse.TimestampParam;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.EventResource;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

import lombok.Setter;

@Path("/events")
public class EventsResource {
	@Context
	private ResourceContext resourceContext;
	
	@Setter
	private Che che;
	

	private NotificationBehavior notificationBehavior;
	
	@Inject
	EventsResource(NotificationBehavior notificationBehavior) {
		this.notificationBehavior = notificationBehavior;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public ResultDisplay<EventDisplay> getEvents(@QueryParam("created") IntervalParam createdParam, @QueryParam("limit") Integer limitParam) {
		Optional<Interval> created = Optional.absent();
		if (createdParam !=null) {
			created = Optional.of(createdParam.getValue());
		}
		
		Optional<Integer> limit = Optional.fromNullable(limitParam);
		ResultDisplay<EventDisplay> results = this.notificationBehavior.getEventsForChe(che, created, limit);
		return results;
	}
	
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
	
	@POST
	@RequiresPermissions("event:create")
	public Response createEvent(@FormParam("created") TimestampParam created, @FormParam("type") String type, @FormParam("workerId") String workerId) throws URISyntaxException {
		EventType eventType = WorkerEvent.EventType.valueOf(type);
		WorkerEvent event = new WorkerEvent(new DateTime(created.getValue().getTime()), eventType, che, workerId);
		WorkerEvent.staticGetDao().store(event);
		return Response.created(new URI(event.getPersistentId().toString())).build();
	}
	
}
