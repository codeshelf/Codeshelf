package com.gadgetworks.codeshelf.api.resources;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.api.BaseResponse.UUIDParam;
import com.gadgetworks.codeshelf.api.ErrorResponse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

@Path("/import")
public class ImportResource {
	@Context
	private ResourceContext resourceContext;	
	private PersistenceService persistence = PersistenceService.getInstance();
	
	@POST
	@Path("/orders/{facilityId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadOrders(
		@PathParam("facilityId") String facilityId,
        @FormDataParam("file") InputStream fileInputStream,
        @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {

		try {
			persistence.beginTenantTransaction();
			// make sure facility exists
			Facility facility = Facility.DAO.findByPersistentId(facilityId);
			if (facility==null) {
				// facility not found
				return BaseResponse.buildResponse(null,404);
			}
			// read file into memory...
			return BaseResponse.buildResponse(null);
		}
		catch (Exception e) {
			ErrorResponse errors = new ErrorResponse();
			errors.processException(e);
			return errors.buildResponse();
		} 
		finally {
			persistence.commitTenantTransaction();
		}
	}
}
