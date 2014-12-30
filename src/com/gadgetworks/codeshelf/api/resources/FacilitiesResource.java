package com.gadgetworks.codeshelf.api.resources;

import java.util.List;

import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.servlet.http.HttpServletResponse;
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
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.sun.jersey.api.core.ResourceContext;

@Path("/facilities")
public class FacilitiesResource {
	@Context
	private ResourceContext resourceContext;
	
	private PersistenceService persistence = PersistenceService.getInstance();
	
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
			//Initialize lazy children collections in Facilities 
			for (Facility f : facilities){
				f.initialize();
			}
			return BaseResponse.buildResponse(facilities, HttpServletResponse.SC_OK);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
}
