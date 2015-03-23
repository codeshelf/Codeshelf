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

import org.hibernate.Session;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.HardwareRequest;
import com.codeshelf.api.HardwareRequest.CheDisplayRequest;
import com.codeshelf.api.HardwareRequest.LightRequest;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedSample;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.manager.User;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.service.OrderService;
import com.codeshelf.service.ProductivityCheSummaryList;
import com.codeshelf.service.ProductivitySummaryList;
import com.codeshelf.ws.jetty.protocol.message.CheDisplayMessage;
import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.codeshelf.ws.jetty.server.WebSocketManagerService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class FacilityResource {

	private final OrderService orderService;
	private final WebSocketManagerService webSocketManagerService;

	@Setter
	private UUIDParam mUUIDParam;

	@Inject
	public FacilityResource(OrderService orderService, WebSocketManagerService webSocketManagerService) {
		this.orderService = orderService;
		this.webSocketManagerService = webSocketManagerService;
	}

	@GET
	@Path("/blockedwork/nolocation")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBlockedWorkNoLocation() {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		try {
			Session session = persistenceService.getSession();
			return BaseResponse.buildResponse(this.orderService.orderDetailsNoLocation(persistenceService.getDefaultSchema(), session, mUUIDParam.getUUID()));
		} catch (Exception e) {
			ErrorResponse errors = new ErrorResponse();
			errors.processException(e);
			return errors.buildResponse();
		}
	}

    /*
	@GET
	@Path("/work/results")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkResults(@QueryParam("startTimestamp") String startTimestamp, @QueryParam("endTimestamp") String endTimestamp) {
		Date startDatetime = dateTimeParser.parse(startTimestamp);
		Date endDatetime = dateTimeParser.parse(endTimestamp);
		List<WorkInstruction> results = this.workService.getWorkResults(mUUIDParam.getUUID(), startDatetime, endDatetime);
		return BaseResponse.buildResponse(results);
	}
	*/

	@GET
	@Path("/work/topitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkByItem() {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		return BaseResponse.buildResponse(this.orderService.itemsInQuantityOrder(session, mUUIDParam.getUUID()));
	}

	@GET
	@Path("/blockedwork/shorts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBlockedWorkShorts() {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		return BaseResponse.buildResponse(this.orderService.orderDetailsByStatus(session, mUUIDParam.getUUID(), OrderStatusEnum.SHORT));
	}

	@GET
	@Path("/productivity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProductivitySummary() throws Exception {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "facilityId", errors)){
			return errors.buildResponse();
		}

		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		ProductivitySummaryList summary = orderService.getProductivitySummary(persistenceService.getDefaultSchema(), mUUIDParam.getUUID(), false);
		return BaseResponse.buildResponse(summary);
	}

	@GET
	@Path("/chesummary")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCheSummary() {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "facilityId", errors)){
			return errors.buildResponse();
		}

		List<WorkInstruction> instructions = WorkInstruction.staticGetDao().getAll();
		ProductivityCheSummaryList summary = new ProductivityCheSummaryList(mUUIDParam.getUUID(), instructions);
		return BaseResponse.buildResponse(summary);
	}



	@GET
	@Path("/groupinstructions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGroupInstructions(@QueryParam("group") String groupName) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(mUUIDParam, "facilityId", errors)){
			return errors.buildResponse();
		}

		List<WorkInstruction> instructions = orderService.getGroupShortInstructions(mUUIDParam.getUUID(), groupName);
		return BaseResponse.buildResponse(instructions);
	}

	@GET
	@Path("filters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFilterNames() {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		Set<String> filterNames = orderService.getFilterNames(session);
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
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		ProductivitySummaryList.StatusSummary summary = orderService.statusSummary(session, mUUIDParam.getUUID(), aggregate, filterName);

		return BaseResponse.buildResponse(summary);
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
			Facility facility = Facility.staticGetDao().findByPersistentId(mUUIDParam.getUUID());
			Set<User> users = facility.getSiteControllerUsers();

			//LIGHTS
			List<LedSample> ledSamples = new ArrayList<LedSample>();

			if (req.getLights() != null) {
				for (LightRequest light :req.getLights()){
					ledSamples.add(new LedSample(light.getPosition(), light.getColor()));
				}

				LedCmdGroup ledCmdGroup = new LedCmdGroup(req.getLightController(), req.getLightChannel(), (short)0, ledSamples);
				LightLedsMessage lightMessage = new LightLedsMessage(req.getLightController(), req.getLightChannel(), req.getLightDuration(), ImmutableList.of(ledCmdGroup));
				webSocketManagerService.sendMessage(users, lightMessage);
			}

			//CHE MESSAGES
			if (req.getCheMessages() != null) {
				for (CheDisplayRequest cheReq : req.getCheMessages()) {
					CheDisplayMessage cheMessage = new CheDisplayMessage(cheReq.getChe(), cheReq.getLine1(), cheReq.getLine2(), cheReq.getLine3(), cheReq.getLine4());
					webSocketManagerService.sendMessage(users, cheMessage);
				}
			}

			//POSCON MESSAGES
			if (req.getPosConInstructions() != null) {
				for (PosControllerInstr posInstr : req.getPosConInstructions()) {
					posInstr.prepareObject();
					Thread.sleep(1000);
					webSocketManagerService.sendMessage(users, posInstr);
				}
			}

			return BaseResponse.buildResponse("Commands Sent");
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		}
	}
}
