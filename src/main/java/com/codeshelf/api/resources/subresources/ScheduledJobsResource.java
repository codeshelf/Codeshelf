package com.codeshelf.api.resources.subresources;

import java.text.ParseException;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.quartz.CronExpression;
import org.quartz.SchedulerException;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.application.FacilitySchedulerService;
import com.codeshelf.model.ScheduledJobType;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.scheduler.ApplicationSchedulerService;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import lombok.Setter;

public class ScheduledJobsResource {

	@Setter
	private Facility facility;
	private ApplicationSchedulerService schedulerService;

	@Inject
	public ScheduledJobsResource(ApplicationSchedulerService service) {
		schedulerService = service;
	}
	
	@GET
	@Path("/{type}/schedule")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedule(@PathParam("type") String typeStr) throws SchedulerException {
		Optional<FacilitySchedulerService> facilityScheduler = schedulerService.findService(facility);
		if (facilityScheduler.isPresent()) {
			ScheduledJobType type = ScheduledJobType.valueOf(typeStr);
			CronExpression expression = facilityScheduler.get().findSchedule(type);
			if (expression != null) {
				return BaseResponse.buildResponse(ImmutableMap.of("cronExpression", expression.getCronExpression()));
			} else {
				return BaseResponse.buildResponse(null, Status.NOT_FOUND);
			}
		} else {
			return BaseResponse.buildResponse(null, Status.NOT_FOUND);
		}
	}

	@POST
	@Path("/{type}/schedule")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateSchedule(@PathParam("type") String typeStr, @FormParam("cronExpression") String cronExpression) throws SchedulerException {
		Optional<FacilitySchedulerService> facilityScheduler = schedulerService.findService(facility);
		if (facilityScheduler.isPresent()) {
			ScheduledJobType type = ScheduledJobType.valueOf(typeStr);
			try {
				CronExpression newCronExpression = new CronExpression(cronExpression);
				facilityScheduler.get().schedule(newCronExpression, type);
				return BaseResponse.buildResponse(ImmutableMap.of("cronExpression", newCronExpression));
			}
			catch(ParseException e) {
				ErrorResponse response = new ErrorResponse();
				response.addBadParameter(cronExpression, "cronExpression");
				return response.buildResponse();
			}
		} else {
			return BaseResponse.buildResponse(null, Status.NOT_FOUND);
		}
	}

}
