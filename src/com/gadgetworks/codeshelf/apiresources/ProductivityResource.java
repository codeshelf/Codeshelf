package com.gadgetworks.codeshelf.apiresources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gadgetworks.codeshelf.apiresources.BaseResponse.UUIDParam;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.ProductivityCheSummaryList;
import com.gadgetworks.codeshelf.service.ProductivitySummaryList;
import com.gadgetworks.codeshelf.service.WorkService;

@Path("/productivity")
public class ProductivityResource {
	private PersistenceService persistence = PersistenceService.getInstance();
	
	@GET
	@Path("/summary")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmployee(@QueryParam("facilityId") UUIDParam facilityIdParam) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(facilityIdParam, "facilityId", errors)){
			return errors.buildResponse();
		}

		try {
			persistence.beginTenantTransaction();
			ProductivitySummaryList result = WorkService.getProductivitySummary(facilityIdParam.getUUID());
			return result.buildResponse();
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
	
	@GET
	@Path("/chesummary")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCheSummary(@QueryParam("facilityId") UUIDParam facilityIdParam) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(facilityIdParam, "facilityId", errors)){
			return errors.buildResponse();
		}
		
		try {
			persistence.beginTenantTransaction();
			ProductivityCheSummaryList summaryList = WorkService.getCheByGroupSummary(facilityIdParam.getUUID());
			return summaryList.buildResponse();
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
}
