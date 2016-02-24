package com.codeshelf.api.resources.subresources;

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

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.resources.EventsResource;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.NotificationBehavior.HistogramParams;
import com.codeshelf.behavior.NotificationBehavior.HistogramResult;
import com.codeshelf.model.domain.Worker;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

import lombok.Setter;

public class WorkerResource {
	
	@Context
	private ResourceContext								resourceContext;
	
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

	
	@Path("/events")
	public EventsResource getEvents() {
		EventsResource r = resourceContext.getResource(EventsResource.class);
		r.setWorker(worker);
		return r;
	}

	//TODO move to events resource for carts and worker
	@GET
	@Path("/events/histogram")
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEventHistogram(@Context UriInfo uriInfo) throws Exception {
		ErrorResponse errors = new ErrorResponse();
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
				HistogramParams params = new HistogramParams(queryParams);  
				HistogramResult result = notificationBehavior.pickRateHistogram(params, worker);
				return BaseResponse.buildResponse(result);
			} catch (Exception e) {
				return errors.processException(e);
			}
		}

	
}
