package com.codeshelf.api.resources;

import java.util.List;

import javax.script.ScriptException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.service.ExtensionPointEngine;
import com.codeshelf.service.ExtensionPointType;
import com.google.inject.Inject;

import lombok.Getter;
import lombok.Setter;

public class ExtensionPointsResource {

	
	private EdiExportService provider;

	@Inject
	ExtensionPointsResource(EdiExportService provider) {
		this.provider = provider;
	}
	
	@Getter
	@Setter
	private ExtensionPointEngine extensionPointEngine;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		List<ExtensionPoint> results = extensionPointEngine.getAllExtensions();
		return BaseResponse.buildResponse(results);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@FormParam("type") String type) {
		ExtensionPointType typeEnum = ExtensionPointType.valueOf(type); 
		try {
			ExtensionPoint point = extensionPointEngine.create(typeEnum);
			provider.updateEdiExporterSafe(extensionPointEngine.getFacility());
			return BaseResponse.buildResponse(point);
		} catch(ScriptException e) {
			ErrorResponse response = new ErrorResponse();
			response.addError(e.getMessage());
			response.setStatus(Status.BAD_REQUEST);
			return response.buildResponse();
		}
	}

	@DELETE
	@Path("/{persistentId}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@PathParam("persistentId") UUIDParam extensionPointId)  {
		ExtensionPoint point = extensionPointEngine.findById(extensionPointId.getValue());
		extensionPointEngine.delete(point);
		return BaseResponse.buildResponse(true);
	}
	
	@PUT
	@Path("/{persistentId}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@PathParam("persistentId") UUIDParam extensionPointId, @FormParam("active") boolean active, @FormParam("script") String script)  {
		ExtensionPoint point = extensionPointEngine.findById(extensionPointId.getValue());
		point.setScript(script);
		point.setActive(active);
		try {
			extensionPointEngine.update(point);
			provider.updateEdiExporterSafe(extensionPointEngine.getFacility());
			return BaseResponse.buildResponse(point);
		} catch(ScriptException e) {
			ErrorResponse response = new ErrorResponse();
			response.addError(e.getMessage());
			return response.buildResponse();
		}
	}

	
}
