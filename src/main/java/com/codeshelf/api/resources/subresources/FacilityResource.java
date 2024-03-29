package com.codeshelf.api.resources.subresources;


import java.io.InputStream;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.script.ScriptException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.EventTypeParam;
import com.codeshelf.api.BaseResponse.IntervalParam;
import com.codeshelf.api.BaseResponse.TimestampParam;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.api.pickscript.ScriptParser;
import com.codeshelf.api.pickscript.ScriptParser.ScriptStep;
import com.codeshelf.api.pickscript.ScriptServerRunner;
import com.codeshelf.api.pickscript.ScriptSiteCallPool;
import com.codeshelf.api.pickscript.ScriptStepParser;
import com.codeshelf.api.pickscript.ScriptStepParser.StepPart;
import com.codeshelf.api.resources.ChesResource;
import com.codeshelf.api.resources.ExtensionPointsResource;
import com.codeshelf.api.resources.OrdersResource;
import com.codeshelf.api.resources.PrintTemplatesResource;
import com.codeshelf.api.resources.WorkersResource;
import com.codeshelf.api.responses.EventDisplay;
import com.codeshelf.api.responses.FacilityShort;
import com.codeshelf.api.responses.ItemDisplay;
import com.codeshelf.api.responses.PickRate;
import com.codeshelf.api.responses.WorkerDisplay;
import com.codeshelf.behavior.NotificationBehavior;
import com.codeshelf.behavior.NotificationBehavior.HistogramParams;
import com.codeshelf.behavior.NotificationBehavior.HistogramResult;
import com.codeshelf.behavior.NotificationBehavior.ItemEventTypeGroup;
import com.codeshelf.behavior.NotificationBehavior.WorkerEventTypeGroup;
import com.codeshelf.behavior.OrderBehavior;
import com.codeshelf.behavior.ProductivitySummaryList;
import com.codeshelf.behavior.TestBehavior;
import com.codeshelf.behavior.UiUpdateBehavior;
import com.codeshelf.behavior.WorkBehavior;
import com.codeshelf.edi.ICsvAislesFileImporter;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvLocationAliasImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.ICsvWorkerImporter;
import com.codeshelf.manager.User;
import com.codeshelf.model.DataPurgeParameters;
import com.codeshelf.model.ReplenishItem;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ResultDisplay;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FacilityMetric;
import com.codeshelf.model.domain.SiteController;
import com.codeshelf.model.domain.Worker;
import com.codeshelf.model.domain.WorkerEvent;
import com.codeshelf.model.domain.WorkerEvent.EventType;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.scheduler.ApplicationSchedulerService;
import com.codeshelf.service.ExtensionPointEngine;
import com.codeshelf.service.ParameterSetBeanABC;
import com.codeshelf.ws.protocol.message.ScriptMessage;
import com.codeshelf.ws.server.WebSocketManagerService;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sun.jersey.api.core.ResourceContext;
import com.sun.jersey.multipart.FormDataMultiPart;

import lombok.Getter;
import lombok.Setter;

public class FacilityResource {

	public class Option {

		@Getter
		private String value;
		@Getter
		private String label;

		public Option(String input, String displayName) {
			this.value = input;
			this.label = displayName;
		}

	}

	private static final Logger							LOGGER			= LoggerFactory.getLogger(FacilityResource.class);

	private final WorkBehavior							workService;
	private final OrderBehavior							orderService;
	private final NotificationBehavior					notificationService;
	private final WebSocketManagerService				webSocketManagerService;
	private final ApplicationSchedulerService applicationSchedulerService;
	private final UiUpdateBehavior						uiUpdateService;
	private final Provider<ICsvAislesFileImporter>		aislesImporterProvider;
	private final Provider<ICsvLocationAliasImporter>	locationsImporterProvider;
	private final Provider<ICsvInventoryImporter>		inventoryImporterProvider;
	private final Provider<ICsvOrderImporter>			orderImporterProvider;
	private final Provider<ICsvWorkerImporter>			workerImporterProvider;

	@Setter
	private Facility									facility;

	@Context
	private ResourceContext								resourceContext;

	@Inject
	public FacilityResource(WorkBehavior workService,
		OrderBehavior orderService,
		NotificationBehavior notificationService,
		WebSocketManagerService webSocketManagerService,
		ApplicationSchedulerService applicationSchedulerService,
		UiUpdateBehavior uiUpdateService,
		Provider<ICsvAislesFileImporter> aislesImporterProvider,
		Provider<ICsvLocationAliasImporter> locationsImporterProvider,
		Provider<ICsvInventoryImporter> inventoryImporterProvider,
		Provider<ICsvOrderImporter> orderImporterProvider,
		Provider<ICsvWorkerImporter> workerImporterProvider) {

		
		this.orderService = orderService;
		this.workService = workService;
		this.webSocketManagerService = webSocketManagerService;
		this.applicationSchedulerService = applicationSchedulerService;
		this.notificationService = notificationService;
		
		this.uiUpdateService = uiUpdateService;
		this.aislesImporterProvider = aislesImporterProvider;
		this.locationsImporterProvider = locationsImporterProvider;
		this.inventoryImporterProvider = inventoryImporterProvider;
		this.orderImporterProvider = orderImporterProvider;
		this.workerImporterProvider = workerImporterProvider;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get() {
		return BaseResponse.buildResponse(new FacilityShort(facility));
	}

	@Path("/import")
	public ImportResource getImportResource() throws Exception {
		ImportResource r = resourceContext.getResource(ImportResource.class);
		r.setFacility(facility);
		return r;
	}

	@Path("/edigateways")
	public EDIGatewaysResource getEdiResource() throws Exception {
		EDIGatewaysResource r = resourceContext.getResource(EDIGatewaysResource.class);
		r.setParent(facility);
		return r;
	}

	@Path("/templates")
	public PrintTemplatesResource getPrintTemplatesResource() throws Exception {
		PrintTemplatesResource r = resourceContext.getResource(PrintTemplatesResource.class);
		r.setParent(facility);
		return r;
	}

	
	@Path("/workers")
	public WorkersResource getWorkersResource() throws Exception {
		WorkersResource r = resourceContext.getResource(WorkersResource.class);
		r.setParent(facility);
		return r;
	}

	@Path("/ches")
	public ChesResource getAllChesInFacility() {
		ChesResource r = resourceContext.getResource(ChesResource.class);
		r.setFacility(facility);
		return r;
	}

	
	@Path("/test")
	public TestResource getTestResource() throws Exception {
		TestResource r = resourceContext.getResource(TestResource.class);
		r.setFacility(facility);
		r.setTestBehavior(new TestBehavior());
		return r;
	}

	@Path("/scheduledjobs")
	public ScheduledJobsResource getScheduledJobResource() throws Exception {
		ScheduledJobsResource r = resourceContext.getResource(ScheduledJobsResource.class);
		r.setFacility(facility);
		return r;
	}

	
	@DELETE
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete() {
		try {
			applicationSchedulerService.stopFacility(facility);
			facility.delete(webSocketManagerService);
			return BaseResponse.buildResponse("Facility Deleted");
		} catch (ConstraintViolationException e) {
			SQLException se = e.getSQLException();
			if (se != null) {
				return new ErrorResponse().processException(se);
			} else {
				return new ErrorResponse().processException(e);
			}
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	@Path("/orders")
	public OrdersResource getOrders() {
		OrdersResource r = resourceContext.getResource(OrdersResource.class);
		r.setFacility(facility);
		return r;
	}

	@Path("/extensionpoints")
	@Produces(MediaType.APPLICATION_JSON)
	public ExtensionPointsResource getExtensionPoints() throws ScriptException {
		ExtensionPointsResource r = resourceContext.getResource(ExtensionPointsResource.class);
		ExtensionPointEngine extensionPointEngine = ExtensionPointEngine.getInstance(facility);
		r.setExtensionPointEngine(extensionPointEngine);
		return r;
	}

	@GET
	@Path("/healthchecks/{type}/configuration")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getHealthCheckConfig(@PathParam("type") String healthCheckType) throws ScriptException {
		Optional<ExtensionPoint> extensionPoint = null;
		ParameterSetBeanABC parameterSet = null;
		if ("ParameterSetDataQuantityHealthCheck".equalsIgnoreCase(healthCheckType)) {
			ExtensionPointEngine epService = ExtensionPointEngine.getInstance(facility);
			extensionPoint = epService.getDataQuantityHealthCheckExtensionPoint();
			parameterSet = epService.getDataQuantityHealthCheckParameters();
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("parameterSet", parameterSet);
			responseMap.put("extensionPoint", extensionPoint.orNull());
			return BaseResponse.buildResponse(responseMap);
		} else if ("ParameterSetDataPurge".equalsIgnoreCase(healthCheckType)) {
			ExtensionPointEngine epService = ExtensionPointEngine.getInstance(facility);
			extensionPoint = epService.getDataPurgeExtensionPoint();
			parameterSet = epService.getDataPurgeParameters();
		} else if ("ParameterEdiFreeSpaceHealthCheck".equalsIgnoreCase(healthCheckType)) {
			ExtensionPointEngine epService = ExtensionPointEngine.getInstance(facility);
			extensionPoint = epService.getEdiFreeSpaceExtensionPoint();
			parameterSet = epService.getEdiFreeSpaceParameters();
		} 
		
		if (extensionPoint != null && parameterSet != null) {
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("parameterSet", parameterSet);
			responseMap.put("extensionPoint", extensionPoint.orNull());
			return BaseResponse.buildResponse(responseMap);
		} else {
			ErrorResponse error = new ErrorResponse();
			error.addBadParameter("type", healthCheckType);
			return BaseResponse.buildResponse(error, Response.Status.BAD_REQUEST);
		}
	}

	@GET
	@Path("/data/summary")
	@RequiresPermissions("facility:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDataSummary() throws ScriptException {
		ExtensionPointEngine service = ExtensionPointEngine.getInstance(facility);
		DataPurgeParameters params = service.getDataPurgeParameters();
		List<String> summary = workService.reportAchiveables(params.getPurgeAfterDaysValue(), this.facility);
		return BaseResponse.buildResponse(summary);
	}

	@GET
	@Path("/work/instructions/references")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findWorkInstructionReferences(@QueryParam("itemId") String itemIdSubstring,
		@QueryParam("containerId") String containerIdSubstring,
		@QueryParam("assigned") IntervalParam assignedDateSpec) {
		List<Object[]> results = this.workService.findWorkInstructionReferences(facility,
			assignedDateSpec.getValue(),
			itemIdSubstring,
			containerIdSubstring);
		return BaseResponse.buildResponse(results);

	}

	@GET
	@Path("/work/instructions/{persistentId}")
	@RequiresPermissions("companion:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWorkInstruction(@PathParam("persistentId") UUIDParam persistentId,
		@QueryParam("properties") List<String> propertyNamesList) {
		String[] propertyNames = new String[] {

		"groupAndSortCode", "pickerId", "type", "assigned", "completed", "pickInstructionUi", "nominalLocationId",
				"wiPosAlongPath", "description", "itemMasterId", "planQuantity", "uomMasterId", "uomNormalized", "orderId",
				"orderDetailId", "containerId", "assignedCheName", "domainId", "persistentId", "status", "planMinQuantity",
				"planMaxQuantity", "actualQuantity", "litLedsForWi", "gtin", "needsScan" };

		List<Map<String, Object>> results = this.workService.findtWorkInstructions(facility, propertyNames, persistentId.getValue());
		if (results.size() == 1) {
			return BaseResponse.buildResponse(results.get(0));
		} else if (results.size() == 0) {
			return BaseResponse.buildResponse(null);

		} else {
			LOGGER.error("Found multiple orders for {} in facility {}", persistentId, facility);
			return BaseResponse.buildResponse(null);
		}
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
		ProductivitySummaryList.StatusSummary summary = orderService.statusSummary(session,
			facility.getPersistentId(),
			aggregate,
			filterName);

		return BaseResponse.buildResponse(summary);
	}


	@GET
	@Path("events")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchEvents(@QueryParam("type") List<EventTypeParam> typeParamList,
		@QueryParam("itemId") String itemId,
		@QueryParam("location") String location,
		@QueryParam("workerId") String workerId,
		@QueryParam("created") IntervalParam created,
		@QueryParam("groupBy") String groupBy) {
		Stopwatch total = Stopwatch.createStarted();

		ErrorResponse errors = new ErrorResponse();
		try {
			Interval createdInterval = created.getValue();
			List<Criterion> filterParams = new ArrayList<Criterion>();
			filterParams.add(Restrictions.eq("parent", facility));
			//If any "type" parameters are provided, filter accordingly
			List<WorkerEvent.EventType> typeList = Lists.newArrayList();
			for (EventTypeParam type : typeParamList) {
				if (type != null) {
					typeList.add(type.getValue());
				}
			}
			if (!typeList.isEmpty()) {
				filterParams.add(Restrictions.in("eventType", typeList)); // empty .in() guard present
			}

			if (!Strings.isNullOrEmpty(itemId)) {
				filterParams.add(Restrictions.eq("itemId", itemId));
			}
			if (!Strings.isNullOrEmpty(location)) {
				filterParams.add(Restrictions.eq("location", location));
			}
			if (!Strings.isNullOrEmpty(workerId)) {
				filterParams.add(Restrictions.eq("workerId", workerId));
			}

			if (created != null) {
				filterParams.add(GenericDaoABC.createIntervalRestriction("created", createdInterval));
			}

			if (!Strings.isNullOrEmpty(itemId)) {
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				List<BeanMap> eventBeans = new ArrayList<>();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					eventBeans.add(new BeanMap(eventDisplay));
				}
				ResultDisplay<BeanMap> result = new ResultDisplay<>(eventBeans);
				return BaseResponse.buildResponse(result);
			} else if (!Strings.isNullOrEmpty(workerId)) {
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				List<BeanMap> eventBeans = new ArrayList<>();
				for (WorkerEvent event : events) {
					EventDisplay eventDisplay = EventDisplay.createEventDisplay(event);
					eventBeans.add(new BeanMap(eventDisplay));
				}
				ResultDisplay<BeanMap> result = new ResultDisplay<>(eventBeans);
				return BaseResponse.buildResponse(result);
			}
			if ("item".equals(groupBy)) {
				List<ItemEventTypeGroup> groupedEvents = notificationService.groupWorkerEventsByTypeAndItem(filterParams);
				TreeSet<Map<Object, Object>> issues = new TreeSet<>(ItemDisplay.ItemComparator);
				for (ItemEventTypeGroup groupedEvent : groupedEvents) {
					ItemDisplay display = new ItemDisplay(groupedEvent);
					Map<Object, Object> values = new HashMap<>();
					values.putAll(new BeanMap(display));
					values.put("count", groupedEvent.getCount());
					issues.add(values);
				}
				ResultDisplay<Map<Object, Object>> result = new ResultDisplay<>(issues);
				return BaseResponse.buildResponse(result);
			} else if ("worker".equals(groupBy)) {

				List<WorkerEventTypeGroup> groupedEvents = notificationService.groupWorkerEventsByTypeAndWorker(filterParams);
				TreeSet<Map<Object, Object>> issues = new TreeSet<>(WorkerDisplay.ItemComparator);
				for (WorkerEventTypeGroup groupedEvent : groupedEvents) {
					Worker worker = Worker.findWorker(facility, groupedEvent.getWorkerId());
					WorkerDisplay display = new WorkerDisplay(worker);
					Map<Object, Object> values = new HashMap<>();
					values.putAll(new BeanMap(display));
					values.put("count", groupedEvent.getCount());
					issues.add(values);

				}
				ResultDisplay<Map<Object, Object>> result = new ResultDisplay<>(issues);
				return BaseResponse.buildResponse(result);
			} else if ("type".equals(groupBy)) {

				List<WorkerEventTypeGroup> issuesByType = notificationService.groupWorkerEventsByType(filterParams);
				ResultDisplay<WorkerEventTypeGroup> result = new ResultDisplay<WorkerEventTypeGroup>(issuesByType);
				return BaseResponse.buildResponse(result);
			} else {
				List<WorkerEvent> events = WorkerEvent.staticGetDao().findByFilter(filterParams);
				List<BeanMap> eventBeans = new ArrayList<>();
				for (WorkerEvent event : events) {
					eventBeans.add(new BeanMap(EventDisplay.createEventDisplay(event)));
				}
				ResultDisplay<BeanMap> result = new ResultDisplay<>(eventBeans);

				return BaseResponse.buildResponse(result);
			}

		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		}
		finally {
			LOGGER.trace("searchEvents total: " +  total.stop().toString());
		}
	}

	@GET
	@Path("pickrate")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pickRate(@QueryParam("purpose") Set<String> purposes, @QueryParam("createdInterval") IntervalParam createdInterval) {
		ErrorResponse errors = new ErrorResponse();
		try {
			List<PickRate> pickRates = notificationService.getPickRate(purposes, createdInterval.getValue());
			return BaseResponse.buildResponse(pickRates);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@GET
	@Path("pickrate/search")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pickRatePurposes() {
		ErrorResponse errors = new ErrorResponse();
		try {
			List<String> purposes = notificationService.getDistinct(facility, "purpose");
			List<Option> options = new ArrayList<>();
			for (String purpose : purposes) {
				if (purpose == null) {
					continue;
				}
				String displayName = WiPurpose.valueOf(purpose).getDisplayName();
				options.add(new Option(purpose, displayName));
			}
			return BaseResponse.buildResponse(ImmutableMap.of("purpose", options));
		} catch (Exception e) {
			return errors.processException(e);
		}
	}

	@GET
	@Path("picks/histogram")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response pickHistogram(@Context UriInfo uriInfo) throws Exception {
		ErrorResponse errors = new ErrorResponse();
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			HistogramParams params = new HistogramParams(queryParams);  
			HistogramResult result = notificationService.pickRateHistogram(params, facility);
			return BaseResponse.buildResponse(result);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}
	
	@GET
	@Path("picks/workers/histogram")
	@RequiresPermissions("event:view")
	@Produces(MediaType.APPLICATION_JSON)
	public Response workersPickHistogram(@Context UriInfo uriInfo) throws Exception {
		ErrorResponse errors = new ErrorResponse();
		try {
			MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
			HistogramParams params = new HistogramParams(queryParams);   
			List<?> result = notificationService.workersPickHistogram(params, facility);
			return BaseResponse.buildResponse(result);
		} catch (Exception e) {
			return errors.processException(e);
		}
	}
	
	
	@POST
	@Path("/process_script")
	@RequiresPermissions("che:simulate")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response processScript(FormDataMultiPart body) {
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
			ScriptStep firstStep = ScriptParser.parseScript(script);
			firstStep.setReport("Script imported");
			//return BaseResponse.buildResponse(new ScriptApiResponse(firstStep, "Script imported"));
			return BaseResponse.buildResponse(firstStep);
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
		FormDataMultiPart body) {
		TenantPersistenceService persistence = TenantPersistenceService.getInstance();
		try {
			ErrorResponse errors = new ErrorResponse();

			//Verify that this Script Server Runner was called with an active transaction, and close it.
			//The Server and Site script runners below will manage transactions themselves
			if (!persistence.hasAnyActiveTransactions()) {
				errors.addError("Server Error: FacilityResponse.runScript() called without an active transaction");
				return errors.buildResponse();
			}
			UUID facilityId = facility.getPersistentId();
			persistence.commitTransaction();

			if (!BaseResponse.isUUIDValid(scriptStepId, "script_step_id", errors)) {
				return errors.buildResponse();
			}

			ScriptStep scriptStep = ScriptParser.getScriptStep(scriptStepId.getValue());
			if (scriptStep == null) {
				errors.addError("Script step " + scriptStepId.getValue() + " doesn't exist");
				return errors.buildResponse();
			}
			//Split the script into a list of SERVER and SITE parts
			ArrayList<StepPart> scriptParts = scriptStep.parts();
			StringBuilder report = new StringBuilder("Running script step " + scriptStep.getComment() + "\n");

			ScriptServerRunner serverScriptRunner = new ScriptServerRunner(facilityId,
				body,
				uiUpdateService,
				aislesImporterProvider,
				locationsImporterProvider,
				inventoryImporterProvider,
				orderImporterProvider,
				workerImporterProvider);
			String error = null;
			//Process script parts
			while (!scriptParts.isEmpty()) {
				StepPart part = scriptParts.remove(0);
				ScriptMessage responseMessage = null;
				if (part.isServer()) {
					//SERVER
					responseMessage = serverScriptRunner.processServerScript(part.getScriptLines());
				} else {
					//SITE
					responseMessage = runSiteScript(part, timeoutMin);
				}
				report.append(responseMessage.getResponse());
				if (!responseMessage.isSuccess()) {
					error = responseMessage.getError();
					break;
				}
			}

			if (error != null) {
				ScriptStep nextStepError = new ScriptStep(report.toString());
				nextStepError.addError(error);
				return BaseResponse.buildResponse(nextStepError, Status.BAD_REQUEST);
			}

			ScriptStep nextStep = scriptStep.nextStep();
			if (nextStep == null) {
				return BaseResponse.buildResponse(new ScriptStep(report.toString()));
			} else {
				nextStep.setReport(report.toString());
				return BaseResponse.buildResponse(nextStep);
			}
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		} finally {
			//Open a new transaction for TransactionFilter to close later
			if (!persistence.hasAnyActiveTransactions()) {
				persistence.beginTransaction();
			}
		}
	}

	private ScriptMessage runSiteScript(StepPart part, int timeoutMin) throws Exception {
		ScriptMessage errorMesage = new ScriptMessage();
		TenantPersistenceService persistence = TenantPersistenceService.getInstance();
		
		persistence.beginTransaction();
		try {
			
			facility = Facility.staticGetDao().reload(facility);
			String networkId = part.getNetworkId();
			if (networkId == null) {
				networkId = CodeshelfNetwork.DEFAULT_NETWORK_NAME;
			}
			CodeshelfNetwork network = facility.getNetwork(networkId);
			if (network == null) {
				errorMesage.setMessageError("Could not find network " + networkId + " in facility " + facility.getDomainId());
				return errorMesage;
			}
			
			SiteController siteController = network.getPrimarySiteController();
			if (siteController == null) {
				errorMesage.setMessageError("Network " + networkId + " does not have a Primary site controller");
				return errorMesage;
			}
			
			WebSocketManagerService sessionManager = WebSocketManagerService.getInstance();
	    	Set<String> connectedUsernames = sessionManager.getConnectedUsernames();	    	
	    	if (!connectedUsernames.contains(siteController.getDomainId())){
	    		errorMesage.setMessageError("Site controller " + siteController.getDomainId() + " is not connected");
	    		return errorMesage;
	    	}

			//Execute script
			UUID id = UUID.randomUUID();
			ScriptMessage scriptMessage = new ScriptMessage(id, part.getScriptLines());
			Set<User> users = new HashSet<>();
			users.add(siteController.getUser());
			webSocketManagerService.sendMessage(users, scriptMessage);
			persistence.commitTransaction();
			ScriptMessage siteResponseMessage = ScriptSiteCallPool.waitForSiteResponse(id, timeoutMin);
			if (siteResponseMessage == null) {
				errorMesage.setMessageError("Site request timed out");
				return errorMesage;
			}
			return siteResponseMessage;
		} catch (Exception e) {
			persistence.rollbackTransaction();
			errorMesage.setMessageError("Site request failed: " + e.getMessage());
			return errorMesage;
		}
	}

	@GET
	@Path("/metrics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetrics(){
		try {
			List<FacilityMetric> facilityMetrics = FacilityMetric.staticGetDao().findByParent(facility);
			return BaseResponse.buildResponse(facilityMetrics);
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	@POST
	@Path("/metrics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response computeMetrics(@FormParam("date") TimestampParam dateParam, 
								   @FormParam("forceRecalculate") @DefaultValue(value = "false") boolean forceRecalculate){
		try {
			String dateStr = null;
			if (dateParam != null) {
				Date date = dateParam.getValue();
				if (date == null){
					throw new Exception("Unparseable date: \"" + dateParam.getRawValue() + "\"");
				}
				if (date.after(new Date())){
					throw new Exception("Provice a past date");
				}
				DateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd"); 
				dateStr = outFormat.format(date);
			}
			FacilityMetric metric = facility.computeMetrics(dateStr, forceRecalculate);
			return BaseResponse.buildResponse(metric);
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}
	}

	/*
	 * count:6
		location:
		uom:EA
		description:
		gtin:
		c	lass:com.codeshelf.api.responses.ItemDisplay
		itemId:6135710
		type:SHORT*/
	@POST
	@Path("replenish")
	@RequiresPermissions("event:edit")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createReplenishOrderForEvent(@FormParam("type") EventTypeParam typeParam, 
												 @FormParam("itemId") String itemId,	
												 @FormParam("gtin") String gtin,
												 @FormParam("uom") String uom,
												 @FormParam("location") String location) {
		try {
			EventType type = typeParam.getValue();
			if (type != EventType.SHORT && type != EventType.SHORT_AHEAD && type != EventType.LOW && type != EventType.SUBSTITUTION){
				throw new Exception(type + " event is illegal for replenishing. Call on SHORT, LOW or SUBSTITUTION events");

			
			}
			
			ReplenishItem item = new ReplenishItem();
			item.setGtin(gtin);
			item.setItemId(itemId);
			item.setLocation(location);
			item.setUom(uom);
			item.setLocation(location);
			String scannableId = orderImporterProvider.get().createReplenishOrderForItem(this.facility, item);
			return BaseResponse.buildResponse(ImmutableMap.of("scannableId", scannableId));
		} catch (Exception e) {
			return new ErrorResponse().processException(e);
		}		
	}

}
