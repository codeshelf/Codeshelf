package com.codeshelf.api.resources.subresources;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Setter;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.EndDateParam;
import com.codeshelf.api.BaseResponse.EventTypeParam;
import com.codeshelf.api.BaseResponse.StartDateParam;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.HardwareRequest;
import com.codeshelf.api.HardwareRequest.CheDisplayRequest;
import com.codeshelf.api.HardwareRequest.LightRequest;
import com.codeshelf.api.pickscript.ScriptParser;
import com.codeshelf.api.pickscript.ScriptParser.ScriptApiResponse;
import com.codeshelf.api.pickscript.ScriptParser.ScriptStep;
import com.codeshelf.api.pickscript.ScriptSiteCallPool;
import com.codeshelf.api.pickscript.ScriptStepParser;
import com.codeshelf.api.pickscript.ScriptStepParser.StepPart;
import com.codeshelf.api.pickscript.ScriptServerRunner;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.api.responses.ItemDisplay;
import com.codeshelf.api.responses.PickRate;
import com.codeshelf.api.responses.ResultDisplay;
import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedInstrListMessage;
import com.codeshelf.device.LedSample;
import com.codeshelf.device.PosControllerInstr;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.manager.Tenant;
import com.codeshelf.manager.User;
import com.codeshelf.metrics.ActiveSiteControllerHealthCheck;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.security.CodeshelfSecurityManager;
import com.codeshelf.service.NotificationService;
import com.codeshelf.service.NotificationService.EventType;
import com.codeshelf.service.OrderService;
import com.codeshelf.service.ProductivityCheSummaryList;
import com.codeshelf.service.ProductivitySummaryList;
import com.codeshelf.service.PropertyService;
import com.codeshelf.service.UiUpdateService;
import com.codeshelf.service.WorkService;
import com.codeshelf.ws.protocol.message.CheDisplayMessage;
import com.codeshelf.ws.protocol.message.LightLedsInstruction;
import com.codeshelf.ws.protocol.message.PickScriptMessage;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.multipart.FormDataMultiPart;

public class FacilityResource {

	private final WorkService	workService;
	private final OrderService orderService;
	private final NotificationService notificationService;
	private final WebSocketManagerService webSocketManagerService;
	private final UiUpdateService uiUpdateService;
	private final PropertyService propertyService;
	private final ICsvAislesFileImporter aislesImporter;
	private final ICsvLocationAliasImporter locationsImporter;
	private final ICsvInventoryImporter inventoryImporter;
	private final ICsvOrderImporter orderImporter;
	
	@Setter
	private Facility facility;

	@Context
	private ResourceContext resourceContext;

	@Inject
	public FacilityResource(WorkService workService,
		OrderService orderService,
		NotificationService notificationService,
		WebSocketManagerService webSocketManagerService,
		UiUpdateService uiUpdateService,
		PropertyService propertyService,
		ICsvAislesFileImporter aislesImporter,
		ICsvLocationAliasImporter locationsImporter,
		ICsvInventoryImporter inventoryImporter,
		ICsvOrderImporter orderImporter) {
		this.orderService = orderService;
		this.workService = workService;
		this.webSocketManagerService = webSocketManagerService;
		this.notificationService = notificationService;
		this.uiUpdateService = uiUpdateService;
		this.propertyService = propertyService;
		this.aislesImporter = aislesImporter;
		this.locationsImporter = locationsImporter;
		this.inventoryImporter = inventoryImporter;
		this.orderImporter = orderImporter;
	}

	@GET
	@RequiresPermissions("companion:view")
	@Path("/blockedwork/nolocation")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBlockedWorkNoLocation() {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Tenant tenant = CodeshelfSecurityManager.getCurrentTenant();
		try {
			Session session = persistenceService.getSession();
			return BaseResponse.buildResponse(this.orderService.orderDetailsNoLocation(tenant, session, facility.getPersistentId()));
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	@Path("/import")
	public ImportResource getImportResource() throws Exception {
		ImportResource r = resourceContext.getResource(ImportResource.class);
	    r.setFacility(facility);
	    return r;
	}

    @GET
	@Path("/work/results")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkResults(@QueryParam("startTimestamp") StartDateParam startTimestamp, @QueryParam("endTimestamp") EndDateParam endTimestamp) {
    	List<WorkInstruction> results = this.workService.getWorkResults(facility.getPersistentId(), startTimestamp.getValue(), endTimestamp.getValue());
		return BaseResponse.buildResponse(results);
	}


	@GET
	@Path("/work/topitems")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkByItem() {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		return BaseResponse.buildResponse(this.orderService.itemsInQuantityOrder(session, facility.getPersistentId()));
	}

	@GET
	@Path("/blockedwork/shorts")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBlockedWorkShorts() {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		return BaseResponse.buildResponse(this.orderService.orderDetailsByStatus(session, facility.getPersistentId(), OrderStatusEnum.SHORT));
	}

	@GET
	@Path("/productivity")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProductivitySummary() throws Exception {
		ProductivitySummaryList summary = orderService.getProductivitySummary(facility.getPersistentId(), false);
		return BaseResponse.buildResponse(summary);
	}

	@GET
	@Path("/chesummary")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCheSummary() {
		List<WorkInstruction> instructions = WorkInstruction.staticGetDao().getAll();
		ProductivityCheSummaryList summary = new ProductivityCheSummaryList(facility.getPersistentId(), instructions);
		return BaseResponse.buildResponse(summary);
	}

	@GET
	@Path("/groupinstructions")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGroupInstructions(@QueryParam("group") String groupName) {
		List<WorkInstruction> instructions = orderService.getGroupShortInstructions(facility.getPersistentId(), groupName);
		return BaseResponse.buildResponse(instructions);
	}

	@GET
	@Path("filters")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFilterNames() {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		Set<String> filterNames = orderService.getFilterNames(session);
		return BaseResponse.buildResponse(filterNames);
	}

	@GET
	@Path("/statussummary/{aggregate}")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOrderStatusSummary(@PathParam("aggregate") String aggregate, @QueryParam("filterName") String filterName) {
		@SuppressWarnings("unused")
		ErrorResponse errors = new ErrorResponse();
		if (Strings.isNullOrEmpty(filterName)) {
			//errors.addParameterError("filterName", ErrorCode.FIELD_REQUIRED);
		}
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		Session session = persistenceService.getSession();
		ProductivitySummaryList.StatusSummary summary = orderService.statusSummary(session, facility.getPersistentId(), aggregate, filterName);

		return BaseResponse.buildResponse(summary);
	}

	@GET
	@Path("/ches")
	@RequiresPermissions("che:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllChesInFacility() {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", facility));
		Criteria cheCriteria = Che.staticGetDao().createCriteria();
		cheCriteria.createCriteria("parent", "network").add(Restrictions.eq("parent", facility));
		List<Che> ches = Che.staticGetDao().findByCriteriaQuery(cheCriteria);
		return BaseResponse.buildResponse(ches);
	}


	@GET
	@Path("/workers")
	@RequiresPermissions("worker:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllWorkersInFacility() {
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("facility", facility));
		List<Worker> workers = Worker.staticGetDao().findByFilter(filterParams);
		return BaseResponse.buildResponse(workers);
	}

	@POST
	@Path("/workers")
	@RequiresPermissions("worker:edit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createWorker(Worker worker) {
		ErrorResponse errors = new ErrorResponse();
		try {
			worker.setFacility(facility);
			worker.generateDomainId();
			if (worker.getActive() == null){
				worker.setActive(true);
			}
			worker.setUpdated(new Timestamp(System.currentTimeMillis()));
			if (!worker.isValid(errors)){
				errors.setStatus(Status.BAD_REQUEST);
				return errors.buildResponse();
			}
			UUID id = worker.getPersistentId();
			Worker.staticGetDao().store(worker);
			Worker createdWorker = Worker.staticGetDao().findByPersistentId(id);
			return BaseResponse.buildResponse(createdWorker);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@GET
	@Path("events")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchEvents(
		@QueryParam("type") List<EventTypeParam> typeParamList,
		@QueryParam("itemId") String itemId,
		@QueryParam("location") String location,
		@QueryParam("groupBy") String groupBy,
		@QueryParam("resolved") Boolean resolved ) {
		ErrorResponse errors = new ErrorResponse();
		try {
			List<Criterion> filterParams = new ArrayList<Criterion>();
			filterParams.add(Restrictions.eq("facility", facility));
			//If any "type" parameters are provided, filter accordingly
			List<EventType> typeList = Lists.newArrayList();
			for (EventTypeParam type : typeParamList) {
				if (type != null) {
					typeList.add(type.getValue());
				}
			}
			if (!typeList.isEmpty()) {
				filterParams.add(Restrictions.in("eventType", typeList));
			}

			if (!Strings.isNullOrEmpty(itemId)) {
				//do by filter param but for now needs to be manually filtered
			}

			//If "resolved" parameter not provided, return, both, resolved and unresolved events
			if (resolved != null) {
				if (resolved){
					filterParams.add(Restrictions.isNotNull("resolution"));
				} else {
					filterParams.add(Restrictions.isNull("resolution"));
				}
			}

			List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
			if (!Strings.isNullOrEmpty(itemId)) {
				ResultDisplay result = new ResultDisplay();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					ItemDisplay itemDisplayKey = new ItemDisplay(eventDisplay);
					if (itemId.equals(itemDisplayKey.getItemId()) &&
						location.equals(itemDisplayKey.getLocation())) {
						result.add(new BeanMap(eventDisplay));
					}
				}
				return BaseResponse.buildResponse(result);
			}

			if ("item".equals(groupBy)) {
				Map<ItemDisplay, Integer> issuesByItem = new HashMap<>();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					ItemDisplay itemDisplayKey = new ItemDisplay(eventDisplay);
					Integer count = MoreObjects.firstNonNull(issuesByItem.get(itemDisplayKey), 0);
					issuesByItem.put(itemDisplayKey, count+1);
				}

				ResultDisplay result = new ResultDisplay(ItemDisplay.ItemComparator);
				for (Map.Entry<ItemDisplay, Integer> issuesByItemEntry : issuesByItem.entrySet()) {
					Map<Object, Object> values = new HashMap<>();
					values.putAll(new BeanMap(issuesByItemEntry.getKey()));
					values.put("count", issuesByItemEntry.getValue());
					result.add(values);
				}
				return BaseResponse.buildResponse(result);
			} else if ("type".equals(groupBy)){
				Map<EventType, Integer> issuesByType = new HashMap<>();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					EventType eventType = eventDisplay.getType();
					Integer count = MoreObjects.firstNonNull(issuesByType.get(eventType), 0);
					issuesByType.put(eventType, count+1);
				}
				ResultDisplay result = new ResultDisplay(issuesByType.size());
				for (Map.Entry<EventType, Integer> issuesByTypeEntry : issuesByType.entrySet()) {
					Map<Object, Object> values = new HashMap<>();
					values.putAll(new BeanMap(issuesByTypeEntry.getKey()));
					values.put("count", issuesByTypeEntry.getValue());
					result.add(values);
				}
				return BaseResponse.buildResponse(result);
			} else {
				ResultDisplay result = new ResultDisplay(events.size());
				for (WorkerEvent event : events) {
					result.add(new BeanMap(EventDisplay.createEventDisplay(event)));
				}
				return BaseResponse.buildResponse(result);
			}


		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		}
	}

	@GET
	@Path("pickrate")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pickRate(@QueryParam("startTimestamp") StartDateParam startDateParam, @QueryParam("endTimestamp") EndDateParam endDateParam) {
		ErrorResponse errors = new ErrorResponse();
		try {
			List<PickRate> pickRates = notificationService.getPickRate(new DateTime(startDateParam.getValue()), new DateTime(endDateParam.getValue()));
			return BaseResponse.buildResponse(pickRates);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@POST
	@Path("/runscript")
	@RequiresPermissions("che:simulate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response runScript(@QueryParam("timeout_min") Integer timeoutMin, FormDataMultiPart body){
		try {
			ErrorResponse errors = new ErrorResponse();

			//Retrieve the script
			InputStream scriptIS = ScriptStepParser.getInputStream(body, "script");
			if (scriptIS == null) {
				errors.addErrorMissingBodyParam("script");
				return errors.buildResponse();
			}
			String script = IOUtils.toString(scriptIS);
			if (script == null || script.isEmpty()) {
				errors.addError("Script file was empty");
				return errors.buildResponse();
			}
			//Split the script into a list of SERVER and SITE parts
			ArrayList<StepPart> scriptParts = ScriptStepParser.parseMixedScript(script);
			Set<User> users = facility.getSiteControllerUsers();
			StringBuilder response = new StringBuilder();
			TenantPersistenceService persistence = TenantPersistenceService.getInstance();
			ScriptServerRunner scriptRunner = new ScriptServerRunner(persistence, facility.getPersistentId(), body, uiUpdateService, propertyService, aislesImporter, locationsImporter, inventoryImporter, orderImporter);
			//Process script parts
			while (!scriptParts.isEmpty()) {
				StepPart part = scriptParts.remove(0);
				if (part.isServer()) {
					//SERVER
					PickScriptMessage serverResponseMessage = scriptRunner.processServerScript(part.getScript());
					response.append(serverResponseMessage.getResponse());
					if (!serverResponseMessage.isSuccess()) {
						break;
					}
				} else {
					//SITE
					//Test is Site Controller is running
					Result siteHealth = new ActiveSiteControllerHealthCheck(webSocketManagerService).execute();
					if (!siteHealth.isHealthy()){
						response.append("Site controller problem: " + siteHealth.getMessage());
						break;
					}
					//Execute script
					UUID id = UUID.randomUUID();
					PickScriptMessage scriptMessage = new PickScriptMessage(id, part.getScript());
					webSocketManagerService.sendMessage(users, scriptMessage);
					PickScriptMessage siteResponseMessage = ScriptSiteCallPool.waitForSiteResponse(id, timeoutMin);
					if (siteResponseMessage == null) {
						response.append("Site request timed out");
						break;
					}
					response.append(siteResponseMessage.getResponse());
					if (!siteResponseMessage.isSuccess()) {
						break;
					}
				}
			}
			return BaseResponse.buildResponse(response.toString(), Status.OK, MediaType.TEXT_PLAIN_TYPE);
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
	
	@POST
	@Path("/process_script")
	@RequiresPermissions("che:simulate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response runScriptSteps(FormDataMultiPart body){
		try {
			ErrorResponse errors = new ErrorResponse();

			//Retrieve the script
			InputStream scriptIS = ScriptStepParser.getInputStream(body, "script");
			if (scriptIS == null) {
				errors.addErrorMissingBodyParam("script");
				return errors.buildResponse();
			}
			String script = IOUtils.toString(scriptIS);
			if (script == null || script.isEmpty()) {
				errors.addError("Script file was empty");
				return errors.buildResponse();
			}
			ScriptApiResponse response = ScriptParser.parseScript(script);
			return BaseResponse.buildResponse(response);
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}
	
	@POST
	@Path("/run_script")
	@RequiresPermissions("che:simulate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response runScript(@QueryParam("script_step_id") UUIDParam scriptStepId, 
		@QueryParam("timeout_min") Integer timeoutMin,
		FormDataMultiPart body){
		try {
			ErrorResponse errors = new ErrorResponse();
			if (!BaseResponse.isUUIDValid(scriptStepId, "script_step_id", errors)){
				return errors.buildResponse();
			}
			
			
			ScriptStep scriptStep = ScriptParser.getScriptStep(scriptStepId.getValue());
			if (scriptStep == null) {
				errors.addError("Script step " + scriptStepId.getValue() + " doesn't exist");
				return errors.buildResponse();
			}
			StringBuilder report = new StringBuilder("Running script step " + scriptStep.getComment());
	
			//Split the script into a list of SERVER and SITE parts
			ArrayList<StepPart> scriptParts = scriptStep.getParts();
			Set<User> users = facility.getSiteControllerUsers();
			
			TenantPersistenceService persistence = TenantPersistenceService.getInstance();
			ScriptServerRunner scriptRunner = new ScriptServerRunner(persistence, facility.getPersistentId(), body, uiUpdateService, propertyService, aislesImporter, locationsImporter, inventoryImporter, orderImporter);
			//Process script parts
			while (!scriptParts.isEmpty()) {
				StepPart part = scriptParts.remove(0);
				if (part.isServer()) {
					//SERVER
					PickScriptMessage serverResponseMessage = scriptRunner.processServerScript(part.getScript());
					report.append(serverResponseMessage.getResponse());
					if (!serverResponseMessage.isSuccess()) {
						break;
					}
				} else {
					//SITE
					//Test is Site Controller is running
					Result siteHealth = new ActiveSiteControllerHealthCheck(webSocketManagerService).execute();
					if (!siteHealth.isHealthy()){
						report.append("Site controller problem: " + siteHealth.getMessage());
						break;
					}
					//Execute script
					UUID id = UUID.randomUUID();
					PickScriptMessage scriptMessage = new PickScriptMessage(id, part.getScript());
					webSocketManagerService.sendMessage(users, scriptMessage);
					PickScriptMessage siteResponseMessage = ScriptSiteCallPool.waitForSiteResponse(id, timeoutMin);
					if (siteResponseMessage == null) {
						report.append("Site request timed out");
						break;
					}
					report.append(siteResponseMessage.getResponse());
					if (!siteResponseMessage.isSuccess()) {
						break;
					}
				}
			}
			return BaseResponse.buildResponse(new ScriptApiResponse(scriptStep.getNextId(), report.toString()));
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	@PUT
	@Path("hardware")
	@RequiresPermissions("companion:view")
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
				LightLedsInstruction instruction = new LightLedsInstruction(req.getLightController(), req.getLightChannel(), req.getLightDuration(), ImmutableList.of(ledCmdGroup));
				LedInstrListMessage lightMessage = new LedInstrListMessage(instruction);
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
			return errors.processException(e);
		}
	}
}
