package com.codeshelf.api.resources.subresources;

import java.sql.Timestamp;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.WorkerEvent;

public class EventResource {
	@Setter
	private WorkerEvent event;

	@GET
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvent() {
		EventDisplay eventDisplay = new EventDisplay(event);
		return BaseResponse.buildResponse(eventDisplay);
	}

	@PUT
	@Path("resolve")
	@RequiresPermissions("event:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response resolveEvent(@QueryParam("resolvedBy") String resolvedBy) {
		ErrorResponse errors = new ErrorResponse();
		try {
			Resolution resolution = event.getResolution();
			if (resolution == null) {
				resolution = new Resolution();
			}
			resolution.setFacility(event.getFacility());
			resolution.setDomainId(event.getDomainId());
			resolution.setResolvedBy(resolvedBy);
			resolution.setTimestamp(new Timestamp(System.currentTimeMillis()));
			event.setResolution(resolution);
			Resolution.staticGetDao().store(resolution);
			WorkerEvent.staticGetDao().store(event);
			EventDisplay eventDisplay = new EventDisplay(event);
			return BaseResponse.buildResponse(eventDisplay);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

}
