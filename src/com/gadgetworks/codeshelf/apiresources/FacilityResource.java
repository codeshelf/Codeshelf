package com.gadgetworks.codeshelf.apiresources;

import java.util.List;

import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

@Path("/facilities")
public class FacilityResource {
	private PersistenceService persistence = PersistenceService.getInstance();
	
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
