package com.gadgetworks.codeshelf.api.resources;

import java.util.List;

import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.api.BaseResponse.UUIDParam;
import com.gadgetworks.codeshelf.api.ErrorResponse;
import com.gadgetworks.codeshelf.api.resources.subresources.FacilityResource;
import com.gadgetworks.codeshelf.api.responses.FacilityShort;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.platform.persistence.TenantPersistenceService;
import com.sun.jersey.api.core.ResourceContext;

@Path("/facilities")
public class FacilitiesResource {
	@Context
	private ResourceContext resourceContext;
	
	private TenantPersistenceService persistence = TenantPersistenceService.getInstance();
	
	@Path("{id}")
	public FacilityResource getManufacturer(@PathParam("id") UUIDParam uuidParam) throws Exception {
		FacilityResource r = resourceContext.getResource(FacilityResource.class);
	    r.setMUUIDParam(uuidParam);
	    return r;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@OneToMany(fetch=FetchType.EAGER)
	public Response getAllFacilities() {
		ErrorResponse errors = new ErrorResponse();
		try {
			persistence.beginTenantTransaction();
			List<Facility> facilities = Facility.DAO.getAll();
			List<FacilityShort> facilitiesShort = FacilityShort.generateList(facilities);
			return BaseResponse.buildResponse(facilitiesShort);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
}
