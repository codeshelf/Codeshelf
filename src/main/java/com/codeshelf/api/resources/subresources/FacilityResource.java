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
import com.codeshelf.api.HardwareRequest.CheDisplayRequest;
import com.codeshelf.api.HardwareRequest.LightRequest;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedSample;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.multitenancy.User;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.LightService;
import com.codeshelf.service.OrderService;
import com.codeshelf.service.ProductivityCheSummaryList;
import com.codeshelf.service.ProductivitySummaryList;
import com.codeshelf.ws.jetty.protocol.message.CheDisplayMessage;
import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.codeshelf.ws.jetty.server.SessionManager;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class FacilityResource {

	private final TenantPersistenceService persistence;
	private final OrderService orderService;
	private final SessionManager sessionManager;

	@Setter
	private UUIDParam mUUIDParam;

	@Inject
	public FacilityResource(TenantPersistenceService persistenceService, OrderService orderService, SessionManager sessionManager) {
		this.persistence = persistenceService;
		this.orderService = orderService;
		this.sessionManager = sessionManager;
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
			Set<User> users = facility.getSiteControllerUsers();
			
			//LIGHTS
			List<LedSample> ledSamples = new ArrayList<LedSample>();
			
			for (LightRequest light :req.getLights()){
				ledSamples.add(new LedSample(light.getPosition(), light.getColor()));				
			}
			
			LedCmdGroup ledCmdGroup = new LedCmdGroup(req.getLightController(), req.getLightChannel(), (short)0, ledSamples);
			LightLedsMessage lightMessage = new LightLedsMessage(req.getLightController(), req.getLightChannel(), req.getLightDuration(), ImmutableList.of(ledCmdGroup));
			sessionManager.sendMessage(users, lightMessage);
			
			//CHE MESSAGES
			for (CheDisplayRequest cheReq : req.getCheMessages()) {
				CheDisplayMessage cheMessage = new CheDisplayMessage(cheReq.getChe(), cheReq.getLine1(), cheReq.getLine2(), cheReq.getLine3(), cheReq.getLine4());
				sessionManager.sendMessage(users, cheMessage);
			}
			return BaseResponse.buildResponse("Commands Sent");
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTransaction();
		}		
	}
}
