package com.codeshelf.api.resources.subresources;

import java.util.List;
import java.util.UUID;

import javax.websocket.server.PathParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.CSVParam;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.resources.EventsResource;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.NotificationBehavior.HistogramParams;
import com.codeshelf.behavior.NotificationBehavior.HistogramResult;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;

import lombok.Setter;

public class CheResource {

	private static final Logger		LOGGER	= LoggerFactory.getLogger(CheResource.class);

	@Context
	private ResourceContext								resourceContext;

	@Setter
	private Che che;
	
	private WorkBehavior workBehavior;
	private NotificationBehavior notificationBehavior;
	
	@Inject
	public CheResource(WorkBehavior workService, NotificationBehavior notificationBehavior) {
		this.workBehavior = workService;
		this.notificationBehavior = notificationBehavior;
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
	@Path("/events/histogram")
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEventHistogram(@Context UriInfo uriInfo) throws Exception {
		ErrorResponse errors = new ErrorResponse();
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			HistogramParams params = new HistogramParams(queryParams);   
			HistogramResult result = notificationBehavior.pickRateHistogram(params, che);
			return BaseResponse.buildResponse(result);
		} catch (Exception e) {
			return errors.processException(e);
		}
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
	
	@POST
	@Path("/workinstructions/complete")
	@RequiresPermissions("che:simulate")
	@Produces(MediaType.APPLICATION_JSON)
	public Response completeWorkinstruction(@FormParam("wiPersistentId") UUIDParam wiPersistentId, @FormParam("workerBadgeId") String workerBadgeId) {
		ErrorResponse errors = new ErrorResponse();

		try {
			UUID wiId = wiPersistentId.getValue();
			WorkInstruction wi = WorkInstruction.staticGetDao().findByPersistentId(wiId);
			wi.setCompleteState(workerBadgeId, wi.getPlanQuantity());
			WorkInstruction updatedWi = workBehavior.completeWorkInstruction(che.getPersistentId(), wi);
			return BaseResponse.buildResponse(updatedWi);
		} catch (Exception e) {
			return errors.processException(e);
		} 
	}
	
	@GET
	@Path("/workinstructions")
	@RequiresPermissions("che:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkInstructions() {
		ErrorResponse errors = new ErrorResponse();

		try {
			List<WorkInstruction> workInstructions = workBehavior.getWorkInstructions(che, null);
			return BaseResponse.buildResponse(new ResultDisplay<WorkInstruction>(workInstructions));
		} catch (Exception e) {
			return errors.processException(e);
		} 
	}
	
	
	
	@POST
	@Path("/commands/{commandName}")
	@RequiresPermissions("che:commands")
	@Produces(MediaType.APPLICATION_JSON)
	public void cheCommand(@PathParam("commandName") String commandName) throws Exception {
		LOGGER.info("command {} for che {}", commandName, this.che);
	}
	
}
