package com.gadgetworks.codeshelf.api.resources.subresources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.api.BaseResponse.UUIDParam;
import com.gadgetworks.codeshelf.api.ErrorResponse;
import com.gadgetworks.codeshelf.api.ObjectResponse;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.codeshelf.service.ProductivityCheSummaryList;
import com.gadgetworks.codeshelf.service.ProductivitySummaryList;
import com.gadgetworks.codeshelf.service.WorkService;

public class FacilityResource {
	private PersistenceService persistence = PersistenceService.getInstance();

	@Setter
	private UUIDParam mUUIDParam;
	
	@GET
	@Path("/productivity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProductivitySummary() {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "facilityId", errors)){
			return errors.buildResponse();
		}

		try {
			persistence.beginTenantTransaction();
			ProductivitySummaryList result = WorkService.getProductivitySummary(mUUIDParam.getUUID(), false);
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
	public Response getCheSummary() {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "facilityId", errors)){
			return errors.buildResponse();
		}
		
		try {
			persistence.beginTenantTransaction();
			List<WorkInstruction> instructions = WorkInstruction.DAO.getAll();
			ProductivityCheSummaryList summary = new ProductivityCheSummaryList(mUUIDParam.getUUID(), instructions);
			return summary.buildResponse();
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
	
	@GET
	@Path("/groupinstructions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGroupInstructions(@QueryParam("group") String groupName) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "facilityId", errors)){
			return errors.buildResponse();
		}
		
		try {
			persistence.beginTenantTransaction();
			List<WorkInstruction> instructions = WorkService.getGroupShortInstructions(mUUIDParam.getUUID(), groupName);
			ObjectResponse response = new ObjectResponse(instructions);
			return response.buildResponse();
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
}