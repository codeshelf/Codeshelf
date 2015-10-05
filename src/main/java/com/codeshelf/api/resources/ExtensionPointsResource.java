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
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.EdiExportService;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.ExtensionPointService;
import com.codeshelf.service.ExtensionPointType;
import com.google.inject.Inject;
import com.sun.jersey.api.client.ClientResponse.Status;

public class ExtensionPointsResource {

	
	private EdiExportService provider;

	@Inject
	ExtensionPointsResource(EdiExportService provider) {
		this.provider = provider;
	}
	
	@Getter
	@Setter
	private Facility facility;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		List<ExtensionPoint> results = ExtensionPoint.staticGetDao().findByParent(facility);
		return BaseResponse.buildResponse(results);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@FormParam("type") String type) {
		ExtensionPointType typeEnum = ExtensionPointType.valueOf(type); 
		ExtensionPoint point = new ExtensionPoint(facility, typeEnum);
		ExtensionPoint.staticGetDao().store(point);
		provider.extensionPointsUpdated(facility);
		return BaseResponse.buildResponse(point);
	}

	@PUT
	@Path("/{persistentId}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@PathParam("persistentId") String extensionPointId, @FormParam("active") boolean active, @FormParam("script") String script) {
		ExtensionPoint point = ExtensionPoint.staticGetDao().findByPersistentId(extensionPointId);
		point.setScript(script);
		point.setActive(active);
		ExtensionPoint.staticGetDao().store(point);
		provider.extensionPointsUpdated(facility);
		return BaseResponse.buildResponse(point);
	}

	
}
