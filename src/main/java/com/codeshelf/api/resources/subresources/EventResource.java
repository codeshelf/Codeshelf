package com.codeshelf.api.resources.subresources;

import java.sql.Timestamp;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.model.domain.Resolution;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.security.CodeshelfSecurityManager;

public class EventResource {
	@Setter
	private WorkerEvent event;

	@GET
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvent() {
		EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
		return BaseResponse.buildResponse(eventDisplay);
	}

	@POST
	@Path("resolve")
	@RequiresPermissions("event:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resolveEvent() {
		String resolvedBy = CodeshelfSecurityManager.getCurrentUserContext().getUsername();
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
			
			EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
			return BaseResponse.buildResponse(eventDisplay);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

}
