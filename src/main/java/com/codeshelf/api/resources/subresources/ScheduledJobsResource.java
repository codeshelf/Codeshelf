package com.codeshelf.api.resources.subresources;

import java.text.ParseException;
import java.util.List;

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
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ScheduledJob;
import com.codeshelf.scheduler.ApplicationSchedulerService;
import com.codeshelf.scheduler.ApplicationSchedulerService.ScheduledJobView;
import com.codeshelf.scheduler.ScheduledJobType;
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
	@Produces(MediaType.APPLICATION_JSON)
	public Response getScheduledJobs() throws SchedulerException {
		List<ScheduledJobView> jobs = schedulerService.getScheduledJobs(facility);
		return BaseResponse.buildResponse(jobs);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@FormParam("type") String type) {
		ScheduledJobType typeEnum = ScheduledJobType.valueOf(type); 
		try {
			ScheduledJob job = new ScheduledJob(this.facility, typeEnum, typeEnum.getDefaultSchedule());
			schedulerService.scheduleJob(job);
			return BaseResponse.buildResponse(job);
		} catch(Exception e) {
			ErrorResponse response = new ErrorResponse();
			response.addError(e.getMessage());
			response.setStatus(Status.BAD_REQUEST);
			return response.buildResponse();
		}
	}

	
	@GET
	@Path("/{type}/schedule")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedule(@PathParam("type") String typeStr) throws SchedulerException {
		ScheduledJobType type = ScheduledJobType.valueOf(typeStr);
		Optional<CronExpression > schedule = schedulerService.findSchedule(facility, type);
		String cronExpression = "";
		if (schedule.isPresent()) {
			cronExpression = schedule.get().getCronExpression();
		} 
		return BaseResponse.buildResponse(ImmutableMap.of("cronExpression", cronExpression));
	}

	@POST
	@Path("/{type}/schedule")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateSchedule(@PathParam("type") String typeStr, @FormParam("cronExpression") String cronExpression) throws SchedulerException {
		ScheduledJobType type = ScheduledJobType.valueOf(typeStr);
		try {
			schedulerService.scheduleJob(new ScheduledJob(facility, type, cronExpression));
			return BaseResponse.buildResponse(ImmutableMap.of("cronExpression", cronExpression));
		}
		catch(ParseException e) {
			ErrorResponse response = new ErrorResponse();
			response.addBadParameter(cronExpression, "cronExpression");
			return response.buildResponse();
		}
	}

	//TODO This will probably change to return a resource that you can then DELETE to cancel
	@POST
	@Path("/{type}/trigger")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateSchedule(@PathParam("type") String typeStr) throws SchedulerException {
		ScheduledJobType type = ScheduledJobType.valueOf(typeStr);
		schedulerService.triggerJob(facility, type);
		return BaseResponse.buildResponse(null);
	}

	//TODO This will probably change to a resource representing  the running job
	@POST
	@Path("/{type}/cancel")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cancelRun(@PathParam("type") String typeStr) throws SchedulerException {
		ScheduledJobType type = ScheduledJobType.valueOf(typeStr);
		boolean result = schedulerService.cancelJob(facility, type);
		return BaseResponse.buildResponse(result);
	}
	
}
