package com.codeshelf.api.resources.subresources;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.HardwareRequest;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.HardwareRequest.LightCommand;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedSample;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.LightService;
import com.codeshelf.service.OrderService;
import com.codeshelf.service.ProductivityCheSummaryList;
import com.codeshelf.service.ProductivitySummaryList;
import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class FacilityResource {

	private final TenantPersistenceService persistence;
	private final OrderService orderService;
	private final LightService lightService;

	@Setter
	private UUIDParam mUUIDParam;

	@Inject
	public FacilityResource(TenantPersistenceService persistenceService, OrderService orderService, LightService lightService) {
		this.persistence = persistenceService;
		this.orderService = orderService;
		this.lightService = lightService;
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
			persistence.beginTransaction();
			ProductivitySummaryList summary = orderService.getProductivitySummary(mUUIDParam.getUUID(), false);
			return BaseResponse.buildResponse(summary);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTransaction();
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
			persistence.beginTransaction();
			List<WorkInstruction> instructions = WorkInstruction.DAO.getAll();
			ProductivityCheSummaryList summary = new ProductivityCheSummaryList(mUUIDParam.getUUID(), instructions);
			return BaseResponse.buildResponse(summary);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTransaction();
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
			persistence.beginTransaction();
			List<WorkInstruction> instructions = orderService.getGroupShortInstructions(mUUIDParam.getUUID(), groupName);
			return BaseResponse.buildResponse(instructions);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTransaction();
		}
	}

	@GET
	@Path("filters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFilterNames() {
		persistence.beginTransaction();
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
			persistence.beginTransaction();
			ProductivitySummaryList.StatusSummary summary = orderService.statusSummary(aggregate, filterName);

			return BaseResponse.buildResponse(summary);
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTransaction();
		}
	}
	
	@PUT
	@Path("hardware")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response performHardwareAction(HardwareRequest req) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "facilityId", errors)){
			return errors.buildResponse();
		}
		if (!req.isValid(errors)){
			return errors.buildResponse();
		}
		try {
			persistence.beginTransaction();
			Facility facility = Facility.DAO.findByPersistentId(mUUIDParam.getUUID());
			List<LedSample> ledSamples = new ArrayList<LedSample>();
			
			for (LightCommand light :req.getLights()){
				ledSamples.add(new LedSample(light.getPosition(), light.getColor()));				
			}
			
			LedCmdGroup ledCmdGroup = new LedCmdGroup(req.getController(), req.getChannel(), (short)0, ledSamples);
			LightLedsMessage message = new LightLedsMessage(req.getController(), req.getChannel(), req.getLightDuration(), ImmutableList.of(ledCmdGroup));
			lightService.sendToAllSiteControllers(facility.getSiteControllerUsers(), message);
			
			return BaseResponse.buildResponse("Commands Sent");
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTransaction();
		}		
	}
}
