package com.codeshelf.api.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.FacilityResource;
import com.codeshelf.api.responses.FacilityShort;
import com.codeshelf.model.domain.Facility;
import com.sun.jersey.api.core.ResourceContext;

@Path("/facilities")
public class FacilitiesResource {
	@Context
	private ResourceContext resourceContext;
	
	@Path("{id}")
	public FacilityResource getManufacturer(@PathParam("id") UUIDParam uuidParam) throws Exception {
		FacilityResource r = resourceContext.getResource(FacilityResource.class);
	    r.setMUUIDParam(uuidParam);
	    return r;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllFacilities() {
		List<Facility> facilities = Facility.staticGetDao().getAll();
		List<FacilityShort> facilitiesShort = FacilityShort.generateList(facilities);
		return BaseResponse.buildResponse(facilitiesShort);
	}
}
