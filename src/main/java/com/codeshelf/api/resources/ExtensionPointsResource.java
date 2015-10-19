package com.codeshelf.api.resources;

import java.util.List;

import javax.script.ScriptException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.service.ExtensionPointEngine;
import com.codeshelf.service.ExtensionPointType;
import com.google.inject.Inject;

public class ExtensionPointsResource {

	
	private EdiExportService provider;

	@Inject
	ExtensionPointsResource(EdiExportService provider) {
		this.provider = provider;
	}
	
	@Getter
	@Setter
	private ExtensionPointEngine extensionPointService;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		List<ExtensionPoint> results = extensionPointService.getAllExtensions();
		return BaseResponse.buildResponse(results);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@FormParam("type") String type) throws ScriptException {
		ExtensionPointType typeEnum = ExtensionPointType.valueOf(type); 
		ExtensionPoint point = extensionPointService.createExtensionPoint(typeEnum);
		provider.updateEdiExporterSafe(extensionPointService.getFacility());
		return BaseResponse.buildResponse(point);
	}

	@PUT
	@Path("/{persistentId}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@PathParam("persistentId") UUIDParam extensionPointId, @FormParam("active") boolean active, @FormParam("script") String script) throws ScriptException {
		ExtensionPoint point = extensionPointService.findById(extensionPointId.getValue());
		point.setScript(script);
		point.setActive(active);
		extensionPointService.update(point);
		provider.updateEdiExporterSafe(extensionPointService.getFacility());
		return BaseResponse.buildResponse(point);
	}

	
}
