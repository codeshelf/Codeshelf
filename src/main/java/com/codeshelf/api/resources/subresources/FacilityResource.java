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
import com.codeshelf.api.BaseResponse.EndDateParam;
import com.codeshelf.api.BaseResponse.StartDateParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.HardwareRequest;
import com.codeshelf.api.HardwareRequest.CheDisplayRequest;
import com.codeshelf.api.HardwareRequest.LightRequest;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedSample;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.platform.persistence.ITenantPersistenceService;
import com.codeshelf.platform.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.OrderService;
import com.codeshelf.service.ProductivityCheSummaryList;
import com.codeshelf.service.ProductivitySummaryList;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.jetty.protocol.message.CheDisplayMessage;
import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.codeshelf.ws.jetty.server.WebSocketManagerService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class FacilityResource {

	private final WorkService	workService;
	private final OrderService orderService;
	private final WebSocketManagerService webSocketManagerService;

	@Setter
	private Facility facility;

	@Inject
	public FacilityResource(WorkService workService, OrderService orderService, WebSocketManagerService webSocketManagerService) {
		this.orderService = orderService;
		this.workService = workService;
		this.webSocketManagerService = webSocketManagerService;
	}

	@GET
	@Path("/blockedwork/nolocation")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBlockedWorkNoLocation() {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		try {
			Session session = persistenceService.getSession();
			return BaseResponse.buildResponse(this.orderService.orderDetailsNoLocation(tenant, session, facility.getPersistentId()));		} catch (Exception e) {
			ErrorResponse errors = new ErrorResponse();
			errors.processException(e);
			return errors.buildResponse();
		}
	}

    @GET
	@Path("/work/results")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkResults(@QueryParam("startTimestamp") StartDateParam startTimestamp, @QueryParam("endTimestamp") EndDateParam endTimestamp) {
    	List<WorkInstruction> results = this.workService.getWorkResults(facility.getPersistentId(), startTimestamp.getValue(), endTimestamp.getValue());
		return BaseResponse.buildResponse(results);
	}
	

	@GET
	@Path("/work/topitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkByItem() {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		return BaseResponse.buildResponse(this.orderService.itemsInQuantityOrder(session, facility.getPersistentId()));
	}

	@GET
	@Path("/blockedwork/shorts")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBlockedWorkShorts() {
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		return BaseResponse.buildResponse(this.orderService.orderDetailsByStatus(session, facility.getPersistentId(), OrderStatusEnum.SHORT));
	}

	@GET
	@Path("/productivity")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProductivitySummary() throws Exception {
		ProductivitySummaryList summary = orderService.getProductivitySummary(facility.getPersistentId(), false);
		return BaseResponse.buildResponse(summary);
	}

	@GET
	@Path("/chesummary")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCheSummary() {
		List<WorkInstruction> instructions = WorkInstruction.staticGetDao().getAll();
		ProductivityCheSummaryList summary = new ProductivityCheSummaryList(facility.getPersistentId(), instructions);
		return BaseResponse.buildResponse(summary);
	}



	@GET
	@Path("/groupinstructions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGroupInstructions(@QueryParam("group") String groupName) {
		List<WorkInstruction> instructions = orderService.getGroupShortInstructions(facility.getPersistentId(), groupName);
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
		@SuppressWarnings("unused")
		ErrorResponse errors = new ErrorResponse();
		if (Strings.isNullOrEmpty(filterName)) {
			//errors.addParameterError("filterName", ErrorCode.FIELD_REQUIRED);
		}
		ITenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		ProductivitySummaryList.StatusSummary summary = orderService.statusSummary(session, facility.getPersistentId(), aggregate, filterName);

		return BaseResponse.buildResponse(summary);
	}

	@PUT
	@Path("hardware")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response performHardwareAction(HardwareRequest req) {
		ErrorResponse errors = new ErrorResponse();
		if (!req.isValid(errors)){
			return errors.buildResponse();
		}
		try {
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
