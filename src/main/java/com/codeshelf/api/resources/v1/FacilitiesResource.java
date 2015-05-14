package com.codeshelf.api.resources.v1;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.resources.subresources.FacilityResource;
import com.codeshelf.api.responses.FacilityShort;
import com.codeshelf.model.domain.Facility;
import com.sun.jersey.api.core.ResourceContext;

@Path("/facilitiesv")
//@Produces("application/vnd.musicstore-v1+json")
@Produces("application/v1")
public class FacilitiesResource {
	@Context
	private ResourceContext resourceContext;
	
	@Path("{id}")
	public FacilityResource getManufacturer(@PathParam("id") UUIDParam uuidParam) throws Exception {
		Facility facility = Facility.staticGetDao().findByPersistentId(uuidParam.getValue());
		if (facility == null) {
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		FacilityResource r = resourceContext.getResource(FacilityResource.class);
	    r.setFacility(facility);
	    return r;
    }
	
	private Response getAllFacilities() {
		List<Facility> facilities = Facility.staticGetDao().getAll();
		List<FacilityShort> facilitiesShort = FacilityShort.generateList(facilities);
		return BaseResponse.buildResponse(facilitiesShort);
	}
	
	@GET
    @Produces("application/v1+json")
    public Response getV1(@PathParam("id") int id) {
        return getAllFacilities();
    }
    
	@GET
    @Produces("application/v2+json")    
    public Response getV2(@PathParam("id") int id) {
		return getAllFacilities();
    }
}
