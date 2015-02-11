package com.gadgetworks.codeshelf.api.resources.subresources;

import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.api.BaseResponse.UUIDParam;
import com.gadgetworks.codeshelf.api.ErrorResponse;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.TenantPersistenceService;
import com.gadgetworks.codeshelf.service.OrderService;
import com.gadgetworks.codeshelf.service.ProductivityCheSummaryList;
import com.gadgetworks.codeshelf.service.ProductivitySummaryList;
import com.google.common.base.Strings;
import com.google.inject.Inject;

public class FacilityResource {

	private final TenantPersistenceService persistence;
	private final OrderService orderService;

	@Setter
	private UUIDParam mUUIDParam;

	@Inject
	public FacilityResource(TenantPersistenceService persistenceService, OrderService orderService) {
		this.persistence = persistenceService;
		this.orderService = orderService;
	}

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
			ProductivitySummaryList summary = orderService.getProductivitySummary(mUUIDParam.getUUID(), false);
			return BaseResponse.buildResponse(summary);
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
			return BaseResponse.buildResponse(summary);
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
			List<WorkInstruction> instructions = orderService.getGroupShortInstructions(mUUIDParam.getUUID(), groupName);
			return BaseResponse.buildResponse(instructions);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}

	@GET
	@Path("filters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFilterNames() {
		persistence.beginTenantTransaction();
		Set<String> filterNames = orderService.getFilterNames();
		return BaseResponse.buildResponse(filterNames);

	}

	@GET
	@Path("/statussummary/{aggregate}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrderStatusSummary(@PathParam("aggregate") String aggregate, @QueryParam("filterName") String filterName) {
		ErrorResponse errors = new ErrorResponse();
		if (Strings.isNullOrEmpty(filterName)) {
			//errors.addParameterError("filterName", ErrorCode.FIELD_REQUIRED);
		}
		try {
			persistence.beginTenantTransaction();
			ProductivitySummaryList.StatusSummary summary = orderService.statusSummary(aggregate, filterName);

			return BaseResponse.buildResponse(summary);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
}
