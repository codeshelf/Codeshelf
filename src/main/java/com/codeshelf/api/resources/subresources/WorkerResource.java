package com.codeshelf.api.resources.subresources;

import static com.google.common.collect.ImmutableList.of;
import static org.hibernate.criterion.Order.desc;
import static org.hibernate.criterion.Property.forName;

import java.net.URLDecoder;
import java.sql.Timestamp;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.api.responses.ResultDisplay;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.model.dao.PageQuery;
import com.codeshelf.model.domain.Worker;
import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.api.uri.UriComponent;

import lombok.Setter;

public class WorkerResource {
	@Setter
	private Worker worker;
	private NotificationBehavior notificationBehavior;
	
	@Inject
	public WorkerResource(NotificationBehavior notificationBehavior) {
		this.notificationBehavior = notificationBehavior;
	}
	
	@GET
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorker() {
		return BaseResponse.buildResponse(worker);
	}
	
	@PUT
	@RequiresPermissions("worker:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateWorker(Worker updatedWorker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			if (!updatedWorker.isValid(errors)){ 
				return errors.buildResponse();
			}
			Worker workerWithSameBadge = Worker.findTenantWorker(updatedWorker.getDomainId());
			if (workerWithSameBadge != null && !worker.equals(workerWithSameBadge)) {
				errors.addError("Another worker with badge " + updatedWorker.getDomainId() + " already exists");
				return errors.buildResponse();
			}
			//Update old object with new values
			worker.update(updatedWorker);
			//Save old object to the DB
			Worker.staticGetDao().store(worker);
			Worker savedWorker = Worker.staticGetDao().findByPersistentId(worker.getPersistentId());
			return BaseResponse.buildResponse(savedWorker);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}
	
	@DELETE
	@RequiresPermissions("worker:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteWorker(Worker updatedWorker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			Worker.staticGetDao().delete(worker);
			return BaseResponse.buildResponse("Not yet implemented");
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@GET
	@Path("/events")
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvents(@Context UriInfo uriInfo) throws Exception {
		MultivaluedMap<String, String> requestParameters = new Form();
		if (uriInfo != null) {
			requestParameters =  uriInfo.getQueryParameters();	
		}
		
		MultivaluedMap<String, String> parametersToUse = null;
		if (requestParameters.containsKey("next")) {
			String queryString = requestParameters.getFirst("next");
			parametersToUse  = UriComponent.decodeQuery(URLDecoder.decode(queryString, "UTF-8"), true);
		}
		else {
			parametersToUse = requestParameters;
		}
		
		//expects pseudo param moniker "<" + timestamp;
		//set default if it doesn't exist
		DateTime now = DateTime.now();
		parametersToUse.putSingle("created", 
			MoreObjects.firstNonNull(parametersToUse.getFirst("created"), new Interval(now.minusDays(14), now).toString()));
		
		String createdIntervalValue = parametersToUse.getFirst("created");
		Interval maxCreated = new Interval(createdIntervalValue); 
		PageQuery pageQuery = new PageQuery(
			parametersToUse,
			of(forName("parent").eq(worker.getFacility()),
			   forName("workerId").eq(worker.getDomainId()),
			   forName("created").between(new Timestamp(maxCreated.getStartMillis()), new Timestamp(maxCreated.getEndMillis()))),
			of(desc("created")));
		ResultDisplay<EventDisplay> results = notificationBehavior.getEventsForWorkerId(pageQuery);
		return BaseResponse.buildResponse(results);
	}


	
}
