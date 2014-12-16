package com.gadgetworks.codeshelf.application.apiresources;

import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gadgetworks.codeshelf.model.domain.ProductivitySummary;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.ws.jetty.io.JsonEncoder;
import com.gadgetworks.codeshelf.ws.jetty.protocol.response.ServiceMethodResponse;

@Path("/productivity")
public class ProductivityResource {
	private PersistenceService persistence = PersistenceService.getInstance();
	
	@GET
	@Path("/summary")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmployee(@QueryParam("facilityId") String facilityId) {
		ErrorResponse errors = new ErrorResponse();
		//Initial validation
		if (facilityId == null) {
			errors.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			errors.addErrorMissingQueryParam("facilityId");
			return errors.buildResponse();
		}
		UUID facilityIdUUID = BaseResponse.parseUUID(facilityId);
		if (facilityIdUUID == null) {
			errors.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			errors.addErrorBadUUID(facilityId);
			return errors.buildResponse();
		}
		
		//Try to get Productivity Summary
		try {
			persistence.beginTenantTransaction();
			ProductivitySummary result = WorkService.getProductivitySummary(facilityIdUUID);
			return result.buildResponse();
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
}
