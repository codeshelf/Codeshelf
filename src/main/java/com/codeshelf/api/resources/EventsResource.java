package com.codeshelf.api.resources;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static org.hibernate.criterion.Order.desc;
import static org.hibernate.criterion.Property.forName;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Property;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.codeshelf.api.BaseResponse.TimestampParam;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.EventResource;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.PageQuery;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.api.uri.UriComponent;

import lombok.Setter;

@Path("/events")
public class EventsResource {
	@Context
	private ResourceContext resourceContext;
	
	@Setter
	private Che che;

	@Setter
	private Worker worker;


	private NotificationBehavior notificationBehavior;
	
	@Inject
	public EventsResource(NotificationBehavior notificationBehavior) {
		this.notificationBehavior = notificationBehavior;
	}

	
	private MultivaluedMap<String, String> toParametersToUse(UriInfo uriInfo) throws UnsupportedEncodingException {
		MultivaluedMap<String, String> requestParameters = MoreObjects.firstNonNull(uriInfo.getQueryParameters(), new Form());
		if (requestParameters.containsKey("next")) {
			String queryString = requestParameters.getFirst("next");
			return UriComponent.decodeQuery(URLDecoder.decode(queryString, "UTF-8"), true);
		}
		else {
			return requestParameters;
		}
	}

	@GET
	//@RequiresPermissions("events:view")
	@Produces(MediaType.APPLICATION_JSON)
	public ResultDisplay<EventDisplay> getPagedEvents(@Context UriInfo uriInfo) throws Exception {
		MultivaluedMap<String, String> parametersToUse = toParametersToUse(uriInfo);

		DateTime now = DateTime.now();
		Interval created = new Interval(now.minusDays(14), now); //default
		String createdIntervalValue = parametersToUse.getFirst("created");
		if (createdIntervalValue != null) {
			created = new Interval(createdIntervalValue);
		}
		
		List<Criterion> filter = new ArrayList<Criterion>();
		filter.add(GenericDaoABC.createIntervalRestriction("created", created));

		if (che != null) {
			filter.add(Property.forName("devicePersistentId").eq(che.getPersistentId().toString()));
		}
		
		if (worker != null) {
			filter.add(forName("parent").eq(worker.getFacility()));
			filter.add(forName("workerId").eq(worker.getDomainId()));
		}

		PageQuery pageQuery = new PageQuery(
			parametersToUse,
			copyOf(filter),
			of(desc("created")));
		ResultDisplay<EventDisplay> results = notificationBehavior.getPagedEvents(pageQuery);
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
